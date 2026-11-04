package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marin.mas_que_amigos.dto.GroupCompositionDTO;
import com.marin.mas_que_amigos.dto.GroupDTO;
import com.marin.mas_que_amigos.dto.GroupsRequestDTO;
import com.marin.mas_que_amigos.dto.StandingDTO;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
class GroupStageServiceTest {

    private static final Long TOURNAMENT_ID = 1L;
    private static final LocalDate START = LocalDate.of(2027, 3, 6);
    private static final LocalTime TIME = LocalTime.of(15, 0);

    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private TournamentTeamRepository tournamentTeamRepository;
    @Mock
    private TournamentGroupRepository tournamentGroupRepository;
    @Mock
    private PartidoRepository partidoRepository;
    @Mock
    private EstadisticaRepository estadisticaRepository;
    @Mock
    private MatchDateConflicts matchDateConflicts;

    private GroupStageService service;

    private final List<TournamentGroup> storedGroups = new ArrayList<>();
    private final AtomicLong groupIds = new AtomicLong(10);
    private List<TournamentTeam> enrollments = new ArrayList<>();

    @BeforeEach
    void setUp() {
        service = new GroupStageService(tournamentRepository, tournamentTeamRepository, tournamentGroupRepository,
                partidoRepository, estadisticaRepository, matchDateConflicts, new Random(3));
        lenient().when(tournamentGroupRepository.save(any(TournamentGroup.class))).thenAnswer(invocation -> {
            TournamentGroup group = invocation.getArgument(0);
            if (group.getId() == null) {
                group.setId(groupIds.incrementAndGet());
                storedGroups.add(group);
            }
            return group;
        });
        lenient().when(tournamentGroupRepository.findByTournamentIdOrderByNameAsc(TOURNAMENT_ID))
                .thenAnswer(invocation -> storedGroups.stream()
                        .sorted((a, b) -> a.getName().compareTo(b.getName())).collect(Collectors.toList()));
        lenient().when(tournamentTeamRepository.findByTournamentId(TOURNAMENT_ID)).thenAnswer(invocation -> enrollments);
        lenient().when(partidoRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------------------------------------------------------- fixtures

    private Equipo team(long id) {
        Equipo team = new Equipo();
        team.setId(id);
        team.setNombre("T" + id);
        return team;
    }

    private Tournament tournament(TournamentFormat format, TournamentStatus status) {
        Tournament tournament = new Tournament();
        tournament.setId(TOURNAMENT_ID);
        tournament.setName("Mundial");
        tournament.setFormat(format);
        tournament.setStatus(status);
        return tournament;
    }

    /** A GROUPS_KNOCKOUT tournament in progress with {@code count} enrolled teams and no groups. */
    private Tournament groupsTournamentWithTeams(int count) {
        Tournament tournament = tournament(TournamentFormat.GROUPS_KNOCKOUT, TournamentStatus.IN_PROGRESS);
        when(tournamentRepository.findByIdForUpdate(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        List<Equipo> teams = new ArrayList<>();
        enrollments = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            Equipo team = team(id);
            teams.add(team);
            TournamentTeam enrollment = new TournamentTeam();
            enrollment.setTournament(tournament);
            enrollment.setTeam(team);
            enrollments.add(enrollment);
        }
        lenient().when(tournamentTeamRepository.findTeamsByTournamentId(TOURNAMENT_ID)).thenReturn(teams);
        return tournament;
    }

    private GroupsRequestDTO request(int groupCount, int qualifiers) {
        GroupsRequestDTO request = new GroupsRequestDTO();
        request.setGroupCount(groupCount);
        request.setQualifiersPerGroup(qualifiers);
        request.setStartDate(START);
        request.setTime(TIME);
        return request;
    }

    private GroupCompositionDTO composition(Long... teamIds) {
        GroupCompositionDTO composition = new GroupCompositionDTO();
        composition.setTeamIds(Arrays.asList(teamIds));
        return composition;
    }

    @SuppressWarnings("unchecked")
    private List<Partido> savedMatches() {
        ArgumentCaptor<List<Partido>> captor = ArgumentCaptor.forClass(List.class);
        verify(partidoRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private Set<Long> teamIdsOf(GroupDTO group) {
        return group.getTeams().stream().map(t -> t.getId()).collect(Collectors.toSet());
    }

    // ---------------------------------------------------------------- createGroups

    @Test
    void create_eightTeamsInTwoGroups_formsAAndBWithTheirCalendars() {
        Tournament tournament = groupsTournamentWithTeams(8);

        List<GroupDTO> groups = service.createGroups(TOURNAMENT_ID, request(2, 2));

        assertThat(groups).extracting(GroupDTO::getName).containsExactly("A", "B");
        assertThat(groups.get(0).getTeams()).hasSize(4);
        assertThat(groups.get(1).getTeams()).hasSize(4);
        assertThat(groups).extracting(GroupDTO::getQualifiersCount).containsExactly(2, 2);
        assertThat(tournament.getQualifiersPerGroup()).isEqualTo(2);
        // Every enrollment now points to its group.
        assertThat(enrollments).extracting(TournamentTeam::getGroup).doesNotContainNull();

        List<Partido> matches = savedMatches();
        assertThat(matches).hasSize(12);
        assertThat(matches).extracting(Partido::getFecha).containsOnly(
                LocalDate.of(2027, 3, 6), LocalDate.of(2027, 3, 13), LocalDate.of(2027, 3, 20));
        assertThat(matches).extracting(Partido::getHora).containsOnly(TIME);
        assertThat(matches).extracting(Partido::getFase).containsOnly(Partido.Fase.FASE_DE_GRUPOS);
        assertThat(matches).extracting(Partido::getEstado).containsOnly(Partido.Estado.PROGRAMADO);
        assertThat(matches).extracting(Partido::getTorneo).containsOnly(tournament);
        assertThat(matches).extracting(Partido::getGolesLocal).containsOnly(0);
    }

    @Test
    void create_eachGroupPlaysItsOwnTeamsOnlyAndEveryPairOnce() {
        groupsTournamentWithTeams(8);

        List<GroupDTO> groups = service.createGroups(TOURNAMENT_ID, request(2, 2));

        Set<Long> groupA = teamIdsOf(groups.get(0));
        Set<Long> groupB = teamIdsOf(groups.get(1));
        Set<String> pairs = new HashSet<>();
        for (Partido match : savedMatches()) {
            long home = match.getEquipoLocal().getId();
            long away = match.getEquipoVisitante().getId();
            boolean sameGroup = (groupA.contains(home) && groupA.contains(away)) || (groupB.contains(home) && groupB.contains(away));
            assertThat(sameGroup).as("match %d-%d inside one group", home, away).isTrue();
            pairs.add(Math.min(home, away) + "-" + Math.max(home, away));
        }
        assertThat(pairs).hasSize(12);
    }

    @Test
    void create_nineTeamsInTwoGroups_fiveAndFour() {
        groupsTournamentWithTeams(9);

        List<GroupDTO> groups = service.createGroups(TOURNAMENT_ID, request(2, 2));

        assertThat(groups).extracting(g -> g.getTeams().size()).containsExactly(5, 4);
        // 10 matches in the group of 5 and 6 in the group of 4.
        assertThat(savedMatches()).hasSize(16);
    }

    @Test
    void create_manualComposition_usesExactlyThoseTeams() {
        groupsTournamentWithTeams(6);
        GroupsRequestDTO request = request(2, 1);
        request.setGroups(Arrays.asList(composition(1L, 2L, 3L), composition(4L, 5L, 6L)));

        List<GroupDTO> groups = service.createGroups(TOURNAMENT_ID, request);

        assertThat(teamIdsOf(groups.get(0))).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(teamIdsOf(groups.get(1))).containsExactlyInAnyOrder(4L, 5L, 6L);
    }

    @Test
    void create_invalidManualComposition_isRejectedAndCreatesNothing() {
        groupsTournamentWithTeams(6);
        GroupsRequestDTO request = request(2, 1);
        request.setGroups(Arrays.asList(composition(1L, 2L, 3L), composition(4L, 5L)));

        assertThatThrownBy(() -> service.createGroups(TOURNAMENT_ID, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("todos los equipos inscritos");

        assertThat(storedGroups).isEmpty();
        verify(partidoRepository, never()).saveAll(any());
    }

    @Test
    void create_aNumberOfGroupsThatIsNotAPowerOfTwo_isRejected() {
        groupsTournamentWithTeams(12);

        assertThatThrownBy(() -> service.createGroups(TOURNAMENT_ID, request(3, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("potencia de 2");
        assertThatThrownBy(() -> service.createGroups(TOURNAMENT_ID, request(1, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("potencia de 2");

        assertThat(storedGroups).isEmpty();
    }

    @Test
    void create_aGroupWithFewerThanTwoTeams_isRejected() {
        groupsTournamentWithTeams(3);

        assertThatThrownBy(() -> service.createGroups(TOURNAMENT_ID, request(2, 1)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("al menos 2 equipos");
    }

    @Test
    void create_qualifiersOutsideOneOrTwo_areRejected() {
        groupsTournamentWithTeams(8);

        assertThatThrownBy(() -> service.createGroups(TOURNAMENT_ID, request(2, 3)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1 o 2");
        assertThatThrownBy(() -> service.createGroups(TOURNAMENT_ID, request(2, 0)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("1 o 2");
    }

    @Test
    void create_qualifiersNotLessThanTheSmallestGroup_areRejected() {
        groupsTournamentWithTeams(4);

        // Two groups of 2 teams cannot qualify 2 each: nobody would be eliminated.
        assertThatThrownBy(() -> service.createGroups(TOURNAMENT_ID, request(2, 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("menos que");
    }

    @Test
    void create_notAGroupsKnockoutTournament_isRejected() {
        for (TournamentFormat format : Arrays.asList(TournamentFormat.LEAGUE, TournamentFormat.KNOCKOUT, TournamentFormat.LEGACY)) {
            Tournament other = tournament(format, TournamentStatus.IN_PROGRESS);
            when(tournamentRepository.findByIdForUpdate(TOURNAMENT_ID)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.createGroups(TOURNAMENT_ID, request(2, 2)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("fase de grupos");
        }
        assertThat(storedGroups).isEmpty();
    }

    @Test
    void create_notInProgress_isRejected() {
        for (TournamentStatus status : Arrays.asList(TournamentStatus.DRAFT, TournamentStatus.FINISHED)) {
            Tournament other = tournament(TournamentFormat.GROUPS_KNOCKOUT, status);
            when(tournamentRepository.findByIdForUpdate(TOURNAMENT_ID)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.createGroups(TOURNAMENT_ID, request(2, 2)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no está en curso");
        }
    }

    @Test
    void create_withGroupsAlreadyFormed_isRejected() {
        groupsTournamentWithTeams(8);
        when(tournamentGroupRepository.existsByTournamentId(TOURNAMENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.createGroups(TOURNAMENT_ID, request(2, 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya fueron formados");
    }

    @Test
    void create_withMatchesAlready_isRejected() {
        groupsTournamentWithTeams(8);
        when(partidoRepository.countByTorneoId(TOURNAMENT_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.createGroups(TOURNAMENT_ID, request(2, 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya tiene partidos");
    }

    @Test
    void create_aTeamBusyOnARoundDate_isRejectedAndCreatesNothing() {
        groupsTournamentWithTeams(8);
        doThrow(new BusinessException("El equipo T3 ya tiene un partido el 2027-03-13"))
                .when(matchDateConflicts).requireFree(any(LocalDate.class), any(Equipo.class));

        assertThatThrownBy(() -> service.createGroups(TOURNAMENT_ID, request(2, 2)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya tiene un partido");

        assertThat(storedGroups).isEmpty();
        verify(partidoRepository, never()).saveAll(any());
    }

    @Test
    void create_missingTournament_throwsNotFound() {
        when(tournamentRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createGroups(99L, request(2, 2))).isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- getGroups

    /** Groups A (teams 1, 2, 3) and B (teams 4, 5, 6), 2 qualifiers, already formed. */
    private void formedGroups() {
        Tournament tournament = tournament(TournamentFormat.GROUPS_KNOCKOUT, TournamentStatus.IN_PROGRESS);
        tournament.setQualifiersPerGroup(2);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(tournament));
        TournamentGroup a = group(10L, "A", tournament);
        TournamentGroup b = group(11L, "B", tournament);
        storedGroups.clear();
        storedGroups.addAll(Arrays.asList(a, b));
        enrollments = new ArrayList<>();
        for (long id = 1; id <= 6; id++) {
            TournamentTeam enrollment = new TournamentTeam();
            enrollment.setTournament(tournament);
            enrollment.setTeam(team(id));
            enrollment.setGroup(id <= 3 ? a : b);
            enrollments.add(enrollment);
        }
    }

    private TournamentGroup group(Long id, String name, Tournament tournament) {
        TournamentGroup group = new TournamentGroup();
        group.setId(id);
        group.setName(name);
        group.setTournament(tournament);
        return group;
    }

    private Partido finished(long home, long away, int homeGoals, int awayGoals) {
        Partido match = new Partido();
        match.setEquipoLocal(team(home));
        match.setEquipoVisitante(team(away));
        match.setGolesLocal(homeGoals);
        match.setGolesVisitante(awayGoals);
        match.setFase(Partido.Fase.FASE_DE_GRUPOS);
        match.setEstado(Partido.Estado.FINALIZADO);
        return match;
    }

    private void groupStageResults(List<Partido> matches, List<EstadisticaRepository.TeamCards> cards) {
        when(partidoRepository.findByTorneoIdAndFaseAndEstado(TOURNAMENT_ID, Partido.Fase.FASE_DE_GRUPOS, Partido.Estado.FINALIZADO))
                .thenReturn(matches);
        when(estadisticaRepository.sumarTarjetasPorEquipoEnTorneoYFase(TOURNAMENT_ID, Partido.Fase.FASE_DE_GRUPOS)).thenReturn(cards);
    }

    private EstadisticaRepository.TeamCards cards(long teamId, long yellow, long red) {
        return new EstadisticaRepository.TeamCards() {
            @Override
            public Long getTeamId() {
                return teamId;
            }

            @Override
            public Long getYellowCards() {
                return yellow;
            }

            @Override
            public Long getRedCards() {
                return red;
            }
        };
    }

    @Test
    void get_eachGroupTableCountsOnlyFinishedGroupMatchesAmongItsTeams() {
        formedGroups();
        // 1 beats 2, and a match between teams of different groups (3 vs 4) that must be ignored.
        groupStageResults(Arrays.asList(finished(1, 2, 2, 0), finished(3, 4, 5, 0)), Collections.emptyList());

        List<GroupDTO> groups = service.getGroups(TOURNAMENT_ID);

        List<StandingDTO> tableA = groups.get(0).getStandings();
        assertThat(tableA).extracting(StandingDTO::getTeamName).containsExactly("T1", "T3", "T2");
        assertThat(tableA.get(0).getPoints()).isEqualTo(3);
        assertThat(tableA.get(0).getGoalDifference()).isEqualTo(2);
        // Team 3's win over team 4 belongs to no group table.
        assertThat(tableA.stream().filter(r -> r.getTeamName().equals("T3")).findFirst().get().getPlayed()).isZero();
        assertThat(groups.get(1).getStandings()).extracting(StandingDTO::getPlayed).containsOnly(0);
    }

    @Test
    void get_aManualReplacementMatchInsideTheGroupCountsLikeAnyOther() {
        formedGroups();
        // The postponed 1 vs 3 was replaced by hand: both are in group A, phase FASE_DE_GRUPOS.
        groupStageResults(Collections.singletonList(finished(3, 1, 4, 0)), Collections.emptyList());

        List<StandingDTO> tableA = service.getGroups(TOURNAMENT_ID).get(0).getStandings();

        assertThat(tableA.get(0).getTeamName()).isEqualTo("T3");
        assertThat(tableA.get(0).getPoints()).isEqualTo(3);
    }

    @Test
    void get_fairPlayUsesOnlyTheCardsOfTheGroupStage() {
        formedGroups();
        groupStageResults(Collections.emptyList(), Collections.singletonList(cards(1, 3, 1)));

        List<StandingDTO> tableA = service.getGroups(TOURNAMENT_ID).get(0).getStandings();

        assertThat(tableA.stream().filter(r -> r.getTeamName().equals("T1")).findFirst().get().getFairPlayPoints()).isEqualTo(5);
    }

    @Test
    void get_reportsTheTeamsAndHowManyQualify() {
        formedGroups();
        groupStageResults(Collections.emptyList(), Collections.emptyList());

        List<GroupDTO> groups = service.getGroups(TOURNAMENT_ID);

        assertThat(groups).extracting(GroupDTO::getName).containsExactly("A", "B");
        assertThat(groups).extracting(GroupDTO::getQualifiersCount).containsExactly(2, 2);
        assertThat(teamIdsOf(groups.get(0))).containsExactlyInAnyOrder(1L, 2L, 3L);
        assertThat(teamIdsOf(groups.get(1))).containsExactlyInAnyOrder(4L, 5L, 6L);
    }

    @Test
    void get_aTournamentWithoutGroups_returnsAnEmptyList() {
        Tournament league = tournament(TournamentFormat.LEAGUE, TournamentStatus.IN_PROGRESS);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(league));

        assertThat(service.getGroups(TOURNAMENT_ID)).isEmpty();
    }

    @Test
    void get_missingTournament_throwsNotFound() {
        when(tournamentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getGroups(99L)).isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- qualifiers and stage state

    @Test
    void qualifiers_areTheFirstOfEachTableInGroupOrder() {
        formedGroups();
        // Group A: 2 beats 1 and 3, 1 beats 3 -> 2, 1, 3. Group B: 6 beats 4 and 5, 5 beats 4 -> 6, 5, 4.
        groupStageResults(Arrays.asList(finished(2, 1, 1, 0), finished(2, 3, 1, 0), finished(1, 3, 1, 0),
                finished(6, 4, 1, 0), finished(6, 5, 1, 0), finished(5, 4, 1, 0)), Collections.emptyList());

        List<List<Long>> qualifiers = service.qualifiers(TOURNAMENT_ID);

        assertThat(qualifiers).containsExactly(Arrays.asList(2L, 1L), Arrays.asList(6L, 5L));
    }

    @Test
    void hasGroupsAndPendingMatches_askTheRepositories() {
        when(tournamentGroupRepository.existsByTournamentId(TOURNAMENT_ID)).thenReturn(true);
        when(partidoRepository.countByTorneoIdAndFaseAndEstadoNot(TOURNAMENT_ID, Partido.Fase.FASE_DE_GRUPOS, Partido.Estado.FINALIZADO))
                .thenReturn(3L);

        assertThat(service.hasGroups(TOURNAMENT_ID)).isTrue();
        assertThat(service.pendingGroupMatches(TOURNAMENT_ID)).isEqualTo(3L);
    }
}
