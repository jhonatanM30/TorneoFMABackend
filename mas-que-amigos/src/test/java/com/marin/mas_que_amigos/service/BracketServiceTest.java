package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marin.mas_que_amigos.dto.BracketDTO;
import com.marin.mas_que_amigos.dto.BracketRequestDTO;
import com.marin.mas_que_amigos.dto.BracketTieDTO;
import com.marin.mas_que_amigos.dto.PairingRequestDTO;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BracketServiceTest {

    private static final Long TOURNAMENT_ID = 1L;
    private static final LocalDate START = LocalDate.of(2027, 5, 1);
    private static final LocalTime TIME = LocalTime.of(18, 0);

    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private TournamentTeamRepository tournamentTeamRepository;
    @Mock
    private PartidoRepository partidoRepository;
    @Mock
    private BracketTieRepository bracketTieRepository;
    @Mock
    private GroupStageService groupStageService;
    @Mock
    private MatchDateConflicts matchDateConflicts;

    private BracketService service;

    /** The ties saved so far, standing in for the table. */
    private final List<BracketTie> storedTies = new ArrayList<>();
    private final AtomicLong tieIds = new AtomicLong(1000);
    private final AtomicLong matchIds = new AtomicLong(100);

    @BeforeEach
    void setUp() {
        service = new BracketService(tournamentRepository, tournamentTeamRepository, partidoRepository,
                bracketTieRepository, groupStageService, matchDateConflicts, new Random(5));
        lenient().when(tournamentRepository.existsById(TOURNAMENT_ID)).thenReturn(true);
        lenient().when(bracketTieRepository.save(any(BracketTie.class))).thenAnswer(invocation -> {
            BracketTie tie = invocation.getArgument(0);
            if (tie.getId() == null) {
                tie.setId(tieIds.incrementAndGet());
                storedTies.add(tie);
            }
            return tie;
        });
        lenient().when(bracketTieRepository.findByTournamentIdOrderByRoundNumberAscPositionInRoundAsc(TOURNAMENT_ID))
                .thenAnswer(invocation -> storedTies.stream()
                        .sorted(Comparator.comparing(BracketTie::getRoundNumber)
                                .thenComparing(t -> t.getKind() == BracketTieKind.THIRD_PLACE ? 1 : 0)
                                .thenComparing(BracketTie::getPositionInRound))
                        .collect(Collectors.toList()));
        lenient().when(bracketTieRepository.findByMatchId(any())).thenAnswer(invocation -> storedTies.stream()
                .filter(t -> t.getMatch() != null && t.getMatch().getId().equals(invocation.getArgument(0)))
                .findFirst());
        lenient().when(bracketTieRepository.existsByMatchId(any())).thenAnswer(invocation -> storedTies.stream()
                .anyMatch(t -> t.getMatch() != null && t.getMatch().getId().equals(invocation.getArgument(0))));
        lenient().when(partidoRepository.save(any(Partido.class))).thenAnswer(invocation -> {
            Partido match = invocation.getArgument(0);
            if (match.getId() == null) {
                match.setId(matchIds.incrementAndGet());
            }
            return match;
        });
    }

    // ---------------------------------------------------------------- fixtures

    private Equipo team(long id) {
        Equipo team = new Equipo();
        team.setId(id);
        team.setNombre("T" + id);
        return team;
    }

    private List<Equipo> teams(int count) {
        List<Equipo> teams = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            teams.add(team(id));
        }
        return teams;
    }

    private Tournament tournament(TournamentFormat format, TournamentStatus status) {
        Tournament tournament = new Tournament();
        tournament.setId(TOURNAMENT_ID);
        tournament.setName("Copa");
        tournament.setFormat(format);
        tournament.setStatus(status);
        return tournament;
    }

    private Tournament knockoutWithTeams(int teamCount) {
        Tournament tournament = tournament(TournamentFormat.KNOCKOUT, TournamentStatus.IN_PROGRESS);
        when(tournamentRepository.findByIdForUpdate(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        // lenient: some cases fail before the teams are read.
        lenient().when(tournamentTeamRepository.findTeamsByTournamentId(TOURNAMENT_ID)).thenReturn(teams(teamCount));
        return tournament;
    }

    private BracketRequestDTO request() {
        BracketRequestDTO request = new BracketRequestDTO();
        request.setStartDate(START);
        request.setTime(TIME);
        return request;
    }

    private PairingRequestDTO pairing(long home, long away) {
        PairingRequestDTO pairing = new PairingRequestDTO();
        pairing.setHomeTeamId(home);
        pairing.setAwayTeamId(away);
        return pairing;
    }

    private List<Partido> savedMatches() {
        ArgumentCaptor<Partido> captor = ArgumentCaptor.forClass(Partido.class);
        verify(partidoRepository, org.mockito.Mockito.atLeast(0)).save(captor.capture());
        return captor.getAllValues();
    }

    private Partido playoff(long homeId, long awayId, int homeGoals, int awayGoals, Partido.Estado state) {
        Partido match = new Partido();
        match.setEquipoLocal(team(homeId));
        match.setEquipoVisitante(team(awayId));
        match.setGolesLocal(homeGoals);
        match.setGolesVisitante(awayGoals);
        match.setFase(Partido.Fase.REPECHAJE);
        match.setEstado(state);
        return match;
    }

    // ---------------------------------------------------------------- generation (KNOCKOUT)

    @Test
    void generate_eightTeams_createsThreeRoundsAndTheFourFirstRoundMatches() {
        Tournament tournament = knockoutWithTeams(8);

        BracketDTO bracket = service.generateBracket(TOURNAMENT_ID, request());

        // 4 + 2 + 1 ties, plus the third place tie.
        assertThat(storedTies).hasSize(8);
        assertThat(bracket.getRounds()).hasSize(3);
        assertThat(bracket.getRounds()).extracting(r -> r.getTies().size()).containsExactly(4, 2, 1);
        assertThat(bracket.getThirdPlace()).isNotNull();
        assertThat(bracket.getChampion()).isNull();

        List<Partido> matches = savedMatches();
        assertThat(matches).hasSize(4);
        assertThat(matches).extracting(Partido::getFecha).containsOnly(START);
        assertThat(matches).extracting(Partido::getHora).containsOnly(TIME);
        assertThat(matches).extracting(Partido::getEstado).containsOnly(Partido.Estado.PROGRAMADO);
        assertThat(matches).extracting(Partido::getFase).containsOnly(Partido.Fase.ELIMINACION_DIRECTA);
        assertThat(matches).extracting(Partido::getTorneo).containsOnly(tournament);
        assertThat(matches).extracting(Partido::getGolesLocal).containsOnly(0);
        assertThat(matches).extracting(Partido::getGolesVisitante).containsOnly(0);
    }

    @Test
    void generate_roundDates_startPlusIntervalPerRound_withFinalAndThirdPlaceTheSameDay() {
        knockoutWithTeams(8);
        BracketRequestDTO request = request();
        request.setIntervalDays(7);

        service.generateBracket(TOURNAMENT_ID, request);

        List<BracketTie> main = storedTies.stream().filter(t -> t.getKind() == BracketTieKind.MAIN).collect(Collectors.toList());
        assertThat(main.stream().filter(t -> t.getRoundNumber() == 1)).extracting(BracketTie::getScheduledDate)
                .containsOnly(LocalDate.of(2027, 5, 1));
        assertThat(main.stream().filter(t -> t.getRoundNumber() == 2)).extracting(BracketTie::getScheduledDate)
                .containsOnly(LocalDate.of(2027, 5, 8));
        assertThat(main.stream().filter(t -> t.getRoundNumber() == 3)).extracting(BracketTie::getScheduledDate)
                .containsOnly(LocalDate.of(2027, 5, 15));
        BracketTie third = storedTies.stream().filter(t -> t.getKind() == BracketTieKind.THIRD_PLACE).findFirst().get();
        assertThat(third.getScheduledDate()).isEqualTo(LocalDate.of(2027, 5, 15));
        assertThat(storedTies).extracting(BracketTie::getScheduledTime).containsOnly(TIME);
    }

    @Test
    void generate_defaultIntervalIsSevenDays() {
        knockoutWithTeams(4);

        service.generateBracket(TOURNAMENT_ID, request());

        assertThat(storedTies.stream().filter(t -> t.getKind() == BracketTieKind.MAIN && t.getRoundNumber() == 2))
                .extracting(BracketTie::getScheduledDate).containsOnly(START.plusDays(7));
    }

    @Test
    void generate_twoTeams_onlyTheFinalWithPhaseFinalAndNoThirdPlace() {
        knockoutWithTeams(2);

        BracketDTO bracket = service.generateBracket(TOURNAMENT_ID, request());

        assertThat(storedTies).hasSize(1);
        assertThat(bracket.getThirdPlace()).isNull();
        assertThat(savedMatches()).hasSize(1);
        assertThat(savedMatches().get(0).getFase()).isEqualTo(Partido.Fase.FINAL);
    }

    @Test
    void generate_withoutThirdPlace_noThirdPlaceTie() {
        knockoutWithTeams(8);
        BracketRequestDTO request = request();
        request.setThirdPlaceMatch(false);

        BracketDTO bracket = service.generateBracket(TOURNAMENT_ID, request);

        assertThat(bracket.getThirdPlace()).isNull();
        assertThat(storedTies).hasSize(7);
    }

    @Test
    void generate_notAKnockoutFormat_isRejected() {
        for (TournamentFormat format : Arrays.asList(TournamentFormat.LEAGUE, TournamentFormat.LEGACY)) {
            Tournament other = tournament(format, TournamentStatus.IN_PROGRESS);
            when(tournamentRepository.findByIdForUpdate(TOURNAMENT_ID)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("llaves");
        }
        assertThat(storedTies).isEmpty();
    }

    @Test
    void generate_notInProgress_isRejected() {
        for (TournamentStatus status : Arrays.asList(TournamentStatus.DRAFT, TournamentStatus.FINISHED)) {
            Tournament other = tournament(TournamentFormat.KNOCKOUT, status);
            when(tournamentRepository.findByIdForUpdate(TOURNAMENT_ID)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request()))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no está en curso");
        }
        assertThat(storedTies).isEmpty();
    }

    @Test
    void generate_missingTournament_throwsNotFound() {
        when(tournamentRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateBracket(99L, request())).isInstanceOf(NotFoundException.class);
    }

    @Test
    void generate_bracketAlreadyGenerated_isRejected() {
        knockoutWithTeams(8);
        when(bracketTieRepository.existsByTournamentId(TOURNAMENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya fueron generadas");

        assertThat(storedTies).isEmpty();
    }

    @Test
    void generate_fiveTeams_isRejectedExplainingThePowerOfTwoAndThePlayoff() {
        knockoutWithTeams(5);

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("potencia de 2")
                .hasMessageContaining("repechaje");

        assertThat(storedTies).isEmpty();
        verify(partidoRepository, never()).save(any());
    }

    @Test
    void generate_aTeamEliminatedInAFinishedPlayoffDoesNotPlay() {
        knockoutWithTeams(5);
        // Team 5 lost the playoff against team 4.
        when(partidoRepository.findByTorneoIdAndFase(TOURNAMENT_ID, Partido.Fase.REPECHAJE))
                .thenReturn(Collections.singletonList(playoff(4, 5, 2, 1, Partido.Estado.FINALIZADO)));

        BracketDTO bracket = service.generateBracket(TOURNAMENT_ID, request());

        Set<Long> playing = new HashSet<>();
        bracket.getRounds().get(0).getTies().forEach(t -> {
            playing.add(t.getHomeTeam().getId());
            playing.add(t.getAwayTeam().getId());
        });
        assertThat(playing).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        assertThat(bracket.getRounds()).hasSize(2);
    }

    @Test
    void generate_aPlayoffWithoutFinishing_isRejected() {
        knockoutWithTeams(5);
        when(partidoRepository.findByTorneoIdAndFase(TOURNAMENT_ID, Partido.Fase.REPECHAJE))
                .thenReturn(Collections.singletonList(playoff(4, 5, 0, 0, Partido.Estado.PROGRAMADO)));

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("repechaje");

        assertThat(storedTies).isEmpty();
    }

    @Test
    void generate_startDateNotAfterTheLastMatch_isRejected() {
        knockoutWithTeams(8);
        when(partidoRepository.findMaxFechaByTorneoId(TOURNAMENT_ID)).thenReturn(Optional.of(START));

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("posterior");

        assertThat(storedTies).isEmpty();
    }

    @Test
    void generate_startDateAfterTheLastMatch_isAccepted() {
        knockoutWithTeams(4);
        when(partidoRepository.findMaxFechaByTorneoId(TOURNAMENT_ID)).thenReturn(Optional.of(START.minusDays(1)));

        service.generateBracket(TOURNAMENT_ID, request());

        assertThat(storedTies).isNotEmpty();
    }

    @Test
    void generate_aFirstRoundTeamBusyThatDate_isRejectedAndCreatesNothing() {
        knockoutWithTeams(8);
        doThrow(new BusinessException("El equipo T3 ya tiene un partido el 2027-05-01"))
                .when(matchDateConflicts).requireFree(any(LocalDate.class), any(Equipo.class));

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya tiene un partido");

        verify(partidoRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- draw and manual pairings

    @Test
    void generate_randomDraw_placesEveryTeamInExactlyOneFirstRoundTie() {
        knockoutWithTeams(8);

        BracketDTO bracket = service.generateBracket(TOURNAMENT_ID, request());

        List<Long> placed = new ArrayList<>();
        bracket.getRounds().get(0).getTies().forEach(t -> {
            placed.add(t.getHomeTeam().getId());
            placed.add(t.getAwayTeam().getId());
        });
        assertThat(placed).hasSize(8);
        assertThat(new HashSet<>(placed)).hasSize(8);
    }

    @Test
    void generate_randomDraw_isRepeatableWithTheSameSeed() {
        knockoutWithTeams(8);
        BracketDTO first = service.generateBracket(TOURNAMENT_ID, request());
        List<Long> firstOrder = first.getRounds().get(0).getTies().stream()
                .flatMap(t -> java.util.stream.Stream.of(t.getHomeTeam().getId(), t.getAwayTeam().getId()))
                .collect(Collectors.toList());

        storedTies.clear();
        service = new BracketService(tournamentRepository, tournamentTeamRepository, partidoRepository,
                bracketTieRepository, groupStageService, matchDateConflicts, new Random(5));
        BracketDTO second = service.generateBracket(TOURNAMENT_ID, request());
        List<Long> secondOrder = second.getRounds().get(0).getTies().stream()
                .flatMap(t -> java.util.stream.Stream.of(t.getHomeTeam().getId(), t.getAwayTeam().getId()))
                .collect(Collectors.toList());

        assertThat(secondOrder).isEqualTo(firstOrder);
    }

    @Test
    void generate_manualPairings_areUsedInTheGivenOrder() {
        knockoutWithTeams(8);
        BracketRequestDTO request = request();
        request.setPairings(Arrays.asList(pairing(8, 1), pairing(2, 7), pairing(3, 6), pairing(4, 5)));

        BracketDTO bracket = service.generateBracket(TOURNAMENT_ID, request);

        assertThat(bracket.getRounds().get(0).getTies()).extracting(t -> t.getHomeTeam().getId())
                .containsExactly(8L, 2L, 3L, 4L);
        assertThat(bracket.getRounds().get(0).getTies()).extracting(t -> t.getAwayTeam().getId())
                .containsExactly(1L, 7L, 6L, 5L);
    }

    @Test
    void generate_manualPairingsThatAreIncomplete_areRejected() {
        knockoutWithTeams(8);
        BracketRequestDTO request = request();
        request.setPairings(Arrays.asList(pairing(1, 2), pairing(3, 4), pairing(5, 6)));

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cruces");

        assertThat(storedTies).isEmpty();
    }

    @Test
    void generate_manualPairingsWithARepeatedTeam_areRejected() {
        knockoutWithTeams(8);
        BracketRequestDTO request = request();
        request.setPairings(Arrays.asList(pairing(1, 2), pairing(3, 4), pairing(5, 6), pairing(6, 7)));

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("repite");

        assertThat(storedTies).isEmpty();
    }

    @Test
    void generate_manualPairingsWithATeamThatIsNotInTheTournament_areRejected() {
        knockoutWithTeams(8);
        BracketRequestDTO request = request();
        request.setPairings(Arrays.asList(pairing(1, 2), pairing(3, 4), pairing(5, 6), pairing(7, 99)));

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no participa");

        assertThat(storedTies).isEmpty();
    }

    @Test
    void generate_manualPairingsWithAnEliminatedTeam_areRejected() {
        knockoutWithTeams(5);
        when(partidoRepository.findByTorneoIdAndFase(TOURNAMENT_ID, Partido.Fase.REPECHAJE))
                .thenReturn(Collections.singletonList(playoff(4, 5, 2, 1, Partido.Estado.FINALIZADO)));
        BracketRequestDTO request = request();
        request.setPairings(Arrays.asList(pairing(1, 2), pairing(3, 5)));

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no participa");

        assertThat(storedTies).isEmpty();
    }

    // ---------------------------------------------------------------- matches that need a winner

    private Partido plainMatch(Partido.Fase phase, TournamentFormat format) {
        Partido match = new Partido();
        match.setId(7000L);
        match.setFase(phase);
        match.setTorneo(tournament(format, TournamentStatus.IN_PROGRESS));
        match.setEquipoLocal(team(1));
        match.setEquipoVisitante(team(2));
        return match;
    }

    @Test
    void requiresWinner_aMatchOfTheBracket_isTrue() {
        Partido match = plainMatch(Partido.Fase.ELIMINACION_DIRECTA, TournamentFormat.KNOCKOUT);
        when(bracketTieRepository.existsByMatchId(7000L)).thenReturn(true);

        assertThat(service.requiresWinner(match)).isTrue();
    }

    @Test
    void requiresWinner_aPlayoffOfAKnockoutTournament_isTrue() {
        assertThat(service.requiresWinner(plainMatch(Partido.Fase.REPECHAJE, TournamentFormat.KNOCKOUT))).isTrue();
    }

    @Test
    void requiresWinner_otherMatches_isFalse() {
        assertThat(service.requiresWinner(plainMatch(Partido.Fase.REPECHAJE, TournamentFormat.LEAGUE))).isFalse();
        assertThat(service.requiresWinner(plainMatch(Partido.Fase.FASE_DE_GRUPOS, TournamentFormat.GROUPS_KNOCKOUT))).isFalse();
        assertThat(service.requiresWinner(plainMatch(Partido.Fase.FASE_DE_GRUPOS, TournamentFormat.LEAGUE))).isFalse();
        assertThat(service.requiresWinner(plainMatch(Partido.Fase.ELIMINACION_DIRECTA, TournamentFormat.KNOCKOUT))).isFalse();
    }

    private Partido tiedPlayoff(Integer homePenalties, Integer awayPenalties) {
        Partido match = plainMatch(Partido.Fase.REPECHAJE, TournamentFormat.KNOCKOUT);
        match.setGolesLocal(1);
        match.setGolesVisitante(1);
        match.setPenalesLocal(homePenalties);
        match.setPenalesVisitante(awayPenalties);
        return match;
    }

    @Test
    void requireDecided_aDecisiveScore_passes() {
        Partido match = plainMatch(Partido.Fase.REPECHAJE, TournamentFormat.KNOCKOUT);
        match.setGolesLocal(2);
        match.setGolesVisitante(1);

        service.requireDecided(match);
    }

    @Test
    void requireDecided_aTieWithDifferentPenalties_passes() {
        service.requireDecided(tiedPlayoff(5, 4));
    }

    @Test
    void requireDecided_aTieWithoutPenaltiesOrWithEqualOnes_isRejected() {
        assertThatThrownBy(() -> service.requireDecided(tiedPlayoff(null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empatado")
                .hasMessageContaining("penales");
        assertThatThrownBy(() -> service.requireDecided(tiedPlayoff(4, 4))).isInstanceOf(BusinessException.class);
    }

    @Test
    void requireDecided_aMatchThatDoesNotNeedAWinner_acceptsATie() {
        Partido match = plainMatch(Partido.Fase.FASE_DE_GRUPOS, TournamentFormat.LEAGUE);
        match.setGolesLocal(1);
        match.setGolesVisitante(1);

        service.requireDecided(match);
    }

    // ---------------------------------------------------------------- advancement

    private BracketDTO fourTeamBracket(boolean thirdPlace) {
        knockoutWithTeams(4);
        BracketRequestDTO request = request();
        request.setPairings(Arrays.asList(pairing(1, 2), pairing(3, 4)));
        request.setThirdPlaceMatch(thirdPlace);
        BracketDTO bracket = service.generateBracket(TOURNAMENT_ID, request);
        org.mockito.Mockito.clearInvocations(matchDateConflicts, partidoRepository, bracketTieRepository);
        return bracket;
    }

    private BracketTie tie(int round, int position) {
        return storedTies.stream()
                .filter(t -> t.getKind() == BracketTieKind.MAIN && t.getRoundNumber() == round && t.getPositionInRound() == position)
                .findFirst().get();
    }

    private BracketTie thirdPlaceTie() {
        return storedTies.stream().filter(t -> t.getKind() == BracketTieKind.THIRD_PLACE).findFirst().get();
    }

    private void finish(BracketTie tie, int homeGoals, int awayGoals) {
        Partido match = tie.getMatch();
        match.setGolesLocal(homeGoals);
        match.setGolesVisitante(awayGoals);
        match.setEstado(Partido.Estado.FINALIZADO);
        service.onMatchFinished(match);
    }

    @Test
    void advance_theWinnerTakesItsPlaceAndTheNextMatchWaitsForTheRival() {
        fourTeamBracket(true);

        finish(tie(1, 0), 2, 1);

        BracketTie finalTie = tie(2, 0);
        assertThat(finalTie.getHomeTeam().getId()).isEqualTo(1L);
        assertThat(finalTie.getAwayTeam()).isNull();
        assertThat(finalTie.getMatch()).isNull();
        assertThat(tie(1, 0).getWinnerTeam().getId()).isEqualTo(1L);
    }

    @Test
    void advance_whenBothTeamsAreKnown_theNextMatchIsCreatedWithTheDateOfItsRound() {
        fourTeamBracket(true);
        finish(tie(1, 0), 2, 1);

        finish(tie(1, 1), 0, 3);

        BracketTie finalTie = tie(2, 0);
        assertThat(finalTie.getAwayTeam().getId()).isEqualTo(4L);
        assertThat(finalTie.getMatch()).isNotNull();
        assertThat(finalTie.getMatch().getEquipoLocal().getId()).isEqualTo(1L);
        assertThat(finalTie.getMatch().getEquipoVisitante().getId()).isEqualTo(4L);
        assertThat(finalTie.getMatch().getFecha()).isEqualTo(START.plusDays(7));
        assertThat(finalTie.getMatch().getHora()).isEqualTo(TIME);
        assertThat(finalTie.getMatch().getFase()).isEqualTo(Partido.Fase.FINAL);
        assertThat(finalTie.getMatch().getEstado()).isEqualTo(Partido.Estado.PROGRAMADO);
        assertThat(finalTie.getMatch().getGolesLocal()).isZero();
    }

    @Test
    void advance_theLosersOfTheSemifinalsPlayThirdPlaceOnTheDateOfTheFinal() {
        fourTeamBracket(true);

        finish(tie(1, 0), 2, 1);
        finish(tie(1, 1), 0, 3);

        BracketTie third = thirdPlaceTie();
        assertThat(third.getHomeTeam().getId()).isEqualTo(2L);
        assertThat(third.getAwayTeam().getId()).isEqualTo(3L);
        assertThat(third.getMatch()).isNotNull();
        assertThat(third.getMatch().getFecha()).isEqualTo(tie(2, 0).getScheduledDate());
        assertThat(third.getMatch().getFase()).isEqualTo(Partido.Fase.ELIMINACION_DIRECTA);
    }

    @Test
    void advance_withoutThirdPlace_theLoserIsNotPlacedAnywhere() {
        fourTeamBracket(false);

        finish(tie(1, 0), 2, 1);
        finish(tie(1, 1), 0, 3);

        assertThat(storedTies.stream().filter(t -> t.getKind() == BracketTieKind.THIRD_PLACE)).isEmpty();
        assertThat(tie(2, 0).getHomeTeam().getId()).isEqualTo(1L);
        assertThat(tie(2, 0).getAwayTeam().getId()).isEqualTo(4L);
    }

    @Test
    void advance_aTiedMatchDecidedByPenalties_movesTheTeamWithMorePenalties() {
        fourTeamBracket(true);
        BracketTie semifinal = tie(1, 0);
        semifinal.getMatch().setPenalesLocal(3);
        semifinal.getMatch().setPenalesVisitante(5);

        finish(semifinal, 1, 1);

        assertThat(tie(2, 0).getHomeTeam().getId()).isEqualTo(2L);
    }

    @Test
    void advance_theFinalLeavesTheChampion() {
        fourTeamBracket(true);
        finish(tie(1, 0), 2, 1);
        finish(tie(1, 1), 0, 3);

        finish(tie(2, 0), 1, 0);

        BracketDTO bracket = service.getBracket(TOURNAMENT_ID);
        assertThat(bracket.getChampion()).isNotNull();
        assertThat(bracket.getChampion().getId()).isEqualTo(1L);
    }

    @Test
    void advance_aMatchThatIsNotInTheBracket_doesNothing() {
        fourTeamBracket(true);
        Partido stranger = plainMatch(Partido.Fase.FASE_DE_GRUPOS, TournamentFormat.KNOCKOUT);

        service.onMatchFinished(stranger);

        verify(bracketTieRepository, never()).save(any(BracketTie.class));
    }

    @Test
    void advance_theMatchesItCreatesAreNotCheckedAgainstDateConflicts() {
        fourTeamBracket(true);

        finish(tie(1, 0), 2, 1);
        finish(tie(1, 1), 0, 3);

        org.mockito.Mockito.verifyNoInteractions(matchDateConflicts);
    }

    // ---------------------------------------------------------------- consultation

    @Test
    void getBracket_roundNamesCountedFromTheFinal() {
        knockoutWithTeams(16);

        BracketDTO sixteen = service.generateBracket(TOURNAMENT_ID, request());

        assertThat(sixteen.getRounds()).extracting(r -> r.getName())
                .containsExactly("Octavos de final", "Cuartos de final", "Semifinales", "Final");
        assertThat(sixteen.getRounds()).extracting(r -> r.getRound()).containsExactly(1, 2, 3, 4);
    }

    @Test
    void getBracket_laterRoundsHaveNoTeamsOrMatchUntilTheyAreKnown() {
        BracketDTO bracket = fourTeamBracket(true);

        assertThat(bracket.getRounds().get(1).getTies().get(0).getHomeTeam()).isNull();
        assertThat(bracket.getRounds().get(1).getTies().get(0).getMatchId()).isNull();
        assertThat(bracket.getRounds().get(0).getTies().get(0).getMatchId()).isNotNull();
        assertThat(bracket.getRounds().get(0).getTies().get(0).getMatchStatus()).isEqualTo("PROGRAMADO");
    }

    @Test
    void getBracket_showsScoreAndPenaltiesAndTheWinnerOfAFinishedTie() {
        fourTeamBracket(true);
        BracketTie semifinal = tie(1, 0);
        semifinal.getMatch().setPenalesLocal(5);
        semifinal.getMatch().setPenalesVisitante(4);
        finish(semifinal, 1, 1);

        BracketTieDTO dto = service.getBracket(TOURNAMENT_ID).getRounds().get(0).getTies().get(0);

        assertThat(dto.getHomeGoals()).isEqualTo(1);
        assertThat(dto.getAwayGoals()).isEqualTo(1);
        assertThat(dto.getHomePenalties()).isEqualTo(5);
        assertThat(dto.getAwayPenalties()).isEqualTo(4);
        assertThat(dto.getWinnerTeamId()).isEqualTo(1L);
        assertThat(dto.getMatchStatus()).isEqualTo("FINALIZADO");
    }

    @Test
    void getBracket_showsTheDateOfTheMatchOnceItExists_evenIfTheAdminEditedIt() {
        fourTeamBracket(true);
        BracketTie semifinal = tie(1, 0);
        semifinal.getMatch().setFecha(LocalDate.of(2027, 5, 3));
        semifinal.getMatch().setHora(LocalTime.of(20, 0));

        BracketTieDTO edited = service.getBracket(TOURNAMENT_ID).getRounds().get(0).getTies().get(0);
        BracketTieDTO notCreatedYet = service.getBracket(TOURNAMENT_ID).getRounds().get(1).getTies().get(0);

        assertThat(edited.getDate()).isEqualTo(LocalDate.of(2027, 5, 3));
        assertThat(edited.getTime()).isEqualTo(LocalTime.of(20, 0));
        // A tie without a match still shows the date of its round.
        assertThat(notCreatedYet.getDate()).isEqualTo(START.plusDays(7));
    }

    @Test
    void getBracket_aTournamentWithoutBracket_hasNoRounds() {

        BracketDTO bracket = service.getBracket(TOURNAMENT_ID);

        assertThat(bracket.getRounds()).isEmpty();
        assertThat(bracket.getThirdPlace()).isNull();
        assertThat(bracket.getChampion()).isNull();
    }

    @Test
    void getBracket_missingTournament_throwsNotFound() {
        when(tournamentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.getBracket(99L)).isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- protection and champion

    @Test
    void isBracketMatch_looksUpTheTies() {
        when(bracketTieRepository.existsByMatchId(5L)).thenReturn(true);
        when(bracketTieRepository.existsByMatchId(6L)).thenReturn(false);

        assertThat(service.isBracketMatch(5L)).isTrue();
        assertThat(service.isBracketMatch(6L)).isFalse();
    }

    @Test
    void requireChampion_withoutBracketOrWithAnUnfinishedFinal_isRejected() {
        Tournament knockout = tournament(TournamentFormat.KNOCKOUT, TournamentStatus.IN_PROGRESS);

        assertThatThrownBy(() -> service.requireChampion(knockout))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("campeón");

        fourTeamBracket(true);
        assertThatThrownBy(() -> service.requireChampion(knockout))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("campeón");
    }

    @Test
    void requireChampion_withAFinishedFinal_passes() {
        Tournament groups = tournament(TournamentFormat.GROUPS_KNOCKOUT, TournamentStatus.IN_PROGRESS);
        fourTeamBracket(true);
        finish(tie(1, 0), 2, 1);
        finish(tie(1, 1), 0, 3);
        finish(tie(2, 0), 1, 0);

        service.requireChampion(groups);
    }

    @Test
    void requireChampion_aLeague_isNotChecked() {
        service.requireChampion(tournament(TournamentFormat.LEAGUE, TournamentStatus.IN_PROGRESS));
    }

    // ---------------------------------------------------------------- generation (GROUPS_KNOCKOUT)

    private Tournament groupsTournament(List<List<Long>> qualifiers) {
        Tournament tournament = tournament(TournamentFormat.GROUPS_KNOCKOUT, TournamentStatus.IN_PROGRESS);
        when(tournamentRepository.findByIdForUpdate(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        List<Equipo> enrolled = new ArrayList<>();
        for (List<Long> group : qualifiers) {
            for (Long id : group) {
                enrolled.add(team(id));
            }
        }
        // Extra teams that did not qualify.
        enrolled.add(team(900));
        enrolled.add(team(901));
        // lenient: several cases are rejected before all of this is read.
        lenient().when(tournamentTeamRepository.findTeamsByTournamentId(TOURNAMENT_ID)).thenReturn(enrolled);
        lenient().when(groupStageService.hasGroups(TOURNAMENT_ID)).thenReturn(true);
        lenient().when(groupStageService.pendingGroupMatches(TOURNAMENT_ID)).thenReturn(0L);
        lenient().when(groupStageService.qualifiers(TOURNAMENT_ID)).thenReturn(qualifiers);
        return tournament;
    }

    private List<Long> group(long first, Long second) {
        return second == null ? Collections.singletonList(first) : Arrays.asList(first, second);
    }

    private List<String> firstRoundOf(BracketDTO bracket) {
        return bracket.getRounds().get(0).getTies().stream()
                .map(t -> t.getHomeTeam().getId() + "-" + t.getAwayTeam().getId())
                .collect(Collectors.toList());
    }

    @Test
    void groups_twoGroupsWithTwoQualifiers_crossFirstAgainstSecondOfTheOtherGroup() {
        groupsTournament(Arrays.asList(group(11, 12L), group(21, 22L)));

        BracketDTO bracket = service.generateBracket(TOURNAMENT_ID, request());

        assertThat(bracket.getRounds()).hasSize(2);
        assertThat(firstRoundOf(bracket)).containsExactly("11-22", "21-12");
        assertThat(bracket.getRounds().get(0).getName()).isEqualTo("Semifinales");
    }

    @Test
    void groups_fourGroupsWithTwoQualifiers_quartersInTheOrderOfTheSpec() {
        groupsTournament(Arrays.asList(group(11, 12L), group(21, 22L), group(31, 32L), group(41, 42L)));

        BracketDTO bracket = service.generateBracket(TOURNAMENT_ID, request());

        assertThat(firstRoundOf(bracket)).containsExactly("11-22", "31-42", "21-12", "41-32");
        assertThat(bracket.getRounds()).hasSize(3);
    }

    @Test
    void groups_oneQualifierPerGroup_theFirstsOfTheGroupPairsMeet() {
        groupsTournament(Arrays.asList(group(11, null), group(21, null), group(31, null), group(41, null)));

        BracketDTO bracket = service.generateBracket(TOURNAMENT_ID, request());

        assertThat(firstRoundOf(bracket)).containsExactly("11-21", "31-41");
    }

    @Test
    void groups_theQualifiedTeamsAreTheOnesThatPlayTheFirstRound() {
        groupsTournament(Arrays.asList(group(11, 12L), group(21, 22L)));

        service.generateBracket(TOURNAMENT_ID, request());

        Set<Long> playing = savedMatches().stream()
                .flatMap(m -> java.util.stream.Stream.of(m.getEquipoLocal().getId(), m.getEquipoVisitante().getId()))
                .collect(Collectors.toSet());
        assertThat(playing).containsExactlyInAnyOrder(11L, 12L, 21L, 22L);
    }

    @Test
    void groups_withGroupMatchesStillPending_isRejectedWithTheCount() {
        groupsTournament(Arrays.asList(group(11, 12L), group(21, 22L)));
        when(groupStageService.pendingGroupMatches(TOURNAMENT_ID)).thenReturn(3L);

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("3 partido(s)");

        assertThat(storedTies).isEmpty();
    }

    @Test
    void groups_withoutGroups_isRejected() {
        groupsTournament(Arrays.asList(group(11, 12L), group(21, 22L)));
        when(groupStageService.hasGroups(TOURNAMENT_ID)).thenReturn(false);

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("todavía no tiene grupos");

        assertThat(storedTies).isEmpty();
    }

    @Test
    void groups_bracketAlreadyGenerated_isRejected() {
        groupsTournament(Arrays.asList(group(11, 12L), group(21, 22L)));
        when(bracketTieRepository.existsByTournamentId(TOURNAMENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya fueron generadas");
    }

    @Test
    void groups_manualPairings_areNotAllowed() {
        groupsTournament(Arrays.asList(group(11, 12L), group(21, 22L)));
        BracketRequestDTO request = request();
        request.setPairings(Arrays.asList(pairing(11, 21), pairing(12, 22)));

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no se pueden indicar a mano");

        assertThat(storedTies).isEmpty();
    }

    @Test
    void groups_startDateNotAfterTheLastMatch_isRejected() {
        groupsTournament(Arrays.asList(group(11, 12L), group(21, 22L)));
        when(partidoRepository.findMaxFechaByTorneoId(TOURNAMENT_ID)).thenReturn(Optional.of(START));

        assertThatThrownBy(() -> service.generateBracket(TOURNAMENT_ID, request()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("posterior");

        assertThat(storedTies).isEmpty();
    }

    @Test
    void groups_theBracketAdvancesLikeAnyOther() {
        groupsTournament(Arrays.asList(group(11, 12L), group(21, 22L)));
        service.generateBracket(TOURNAMENT_ID, request());

        // Semifinal 11 vs 22 won by 22; semifinal 21 vs 12 won by 21.
        finish(tie(1, 0), 0, 2);
        finish(tie(1, 1), 3, 0);

        assertThat(tie(2, 0).getHomeTeam().getId()).isEqualTo(22L);
        assertThat(tie(2, 0).getAwayTeam().getId()).isEqualTo(21L);
        assertThat(tie(2, 0).getMatch()).isNotNull();
        assertThat(tie(2, 0).getMatch().getFase()).isEqualTo(Partido.Fase.FINAL);
    }
}
