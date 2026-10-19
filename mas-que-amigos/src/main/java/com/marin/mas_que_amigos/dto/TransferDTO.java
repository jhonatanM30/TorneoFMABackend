package com.marin.mas_que_amigos.dto;

import com.marin.mas_que_amigos.model.TransferStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransferDTO {

    private Long id;
    private Long playerId;
    private String playerName;
    private Long originTeamId;
    private String originTeamName;
    private Long destinationTeamId;
    private String destinationTeamName;
    private BigDecimal cost;
    private TransferStatus status;
    private Integer destinationDorsal;
    private LocalDateTime requestedAt;
    // Null while the transfer is still PENDING.
    private LocalDateTime resolvedAt;
}
