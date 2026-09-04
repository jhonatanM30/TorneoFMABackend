package com.marin.mas_que_amigos.model;

import java.io.Serializable;
import java.time.LocalDateTime;
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
 * Sustitucion de un jugador por otro durante un partido (FRONTEND_VISION.md
 * Fase3-09: "realizar un cambio entre un suplente y salga un titular y se
 * vea reflejado el tiempo"). Es un registro de auditoria, tipo bitacora:
 * junto con Estadistica (que ahora tambien puede llevar minuto), forma el
 * historial consultable del partido que pide Fase3-10.
 *
 * @author JhonatanAlexanderCue
 */
@Entity
@Getter
@Setter
@Table(name = "cambio_jugador")
public class CambioJugador implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_cambio_jugador")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "id_partido", nullable = false)
    private Partido partido;

    @ManyToOne
    @JoinColumn(name = "id_jugador_sale", nullable = false)
    private Jugador jugadorSale;

    @ManyToOne
    @JoinColumn(name = "id_jugador_entra", nullable = false)
    private Jugador jugadorEntra;

    @Column(nullable = false)
    private int minuto;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
}
