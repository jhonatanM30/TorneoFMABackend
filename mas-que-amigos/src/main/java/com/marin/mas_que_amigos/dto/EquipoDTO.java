package com.marin.mas_que_amigos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
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

    //@NotBlank(message = "El nombre del equipo es obligatorio.")
    private String nombre;

    //@NotBlank(message = "El nombre del Tecnico del equipo es obligatorio.")
    private String directorTecnico;

    //@NotBlank(message = "El escudo  del equipo es obligatorio.")
    private String imagenUrl;

    private Integer titulos;

    //@NotBlank(message = "El tipo de clasificacion del equipo es obligatorio.")
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
