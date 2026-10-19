package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marin.mas_que_amigos.dto.TransferDTO;
import com.marin.mas_que_amigos.dto.TransferRequestDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.JugadorNotFoundException;
import com.marin.mas_que_amigos.exception.TransferNotFoundException;
import com.marin.mas_que_amigos.mapper.PlayerTransferMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.model.PlayerTransfer;
import com.marin.mas_que_amigos.model.TransferStatus;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.JugadorRepository;
import com.marin.mas_que_amigos.repository.PlayerTransferRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlayerTransferServiceTest {

    private static final Long ORIGIN_TEAM_ID = 1L;
    private static final Long DESTINATION_TEAM_ID = 2L;
    private static final Long PLAYER_ID = 10L;

    @Mock
    private PlayerTransferRepository transferRepository;
    @Mock
    private JugadorRepository jugadorRepository;
    @Mock
    private EquipoRepository equipoRepository;

    private PlayerTransferService service;

    private Equipo originTeam;
    private Equipo destinationTeam;
    private Jugador player;

    @BeforeEach
    void setUp() {
        service = new PlayerTransferService(transferRepository, jugadorRepository, equipoRepository,
                new PlayerTransferMapper());

        originTeam = team(ORIGIN_TEAM_ID, "Macalister");
        destinationTeam = team(DESTINATION_TEAM_ID, "Real Madrid");
        player = new Jugador();
        player.setId(PLAYER_ID);
        player.setNombre("Jhon");
        player.setDorsal(7);
        player.setCosto(new BigDecimal("1500.00"));
        player.setEquipo(originTeam);
    }

    private Equipo team(Long id, String name) {
        Equipo team = new Equipo();
        team.setId(id);
        team.setNombre(name);
        return team;
    }

    private TransferRequestDTO request(Long playerId, Long destinationTeamId, Integer destinationDorsal) {
        TransferRequestDTO request = new TransferRequestDTO();
        request.setPlayerId(playerId);
        request.setDestinationTeamId(destinationTeamId);
        request.setDestinationDorsal(destinationDorsal);
        return request;
    }

    // ---------------------------------------------------------------- requestTransfer

    @Test
    void requestTransfer_valid_createsPendingTransferWithCostSnapshotAndOriginTeam() {
        when(jugadorRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
        when(equipoRepository.findById(DESTINATION_TEAM_ID)).thenReturn(Optional.of(destinationTeam));
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(7, DESTINATION_TEAM_ID, PLAYER_ID)).thenReturn(false);
        when(transferRepository.save(any(PlayerTransfer.class))).thenAnswer(invocation -> {
            PlayerTransfer saved = invocation.getArgument(0);
            saved.setId(100L);
            return saved;
        });

        TransferDTO result = service.requestTransfer(request(PLAYER_ID, DESTINATION_TEAM_ID, null));

        ArgumentCaptor<PlayerTransfer> captor = ArgumentCaptor.forClass(PlayerTransfer.class);
        verify(transferRepository).save(captor.capture());
        PlayerTransfer saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TransferStatus.PENDING);
        assertThat(saved.getPlayer()).isSameAs(player);
        assertThat(saved.getOriginTeam()).isSameAs(originTeam);
        assertThat(saved.getDestinationTeam()).isSameAs(destinationTeam);
        assertThat(saved.getCost()).isEqualByComparingTo("1500.00");
        assertThat(saved.getDestinationDorsal()).isNull();
        assertThat(saved.getRequestedAt()).isNotNull();
        assertThat(saved.getResolvedAt()).isNull();

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo(TransferStatus.PENDING);
        assertThat(result.getPlayerName()).isEqualTo("Jhon");
        assertThat(result.getOriginTeamName()).isEqualTo("Macalister");
        assertThat(result.getDestinationTeamName()).isEqualTo("Real Madrid");
        assertThat(result.getCost()).isEqualByComparingTo("1500.00");
        // La solicitud no mueve al jugador.
        assertThat(player.getEquipo()).isSameAs(originTeam);
        verify(jugadorRepository, never()).save(any(Jugador.class));
    }

    @Test
    void requestTransfer_costIsASnapshot_laterEditsToThePlayerCostDoNotChangeIt() {
        when(jugadorRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
        when(equipoRepository.findById(DESTINATION_TEAM_ID)).thenReturn(Optional.of(destinationTeam));
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(7, DESTINATION_TEAM_ID, PLAYER_ID)).thenReturn(false);
        when(transferRepository.save(any(PlayerTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferDTO result = service.requestTransfer(request(PLAYER_ID, DESTINATION_TEAM_ID, null));
        player.setCosto(new BigDecimal("9999.00"));

        assertThat(result.getCost()).isEqualByComparingTo("1500.00");
        ArgumentCaptor<PlayerTransfer> captor = ArgumentCaptor.forClass(PlayerTransfer.class);
        verify(transferRepository).save(captor.capture());
        assertThat(captor.getValue().getCost()).isEqualByComparingTo("1500.00");
    }

    @Test
    void requestTransfer_sameTeamAsCurrent_throwsBusinessException() {
        when(jugadorRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
        when(equipoRepository.findById(ORIGIN_TEAM_ID)).thenReturn(Optional.of(originTeam));

        assertThatThrownBy(() -> service.requestTransfer(request(PLAYER_ID, ORIGIN_TEAM_ID, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya pertenece");
        verify(transferRepository, never()).save(any(PlayerTransfer.class));
    }

    @Test
    void requestTransfer_playerNotFound_throwsJugadorNotFoundException() {
        when(jugadorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestTransfer(request(99L, DESTINATION_TEAM_ID, null)))
                .isInstanceOf(JugadorNotFoundException.class);
        verify(transferRepository, never()).save(any(PlayerTransfer.class));
    }

    @Test
    void requestTransfer_destinationTeamNotFound_throwsBusinessException() {
        when(jugadorRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
        when(equipoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requestTransfer(request(PLAYER_ID, 99L, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("equipos existentes");
        verify(transferRepository, never()).save(any(PlayerTransfer.class));
    }

    @Test
    void requestTransfer_whenPlayerAlreadyHasPendingRequests_createsAnotherOne() {
        when(jugadorRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
        when(equipoRepository.findById(DESTINATION_TEAM_ID)).thenReturn(Optional.of(destinationTeam));
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(7, DESTINATION_TEAM_ID, PLAYER_ID)).thenReturn(false);
        when(transferRepository.save(any(PlayerTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferDTO first = service.requestTransfer(request(PLAYER_ID, DESTINATION_TEAM_ID, null));
        TransferDTO second = service.requestTransfer(request(PLAYER_ID, DESTINATION_TEAM_ID, null));

        assertThat(first.getStatus()).isEqualTo(TransferStatus.PENDING);
        assertThat(second.getStatus()).isEqualTo(TransferStatus.PENDING);
        verify(transferRepository, org.mockito.Mockito.times(2)).save(any(PlayerTransfer.class));
    }

    @Test
    void requestTransfer_dorsalTakenInDestinationWithoutNewDorsal_throwsBusinessException() {
        when(jugadorRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
        when(equipoRepository.findById(DESTINATION_TEAM_ID)).thenReturn(Optional.of(destinationTeam));
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(7, DESTINATION_TEAM_ID, PLAYER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.requestTransfer(request(PLAYER_ID, DESTINATION_TEAM_ID, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dorsal 7");
        verify(transferRepository, never()).save(any(PlayerTransfer.class));
    }

    @Test
    void requestTransfer_withFreeNewDorsal_storesItInTheRequest() {
        when(jugadorRepository.findById(PLAYER_ID)).thenReturn(Optional.of(player));
        when(equipoRepository.findById(DESTINATION_TEAM_ID)).thenReturn(Optional.of(destinationTeam));
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(11, DESTINATION_TEAM_ID, PLAYER_ID)).thenReturn(false);
        when(transferRepository.save(any(PlayerTransfer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TransferDTO result = service.requestTransfer(request(PLAYER_ID, DESTINATION_TEAM_ID, 11));

        assertThat(result.getDestinationDorsal()).isEqualTo(11);
    }

    // ---------------------------------------------------------------- confirmTransfer

    private PlayerTransfer pendingTransfer(Long id, Integer destinationDorsal) {
        PlayerTransfer transfer = new PlayerTransfer();
        transfer.setId(id);
        transfer.setPlayer(player);
        transfer.setOriginTeam(originTeam);
        transfer.setDestinationTeam(destinationTeam);
        transfer.setCost(new BigDecimal("1500.00"));
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setDestinationDorsal(destinationDorsal);
        transfer.setRequestedAt(java.time.LocalDateTime.now());
        return transfer;
    }

    private void stubLockedLookups(PlayerTransfer transfer) {
        when(transferRepository.findPlayerIdById(transfer.getId())).thenReturn(Optional.of(PLAYER_ID));
        when(jugadorRepository.findByIdForUpdate(PLAYER_ID)).thenReturn(Optional.of(player));
        when(transferRepository.findByIdForUpdate(transfer.getId())).thenReturn(Optional.of(transfer));
    }

    @Test
    void confirmTransfer_valid_movesPlayerAndMarksTransferConfirmed() {
        PlayerTransfer transfer = pendingTransfer(100L, null);
        stubLockedLookups(transfer);
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(7, DESTINATION_TEAM_ID, PLAYER_ID)).thenReturn(false);
        when(transferRepository.save(transfer)).thenReturn(transfer);

        TransferDTO result = service.confirmTransfer(100L);

        assertThat(player.getEquipo()).isSameAs(destinationTeam);
        assertThat(player.getDorsal()).isEqualTo(7);
        verify(jugadorRepository).saveAndFlush(player);
        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.CONFIRMED);
        assertThat(transfer.getResolvedAt()).isNotNull();
        assertThat(result.getStatus()).isEqualTo(TransferStatus.CONFIRMED);
        assertThat(result.getResolvedAt()).isNotNull();
    }

    @Test
    void confirmTransfer_withNewDorsal_appliesItToThePlayer() {
        PlayerTransfer transfer = pendingTransfer(100L, 11);
        stubLockedLookups(transfer);
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(11, DESTINATION_TEAM_ID, PLAYER_ID)).thenReturn(false);
        when(transferRepository.save(transfer)).thenReturn(transfer);

        service.confirmTransfer(100L);

        assertThat(player.getDorsal()).isEqualTo(11);
        assertThat(player.getEquipo()).isSameAs(destinationTeam);
    }

    @Test
    void confirmTransfer_dorsalTakenAtConfirmation_failsWithoutChangingAnything() {
        PlayerTransfer transfer = pendingTransfer(100L, null);
        stubLockedLookups(transfer);
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(7, DESTINATION_TEAM_ID, PLAYER_ID)).thenReturn(true);

        assertThatThrownBy(() -> service.confirmTransfer(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("dorsal 7");

        assertThat(player.getEquipo()).isSameAs(originTeam);
        assertThat(transfer.getStatus()).isEqualTo(TransferStatus.PENDING);
        verify(jugadorRepository, never()).saveAndFlush(any(Jugador.class));
        verify(transferRepository, never()).rejectOtherPending(any(), any(), any());
    }

    @Test
    void confirmTransfer_playerNoLongerInOriginTeam_throwsBusinessException() {
        PlayerTransfer transfer = pendingTransfer(100L, null);
        stubLockedLookups(transfer);
        player.setEquipo(team(3L, "Otro equipo"));

        assertThatThrownBy(() -> service.confirmTransfer(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("equipo de origen");
        verify(transferRepository, never()).rejectOtherPending(any(), any(), any());
    }

    @Test
    void confirmTransfer_notPending_throwsBusinessException() {
        PlayerTransfer transfer = pendingTransfer(100L, null);
        transfer.setStatus(TransferStatus.REJECTED);
        stubLockedLookups(transfer);

        assertThatThrownBy(() -> service.confirmTransfer(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya no está pendiente");
        assertThat(player.getEquipo()).isSameAs(originTeam);
        verify(transferRepository, never()).rejectOtherPending(any(), any(), any());
    }

    @Test
    void confirmTransfer_notFound_throwsTransferNotFoundException() {
        when(transferRepository.findPlayerIdById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirmTransfer(999L))
                .isInstanceOf(TransferNotFoundException.class);
    }

    @Test
    void confirmTransfer_autoRejectsOtherPendingRequestsOfTheSamePlayerOnly() {
        PlayerTransfer transfer = pendingTransfer(100L, null);
        stubLockedLookups(transfer);
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(7, DESTINATION_TEAM_ID, PLAYER_ID)).thenReturn(false);
        when(transferRepository.save(transfer)).thenReturn(transfer);

        service.confirmTransfer(100L);

        // Bulk rejection scoped to this player and excluding the confirmed request.
        verify(transferRepository).rejectOtherPending(org.mockito.ArgumentMatchers.eq(PLAYER_ID),
                org.mockito.ArgumentMatchers.eq(100L), any(java.time.LocalDateTime.class));
    }

    // ---------------------------------------------------------------- rejectTransfer

    @Test
    void rejectTransfer_valid_marksRejectedWithoutTouchingThePlayer() {
        PlayerTransfer pending = pendingTransfer(100L, null);
        PlayerTransfer rejected = pendingTransfer(100L, null);
        rejected.setStatus(TransferStatus.REJECTED);
        rejected.setResolvedAt(java.time.LocalDateTime.now());
        when(transferRepository.findById(100L)).thenReturn(Optional.of(pending), Optional.of(rejected));
        when(transferRepository.rejectIfPending(org.mockito.ArgumentMatchers.eq(100L), any(java.time.LocalDateTime.class)))
                .thenReturn(1);

        TransferDTO result = service.rejectTransfer(100L);

        assertThat(result.getStatus()).isEqualTo(TransferStatus.REJECTED);
        assertThat(result.getResolvedAt()).isNotNull();
        assertThat(player.getEquipo()).isSameAs(originTeam);
        verify(jugadorRepository, never()).save(any(Jugador.class));
        verify(transferRepository, never()).rejectOtherPending(any(), any(), any());
    }

    @Test
    void rejectTransfer_notPending_throwsBusinessException() {
        PlayerTransfer confirmed = pendingTransfer(100L, null);
        confirmed.setStatus(TransferStatus.CONFIRMED);
        when(transferRepository.findById(100L)).thenReturn(Optional.of(confirmed));
        when(transferRepository.rejectIfPending(org.mockito.ArgumentMatchers.eq(100L), any(java.time.LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(() -> service.rejectTransfer(100L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya no está pendiente");
    }

    @Test
    void rejectTransfer_notFound_throwsTransferNotFoundException() {
        when(transferRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.rejectTransfer(999L))
                .isInstanceOf(TransferNotFoundException.class);
    }

    // ---------------------------------------------------------------- findTransfers

    @Test
    void findTransfers_byPendingStatus_returnsOnlyWhatTheRepositoryReturnsForThatStatus() {
        when(transferRepository.findByStatusOrderByRequestedAtDescIdDesc(TransferStatus.PENDING))
                .thenReturn(java.util.Collections.singletonList(pendingTransfer(100L, null)));

        java.util.List<TransferDTO> result = service.findTransfers("PENDING", null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(TransferStatus.PENDING);
    }

    @Test
    void findTransfers_confirmedHistoryOfAPlayer_filtersByStatusAndPlayer() {
        PlayerTransfer confirmed = pendingTransfer(101L, null);
        confirmed.setStatus(TransferStatus.CONFIRMED);
        when(transferRepository.findByStatusAndPlayerIdOrderByRequestedAtDescIdDesc(TransferStatus.CONFIRMED, PLAYER_ID))
                .thenReturn(java.util.Collections.singletonList(confirmed));

        java.util.List<TransferDTO> result = service.findTransfers("confirmed", PLAYER_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOriginTeamName()).isEqualTo("Macalister");
        assertThat(result.get(0).getDestinationTeamName()).isEqualTo("Real Madrid");
        assertThat(result.get(0).getCost()).isEqualByComparingTo("1500.00");
    }

    @Test
    void findTransfers_playerWithoutTransfers_returnsEmptyList() {
        when(transferRepository.findByPlayerIdOrderByRequestedAtDescIdDesc(PLAYER_ID))
                .thenReturn(java.util.Collections.emptyList());

        assertThat(service.findTransfers(null, PLAYER_ID)).isEmpty();
    }

    @Test
    void findTransfers_withoutFilters_returnsAllTransfers() {
        PlayerTransfer rejected = pendingTransfer(102L, null);
        rejected.setStatus(TransferStatus.REJECTED);
        when(transferRepository.findAllByOrderByRequestedAtDescIdDesc())
                .thenReturn(java.util.Arrays.asList(pendingTransfer(103L, null), rejected));

        java.util.List<TransferDTO> result = service.findTransfers(null, null);

        // Order is the one returned by the repository (requested_at descending).
        assertThat(result).extracting(TransferDTO::getId).containsExactly(103L, 102L);
    }

    @Test
    void findTransfers_blankStatus_isTreatedAsNoFilter() {
        when(transferRepository.findAllByOrderByRequestedAtDescIdDesc())
                .thenReturn(java.util.Collections.emptyList());

        assertThat(service.findTransfers("  ", null)).isEmpty();
    }

    @Test
    void findTransfers_invalidStatus_throwsBusinessException() {
        assertThatThrownBy(() -> service.findTransfers("DONE", null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("PENDING, CONFIRMED, REJECTED");
    }
}
