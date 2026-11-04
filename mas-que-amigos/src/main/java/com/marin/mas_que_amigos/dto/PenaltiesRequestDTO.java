package com.marin.mas_que_amigos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PenaltiesRequestDTO {

    // The JSON names match the Spanish match API (penalesLocal in PartidoDTO).
    @JsonProperty("penalesLocal")
    @NotNull(message = "Los penales del equipo local son obligatorios.")
    @Min(value = 0, message = "Los penales del equipo local no pueden ser negativos.")
    private Integer homePenalties;

    @JsonProperty("penalesVisitante")
    @NotNull(message = "Los penales del equipo visitante son obligatorios.")
    @Min(value = 0, message = "Los penales del equipo visitante no pueden ser negativos.")
    private Integer awayPenalties;
}
