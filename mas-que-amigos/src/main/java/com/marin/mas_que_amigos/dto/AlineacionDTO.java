/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Getter
@Setter
@NoArgsConstructor  // Constructor vacío necesario para serialización
@AllArgsConstructor // Constructor con todos los campos
public class AlineacionDTO {

    private Long idPartido;
    private Long idJugador;
    private boolean titular;
    private PartidoDTO partido;
    private JugadorDTO jugador;
}
