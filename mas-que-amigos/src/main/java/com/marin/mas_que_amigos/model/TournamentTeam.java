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
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * Enrollment of a team in a tournament. A team can be enrolled in several
 * tournaments, but only once in each one.
 */
@Entity
@Getter
@Setter
@Table(name = "tournament_team", uniqueConstraints = @UniqueConstraint(columnNames = {"tournament_id", "team_id"}))
public class TournamentTeam implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @ManyToOne
    @JoinColumn(name = "team_id", nullable = false)
    private Equipo team;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDateTime enrolledAt;
}
