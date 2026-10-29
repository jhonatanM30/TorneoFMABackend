package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.TransferDTO;
import com.marin.mas_que_amigos.dto.TransferRequestDTO;
import com.marin.mas_que_amigos.exception.AuthenticationFailedException;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.ForbiddenOperationException;
import com.marin.mas_que_amigos.exception.JugadorNotFoundException;
import com.marin.mas_que_amigos.exception.TransferNotFoundException;
import com.marin.mas_que_amigos.mapper.PlayerTransferMapper;
import com.marin.mas_que_amigos.model.AppUser;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.model.PlayerTransfer;
import com.marin.mas_que_amigos.model.TransferStatus;
import com.marin.mas_que_amigos.model.UserRole;
import com.marin.mas_que_amigos.repository.AppUserRepository;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.JugadorRepository;
import com.marin.mas_que_amigos.repository.PlayerTransferRepository;
import com.marin.mas_que_amigos.security.AuthenticatedUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlayerTransferService {

    private final PlayerTransferRepository transferRepository;
    private final JugadorRepository jugadorRepository;
    private final EquipoRepository equipoRepository;
    private final PlayerTransferMapper mapper;
    private final AppUserRepository userRepository;

    public PlayerTransferService(PlayerTransferRepository transferRepository, JugadorRepository jugadorRepository,
            EquipoRepository equipoRepository, PlayerTransferMapper mapper, AppUserRepository userRepository) {
        this.transferRepository = transferRepository;
        this.jugadorRepository = jugadorRepository;
        this.equipoRepository = equipoRepository;
        this.mapper = mapper;
        this.userRepository = userRepository;
    }

    /**
     * Creates a PENDING transfer request. The player is not moved here, and
     * several PENDING requests may coexist for the same player: only the first
     * one to be confirmed wins. An ADMIN may target any team; a DT only its own.
     */
    public TransferDTO requestTransfer(TransferRequestDTO request, AuthenticatedUser requester) {
        authorizeDestination(requester, request.getDestinationTeamId());

        Jugador player = jugadorRepository.findById(request.getPlayerId())
                .orElseThrow(() -> new JugadorNotFoundException(
                        "Fuera de juego! No se encontró registros de jugador con Id " + request.getPlayerId() + "."));

        Equipo destinationTeam = equipoRepository.findById(request.getDestinationTeamId())
                .orElseThrow(() -> new BusinessException("Fuera de juego! No se ha seleccionado un o unos equipos existentes."));

        if (player.getEquipo().getId().equals(destinationTeam.getId())) {
            throw new BusinessException("El jugador ya pertenece al equipo destino.");
        }

        validateDorsalAvailable(effectiveDorsal(request.getDestinationDorsal(), player), destinationTeam.getId(), player.getId());

        PlayerTransfer transfer = new PlayerTransfer();
        transfer.setPlayer(player);
        transfer.setOriginTeam(player.getEquipo());
        transfer.setDestinationTeam(destinationTeam);
        transfer.setCost(player.getCosto());
        transfer.setStatus(TransferStatus.PENDING);
        transfer.setDestinationDorsal(request.getDestinationDorsal());
        transfer.setRequestedAt(LocalDateTime.now());

        return mapper.toDTO(transferRepository.save(transfer));
    }

    /**
     * Confirms a PENDING request: the player moves to the destination team (and
     * dorsal, if one was requested), and every other PENDING request of the same
     * player is rejected in the same transaction.
     *
     * Lock order matters. The player row is locked first so two confirmations of
     * the same player are serialized; only then is the transfer re-read with a
     * locking read (a plain read would return the snapshot taken at the first
     * SELECT of the transaction, possibly already rejected by the winner).
     * Locking the player before the transfer in every path avoids deadlocks.
     */
    @Transactional
    public TransferDTO confirmTransfer(Long transferId) {
        Long playerId = transferRepository.findPlayerIdById(transferId)
                .orElseThrow(() -> transferNotFound(transferId));

        Jugador player = jugadorRepository.findByIdForUpdate(playerId)
                .orElseThrow(() -> new JugadorNotFoundException(
                        "Fuera de juego! No se encontró registros de jugador con Id " + playerId + "."));
        PlayerTransfer transfer = transferRepository.findByIdForUpdate(transferId)
                .orElseThrow(() -> transferNotFound(transferId));

        if (transfer.getStatus() != TransferStatus.PENDING) {
            throw notPending();
        }
        if (!player.getEquipo().getId().equals(transfer.getOriginTeam().getId())) {
            throw new BusinessException("El jugador ya no pertenece al equipo de origen de esta solicitud.");
        }

        Equipo destinationTeam = transfer.getDestinationTeam();
        int dorsal = effectiveDorsal(transfer.getDestinationDorsal(), player);
        validateDorsalAvailable(dorsal, destinationTeam.getId(), player.getId());

        player.setEquipo(destinationTeam);
        player.setDorsal(dorsal);
        try {
            // Flush now so the UNIQUE (id_equipo, dorsal) safety net fails here,
            // inside the service, and not later at commit time.
            jugadorRepository.saveAndFlush(player);
        } catch (DataIntegrityViolationException ex) {
            throw new BusinessException("El dorsal " + dorsal
                    + " ya está asignado en el equipo destino. Indica un dorsal nuevo para el traspaso.");
        }

        LocalDateTime now = LocalDateTime.now();
        transfer.setStatus(TransferStatus.CONFIRMED);
        transfer.setResolvedAt(now);
        TransferDTO result = mapper.toDTO(transferRepository.save(transfer));

        transferRepository.rejectOtherPending(playerId, transferId, now);
        return result;
    }

    /**
     * Rejects a PENDING request without touching the player or other requests.
     * Uses a conditional update so it can never overwrite a request that a
     * concurrent confirmation has just resolved.
     */
    @Transactional
    public TransferDTO rejectTransfer(Long transferId) {
        transferRepository.findById(transferId).orElseThrow(() -> transferNotFound(transferId));

        if (transferRepository.rejectIfPending(transferId, LocalDateTime.now()) == 0) {
            throw notPending();
        }
        return mapper.toDTO(transferRepository.findById(transferId).orElseThrow(() -> transferNotFound(transferId)));
    }

    /**
     * Read-only query used for both the administrators' pending inbox
     * (status=PENDING) and a player's team history (status=CONFIRMED + playerId).
     * Both filters are optional; results are newest first.
     */
    public List<TransferDTO> findTransfers(String status, Long playerId) {
        TransferStatus parsedStatus = parseStatus(status);

        List<PlayerTransfer> transfers;
        if (parsedStatus != null && playerId != null) {
            transfers = transferRepository.findByStatusAndPlayerIdOrderByRequestedAtDescIdDesc(parsedStatus, playerId);
        } else if (parsedStatus != null) {
            transfers = transferRepository.findByStatusOrderByRequestedAtDescIdDesc(parsedStatus);
        } else if (playerId != null) {
            transfers = transferRepository.findByPlayerIdOrderByRequestedAtDescIdDesc(playerId);
        } else {
            transfers = transferRepository.findAllByOrderByRequestedAtDescIdDesc();
        }
        return transfers.stream().map(mapper::toDTO).collect(Collectors.toList());
    }

    private TransferStatus parseStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return null;
        }
        try {
            return TransferStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("El estado '" + status + "' no es válido. Valores permitidos: PENDING, CONFIRMED, REJECTED.");
        }
    }

    private TransferNotFoundException transferNotFound(Long transferId) {
        return new TransferNotFoundException("Fuera de juego! No se encontró la solicitud de traspaso con Id " + transferId + ".");
    }

    private BusinessException notPending() {
        return new BusinessException("La solicitud de traspaso ya no está pendiente.");
    }

    /**
     * A DT can only ask for transfers into its own team. The team is read from the
     * account now (not from the token), because an admin may have assigned it after
     * the DT logged in.
     */
    private void authorizeDestination(AuthenticatedUser requester, Long destinationTeamId) {
        if (requester.getRole() != UserRole.DT) {
            return;
        }
        AppUser account = userRepository.findById(requester.getId())
                .orElseThrow(() -> new AuthenticationFailedException("La sesión ya no es válida. Inicia sesión de nuevo."));
        Equipo ownTeam = account.getTeam();
        if (ownTeam == null) {
            throw new ForbiddenOperationException("No tienes un equipo asignado, por eso no puedes solicitar traspasos. "
                    + "Pídele a la organización que te asigne uno.");
        }
        if (!ownTeam.getId().equals(destinationTeamId)) {
            throw new ForbiddenOperationException("Solo puedes solicitar traspasos hacia tu equipo.");
        }
    }

    private int effectiveDorsal(Integer requestedDorsal, Jugador player) {
        return requestedDorsal != null ? requestedDorsal : player.getDorsal();
    }

    private void validateDorsalAvailable(int dorsal, Long destinationTeamId, Long playerId) {
        if (jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(dorsal, destinationTeamId, playerId)) {
            throw new BusinessException("El dorsal " + dorsal
                    + " ya está asignado en el equipo destino. Indica un dorsal nuevo para el traspaso.");
        }
    }
}
