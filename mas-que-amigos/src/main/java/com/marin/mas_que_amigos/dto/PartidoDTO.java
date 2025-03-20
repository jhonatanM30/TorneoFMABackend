/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDate;
import java.time.LocalTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Getter
@Setter
@NoArgsConstructor  // Constructor vacío necesario para serialización
@AllArgsConstructor // Constructor con todos los campos
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class PartidoDTO {

    private Long id;
    
    private Long idEquipoLocal;

    @JsonInclude(JsonInclude.Include.NON_NULL) // 🔹 Si equipo es null, no se muestra
    private EquipoDTO equipoLocal;

    private Long idEquipoVisitante;

    @JsonInclude(JsonInclude.Include.NON_NULL) // 🔹 Si equipo es null, no se muestra
    private EquipoDTO equipoVisitante;

    private LocalDate fecha;

    private LocalTime hora;

    private int golesLocal;

    private int golesVisitante;

    private String fase;

    private String indicadorRespuesta;
    private String mensaje;

    // 🔹 Constructor solo con `indicadorRespuesta` y `mensaje`
    public PartidoDTO(String indicadorRespuesta, String mensaje) {
        this.indicadorRespuesta = indicadorRespuesta;
        this.mensaje = mensaje;
    }


}
