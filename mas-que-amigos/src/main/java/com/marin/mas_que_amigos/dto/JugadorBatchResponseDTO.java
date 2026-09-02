package com.marin.mas_que_amigos.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Respuesta de POST /api/jugadores/batch: resumen del procesamiento
 * optimista del lote (cada jugador se valida y guarda de forma
 * independiente, así que el lote puede terminar con éxitos y fallos mezclados).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JugadorBatchResponseDTO {

    private int total;
    private int exitosos;
    private int fallidos;

    private List<JugadorBatchItemResultDTO> resultados;
}
