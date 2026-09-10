package com.marin.mas_que_amigos.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * @author JhonatanAlexanderCue
 */
@Data
public class ImagenGaleriaDTO {

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private Long id;

    // Se completa al subir el archivo (ImagenGaleriaService#subirImagen);
    // nunca se lee de un request, la subida es siempre multipart/form-data.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String imagenUrl;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private LocalDateTime fechaSubida;

    private String mensaje;
}
