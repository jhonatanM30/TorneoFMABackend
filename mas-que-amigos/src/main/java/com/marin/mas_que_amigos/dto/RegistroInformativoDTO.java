package com.marin.mas_que_amigos.dto;

import java.time.LocalDateTime;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Data;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Data
public class RegistroInformativoDTO {
    private Long id;

    @NotBlank(message = "El titulo es obligatorio.")
    @Size(max = 150, message = "El titulo no puede superar los 150 caracteres.")
    private String titulo;

    @NotBlank(message = "El contenido es obligatorio.")
    private String contenido;

    private LocalDateTime fechaPublicacion;
}
