/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Getter
@Setter
@Entity
@Table(name = "partido")
public class Partido implements Serializable{
    
  @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_partido")
    private Long id; 

    @ManyToOne
    @JoinColumn(name = "equipo_local", nullable = false)
    private Equipo equipoLocal;

    @ManyToOne
    @JoinColumn(name = "equipo_visitante", nullable = false)
    private Equipo equipoVisitante;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private LocalTime hora;

    @Column(name = "goles_local", nullable = false)
    private int golesLocal;

    @Column(name = "goles_visitante", nullable = false)
    private int golesVisitante;
    
    @Enumerated(EnumType.STRING)
    private Fase fase;

    // FRONTEND_VISION.md Fase3-09: estado del partido, para saber si esta
    // en curso (habilita registrar cambios de jugador y minuto en las
    // estadisticas). Todo partido nace PROGRAMADO; solo
    // PartidoService#iniciarPartido/#finalizarPartido lo mueven de estado,
    // nunca el create/edit normal (ver PartidoDTO.estado, de solo lectura).
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Estado estado = Estado.PROGRAMADO;

    public enum Fase {
        FASE_DE_GRUPOS, REPECHAJE, ELIMINACION_DIRECTA, FINAL
    }

    public enum Estado {
        PROGRAMADO, EN_CURSO, FINALIZADO
    }
}
