package com.marin.mas_que_amigos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Resultado de un jugador dentro de una creación en lote
 * (POST /api/jugadores/batch). Cada jugador del arreglo enviado se procesa
 * de forma independiente: si falla, no afecta a los demás. "indice" permite
 * al cliente saber a qué posición del arreglo original corresponde este
 * resultado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JugadorBatchItemResultDTO {

    private int indice;

    private boolean exito;

    // Presente solo si exito = true: el jugador completo, igual que en la
    // creación individual (sin campos en null).
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private JugadorDTO jugador;

    // Presente solo si exito = false: el motivo del fallo para ese jugador
    // en particular (validación de datos o regla de negocio).
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private String error;
}
