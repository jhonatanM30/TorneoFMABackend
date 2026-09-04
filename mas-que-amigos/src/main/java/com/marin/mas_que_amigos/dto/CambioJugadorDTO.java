package com.marin.mas_que_amigos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Data
public class CambioJugadorDTO {
    private Long id;

    // Se recibe por la URL (POST /api/partidos/{idPartido}/cambios), no en
    // el body; se completa igual en la respuesta para que el DTO sea
    // autocontenido al consultarse despues via GET.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long idPartido;

    @NotNull(message = "El jugador que sale es obligatorio.")
    private Long idJugadorSale;

    @NotNull(message = "El jugador que entra es obligatorio.")
    private Long idJugadorEntra;

    @Min(value = 0, message = "El minuto no puede ser negativo.")
    private int minuto;

    // Solo se completan al responder (CambioJugadorMapper.toDTO).
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private JugadorDTO jugadorSale;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private JugadorDTO jugadorEntra;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaRegistro;
}
