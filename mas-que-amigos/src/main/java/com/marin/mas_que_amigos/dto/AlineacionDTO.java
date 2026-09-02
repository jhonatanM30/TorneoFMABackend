/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
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
public class AlineacionDTO {

    @NotNull(message = "El id del partido es obligatorio.")
    private Long idPartido;

    @NotNull(message = "El id del jugador es obligatorio.")
    private Long idJugador;

    private boolean titular;

    // 🔹 Se completan al responder (AlineacionMapper.toDTO) con el detalle
    // completo del partido y del jugador, siguiendo la misma convención que
    // EquipoDTO.jugadores/JugadorDTO.equipo/PartidoDTO.equipoLocal. Nunca se
    // leen en el request, así que se marcan de solo lectura.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private PartidoDTO partido;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private JugadorDTO jugador;

    private String indicadorRespuesta;
    private String mensaje;

    // 🔹 Constructor solo con `indicadorRespuesta` y `mensaje`
    public AlineacionDTO(String indicadorRespuesta, String mensaje) {
        this.indicadorRespuesta = indicadorRespuesta;
        this.mensaje = mensaje;
    }
}
