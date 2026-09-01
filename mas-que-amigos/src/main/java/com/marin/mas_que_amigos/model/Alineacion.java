/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.model;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
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
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "alineacion")
public class Alineacion {

    @EmbeddedId
    private AlineacionId id;

    @Column(name = "titular", nullable = false)
    private Boolean titular;

    @ManyToOne
    @JoinColumn(name = "id_partido", insertable = false, updatable = false)
    private Partido partido;

    @ManyToOne
    @JoinColumn(name = "id_jugador", insertable = false, updatable = false)
    private Jugador jugador;
}
