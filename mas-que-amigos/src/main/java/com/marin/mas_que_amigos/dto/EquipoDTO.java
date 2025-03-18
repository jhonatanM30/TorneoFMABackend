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
    private String nombre;
    private String directorTecnico;
    private String imagenUrl;
    private Integer titulos;
    private Integer idTorneo;
    private String tipoClasificacion;
    private String indicadorRespuesta;
    private String mensaje;   
    
     // 🔹 Constructor solo con `indicadorRespuesta` y `mensaje`
    public EquipoDTO(Long id, String indicadorRespuesta, String mensaje) {
        this.id = id;
        this.indicadorRespuesta = indicadorRespuesta;
        this.mensaje = mensaje;
    }
   
    private List<JugadorDTO> jugadores;  // Aquí agregamos los jugadores
}
