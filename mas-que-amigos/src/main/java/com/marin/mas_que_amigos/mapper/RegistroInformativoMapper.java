package com.marin.mas_que_amigos.mapper;

import com.marin.mas_que_amigos.dto.RegistroInformativoDTO;
import com.marin.mas_que_amigos.model.RegistroInformativo;
import org.springframework.stereotype.Component;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Component
public class RegistroInformativoMapper {

    public RegistroInformativoDTO toDTO(RegistroInformativo registro) {
        RegistroInformativoDTO dto = new RegistroInformativoDTO();
        dto.setId(registro.getId());
        dto.setTitulo(registro.getTitulo());
        dto.setContenido(registro.getContenido());
        dto.setFechaPublicacion(registro.getFechaPublicacion());
        return dto;
    }

    public RegistroInformativo toEntity(RegistroInformativoDTO dto) {
        RegistroInformativo registro = new RegistroInformativo();
        registro.setTitulo(dto.getTitulo());
        registro.setContenido(dto.getContenido());
        return registro;
    }
}
