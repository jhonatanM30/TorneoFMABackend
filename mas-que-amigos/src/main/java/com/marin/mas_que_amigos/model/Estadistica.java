/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.model;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import lombok.*;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Entity
@Getter
@Setter
@Table(name = "estadistica")
public class Estadistica implements Serializable{
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estadistica")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_jugador", nullable = false)
    private Jugador jugador;

    @ManyToOne
    @JoinColumn(name = "id_partido", nullable = false)
    private Partido partido;

    @Column(nullable = false)
    private int goles = 0;

    @Column(name = "tarjetas_amarillas", nullable = false)
    private int tarjetasAmarillas = 0;

    @Column(name = "tarjetas_rojas", nullable = false)
    private int tarjetasRojas = 0;

    @Column(nullable = false)
    private int asistencias = 0;
}
