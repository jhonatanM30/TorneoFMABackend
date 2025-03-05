package com.marin.mas_que_amigos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.marin.mas_que_amigos.model.Jugador.Posicion;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JugadorDTO {
    private Long id;
    private String nombre;
    private Posicion posicion;
    private int edad;
    private int dorsal;
    
    @JsonInclude(JsonInclude.Include.NON_NULL) // 🔹 Si idEquipo es null, no se muestra
    private Long idEquipo;  
    
    @JsonInclude(JsonInclude.Include.NON_NULL) // 🔹 Si equipo es null, no se muestra
    private EquipoDTO equipo; 
}
