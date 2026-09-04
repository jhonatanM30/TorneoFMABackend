package com.marin.mas_que_amigos.mapper;

import com.marin.mas_que_amigos.dto.CambioJugadorDTO;
import com.marin.mas_que_amigos.model.CambioJugador;
import org.springframework.stereotype.Component;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Component
public class CambioJugadorMapper {

    private final JugadorMapper jugadorMapper;

    public CambioJugadorMapper(JugadorMapper jugadorMapper) {
        this.jugadorMapper = jugadorMapper;
    }

    public CambioJugadorDTO toDTO(CambioJugador cambio) {
        CambioJugadorDTO dto = new CambioJugadorDTO();
        dto.setId(cambio.getId());
        dto.setIdPartido(cambio.getPartido().getId());
        dto.setIdJugadorSale(cambio.getJugadorSale().getId());
        dto.setIdJugadorEntra(cambio.getJugadorEntra().getId());
        dto.setMinuto(cambio.getMinuto());
        dto.setFechaRegistro(cambio.getFechaRegistro());
        dto.setJugadorSale(jugadorMapper.toDTOExt(cambio.getJugadorSale()));
        dto.setJugadorEntra(jugadorMapper.toDTOExt(cambio.getJugadorEntra()));
        return dto;
    }
}
