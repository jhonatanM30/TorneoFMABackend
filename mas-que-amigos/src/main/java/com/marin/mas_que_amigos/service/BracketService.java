package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.BracketDTO;
import com.marin.mas_que_amigos.dto.BracketRequestDTO;
import com.marin.mas_que_amigos.dto.BracketRoundDTO;
import com.marin.mas_que_amigos.dto.BracketTieDTO;
import com.marin.mas_que_amigos.dto.PairingRequestDTO;
import com.marin.mas_que_amigos.dto.TeamRefDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.NotFoundException;
import com.marin.mas_que_amigos.model.BracketTie;
import com.marin.mas_que_amigos.model.BracketTieKind;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.model.Tournament;
import com.marin.mas_que_amigos.model.TournamentFormat;
import com.marin.mas_que_amigos.model.TournamentStatus;
import com.marin.mas_que_amigos.repository.BracketTieRepository;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import com.marin.mas_que_amigos.repository.TournamentRepository;
import com.marin.mas_que_amigos.repository.TournamentTeamRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Knockout brackets: generation (draw, manual pairings, playoffs before the
 * bracket, qualifiers of a group stage), the consultation of the bracket and,
 * as the tasks of the change progress, the advancement of the rounds.
 */
@Service
public class BracketService {

    private static final int DEFAULT_ROUND_INTERVAL_DAYS = 7;

    private final TournamentRepository tournamentRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final PartidoRepository partidoRepository;
    private final BracketTieRepository bracketTieRepository;
    private final GroupStageService groupStageService;
    private final MatchDateConflicts matchDateConflicts;
    private final Random random;

    private final BracketBuilder builder = new BracketBuilder();

    @Autowired
    public BracketService(TournamentRepository tournamentRepository, TournamentTeamRepository tournamentTeamRepository,
            PartidoRepository partidoRepository, BracketTieRepository bracketTieRepository,
            GroupStageService groupStageService, MatchDateConflicts matchDateConflicts) {
        this(tournamentRepository, tournamentTeamRepository, partidoRepository, bracketTieRepository,
                groupStageService, matchDateConflicts, new Random());
    }

    // The Random is injectable so the draw can be repeated in tests.
    BracketService(TournamentRepository tournamentRepository, TournamentTeamRepository tournamentTeamRepository,
            PartidoRepository partidoRepository, BracketTieRepository bracketTieRepository,
            GroupStageService groupStageService, MatchDateConflicts matchDateConflicts, Random random) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentTeamRepository = tournamentTeamRepository;
        this.partidoRepository = partidoRepository;
        this.bracketTieRepository = bracketTieRepository;
        this.groupStageService = groupStageService;
        this.matchDateConflicts = matchDateConflicts;
        this.random = random;
    }

    // ------------------------------------------------------------------ generation

    /**
     * Generates the bracket of a KNOCKOUT or GROUPS_KNOCKOUT tournament that is
     * IN_PROGRESS and has no bracket yet. The first round matches are created
     * now; the rest are created as the winners advance.
     */
    @Transactional
    public BracketDTO generateBracket(Long tournamentId, BracketRequestDTO request) {
        Tournament tournament = tournamentRepository.findByIdForUpdate(tournamentId)
                .orElseThrow(() -> tournamentNotFound(tournamentId));
        if (tournament.getFormat() != TournamentFormat.KNOCKOUT && tournament.getFormat() != TournamentFormat.GROUPS_KNOCKOUT) {
            throw new BusinessException("Este torneo no tiene llaves de eliminatoria: solo los torneos KNOCKOUT y GROUPS_KNOCKOUT las tienen.");
        }
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new BusinessException("El torneo no está en curso: las llaves se generan en un torneo en curso (estado actual: "
                    + tournament.getStatus() + ").");
        }
        if (bracketTieRepository.existsByTournamentId(tournamentId)) {
            throw new BusinessException("Las llaves de este torneo ya fueron generadas.");
        }

        List<Equipo> enrolled = tournamentTeamRepository.findTeamsByTournamentId(tournamentId);
        List<BracketBuilder.Pairing> pairings = tournament.getFormat() == TournamentFormat.KNOCKOUT
                ? knockoutPairings(tournamentId, enrolled, request)
                : groupStagePairings(tournamentId, request);

        Optional<LocalDate> lastMatchDate = partidoRepository.findMaxFechaByTorneoId(tournamentId);
        if (lastMatchDate.isPresent() && !request.getStartDate().isAfter(lastMatchDate.get())) {
            throw new BusinessException("La fecha de inicio de las llaves debe ser posterior a la del último partido del torneo ("
                    + lastMatchDate.get() + ").");
        }

        boolean thirdPlace = request.getThirdPlaceMatch() == null || request.getThirdPlaceMatch();
        BracketBuilder.Plan plan = builder.build(pairings, thirdPlace);
        int intervalDays = request.getIntervalDays() == null ? DEFAULT_ROUND_INTERVAL_DAYS : request.getIntervalDays();

        Map<Long, Equipo> teamsById = new HashMap<>();
        for (Equipo team : enrolled) {
            teamsById.put(team.getId(), team);
        }

        // Every first round team must be free that date before anything is created.
        for (BracketBuilder.Node node : plan.getRounds().get(0)) {
            matchDateConflicts.requireFree(request.getStartDate(), teamsById.get(node.getHomeTeamId()));
            matchDateConflicts.requireFree(request.getStartDate(), teamsById.get(node.getAwayTeamId()));
        }

        persist(tournament, plan, teamsById, request, intervalDays);
        return toDTO(bracketTieRepository.findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(tournamentId));
    }

    private void persist(Tournament tournament, BracketBuilder.Plan plan, Map<Long, Equipo> teamsById,
            BracketRequestDTO request, int intervalDays) {
        Map<BracketBuilder.Node, BracketTie> tieOf = new IdentityHashMap<>();
        List<BracketBuilder.Node> nodes = new ArrayList<>();
        plan.getRounds().forEach(nodes::addAll);
        if (plan.getThirdPlace() != null) {
            nodes.add(plan.getThirdPlace());
        }

        // First pass: the ties themselves; second pass: the links between them.
        for (BracketBuilder.Node node : nodes) {
            BracketTie tie = new BracketTie();
            tie.setTournament(tournament);
            tie.setKind(node.getKind());
            tie.setRoundNumber(node.getRound());
            tie.setPositionInRound(node.getPosition());
            tie.setHomeTeam(node.getHomeTeamId() == null ? null : teamsById.get(node.getHomeTeamId()));
            tie.setAwayTeam(node.getAwayTeamId() == null ? null : teamsById.get(node.getAwayTeamId()));
            tie.setScheduledDate(request.getStartDate().plusDays((long) (node.getRound() - 1) * intervalDays));
            tie.setScheduledTime(request.getTime());
            tieOf.put(node, bracketTieRepository.save(tie));
        }
        for (BracketBuilder.Node node : nodes) {
            BracketTie tie = tieOf.get(node);
            if (node.getWinnerNext() != null) {
                tie.setWinnerNext(tieOf.get(node.getWinnerNext()));
                tie.setWinnerNextIsHome(node.isWinnerNextIsHome());
            }
            if (node.getLoserNext() != null) {
                tie.setLoserNext(tieOf.get(node.getLoserNext()));
                tie.setLoserNextIsHome(node.isLoserNextIsHome());
            }
            bracketTieRepository.save(tie);
        }
        for (BracketBuilder.Node node : plan.getRounds().get(0)) {
            createMatch(tieOf.get(node));
        }
    }

    private List<BracketBuilder.Pairing> knockoutPairings(Long tournamentId, List<Equipo> enrolled, BracketRequestDTO request) {
        List<Partido> playoffs = partidoRepository.findByTorneoIdAndFase(tournamentId, Partido.Fase.REPECHAJE);
        long pending = playoffs.stream().filter(p -> p.getEstado() != Partido.Estado.FINALIZADO).count();
        if (pending > 0) {
            throw new BusinessException("Hay " + pending + " partido(s) de repechaje sin finalizar: finalízalos antes de generar las llaves.");
        }
        Set<Long> eliminated = new HashSet<>();
        for (Partido playoff : playoffs) {
            MatchWinner.of(playoff).ifPresent(result -> eliminated.add(result.getLoser().getId()));
        }
        List<Long> participants = enrolled.stream()
                .map(Equipo::getId)
                .filter(id -> !eliminated.contains(id))
                .collect(Collectors.toList());

        int count = participants.size();
        if (count < 2 || Integer.bitCount(count) != 1) {
            throw new BusinessException("Los equipos que siguen en competencia (" + count + ") deben ser una potencia de 2 "
                    + "(2, 4, 8...) para formar las llaves: arma un partido de repechaje entre dos equipos para eliminar a uno.");
        }

        List<BracketBuilder.Pairing> pairings = new ArrayList<>();
        if (request.getPairings() != null && !request.getPairings().isEmpty()) {
            return manualPairings(request.getPairings(), participants);
        }
        List<Long> shuffled = new ArrayList<>(participants);
        Collections.shuffle(shuffled, random);
        for (int i = 0; i < shuffled.size(); i += 2) {
            pairings.add(new BracketBuilder.Pairing(shuffled.get(i), shuffled.get(i + 1)));
        }
        return pairings;
    }

    private List<BracketBuilder.Pairing> manualPairings(List<PairingRequestDTO> requested, List<Long> participants) {
        if (requested.size() != participants.size() / 2) {
            throw new BusinessException("Los cruces indicados deben cubrir a los " + participants.size()
                    + " equipos que participan (" + participants.size() / 2 + " cruces).");
        }
        Set<Long> allowed = new HashSet<>(participants);
        Set<Long> seen = new HashSet<>();
        List<BracketBuilder.Pairing> pairings = new ArrayList<>();
        for (PairingRequestDTO pairing : requested) {
            for (Long teamId : new Long[]{pairing.getHomeTeamId(), pairing.getAwayTeamId()}) {
                if (!allowed.contains(teamId)) {
                    throw new BusinessException("El equipo con Id " + teamId + " no participa en las llaves de este torneo.");
                }
                if (!seen.add(teamId)) {
                    throw new BusinessException("La lista de cruces repite al equipo con Id " + teamId + ".");
                }
            }
            pairings.add(new BracketBuilder.Pairing(pairing.getHomeTeamId(), pairing.getAwayTeamId()));
        }
        return pairings;
    }

    private List<BracketBuilder.Pairing> groupStagePairings(Long tournamentId, BracketRequestDTO request) {
        if (request.getPairings() != null && !request.getPairings().isEmpty()) {
            throw new BusinessException("Los cruces de la eliminatoria de un torneo con grupos los define la clasificación de los grupos: no se pueden indicar a mano.");
        }
        if (!groupStageService.hasGroups(tournamentId)) {
            throw new BusinessException("Este torneo todavía no tiene grupos: forma los grupos antes de generar las llaves.");
        }
        long pending = groupStageService.pendingGroupMatches(tournamentId);
        if (pending > 0) {
            throw new BusinessException("Faltan " + pending + " partido(s) de la fase de grupos por finalizar.");
        }
        return builder.groupPairings(groupStageService.qualifiers(tournamentId));
    }

    /**
     * Creates the match of a tie whose two teams are known. Matches created by
     * the advancement are not checked against date conflicts, so finishing a
     * match never fails because of a match of another tournament; the date can
     * be edited afterwards.
     */
    private void createMatch(BracketTie tie) {
        Partido match = new Partido();
        match.setEquipoLocal(tie.getHomeTeam());
        match.setEquipoVisitante(tie.getAwayTeam());
        match.setFecha(tie.getScheduledDate());
        match.setHora(tie.getScheduledTime());
        match.setFase(tie.getKind() == BracketTieKind.MAIN && tie.getWinnerNext() == null
                ? Partido.Fase.FINAL : Partido.Fase.ELIMINACION_DIRECTA);
        match.setEstado(Partido.Estado.PROGRAMADO);
        match.setGolesLocal(0);
        match.setGolesVisitante(0);
        match.setTorneo(tie.getTournament());
        tie.setMatch(partidoRepository.save(match));
        bracketTieRepository.save(tie);
    }

    // ------------------------------------------------------------------ consultation

    @Transactional(readOnly = true)
    public BracketDTO getBracket(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw tournamentNotFound(tournamentId);
        }
        return toDTO(bracketTieRepository.findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(tournamentId));
    }

    private BracketDTO toDTO(List<BracketTie> ties) {
        BracketDTO bracket = new BracketDTO();
        Map<Integer, List<BracketTieDTO>> byRound = new LinkedHashMap<>();
        BracketTie finalTie = null;
        BracketTie thirdPlace = null;
        for (BracketTie tie : ties) {
            if (tie.getKind() == BracketTieKind.THIRD_PLACE) {
                thirdPlace = tie;
                continue;
            }
            byRound.computeIfAbsent(tie.getRoundNumber(), round -> new ArrayList<>()).add(toDTO(tie));
            if (tie.getWinnerNext() == null) {
                finalTie = tie;
            }
        }

        int roundCount = byRound.size();
        List<BracketRoundDTO> rounds = new ArrayList<>();
        for (Map.Entry<Integer, List<BracketTieDTO>> entry : byRound.entrySet()) {
            BracketRoundDTO round = new BracketRoundDTO();
            round.setRound(entry.getKey());
            round.setName(roundName(entry.getKey(), roundCount));
            round.setTies(entry.getValue());
            rounds.add(round);
        }
        bracket.setRounds(rounds);
        bracket.setThirdPlace(thirdPlace == null ? null : toDTO(thirdPlace));
        if (finalTie != null && finalTie.getWinnerTeam() != null) {
            bracket.setChampion(teamRef(finalTie.getWinnerTeam()));
        }
        return bracket;
    }

    private BracketTieDTO toDTO(BracketTie tie) {
        BracketTieDTO dto = new BracketTieDTO();
        dto.setId(tie.getId());
        dto.setPosition(tie.getPositionInRound());
        dto.setHomeTeam(teamRef(tie.getHomeTeam()));
        dto.setAwayTeam(teamRef(tie.getAwayTeam()));
        dto.setWinnerTeamId(tie.getWinnerTeam() == null ? null : tie.getWinnerTeam().getId());
        // The date and time of the tie until its match exists; then those of the match,
        // which the administrator may have edited.
        dto.setDate(tie.getScheduledDate());
        dto.setTime(tie.getScheduledTime());
        Partido match = tie.getMatch();
        if (match != null) {
            dto.setDate(match.getFecha());
            dto.setTime(match.getHora());
            dto.setMatchId(match.getId());
            dto.setMatchStatus(match.getEstado().name());
            dto.setHomeGoals(match.getGolesLocal());
            dto.setAwayGoals(match.getGolesVisitante());
            dto.setHomePenalties(match.getPenalesLocal());
            dto.setAwayPenalties(match.getPenalesVisitante());
        }
        return dto;
    }

    private TeamRefDTO teamRef(Equipo team) {
        return team == null ? null : new TeamRefDTO(team.getId(), team.getNombre());
    }

    // Names counted from the final: final, semifinales, cuartos, octavos.
    private String roundName(int round, int roundCount) {
        switch (roundCount - round) {
            case 0:
                return "Final";
            case 1:
                return "Semifinales";
            case 2:
                return "Cuartos de final";
            case 3:
                return "Octavos de final";
            default:
                return "Ronda " + round;
        }
    }

    private NotFoundException tournamentNotFound(Long id) {
        return new NotFoundException("Fuera de juego! El torneo con Id " + id + " no existe.");
    }

    // ------------------------------------------------------------------ matches that need a winner

    /**
     * A match must have a winner when it belongs to a bracket or is a playoff
     * ("REPECHAJE") of a KNOCKOUT tournament.
     */
    public boolean requiresWinner(Partido match) {
        if (match.getId() != null && bracketTieRepository.existsByMatchId(match.getId())) {
            return true;
        }
        return match.getFase() == Partido.Fase.REPECHAJE
                && match.getTorneo() != null
                && match.getTorneo().getFormat() == TournamentFormat.KNOCKOUT;
    }

    /**
     * Rejects finishing a match that needs a winner while it is still tied (equal
     * score and no distinct penalties).
     */
    public void requireDecided(Partido match) {
        if (requiresWinner(match) && !MatchWinner.of(match).isPresent()) {
            throw new BusinessException("Este partido no puede terminar empatado: registra los penales antes de finalizarlo.");
        }
    }

    /**
     * Moves the winner (and, from the semifinals, the loser) of a finished
     * bracket match into their next ties, and creates the matches of the ties
     * whose two teams are now known. Does nothing for a match outside a bracket.
     */
    @Transactional
    public void onMatchFinished(Partido match) {
        Optional<BracketTie> found = bracketTieRepository.findByMatchId(match.getId());
        if (!found.isPresent()) {
            return;
        }
        BracketTie tie = found.get();
        MatchWinner.Result result = MatchWinner.of(match).orElseThrow(
                () -> new IllegalStateException("A bracket match was finished without a winner: " + match.getId()));

        tie.setWinnerTeam(result.getWinner());
        bracketTieRepository.save(tie);
        if (tie.getWinnerNext() != null) {
            place(tie.getWinnerNext(), Boolean.TRUE.equals(tie.getWinnerNextIsHome()), result.getWinner());
        }
        if (tie.getLoserNext() != null) {
            place(tie.getLoserNext(), Boolean.TRUE.equals(tie.getLoserNextIsHome()), result.getLoser());
        }
    }

    private void place(BracketTie next, boolean asHome, Equipo team) {
        if (asHome) {
            next.setHomeTeam(team);
        } else {
            next.setAwayTeam(team);
        }
        bracketTieRepository.save(next);
        if (next.getHomeTeam() != null && next.getAwayTeam() != null && next.getMatch() == null) {
            createMatch(next);
        }
    }

    /** The matches of a bracket cannot be deleted or change their teams: other ties depend on them. */
    public boolean isBracketMatch(Long matchId) {
        return matchId != null && bracketTieRepository.existsByMatchId(matchId);
    }

    /**
     * A KNOCKOUT or GROUPS_KNOCKOUT tournament can only be finished once its
     * final is finished: between rounds there may be no pending match at all,
     * because the matches of the next rounds do not exist yet.
     */
    public void requireChampion(Tournament tournament) {
        if (tournament.getFormat() != TournamentFormat.KNOCKOUT && tournament.getFormat() != TournamentFormat.GROUPS_KNOCKOUT) {
            return;
        }
        List<BracketTie> ties = bracketTieRepository.findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(tournament.getId());
        boolean hasChampion = ties.stream()
                .anyMatch(t -> t.getKind() == BracketTieKind.MAIN && t.getWinnerNext() == null && t.getWinnerTeam() != null);
        if (!hasChampion) {
            throw new BusinessException("Falta definir al campeón: genera las llaves y finaliza la final antes de finalizar el torneo.");
        }
    }
}
