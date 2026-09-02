/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.mapper;

import com.marin.mas_que_amigos.dto.AlineacionDTO;
import com.marin.mas_que_amigos.model.Alineacion;
import com.marin.mas_que_amigos.model.AlineacionId;
import org.springframework.stereotype.Component;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Component
public class AlineacionMapper {

    private final PartidoMapper partidoMapper;
    private final JugadorMapper jugadorMapper;

    public AlineacionMapper(PartidoMapper partidoMapper, JugadorMapper jugadorMapper) {
        this.partidoMapper = partidoMapper;
        this.jugadorMapper = jugadorMapper;
    }

    // No asigna partido/jugador aquí: esas asociaciones están mapeadas como
    // insertable=false, updatable=false (los datos reales se escriben a
    // través del @EmbeddedId), así que solo se completan al leer la entidad
    // de vuelta desde la base de datos. Ver AlineacionService.guardarAlineacion,
    // que relee la alineación tras guardarla para obtenerlas ya hidratadas.
    public Alineacion toEntity(AlineacionDTO dto) {
        Alineacion alineacion = new Alineacion();

        alineacion.setId(new AlineacionId(dto.getIdPartido(), dto.getIdJugador()));
        alineacion.setTitular(dto.isTitular());

        return alineacion;
    }

    public AlineacionDTO toDTO(Alineacion entity) {
        AlineacionDTO dto = new AlineacionDTO();

        dto.setIdPartido(entity.getId().getIdPartido());
        dto.setIdJugador(entity.getId().getIdJugador());
        dto.setTitular(entity.getTitular());

        // 🔹 Se enriquece la respuesta con el detalle completo del partido y
        // del jugador (nombre, posición, equipo, fecha, etc.), igual que el
        // resto de la API hace con sus relaciones, para que el frontend no
        // tenga que hacer llamadas adicionales para mostrar una alineación.
        if (entity.getPartido() != null) {
            dto.setPartido(partidoMapper.toDTO(entity.getPartido()));
        }
        if (entity.getJugador() != null) {
            dto.setJugador(jugadorMapper.toDTO(entity.getJugador()));
        }

        dto.setIndicadorRespuesta("Success");
        dto.setMensaje("");

        return dto;
    }

}
