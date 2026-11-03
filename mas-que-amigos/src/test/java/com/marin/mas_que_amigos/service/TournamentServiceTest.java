package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.dto.ScheduleRequestDTO;
import com.marin.mas_que_amigos.dto.StandingDTO;
import com.marin.mas_que_amigos.dto.TournamentDTO;
import com.marin.mas_que_amigos.dto.TournamentRequestDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.NotFoundException;
import com.marin.mas_que_amigos.mapper.EquipoMapper;
import com.marin.mas_que_amigos.mapper.PartidoMapper;
import com.marin.mas_que_amigos.mapper.TournamentMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.model.Tournament;
import com.marin.mas_que_amigos.model.TournamentFormat;
import com.marin.mas_que_amigos.model.TournamentStatus;
import com.marin.mas_que_amigos.model.TournamentTeam;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.EstadisticaRepository;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import com.marin.mas_que_amigos.repository.TournamentRepository;
import com.marin.mas_que_amigos.repository.TournamentTeamRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TournamentServiceTest {

    private static final Long TOURNAMENT_ID = 1L;

    @Mock
    private TournamentRepository tournamentRepository;
    @Mock
    private TournamentTeamRepository tournamentTeamRepository;
    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private PartidoRepository partidoRepository;
    @Mock
    private EquipoMapper equipoMapper;
    @Mock
    private PartidoMapper partidoMapper;
    @Mock
    private EstadisticaRepository estadisticaRepository;

    private TournamentService service;

    @BeforeEach
    void setUp() {
        service = new TournamentService(tournamentRepository, tournamentTeamRepository, equipoRepository,
                partidoRepository, new TournamentMapper(), equipoMapper, partidoMapper, estadisticaRepository);
    }

    private TournamentRequestDTO request(String name, String format) {
        TournamentRequestDTO request = new TournamentRequestDTO();
        request.setName(name);
        request.setFormat(format);
        return request;
    }

    private Tournament tournament(Long id, String name, TournamentFormat format, TournamentStatus status) {
        Tournament tournament = new Tournament();
        tournament.setId(id);
        tournament.setName(name);
        tournament.setFormat(format);
        tournament.setStatus(status);
        tournament.setCreatedAt(LocalDateTime.of(2026, 11, 2, 10, 0));
        return tournament;
    }

    private void saveReturnsArgumentWithId() {
        when(tournamentRepository.save(any(Tournament.class))).thenAnswer(invocation -> {
            Tournament saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(TOURNAMENT_ID);
            }
            return saved;
        });
    }

    // ---------------------------------------------------------------- create

    @Test
    void create_valid_savesDraftTournamentWithFormat() {
        when(tournamentRepository.existsByNameIgnoreCase("Copa Apertura")).thenReturn(false);
        saveReturnsArgumentWithId();

        TournamentDTO result = service.create(request("  Copa Apertura  ", "league"));

        ArgumentCaptor<Tournament> captor = ArgumentCaptor.forClass(Tournament.class);
        verify(tournamentRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("Copa Apertura");
        assertThat(captor.getValue().getFormat()).isEqualTo(TournamentFormat.LEAGUE);
        assertThat(captor.getValue().getStatus()).isEqualTo(TournamentStatus.DRAFT);
        assertThat(captor.getValue().getCreatedAt()).isNotNull();
        assertThat(captor.getValue().getStartedAt()).isNull();
        assertThat(result.getId()).isEqualTo(TOURNAMENT_ID);
        assertThat(result.getStatus()).isEqualTo(TournamentStatus.DRAFT);
        assertThat(result.getTeamCount()).isZero();
    }

    @Test
    void create_acceptsEveryRealFormat() {
        when(tournamentRepository.existsByNameIgnoreCase(any())).thenReturn(false);
        saveReturnsArgumentWithId();

        assertThat(service.create(request("Liga", "LEAGUE")).getFormat()).isEqualTo(TournamentFormat.LEAGUE);
        assertThat(service.create(request("Mundial", "GROUPS_KNOCKOUT")).getFormat())
                .isEqualTo(TournamentFormat.GROUPS_KNOCKOUT);
        assertThat(service.create(request("Copa", "KNOCKOUT")).getFormat()).isEqualTo(TournamentFormat.KNOCKOUT);
    }

    @Test
    void create_invalidFormat_isRejectedInSpanish() {
        assertThatThrownBy(() -> service.create(request("Copa Apertura", "ROUND_ROBIN")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("formato");

        verify(tournamentRepository, never()).save(any());
    }

    @Test
    void create_legacyFormat_isRejected() {
        assertThatThrownBy(() -> service.create(request("Copa Apertura", "LEGACY")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("formato");

        verify(tournamentRepository, never()).save(any());
    }

    @Test
    void create_repeatedNameIgnoringCase_isRejected() {
        when(tournamentRepository.existsByNameIgnoreCase("copa apertura")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("copa apertura", "LEAGUE")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya existe un torneo");

        verify(tournamentRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- findAll / findById

    @Test
    void findAll_returnsTournamentsWithTeamCount() {
        Tournament league = tournament(1L, "Liga", TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        Tournament cup = tournament(2L, "Copa", TournamentFormat.KNOCKOUT, TournamentStatus.IN_PROGRESS);
        when(tournamentRepository.findAllByOrderByNameAsc()).thenReturn(Arrays.asList(cup, league));
        when(tournamentTeamRepository.countByTournamentId(1L)).thenReturn(3L);
        when(tournamentTeamRepository.countByTournamentId(2L)).thenReturn(8L);

        List<TournamentDTO> result = service.findAll();

        assertThat(result).extracting(TournamentDTO::getName).containsExactly("Copa", "Liga");
        assertThat(result).extracting(TournamentDTO::getTeamCount).containsExactly(8, 3);
    }

    @Test
    void findById_existing_returnsTournament() {
        Tournament league = tournament(TOURNAMENT_ID, "Liga", TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(league));
        when(tournamentTeamRepository.countByTournamentId(TOURNAMENT_ID)).thenReturn(2L);

        TournamentDTO result = service.findById(TOURNAMENT_ID);

        assertThat(result.getName()).isEqualTo("Liga");
        assertThat(result.getTeamCount()).isEqualTo(2);
        assertThat(result.getCreatedAt()).isNotNull();
        assertThat(result.getStartedAt()).isNull();
    }

    @Test
    void findById_missing_throwsNotFound() {
        when(tournamentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("torneo");
    }

    // ---------------------------------------------------------------- update

    @Test
    void update_draft_changesNameAndFormat() {
        Tournament draft = tournament(TOURNAMENT_ID, "Liga", TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        when(tournamentRepository.findByIdForUpdate(TOURNAMENT_ID)).thenReturn(Optional.of(draft));
        when(tournamentRepository.existsByNameIgnoreCaseAndIdNot("Copa", TOURNAMENT_ID)).thenReturn(false);
        saveReturnsArgumentWithId();

        TournamentDTO result = service.update(TOURNAMENT_ID, request("Copa", "KNOCKOUT"));

        assertThat(result.getName()).isEqualTo("Copa");
        assertThat(result.getFormat()).isEqualTo(TournamentFormat.KNOCKOUT);
        assertThat(result.getStatus()).isEqualTo(TournamentStatus.DRAFT);
    }

    @Test
    void update_keepingItsOwnName_isAllowed() {
        Tournament draft = tournament(TOURNAMENT_ID, "Liga", TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        when(tournamentRepository.findByIdForUpdate(TOURNAMENT_ID)).thenReturn(Optional.of(draft));
        when(tournamentRepository.existsByNameIgnoreCaseAndIdNot("Liga", TOURNAMENT_ID)).thenReturn(false);
        saveReturnsArgumentWithId();

        TournamentDTO result = service.update(TOURNAMENT_ID, request("Liga", "KNOCKOUT"));

        assertThat(result.getFormat()).isEqualTo(TournamentFormat.KNOCKOUT);
    }

    @Test
    void update_repeatedNameOfAnotherTournament_isRejected() {
        Tournament draft = tournament(TOURNAMENT_ID, "Liga", TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        when(tournamentRepository.findByIdForUpdate(TOURNAMENT_ID)).thenReturn(Optional.of(draft));
        when(tournamentRepository.existsByNameIgnoreCaseAndIdNot("Copa", TOURNAMENT_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.update(TOURNAMENT_ID, request("Copa", "LEAGUE")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya existe un torneo");

        verify(tournamentRepository, never()).save(any());
    }

    @Test
    void update_notDraft_isRejectedAndKeepsData() {
        for (TournamentStatus status : Arrays.asList(TournamentStatus.IN_PROGRESS, TournamentStatus.FINISHED)) {
            Tournament started = tournament(TOURNAMENT_ID, "Liga", TournamentFormat.LEAGUE, status);
            when(tournamentRepository.findByIdForUpdate(TOURNAMENT_ID)).thenReturn(Optional.of(started));

            assertThatThrownBy(() -> service.update(TOURNAMENT_ID, request("Otro", "KNOCKOUT")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya inició");

            assertThat(started.getName()).isEqualTo("Liga");
            assertThat(started.getFormat()).isEqualTo(TournamentFormat.LEAGUE);
        }
        verify(tournamentRepository, never()).save(any());
    }

    @Test
    void update_missing_throwsNotFound() {
        when(tournamentRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(99L, request("Copa", "LEAGUE")))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- delete

    @Test
    void delete_draftWithEnrollments_removesEnrollmentsButNotTeams() {
        Tournament draft = tournament(TOURNAMENT_ID, "Liga", TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        when(tournamentRepository.findByIdForUpdate(TOURNAMENT_ID)).thenReturn(Optional.of(draft));

        service.delete(TOURNAMENT_ID);

        verify(tournamentTeamRepository).deleteByTournamentId(TOURNAMENT_ID);
        verify(tournamentRepository).delete(draft);
        verify(equipoRepository, never()).delete(any());
        verify(equipoRepository, never()).deleteById(any());
    }

    @Test
    void delete_notDraft_isRejected() {
        for (TournamentStatus status : Arrays.asList(TournamentStatus.IN_PROGRESS, TournamentStatus.FINISHED)) {
            Tournament started = tournament(TOURNAMENT_ID, "Liga", TournamentFormat.LEAGUE, status);
            when(tournamentRepository.findByIdForUpdate(TOURNAMENT_ID)).thenReturn(Optional.of(started));

            assertThatThrownBy(() -> service.delete(TOURNAMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya inició");
        }
        verify(tournamentRepository, never()).delete(any());
        verify(tournamentTeamRepository, never()).deleteByTournamentId(any());
    }

    @Test
    void delete_missing_throwsNotFound() {
        when(tournamentRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(99L)).isInstanceOf(NotFoundException.class);

        verify(tournamentRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------- enrollTeam

    private Equipo team(Long id, String name) {
        Equipo team = new Equipo();
        team.setId(id);
        team.setNombre(name);
        return team;
    }

    private Tournament lockedTournament(Long id, TournamentFormat format, TournamentStatus status) {
        Tournament tournament = tournament(id, "Torneo " + id, format, status);
        when(tournamentRepository.findByIdForUpdate(id)).thenReturn(Optional.of(tournament));
        return tournament;
    }

    @Test
    void enrollTeam_valid_createsEnrollment() {
        Tournament draft = lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        Equipo madrid = team(5L, "Real Madrid");
        EquipoDTO madridDto = new EquipoDTO();
        madridDto.setId(5L);
        when(equipoRepository.findById(5L)).thenReturn(Optional.of(madrid));
        when(tournamentTeamRepository.existsByTournamentIdAndTeamId(TOURNAMENT_ID, 5L)).thenReturn(false);
        when(equipoMapper.toDTO(madrid)).thenReturn(madridDto);

        EquipoDTO result = service.enrollTeam(TOURNAMENT_ID, 5L);

        ArgumentCaptor<TournamentTeam> captor = ArgumentCaptor.forClass(TournamentTeam.class);
        verify(tournamentTeamRepository).save(captor.capture());
        assertThat(captor.getValue().getTournament()).isSameAs(draft);
        assertThat(captor.getValue().getTeam()).isSameAs(madrid);
        assertThat(captor.getValue().getEnrolledAt()).isNotNull();
        assertThat(result.getId()).isEqualTo(5L);
    }

    @Test
    void enrollTeam_alreadyEnrolledInThatTournament_isRejected() {
        lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        when(equipoRepository.findById(5L)).thenReturn(Optional.of(team(5L, "Real Madrid")));
        when(tournamentTeamRepository.existsByTournamentIdAndTeamId(TOURNAMENT_ID, 5L)).thenReturn(true);

        assertThatThrownBy(() -> service.enrollTeam(TOURNAMENT_ID, 5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya está inscrito");

        verify(tournamentTeamRepository, never()).save(any());
    }

    @Test
    void enrollTeam_sameTeamInSeveralTournaments_isAllowed() {
        Equipo madrid = team(5L, "Real Madrid");
        lockedTournament(1L, TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        lockedTournament(2L, TournamentFormat.KNOCKOUT, TournamentStatus.DRAFT);
        when(equipoRepository.findById(5L)).thenReturn(Optional.of(madrid));
        when(tournamentTeamRepository.existsByTournamentIdAndTeamId(1L, 5L)).thenReturn(false);
        when(tournamentTeamRepository.existsByTournamentIdAndTeamId(2L, 5L)).thenReturn(false);

        service.enrollTeam(1L, 5L);
        service.enrollTeam(2L, 5L);

        verify(tournamentTeamRepository, times(2)).save(any(TournamentTeam.class));
    }

    @Test
    void enrollTeam_missingTeam_isRejected() {
        lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        when(equipoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enrollTeam(TOURNAMENT_ID, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("equipos existentes");

        verify(tournamentTeamRepository, never()).save(any());
    }

    @Test
    void enrollTeam_tournamentNotDraft_isRejected() {
        for (TournamentStatus status : Arrays.asList(TournamentStatus.IN_PROGRESS, TournamentStatus.FINISHED)) {
            lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, status);

            assertThatThrownBy(() -> service.enrollTeam(TOURNAMENT_ID, 5L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya inició");
        }
        verify(tournamentTeamRepository, never()).save(any());
    }

    @Test
    void enrollTeam_missingTournament_throwsNotFound() {
        when(tournamentRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enrollTeam(99L, 5L)).isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- withdrawTeam

    @Test
    void withdrawTeam_valid_removesOnlyThatEnrollment() {
        lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        TournamentTeam enrollment = new TournamentTeam();
        when(tournamentTeamRepository.findByTournamentIdAndTeamId(TOURNAMENT_ID, 5L))
                .thenReturn(Optional.of(enrollment));

        service.withdrawTeam(TOURNAMENT_ID, 5L);

        verify(tournamentTeamRepository).delete(enrollment);
        verify(tournamentTeamRepository, never()).deleteByTournamentId(any());
        verify(equipoRepository, never()).delete(any());
    }

    @Test
    void withdrawTeam_notEnrolled_throwsNotFound() {
        lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        when(tournamentTeamRepository.findByTournamentIdAndTeamId(TOURNAMENT_ID, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.withdrawTeam(TOURNAMENT_ID, 5L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("no está inscrito");

        verify(tournamentTeamRepository, never()).delete(any());
    }

    @Test
    void withdrawTeam_tournamentNotDraft_isRejected() {
        for (TournamentStatus status : Arrays.asList(TournamentStatus.IN_PROGRESS, TournamentStatus.FINISHED)) {
            lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, status);

            assertThatThrownBy(() -> service.withdrawTeam(TOURNAMENT_ID, 5L))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("ya inició");
        }
        verify(tournamentTeamRepository, never()).delete(any());
    }

    // ---------------------------------------------------------------- findTeams

    @Test
    void findTeams_returnsEnrolledTeamsInRepositoryOrder() {
        Equipo barcelona = team(1L, "Barcelona");
        Equipo madrid = team(2L, "Real Madrid");
        EquipoDTO barcelonaDto = new EquipoDTO();
        barcelonaDto.setNombre("Barcelona");
        EquipoDTO madridDto = new EquipoDTO();
        madridDto.setNombre("Real Madrid");
        when(tournamentRepository.existsById(TOURNAMENT_ID)).thenReturn(true);
        when(tournamentTeamRepository.findTeamsByTournamentId(TOURNAMENT_ID))
                .thenReturn(Arrays.asList(barcelona, madrid));
        when(equipoMapper.toDTO(barcelona)).thenReturn(barcelonaDto);
        when(equipoMapper.toDTO(madrid)).thenReturn(madridDto);

        List<EquipoDTO> result = service.findTeams(TOURNAMENT_ID);

        assertThat(result).extracting(EquipoDTO::getNombre).containsExactly("Barcelona", "Real Madrid");
    }

    @Test
    void findTeams_tournamentWithoutEnrollments_returnsEmptyList() {
        when(tournamentRepository.existsById(TOURNAMENT_ID)).thenReturn(true);
        when(tournamentTeamRepository.findTeamsByTournamentId(TOURNAMENT_ID)).thenReturn(Collections.emptyList());

        assertThat(service.findTeams(TOURNAMENT_ID)).isEmpty();
    }

    @Test
    void findTeams_missingTournament_throwsNotFound() {
        when(tournamentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findTeams(99L)).isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- start

    @Test
    void start_withAtLeastTwoTeams_movesToInProgressAndRegistersStartDate() {
        Tournament draft = lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        when(tournamentTeamRepository.countByTournamentId(TOURNAMENT_ID)).thenReturn(4L);
        saveReturnsArgumentWithId();

        TournamentDTO result = service.start(TOURNAMENT_ID);

        assertThat(draft.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
        assertThat(draft.getStartedAt()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getTeamCount()).isEqualTo(4);
    }

    @Test
    void start_withFewerThanTwoTeams_isRejectedAndStaysDraft() {
        Tournament draft = lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, TournamentStatus.DRAFT);
        when(tournamentTeamRepository.countByTournamentId(TOURNAMENT_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.start(TOURNAMENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("al menos 2 equipos");

        assertThat(draft.getStatus()).isEqualTo(TournamentStatus.DRAFT);
        assertThat(draft.getStartedAt()).isNull();
        verify(tournamentRepository, never()).save(any());
    }

    @Test
    void start_notDraft_isRejected() {
        for (TournamentStatus status : Arrays.asList(TournamentStatus.IN_PROGRESS, TournamentStatus.FINISHED)) {
            lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, status);

            assertThatThrownBy(() -> service.start(TOURNAMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("borrador");
        }
        verify(tournamentRepository, never()).save(any());
    }

    @Test
    void start_missingTournament_throwsNotFound() {
        when(tournamentRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.start(99L)).isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- finish

    @Test
    void finish_withAllMatchesFinished_movesToFinished() {
        Tournament inProgress = lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, TournamentStatus.IN_PROGRESS);
        when(partidoRepository.countByTorneoIdAndEstadoNot(TOURNAMENT_ID, Partido.Estado.FINALIZADO)).thenReturn(0L);
        saveReturnsArgumentWithId();

        TournamentDTO result = service.finish(TOURNAMENT_ID);

        assertThat(inProgress.getStatus()).isEqualTo(TournamentStatus.FINISHED);
        assertThat(result.getStatus()).isEqualTo(TournamentStatus.FINISHED);
    }

    @Test
    void finish_withPendingMatches_isRejectedWithTheCount() {
        Tournament inProgress = lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, TournamentStatus.IN_PROGRESS);
        when(partidoRepository.countByTorneoIdAndEstadoNot(TOURNAMENT_ID, Partido.Estado.FINALIZADO)).thenReturn(3L);

        assertThatThrownBy(() -> service.finish(TOURNAMENT_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("3 partido(s)");

        assertThat(inProgress.getStatus()).isEqualTo(TournamentStatus.IN_PROGRESS);
        verify(tournamentRepository, never()).save(any());
    }

    @Test
    void finish_notInProgress_isRejected() {
        for (TournamentStatus status : Arrays.asList(TournamentStatus.DRAFT, TournamentStatus.FINISHED)) {
            lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, status);

            assertThatThrownBy(() -> service.finish(TOURNAMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no está en curso");
        }
        verify(tournamentRepository, never()).save(any());
    }

    // ---------------------------------------------------------------- resolveForNewMatch

    @Test
    void resolveForNewMatch_withoutTournament_returnsTheLegacyOne() {
        Tournament legacy = tournament(1L, "Histórico", TournamentFormat.LEGACY, TournamentStatus.IN_PROGRESS);
        when(tournamentRepository.findFirstByFormat(TournamentFormat.LEGACY)).thenReturn(Optional.of(legacy));

        Tournament result = service.resolveForNewMatch(null, team(1L, "A"), team(2L, "B"));

        assertThat(result).isSameAs(legacy);
        verify(tournamentTeamRepository, never()).existsByTournamentIdAndTeamId(any(), any());
    }

    @Test
    void resolveForNewMatch_legacyTournament_doesNotRequireEnrollment() {
        Tournament legacy = lockedTournament(1L, TournamentFormat.LEGACY, TournamentStatus.IN_PROGRESS);

        Tournament result = service.resolveForNewMatch(1L, team(1L, "A"), team(2L, "B"));

        assertThat(result).isSameAs(legacy);
        verify(tournamentTeamRepository, never()).existsByTournamentIdAndTeamId(any(), any());
    }

    @Test
    void resolveForNewMatch_inProgressWithEnrolledTeams_returnsTheTournamentInAnyFormat() {
        for (TournamentFormat format : Arrays.asList(TournamentFormat.LEAGUE, TournamentFormat.GROUPS_KNOCKOUT,
                TournamentFormat.KNOCKOUT)) {
            Tournament inProgress = lockedTournament(TOURNAMENT_ID, format, TournamentStatus.IN_PROGRESS);
            when(tournamentTeamRepository.existsByTournamentIdAndTeamId(TOURNAMENT_ID, 1L)).thenReturn(true);
            when(tournamentTeamRepository.existsByTournamentIdAndTeamId(TOURNAMENT_ID, 3L)).thenReturn(true);

            // A vs C replacing a postponed A vs B: any pair of enrolled teams is valid.
            assertThat(service.resolveForNewMatch(TOURNAMENT_ID, team(1L, "A"), team(3L, "C")))
                    .isSameAs(inProgress);
        }
    }

    @Test
    void resolveForNewMatch_unknownTournament_isRejected() {
        when(tournamentRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveForNewMatch(99L, team(1L, "A"), team(2L, "B")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("torneo");
    }

    @Test
    void resolveForNewMatch_tournamentNotInProgress_isRejected() {
        for (TournamentStatus status : Arrays.asList(TournamentStatus.DRAFT, TournamentStatus.FINISHED)) {
            lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, status);

            assertThatThrownBy(() -> service.resolveForNewMatch(TOURNAMENT_ID, team(1L, "A"), team(2L, "B")))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no está en curso");
        }
    }

    @Test
    void resolveForNewMatch_teamNotEnrolled_isRejectedNamingTheTeam() {
        lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, TournamentStatus.IN_PROGRESS);
        when(tournamentTeamRepository.existsByTournamentIdAndTeamId(TOURNAMENT_ID, 1L)).thenReturn(true);
        when(tournamentTeamRepository.existsByTournamentIdAndTeamId(TOURNAMENT_ID, 2L)).thenReturn(false);

        assertThatThrownBy(() -> service.resolveForNewMatch(TOURNAMENT_ID, team(1L, "A"), team(2L, "B")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("B")
                .hasMessageContaining("no está inscrito");
    }

    @Test
    void validateTeamsEnrolled_legacyTournament_neverFails() {
        Tournament legacy = tournament(1L, "Histórico", TournamentFormat.LEGACY, TournamentStatus.IN_PROGRESS);

        service.validateTeamsEnrolled(legacy, team(1L, "A"), team(2L, "B"));

        verify(tournamentTeamRepository, never()).existsByTournamentIdAndTeamId(any(), any());
    }

    // ---------------------------------------------------------------- findMatches

    @Test
    void findMatches_returnsTheTournamentMatchesInRepositoryOrder() {
        Partido first = new Partido();
        first.setId(10L);
        Partido second = new Partido();
        second.setId(11L);
        PartidoDTO firstDto = new PartidoDTO();
        firstDto.setId(10L);
        PartidoDTO secondDto = new PartidoDTO();
        secondDto.setId(11L);
        when(tournamentRepository.existsById(TOURNAMENT_ID)).thenReturn(true);
        when(partidoRepository.findByTorneoIdOrderByFechaAscHoraAsc(TOURNAMENT_ID))
                .thenReturn(Arrays.asList(first, second));
        when(partidoMapper.toDTO(first)).thenReturn(firstDto);
        when(partidoMapper.toDTO(second)).thenReturn(secondDto);

        assertThat(service.findMatches(TOURNAMENT_ID)).extracting(PartidoDTO::getId).containsExactly(10L, 11L);
    }

    @Test
    void findMatches_tournamentWithoutMatches_returnsEmptyList() {
        when(tournamentRepository.existsById(TOURNAMENT_ID)).thenReturn(true);
        when(partidoRepository.findByTorneoIdOrderByFechaAscHoraAsc(TOURNAMENT_ID))
                .thenReturn(Collections.emptyList());

        assertThat(service.findMatches(TOURNAMENT_ID)).isEmpty();
    }

    @Test
    void findMatches_missingTournament_throwsNotFound() {
        when(tournamentRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> service.findMatches(99L)).isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- standings (league-standings)

    private Partido finishedMatch(long homeId, long awayId, int homeGoals, int awayGoals) {
        Partido match = new Partido();
        match.setEquipoLocal(team(homeId, "T" + homeId));
        match.setEquipoVisitante(team(awayId, "T" + awayId));
        match.setGolesLocal(homeGoals);
        match.setGolesVisitante(awayGoals);
        match.setEstado(Partido.Estado.FINALIZADO);
        return match;
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

    private void leagueWithTeams(TournamentStatus status, Equipo... teams) {
        Tournament league = tournament(TOURNAMENT_ID, "Liga", TournamentFormat.LEAGUE, status);
        when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(league));
        when(tournamentTeamRepository.findTeamsByTournamentId(TOURNAMENT_ID)).thenReturn(Arrays.asList(teams));
    }

    @Test
    void standings_countsOnlyFinishedMatchesOfTheTournament() {
        leagueWithTeams(TournamentStatus.IN_PROGRESS, team(1L, "A"), team(2L, "B"), team(3L, "C"));
        when(partidoRepository.findByTorneoIdAndEstado(TOURNAMENT_ID, Partido.Estado.FINALIZADO))
                .thenReturn(Arrays.asList(finishedMatch(1, 2, 2, 0), finishedMatch(1, 3, 1, 1)));
        when(estadisticaRepository.sumarTarjetasPorEquipoEnTorneo(TOURNAMENT_ID)).thenReturn(Collections.emptyList());

        List<StandingDTO> table = service.standings(TOURNAMENT_ID);

        assertThat(table).extracting(StandingDTO::getTeamName).containsExactly("A", "C", "B");
        assertThat(table).extracting(StandingDTO::getPoints).containsExactly(4, 1, 0);
        // Programmed and in-progress matches are never requested, so they cannot count.
        verify(partidoRepository, never()).findByTorneoIdOrderByFechaAscHoraAsc(any());
    }

    @Test
    void standings_aManualReplacementMatchCountsLikeAnyOther() {
        // A vs B was postponed and A vs C was created by hand and finished.
        leagueWithTeams(TournamentStatus.IN_PROGRESS, team(1L, "A"), team(2L, "B"), team(3L, "C"));
        when(partidoRepository.findByTorneoIdAndEstado(TOURNAMENT_ID, Partido.Estado.FINALIZADO))
                .thenReturn(Collections.singletonList(finishedMatch(1, 3, 3, 0)));
        when(estadisticaRepository.sumarTarjetasPorEquipoEnTorneo(TOURNAMENT_ID)).thenReturn(Collections.emptyList());

        List<StandingDTO> table = service.standings(TOURNAMENT_ID);

        StandingDTO a = table.get(0);
        assertThat(a.getTeamName()).isEqualTo("A");
        assertThat(a.getPlayed()).isEqualTo(1);
        assertThat(a.getPoints()).isEqualTo(3);
        assertThat(a.getGoalDifference()).isEqualTo(3);
    }

    @Test
    void standings_includesEnrolledTeamsWithoutMatches() {
        leagueWithTeams(TournamentStatus.DRAFT, team(1L, "A"), team(2L, "B"));
        when(partidoRepository.findByTorneoIdAndEstado(TOURNAMENT_ID, Partido.Estado.FINALIZADO))
                .thenReturn(Collections.emptyList());
        when(estadisticaRepository.sumarTarjetasPorEquipoEnTorneo(TOURNAMENT_ID)).thenReturn(Collections.emptyList());

        List<StandingDTO> table = service.standings(TOURNAMENT_ID);

        assertThat(table).extracting(StandingDTO::getTeamName).containsExactly("A", "B");
        assertThat(table).extracting(StandingDTO::getPlayed).containsOnly(0);
        assertThat(table).extracting(StandingDTO::getPosition).containsExactly(1, 2);
    }

    @Test
    void standings_fairPlayPointsAreYellowTimesOnePlusRedTimesTwo() {
        leagueWithTeams(TournamentStatus.IN_PROGRESS, team(1L, "A"), team(2L, "B"));
        when(partidoRepository.findByTorneoIdAndEstado(TOURNAMENT_ID, Partido.Estado.FINALIZADO))
                .thenReturn(Collections.emptyList());
        when(estadisticaRepository.sumarTarjetasPorEquipoEnTorneo(TOURNAMENT_ID))
                .thenReturn(Collections.singletonList(cards(1L, 3, 1)));

        List<StandingDTO> table = service.standings(TOURNAMENT_ID);

        StandingDTO a = table.stream().filter(r -> r.getTeamName().equals("A")).findFirst().get();
        StandingDTO b = table.stream().filter(r -> r.getTeamName().equals("B")).findFirst().get();
        assertThat(a.getFairPlayPoints()).isEqualTo(5);
        assertThat(b.getFairPlayPoints()).isZero();
    }

    @Test
    void standings_notALeague_isRejected() {
        for (TournamentFormat format : Arrays.asList(TournamentFormat.KNOCKOUT, TournamentFormat.GROUPS_KNOCKOUT,
                TournamentFormat.LEGACY)) {
            Tournament other = tournament(TOURNAMENT_ID, "Otro", format, TournamentStatus.IN_PROGRESS);
            when(tournamentRepository.findById(TOURNAMENT_ID)).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> service.standings(TOURNAMENT_ID))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("tabla de posiciones");
        }
    }

    @Test
    void standings_missingTournament_throwsNotFound() {
        when(tournamentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.standings(99L)).isInstanceOf(NotFoundException.class);
    }

    // ---------------------------------------------------------------- generateSchedule (league-schedule)

    private ScheduleRequestDTO scheduleRequest(LocalDate startDate, LocalTime time, Integer intervalDays) {
        ScheduleRequestDTO request = new ScheduleRequestDTO();
        request.setStartDate(startDate);
        request.setTime(time);
        request.setIntervalDays(intervalDays);
        return request;
    }

    private Tournament leagueReadyForSchedule(int teamCount) {
        Tournament league = lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, TournamentStatus.IN_PROGRESS);
        List<Equipo> teams = new java.util.ArrayList<>();
        for (long id = 1; id <= teamCount; id++) {
            teams.add(team(id, "Team " + id));
        }
        when(tournamentTeamRepository.findTeamsByTournamentId(TOURNAMENT_ID)).thenReturn(teams);
        return league;
    }

    @SuppressWarnings("unchecked")
    private List<Partido> savedMatches() {
        ArgumentCaptor<List<Partido>> captor = ArgumentCaptor.forClass(List.class);
        verify(partidoRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    private void saveAllReturnsItsArgument() {
        when(partidoRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(partidoMapper.toDTO(any(Partido.class))).thenAnswer(invocation -> new PartidoDTO());
    }

    @Test
    void generateSchedule_valid_createsEveryPairOnceInRoundsOneIntervalApart() {
        Tournament league = leagueReadyForSchedule(4);
        when(partidoRepository.countByTorneoId(TOURNAMENT_ID)).thenReturn(0L);
        saveAllReturnsItsArgument();

        List<PartidoDTO> result = service.generateSchedule(TOURNAMENT_ID,
                scheduleRequest(LocalDate.of(2027, 3, 6), LocalTime.of(15, 0), null));

        assertThat(result).hasSize(6);
        List<Partido> saved = savedMatches();
        assertThat(saved).hasSize(6);
        assertThat(saved).extracting(Partido::getFecha).containsOnly(
                LocalDate.of(2027, 3, 6), LocalDate.of(2027, 3, 13), LocalDate.of(2027, 3, 20));
        assertThat(saved).extracting(Partido::getHora).containsOnly(LocalTime.of(15, 0));
        assertThat(saved).extracting(Partido::getEstado).containsOnly(Partido.Estado.PROGRAMADO);
        assertThat(saved).extracting(Partido::getFase).containsOnly(Partido.Fase.FASE_DE_GRUPOS);
        assertThat(saved).extracting(Partido::getTorneo).containsOnly(league);
        assertThat(saved).extracting(Partido::getGolesLocal).containsOnly(0);
        assertThat(saved).extracting(Partido::getGolesVisitante).containsOnly(0);
        // Every pair of the 4 teams appears exactly once.
        java.util.Set<String> pairs = new java.util.HashSet<>();
        for (Partido match : saved) {
            long a = match.getEquipoLocal().getId();
            long b = match.getEquipoVisitante().getId();
            pairs.add(Math.min(a, b) + "-" + Math.max(a, b));
        }
        assertThat(pairs).hasSize(6);
    }

    @Test
    void generateSchedule_customInterval_separatesTheRoundsByThoseDays() {
        leagueReadyForSchedule(4);
        when(partidoRepository.countByTorneoId(TOURNAMENT_ID)).thenReturn(0L);
        saveAllReturnsItsArgument();

        service.generateSchedule(TOURNAMENT_ID, scheduleRequest(LocalDate.of(2027, 3, 6), LocalTime.of(15, 0), 3));

        assertThat(savedMatches()).extracting(Partido::getFecha).containsOnly(
                LocalDate.of(2027, 3, 6), LocalDate.of(2027, 3, 9), LocalDate.of(2027, 3, 12));
    }

    @Test
    void generateSchedule_notALeague_isRejected() {
        for (TournamentFormat format : Arrays.asList(TournamentFormat.KNOCKOUT, TournamentFormat.GROUPS_KNOCKOUT,
                TournamentFormat.LEGACY)) {
            lockedTournament(TOURNAMENT_ID, format, TournamentStatus.IN_PROGRESS);

            assertThatThrownBy(() -> service.generateSchedule(TOURNAMENT_ID,
                    scheduleRequest(LocalDate.of(2027, 3, 6), LocalTime.of(15, 0), null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("solo los torneos de liga");
        }
        verify(partidoRepository, never()).saveAll(any());
    }

    @Test
    void generateSchedule_notInProgress_isRejected() {
        for (TournamentStatus status : Arrays.asList(TournamentStatus.DRAFT, TournamentStatus.FINISHED)) {
            lockedTournament(TOURNAMENT_ID, TournamentFormat.LEAGUE, status);

            assertThatThrownBy(() -> service.generateSchedule(TOURNAMENT_ID,
                    scheduleRequest(LocalDate.of(2027, 3, 6), LocalTime.of(15, 0), null)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("no está en curso");
        }
        verify(partidoRepository, never()).saveAll(any());
    }

    @Test
    void generateSchedule_tournamentWithMatches_isRejectedAndCreatesNothing() {
        leagueReadyForSchedule(4);
        when(partidoRepository.countByTorneoId(TOURNAMENT_ID)).thenReturn(1L);

        assertThatThrownBy(() -> service.generateSchedule(TOURNAMENT_ID,
                scheduleRequest(LocalDate.of(2027, 3, 6), LocalTime.of(15, 0), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya tiene partidos");

        verify(partidoRepository, never()).saveAll(any());
    }

    @Test
    void generateSchedule_fewerThanTwoTeams_isRejected() {
        leagueReadyForSchedule(1);

        assertThatThrownBy(() -> service.generateSchedule(TOURNAMENT_ID,
                scheduleRequest(LocalDate.of(2027, 3, 6), LocalTime.of(15, 0), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("al menos 2 equipos");

        verify(partidoRepository, never()).saveAll(any());
    }

    @Test
    void generateSchedule_missingTournament_throwsNotFound() {
        when(tournamentRepository.findByIdForUpdate(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.generateSchedule(99L,
                scheduleRequest(LocalDate.of(2027, 3, 6), LocalTime.of(15, 0), null)))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void generateSchedule_teamBusyOnARoundDate_isRejectedNamingTheTeamAndDateWithoutCreatingAnything() {
        leagueReadyForSchedule(4);
        when(partidoRepository.countByTorneoId(TOURNAMENT_ID)).thenReturn(0L);
        // Strict stubs reject calls with other arguments, so the default is stubbed first.
        lenient().when(partidoRepository.existePartidoEnFechaParaEquipo(any(), any())).thenReturn(false);
        when(partidoRepository.existePartidoEnFechaParaEquipo(LocalDate.of(2027, 3, 13), 2L)).thenReturn(true);

        assertThatThrownBy(() -> service.generateSchedule(TOURNAMENT_ID,
                scheduleRequest(LocalDate.of(2027, 3, 6), LocalTime.of(15, 0), null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Team 2")
                .hasMessageContaining("2027-03-13");

        verify(partidoRepository, never()).saveAll(any());
    }

    // ---------------------------------------------------------------- protected "Histórico"

    @Test
    void legacyTournament_cannotBeEditedDeletedStartedFinishedOrEnrolled() {
        lockedTournament(TOURNAMENT_ID, TournamentFormat.LEGACY, TournamentStatus.IN_PROGRESS);

        assertThatThrownBy(() -> service.update(TOURNAMENT_ID, request("Otro", "LEAGUE")))
                .isInstanceOf(BusinessException.class).hasMessageContaining("protegido");
        assertThatThrownBy(() -> service.delete(TOURNAMENT_ID))
                .isInstanceOf(BusinessException.class).hasMessageContaining("protegido");
        assertThatThrownBy(() -> service.start(TOURNAMENT_ID))
                .isInstanceOf(BusinessException.class).hasMessageContaining("protegido");
        assertThatThrownBy(() -> service.finish(TOURNAMENT_ID))
                .isInstanceOf(BusinessException.class).hasMessageContaining("protegido");
        assertThatThrownBy(() -> service.enrollTeam(TOURNAMENT_ID, 5L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("inscripción");
        assertThatThrownBy(() -> service.withdrawTeam(TOURNAMENT_ID, 5L))
                .isInstanceOf(BusinessException.class).hasMessageContaining("inscripción");

        verify(tournamentRepository, never()).save(any());
        verify(tournamentRepository, never()).delete(any());
        verify(tournamentTeamRepository, never()).save(any());
    }
}
