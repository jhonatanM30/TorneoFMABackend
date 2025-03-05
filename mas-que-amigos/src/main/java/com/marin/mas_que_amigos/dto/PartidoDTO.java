/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Data;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Data
public class PartidoDTO {
    private Long id;
    private Long idTorneo;
    private Long idEquipoLocal;
    private Long idEquipoVisitante;
    private LocalDate fecha;
    private LocalTime hora;
    private int golesLocal;
    private int golesVisitante;
    private String fase;
}
