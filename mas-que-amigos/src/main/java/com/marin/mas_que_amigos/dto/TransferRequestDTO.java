package com.marin.mas_que_amigos.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TransferRequestDTO {

    @NotNull(message = "El jugador del traspaso es obligatorio.")
    private Long playerId;

    @NotNull(message = "El equipo destino del traspaso es obligatorio.")
    private Long destinationTeamId;

    // Optional: new jersey number in the destination team (defaults to the player's current one).
    @Min(value = 1, message = "El dorsal debe ser mayor a 0.")
    private Integer destinationDorsal;
}
