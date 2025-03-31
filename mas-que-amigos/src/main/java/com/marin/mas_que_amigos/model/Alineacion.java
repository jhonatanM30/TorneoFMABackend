/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.model;

import javax.persistence.Column;
import javax.persistence.Entity;
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
@Table(name = "alineacion")
public class Alineacion {

    @Id
    @Column(name = "id_partido")
    private Integer idPartido;

    @Id
    @Column(name = "id_jugador")
    private Integer idJugador;

    @Column(name = "titular", nullable = false)
    private Boolean titular;

    @ManyToOne
    @JoinColumn(name = "id_partido", insertable = false, updatable = false)
    private Partido partido;

    @ManyToOne
    @JoinColumn(name = "id_jugador", insertable = false, updatable = false)
    private Jugador jugador;
}
