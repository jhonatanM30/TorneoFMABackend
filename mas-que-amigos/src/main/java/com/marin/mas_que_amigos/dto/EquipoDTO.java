package com.marin.mas_que_amigos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor  // Constructor vacío necesario para serialización
@AllArgsConstructor // Constructor con todos los campos
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class EquipoDTO {

    private Long id;

    @NotBlank(message = "El nombre del equipo es obligatorio.")
    @Size(max = 100, message = "El nombre del equipo no puede exceder 100 caracteres.")
    private String nombre;

    @NotBlank(message = "El nombre del técnico del equipo es obligatorio.")
    @Size(max = 100, message = "El nombre del técnico no puede exceder 100 caracteres.")
    private String directorTecnico;

    @Size(max = 255, message = "La URL de la imagen no puede exceder 255 caracteres.")
    private String imagenUrl;

    private Integer titulos;

    @NotBlank(message = "El tipo de clasificación es obligatorio.")
    @Size(max = 50, message = "El tipo de clasificación no puede exceder 50 caracteres.")
    private String tipoClasificacion;

    private String indicadorRespuesta;
    private String mensaje;

    // 🔹 Constructor solo con id `indicadorRespuesta` y `mensaje`
    public EquipoDTO(Long id, String indicadorRespuesta, String mensaje) {
        this.id = id;
        this.indicadorRespuesta = indicadorRespuesta;
        this.mensaje = mensaje;
    }

    // 🔹 Constructor solo con id `indicadorRespuesta` y `mensaje`
    public EquipoDTO(String indicadorRespuesta, String mensaje) {
        this.indicadorRespuesta = indicadorRespuesta;
        this.mensaje = mensaje;
    }

    private List<JugadorDTO> jugadores;  // Aquí agregamos los jugadores
}
