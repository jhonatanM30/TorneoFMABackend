package com.marin.mas_que_amigos.mapper;

import com.marin.mas_que_amigos.dto.ImagenGaleriaDTO;
import com.marin.mas_que_amigos.model.ImagenGaleria;
import org.springframework.stereotype.Component;

/**
 * @author JhonatanAlexanderCue
 */
@Component
public class ImagenGaleriaMapper {

    public ImagenGaleriaDTO toDTO(ImagenGaleria imagen) {
        ImagenGaleriaDTO dto = new ImagenGaleriaDTO();
        dto.setId(imagen.getId());
        dto.setImagenUrl(imagen.getImagenUrl());
        dto.setFechaSubida(imagen.getFechaSubida());
        return dto;
    }
}
