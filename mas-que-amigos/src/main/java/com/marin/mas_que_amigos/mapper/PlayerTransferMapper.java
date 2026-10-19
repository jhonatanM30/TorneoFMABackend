package com.marin.mas_que_amigos.mapper;

import com.marin.mas_que_amigos.dto.TransferDTO;
import com.marin.mas_que_amigos.model.PlayerTransfer;
import org.springframework.stereotype.Component;

@Component
public class PlayerTransferMapper {

    public TransferDTO toDTO(PlayerTransfer transfer) {
        TransferDTO dto = new TransferDTO();
        dto.setId(transfer.getId());
        dto.setPlayerId(transfer.getPlayer().getId());
        dto.setPlayerName(transfer.getPlayer().getNombre());
        dto.setOriginTeamId(transfer.getOriginTeam().getId());
        dto.setOriginTeamName(transfer.getOriginTeam().getNombre());
        dto.setDestinationTeamId(transfer.getDestinationTeam().getId());
        dto.setDestinationTeamName(transfer.getDestinationTeam().getNombre());
        dto.setCost(transfer.getCost());
        dto.setStatus(transfer.getStatus());
        dto.setDestinationDorsal(transfer.getDestinationDorsal());
        dto.setRequestedAt(transfer.getRequestedAt());
        dto.setResolvedAt(transfer.getResolvedAt());
        return dto;
    }
}
