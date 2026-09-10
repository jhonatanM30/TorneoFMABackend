/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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

    // 🔹 Solo se completa al responder (PartidoMapper.toDTO); nunca se lee en
    // el request (se usa idEquipoLocal para eso), así que se marca de solo
    // lectura.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private EquipoDTO equipoLocal;

    private Long idEquipoVisitante;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private EquipoDTO equipoVisitante;

    private LocalDate fecha;

    private LocalTime hora;

    // Hallazgo post-despliegue (fase9): "los goles que se reflejan en
    // partido deben ser ingresados por el registro de estadisticas y si se
    // desea editar se debe editar por la estadistica registrada en
    // estadisticas, asi se tiene relacion con el actor". Antes estos dos
    // campos se leian y escribian libremente en create/edit de Partido; a
    // partir de ahora se calculan siempre en el servidor sumando los goles
    // de Estadistica por equipo (ver PartidoService#recalcularGoles) y se
    // marcan de solo lectura para que un cliente que aun envie estos campos
    // en el body simplemente los vea ignorados, en vez de fallar.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int golesLocal;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int golesVisitante;

    private String fase;

    // FRONTEND_VISION.md Fase3-09: solo se completa al responder
    // (PartidoMapper.toDTO); nunca se lee en create/edit - el estado
    // cambia unicamente via PUT /api/partidos/{id}/iniciar y /finalizar.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String estado;

    private String indicadorRespuesta;
    private String mensaje;

    // 🔹 Constructor solo con `indicadorRespuesta` y `mensaje`
    public PartidoDTO(String indicadorRespuesta, String mensaje) {
        this.indicadorRespuesta = indicadorRespuesta;
        this.mensaje = mensaje;
    }


}
