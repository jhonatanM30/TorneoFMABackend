package com.marin.mas_que_amigos.dto;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A first round pairing chosen by hand when generating a knockout bracket.
 */
@Getter
@Setter
@NoArgsConstructor
public class PairingRequestDTO {

    @NotNull(message = "El equipo local del cruce es obligatorio.")
    private Long homeTeamId;

    @NotNull(message = "El equipo visitante del cruce es obligatorio.")
    private Long awayTeamId;
}
