package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.GroupCompositionDTO;
import com.marin.mas_que_amigos.dto.GroupDTO;
import com.marin.mas_que_amigos.dto.GroupsRequestDTO;
import com.marin.mas_que_amigos.dto.StandingDTO;
import com.marin.mas_que_amigos.dto.TeamRefDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.NotFoundException;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.model.Tournament;
import com.marin.mas_que_amigos.model.TournamentFormat;
import com.marin.mas_que_amigos.model.TournamentGroup;
import com.marin.mas_que_amigos.model.TournamentStatus;
import com.marin.mas_que_amigos.model.TournamentTeam;
import com.marin.mas_que_amigos.repository.EstadisticaRepository;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import com.marin.mas_que_amigos.repository.TournamentGroupRepository;
import com.marin.mas_que_amigos.repository.TournamentRepository;
import com.marin.mas_que_amigos.repository.TournamentTeamRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
 * Group stage of a GROUPS_KNOCKOUT tournament: forming the groups with their
 * calendars, the table of each group (with the league rules) and the teams that
 * qualify for the knockout stage.
 */
@Service
public class GroupStageService {

    private static final int DEFAULT_ROUND_INTERVAL_DAYS = 7;
    private static final int MINIMUM_TEAMS_PER_GROUP = 2;

    private final TournamentRepository tournamentRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final TournamentGroupRepository tournamentGroupRepository;
    private final PartidoRepository partidoRepository;
    private final EstadisticaRepository estadisticaRepository;
    private final MatchDateConflicts matchDateConflicts;
    private final GroupDraw groupDraw;

    private final LeagueStandingsCalculator standingsCalculator = new LeagueStandingsCalculator();
    private final LeagueScheduleGenerator scheduleGenerator = new LeagueScheduleGenerator();

    @Autowired
    public GroupStageService(TournamentRepository tournamentRepository, TournamentTeamRepository tournamentTeamRepository,
            TournamentGroupRepository tournamentGroupRepository, PartidoRepository partidoRepository,
            EstadisticaRepository estadisticaRepository, MatchDateConflicts matchDateConflicts) {
        this(tournamentRepository, tournamentTeamRepository, tournamentGroupRepository, partidoRepository,
                estadisticaRepository, matchDateConflicts, new Random());
    }

    // The Random is injectable so the draw can be repeated in tests.
    GroupStageService(TournamentRepository tournamentRepository, TournamentTeamRepository tournamentTeamRepository,
            TournamentGroupRepository tournamentGroupRepository, PartidoRepository partidoRepository,
            EstadisticaRepository estadisticaRepository, MatchDateConflicts matchDateConflicts, Random random) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentTeamRepository = tournamentTeamRepository;
        this.tournamentGroupRepository = tournamentGroupRepository;
        this.partidoRepository = partidoRepository;
        this.estadisticaRepository = estadisticaRepository;
        this.matchDateConflicts = matchDateConflicts;
        this.groupDraw = new GroupDraw(random);
    }

    // ------------------------------------------------------------------ forming the groups

    /**
     * Forms the groups of a GROUPS_KNOCKOUT tournament that is IN_PROGRESS and has
     * no groups or matches yet, and creates the single round-robin calendar of
     * every group. All or nothing: a team that already plays on a round date in
     * any tournament aborts the whole operation.
     */
    @Transactional
    public List<GroupDTO> createGroups(Long tournamentId, GroupsRequestDTO request) {
        Tournament tournament = tournamentRepository.findByIdForUpdate(tournamentId)
                .orElseThrow(() -> tournamentNotFound(tournamentId));
        if (tournament.getFormat() != TournamentFormat.GROUPS_KNOCKOUT) {
            throw new BusinessException("Este torneo no tiene fase de grupos: solo los torneos GROUPS_KNOCKOUT la tienen.");
        }
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new BusinessException("El torneo no está en curso: los grupos se forman en un torneo en curso (estado actual: "
                    + tournament.getStatus() + ").");
        }
        if (tournamentGroupRepository.existsByTournamentId(tournamentId)) {
            throw new BusinessException("Los grupos de este torneo ya fueron formados.");
        }
        if (partidoRepository.countByTorneoId(tournamentId) > 0) {
            throw new BusinessException("El torneo ya tiene partidos: los grupos se forman en un torneo sin partidos.");
        }

        List<Equipo> teams = tournamentTeamRepository.findTeamsByTournamentId(tournamentId);
        int groupCount = request.getGroupCount();
        int qualifiers = request.getQualifiersPerGroup();
        if (groupCount < 2 || Integer.bitCount(groupCount) != 1) {
            throw new BusinessException("La cantidad de grupos debe ser una potencia de 2 (2, 4, 8...).");
        }
        if (teams.size() < MINIMUM_TEAMS_PER_GROUP * groupCount) {
            throw new BusinessException("Cada grupo necesita al menos " + MINIMUM_TEAMS_PER_GROUP + " equipos: con " + teams.size()
                    + " equipos inscritos no se pueden formar " + groupCount + " grupos.");
        }
        if (qualifiers != 1 && qualifiers != 2) {
            throw new BusinessException("Los clasificados por grupo deben ser 1 o 2.");
        }

        Map<Long, Equipo> teamsById = new HashMap<>();
        for (Equipo team : teams) {
            teamsById.put(team.getId(), team);
        }
        List<List<Long>> composition = composition(request, teams, teamsById, groupCount);

        int smallest = composition.stream().mapToInt(List::size).min().getAsInt();
        if (smallest < MINIMUM_TEAMS_PER_GROUP) {
            throw new BusinessException("Cada grupo necesita al menos " + MINIMUM_TEAMS_PER_GROUP + " equipos.");
        }
        if (qualifiers >= smallest) {
            throw new BusinessException("Los clasificados por grupo (" + qualifiers
                    + ") deben ser menos que los equipos del grupo más pequeño (" + smallest + ").");
        }

        int intervalDays = request.getIntervalDays() == null ? DEFAULT_ROUND_INTERVAL_DAYS : request.getIntervalDays();
        List<Partido> matches = buildCalendar(tournament, composition, teamsById, request, intervalDays);

        List<TournamentGroup> groups = new ArrayList<>();
        for (int index = 0; index < groupCount; index++) {
            TournamentGroup group = new TournamentGroup();
            group.setTournament(tournament);
            group.setName(GroupDraw.groupName(index));
            groups.add(tournamentGroupRepository.save(group));
        }
        Map<Long, TournamentGroup> groupOfTeam = new HashMap<>();
        for (int index = 0; index < groupCount; index++) {
            for (Long teamId : composition.get(index)) {
                groupOfTeam.put(teamId, groups.get(index));
            }
        }
        List<TournamentTeam> enrollments = tournamentTeamRepository.findByTournamentId(tournamentId);
        for (TournamentTeam enrollment : enrollments) {
            enrollment.setGroup(groupOfTeam.get(enrollment.getTeam().getId()));
        }
        tournamentTeamRepository.saveAll(enrollments);

        tournament.setQualifiersPerGroup(qualifiers);
        tournamentRepository.save(tournament);
        partidoRepository.saveAll(matches);

        return toDTOs(tournament, groups, enrollments, new ArrayList<>(), new HashMap<>());
    }

    private List<List<Long>> composition(GroupsRequestDTO request, List<Equipo> teams, Map<Long, Equipo> teamsById, int groupCount) {
        List<Long> teamIds = teams.stream().map(Equipo::getId).collect(Collectors.toList());
        if (request.getGroups() == null || request.getGroups().isEmpty()) {
            return groupDraw.draw(teamIds, groupCount);
        }
        List<List<Long>> manual = new ArrayList<>();
        for (GroupCompositionDTO group : request.getGroups()) {
            manual.add(group.getTeamIds());
        }
        Optional<String> error = GroupDraw.validateManual(manual, teamsById.keySet(), groupCount);
        if (error.isPresent()) {
            throw new BusinessException(error.get());
        }
        return manual;
    }

    /**
     * The matches of every group's round-robin: round {@code k} of all the groups
     * falls on the same date. Every team is checked against date conflicts
     * before anything is created.
     */
    private List<Partido> buildCalendar(Tournament tournament, List<List<Long>> composition, Map<Long, Equipo> teamsById,
            GroupsRequestDTO request, int intervalDays) {
        List<Partido> matches = new ArrayList<>();
        for (List<Long> group : composition) {
            List<List<LeagueScheduleGenerator.Pairing>> rounds = scheduleGenerator.generate(group);
            for (int round = 0; round < rounds.size(); round++) {
                LocalDate date = request.getStartDate().plusDays((long) round * intervalDays);
                for (LeagueScheduleGenerator.Pairing pairing : rounds.get(round)) {
                    Equipo home = teamsById.get(pairing.getHomeTeamId());
                    Equipo away = teamsById.get(pairing.getAwayTeamId());
                    matchDateConflicts.requireFree(date, home);
                    matchDateConflicts.requireFree(date, away);

                    Partido match = new Partido();
                    match.setEquipoLocal(home);
                    match.setEquipoVisitante(away);
                    match.setFecha(date);
                    match.setHora(request.getTime());
                    match.setFase(Partido.Fase.FASE_DE_GRUPOS);
                    match.setEstado(Partido.Estado.PROGRAMADO);
                    match.setGolesLocal(0);
                    match.setGolesVisitante(0);
                    match.setTorneo(tournament);
                    matches.add(match);
                }
            }
        }
        return matches;
    }

    // ------------------------------------------------------------------ tables

    @Transactional(readOnly = true)
    public List<GroupDTO> getGroups(Long tournamentId) {
        Tournament tournament = tournamentRepository.findById(tournamentId)
                .orElseThrow(() -> tournamentNotFound(tournamentId));
        List<TournamentGroup> groups = tournamentGroupRepository.findByTournamentIdOrderByNameAsc(tournamentId);
        if (groups.isEmpty()) {
            return new ArrayList<>();
        }

        List<TournamentTeam> enrollments = tournamentTeamRepository.findByTournamentId(tournamentId);
        List<Partido> finished = partidoRepository.findByTorneoIdAndFaseAndEstado(
                tournamentId, Partido.Fase.FASE_DE_GRUPOS, Partido.Estado.FINALIZADO);
        Map<Long, Integer> fairPlay = new HashMap<>();
        for (EstadisticaRepository.TeamCards cards : estadisticaRepository
                .sumarTarjetasPorEquipoEnTorneoYFase(tournamentId, Partido.Fase.FASE_DE_GRUPOS)) {
            fairPlay.put(cards.getTeamId(), LeagueStandingsCalculator.fairPlayPoints(cards.getYellowCards(), cards.getRedCards()));
        }
        return toDTOs(tournament, groups, enrollments, finished, fairPlay);
    }

    private List<GroupDTO> toDTOs(Tournament tournament, List<TournamentGroup> groups, List<TournamentTeam> enrollments,
            List<Partido> finishedGroupMatches, Map<Long, Integer> fairPlay) {
        Map<Long, List<Equipo>> teamsOfGroup = new LinkedHashMap<>();
        for (TournamentGroup group : groups) {
            teamsOfGroup.put(group.getId(), new ArrayList<>());
        }
        for (TournamentTeam enrollment : enrollments) {
            if (enrollment.getGroup() != null && teamsOfGroup.containsKey(enrollment.getGroup().getId())) {
                teamsOfGroup.get(enrollment.getGroup().getId()).add(enrollment.getTeam());
            }
        }

        List<GroupDTO> result = new ArrayList<>();
        for (TournamentGroup group : groups) {
            List<Equipo> teams = teamsOfGroup.get(group.getId());
            Set<Long> ids = teams.stream().map(Equipo::getId).collect(Collectors.toSet());
            List<LeagueStandingsCalculator.MatchResult> results = new ArrayList<>();
            for (Partido match : finishedGroupMatches) {
                // Only the matches among the teams of this group count in its table.
                if (ids.contains(match.getEquipoLocal().getId()) && ids.contains(match.getEquipoVisitante().getId())) {
                    results.add(new LeagueStandingsCalculator.MatchResult(match.getEquipoLocal().getId(),
                            match.getEquipoVisitante().getId(), match.getGolesLocal(), match.getGolesVisitante()));
                }
            }
            List<StandingDTO> standings = standingsCalculator.calculate(
                    teams.stream().map(t -> new LeagueStandingsCalculator.TeamRef(t.getId(), t.getNombre())).collect(Collectors.toList()),
                    results, fairPlay);

            GroupDTO dto = new GroupDTO();
            dto.setId(group.getId());
            dto.setName(group.getName());
            dto.setTeams(teams.stream().map(t -> new TeamRefDTO(t.getId(), t.getNombre())).collect(Collectors.toList()));
            dto.setQualifiersCount(tournament.getQualifiersPerGroup() == null ? 0 : tournament.getQualifiersPerGroup());
            dto.setStandings(standings);
            result.add(dto);
        }
        return result;
    }

    // ------------------------------------------------------------------ stage state

    public boolean hasGroups(Long tournamentId) {
        return tournamentGroupRepository.existsByTournamentId(tournamentId);
    }

    /** Group stage matches that are not finished yet. */
    public long pendingGroupMatches(Long tournamentId) {
        return partidoRepository.countByTorneoIdAndFaseAndEstadoNot(
                tournamentId, Partido.Fase.FASE_DE_GRUPOS, Partido.Estado.FINALIZADO);
    }

    /**
     * The qualified team ids of each group, in group order (A, B, ...), each list
     * ordered by position (1st, 2nd).
     */
    @Transactional(readOnly = true)
    public List<List<Long>> qualifiers(Long tournamentId) {
        List<List<Long>> qualified = new ArrayList<>();
        for (GroupDTO group : getGroups(tournamentId)) {
            qualified.add(group.getStandings().stream()
                    .limit(group.getQualifiersCount())
                    .map(StandingDTO::getTeamId)
                    .collect(Collectors.toList()));
        }
        return qualified;
    }

    private NotFoundException tournamentNotFound(Long id) {
        return new NotFoundException("Fuera de juego! El torneo con Id " + id + " no existe.");
    }
}
