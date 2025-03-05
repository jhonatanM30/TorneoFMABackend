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
import lombok.Data;

@Data
public class EstadisticaDTO {
    private Long id;
    private Long idJugador;
    private Long idPartido;
    private int goles;
    private int tarjetasAmarillas;
    private int tarjetasRojas;
    private int asistencias;
}