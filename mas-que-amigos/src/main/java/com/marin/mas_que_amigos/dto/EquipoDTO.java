package com.marin.mas_que_amigos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class EquipoDTO {
    private Long id;
    private String nombre;
    private String directorTecnico;
    private String imagenUrl;
    private int titulos;
    private int idTorneo;
    
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private List<JugadorDTO> jugadores;  // Aquí agregamos los jugadores
}
