/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.dto;

/**
 *
 * @author JhonatanAlexanderCue
 */
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EstadisticaDTO {
    private Long id;

    @NotNull(message = "El id del jugador es obligatorio.")
    private Long idJugador;

    @NotNull(message = "El id del partido es obligatorio.")
    private Long idPartido;

    @Min(value = 0, message = "Los goles no pueden ser negativos.")
    private int goles;

    @Min(value = 0, message = "Las tarjetas amarillas no pueden ser negativas.")
    private int tarjetasAmarillas;

    @Min(value = 0, message = "Las tarjetas rojas no pueden ser negativas.")
    private int tarjetasRojas;

    @Min(value = 0, message = "Las asistencias no pueden ser negativas.")
    private int asistencias;

    // FRONTEND_VISION.md Fase3-09: minuto del partido, opcional (Integer,
    // no int) porque no todo registro de estadistica ocurre durante un
    // partido en curso con minuto conocido.
    @Min(value = 0, message = "El minuto no puede ser negativo.")
    private Integer minuto;
}
