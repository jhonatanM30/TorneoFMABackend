/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.mapper;

import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Partido;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Component
public class PartidoMapper {

    private final EquipoMapper equipoMapper;

    public PartidoMapper(@Lazy EquipoMapper equipoMapper) {
        this.equipoMapper = equipoMapper;
    }

    public Partido toEntity(PartidoDTO dto) {
        Partido partido = new Partido();
        partido.setFecha(dto.getFecha());
        partido.setHora(dto.getHora());
        partido.setGolesLocal(dto.getGolesLocal());
        partido.setGolesVisitante(dto.getGolesVisitante());
        partido.setFase(resolverFase(dto.getFase()));

        Equipo equipoLocal = new Equipo();
        equipoLocal.setId(dto.getIdEquipoLocal());
        partido.setEquipoLocal(equipoLocal);

        Equipo equipoVisitante = new Equipo();
        equipoVisitante.setId(dto.getIdEquipoVisitante());
        partido.setEquipoVisitante(equipoVisitante);

        return partido;
    }

    public PartidoDTO toDTO(Partido partido) {

        PartidoDTO dto = new PartidoDTO();

        dto.setId(partido.getId());
        dto.setFecha(partido.getFecha());
        dto.setHora(partido.getHora());
        dto.setGolesLocal(partido.getGolesLocal());
        dto.setGolesVisitante(partido.getGolesVisitante());
        dto.setFase(partido.getFase().name());

        dto.setEquipoLocal(equipoMapper.toDTOExt(partido.getEquipoLocal()));
        dto.setEquipoVisitante(equipoMapper.toDTOExt(partido.getEquipoVisitante()));

        dto.setIndicadorRespuesta("Success");
        dto.setMensaje("");

        return dto;
    }
    
    public PartidoDTO toRSPDTO(String indicadorRespuesta, String mensaje){
    
        return new PartidoDTO(indicadorRespuesta, mensaje);
    }

    // 🔹 Antes se ignoraba el valor de "fase" recibido en el DTO y siempre se
    // guardaba como FINAL. Ahora se respeta lo que envía el cliente y, si no
    // envía nada, se asume la fase inicial de un torneo (fase de grupos).
    private Partido.Fase resolverFase(String fase) {
        if (fase == null || fase.trim().isEmpty()) {
            return Partido.Fase.FASE_DE_GRUPOS;
        }

        try {
            return Partido.Fase.valueOf(fase.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("Fase de partido inválida: " + fase);
        }
    }

}
