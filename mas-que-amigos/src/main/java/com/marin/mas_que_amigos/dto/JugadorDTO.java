package com.marin.mas_que_amigos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.marin.mas_que_amigos.model.Jugador.Posicion;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor  // Constructor vacío necesario para serialización
@AllArgsConstructor // Constructor con todos los campos
public class JugadorDTO {

    private Long id;

    //@NotBlank(message = "El nombre del jugador es obligatorio.")
    private String nombre;

    //@NotNull(message = "La posicion del jugador es obligatorio.")
    private Posicion posicion;

    //@NotNull(message = "La edad del jugador es obligatorio.")
    private int edad;

    //@NotNull(message = "El dorsal del jugador es obligatorio.")
    private int dorsal;
    
    private String indicadorRespuesta;
    private String mensaje;

   

    @JsonInclude(JsonInclude.Include.NON_NULL) // 🔹 Si idEquipo es null, no se muestra
    private Long idEquipo;

    @JsonInclude(JsonInclude.Include.NON_NULL) // 🔹 Si equipo es null, no se muestra
    private EquipoDTO equipo;
    
     // 🔹 Constructor solo con `indicadorRespuesta` y `mensaje`
    public JugadorDTO(String indicadorRespuesta, String mensaje) {
        this.indicadorRespuesta = indicadorRespuesta;
        this.mensaje = mensaje;
    }
}

