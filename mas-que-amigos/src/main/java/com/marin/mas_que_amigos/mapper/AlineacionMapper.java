/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.mapper;

import com.marin.mas_que_amigos.dto.AlineacionDTO;
import com.marin.mas_que_amigos.model.Alineacion;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.model.Partido;
import org.springframework.stereotype.Component;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Component
public class AlineacionMapper {

    public Alineacion toEntity(AlineacionDTO dto) {
        Alineacion alineacion = new Alineacion();

        Partido partido = new Partido();
        partido.setId(dto.getIdPartido());
        alineacion.setPartido(partido);

        Jugador jugador = new Jugador();
        jugador.setId(dto.getIdJugador());
        alineacion.setJugador(jugador);

        alineacion.setTitular(dto.isTitular());

        return alineacion;
    }

    public AlineacionDTO toDTO(Alineacion entity) {
        AlineacionDTO dto = new AlineacionDTO();

        dto.setIdPartido(entity.getPartido().getId());
        dto.setIdJugador(entity.getJugador().getId());
        dto.setTitular(entity.getTitular());

        return dto;
    }
    
}
