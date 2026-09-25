package com.marin.mas_que_amigos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Representa a un jugador en el ranking de goleadores del torneo: su
 * identidad, el equipo por el que juega actualmente y el total de goles
 * anotados sumando todos los partidos.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoleadorDTO {

    private Long idJugador;
    private String nombreJugador;
    private Long idEquipo;
    private String nombreEquipo;
    private int totalGoles;
}
