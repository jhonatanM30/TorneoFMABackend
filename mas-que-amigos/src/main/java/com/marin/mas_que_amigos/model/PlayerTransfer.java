package com.marin.mas_que_amigos.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
 * Request to move a player from one team to another. The cost is a snapshot of
 * {@code Jugador.costo} taken when the request is created, so later edits to
 * the player's cost never alter the transfer history.
 */
@Entity
@Getter
@Setter
@Table(name = "player_transfer")
public class PlayerTransfer implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Jugador player;

    @ManyToOne
    @JoinColumn(name = "origin_team_id", nullable = false)
    private Equipo originTeam;

    @ManyToOne
    @JoinColumn(name = "destination_team_id", nullable = false)
    private Equipo destinationTeam;

    @Column(name = "cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal cost;

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TransferStatus status;

    // Optional new jersey number to apply if the transfer is confirmed.
    @Column(name = "destination_dorsal")
    private Integer destinationDorsal;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
}
