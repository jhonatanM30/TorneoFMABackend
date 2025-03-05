/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.mapper;

import com.marin.mas_que_amigos.dto.EstadisticaDTO;
import com.marin.mas_que_amigos.model.Estadistica;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.model.Partido;
import org.springframework.stereotype.Component;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Component
public class EstadisticaMapper {

    public EstadisticaDTO toDTO(Estadistica estadistica) {
        EstadisticaDTO dto = new EstadisticaDTO();
        dto.setId(estadistica.getId());
        dto.setGoles(estadistica.getGoles());
        dto.setTarjetasAmarillas(estadistica.getTarjetasAmarillas());
        dto.setTarjetasRojas(estadistica.getTarjetasRojas());
        dto.setAsistencias(estadistica.getAsistencias());

        // Solo incluir los IDs de jugador y partido
        dto.setIdJugador(estadistica.getJugador().getId());
        dto.setIdPartido(estadistica.getPartido().getId());

        return dto;
    }

    public Estadistica toEntity(EstadisticaDTO dto) {
        Estadistica estadistica = new Estadistica();
        estadistica.setGoles(dto.getGoles());
        estadistica.setTarjetasAmarillas(dto.getTarjetasAmarillas());
        estadistica.setTarjetasRojas(dto.getTarjetasRojas());
        estadistica.setAsistencias(dto.getAsistencias());

        // Evitar consultas extra asignando solo referencias
        Jugador jugador = new Jugador();
        jugador.setId(dto.getIdJugador());
        estadistica.setJugador(jugador);

        Partido partido = new Partido();
        partido.setId(dto.getIdPartido());
        estadistica.setPartido(partido);

        return estadistica;
    }
}
