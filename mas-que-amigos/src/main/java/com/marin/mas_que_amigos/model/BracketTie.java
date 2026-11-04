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
import javax.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * One tie of a knockout bracket. The teams and the match stay empty until they
 * are known (the winners of the previous round advance into them); the date and
 * time are fixed when the bracket is generated, from the round of the tie.
 */
@Entity
@Getter
@Setter
@Table(name = "bracket_tie", uniqueConstraints = @UniqueConstraint(
        columnNames = {"tournament_id", "kind", "round_number", "position_in_round"}))
public class BracketTie implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne
    @JoinColumn(name = "tournament_id", nullable = false)
    private Tournament tournament;

    @Column(name = "kind", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private BracketTieKind kind;

    // 1-based; the last round is the final.
    @Column(name = "round_number", nullable = false)
    private int roundNumber;

    // 0-based inside the round.
    @Column(name = "position_in_round", nullable = false)
    private int positionInRound;

    @ManyToOne
    @JoinColumn(name = "home_team_id")
    private Equipo homeTeam;

    @ManyToOne
    @JoinColumn(name = "away_team_id")
    private Equipo awayTeam;

    @ManyToOne
    @JoinColumn(name = "winner_team_id")
    private Equipo winnerTeam;

    @ManyToOne
    @JoinColumn(name = "match_id")
    private Partido match;

    @ManyToOne
    @JoinColumn(name = "winner_next_tie_id")
    private BracketTie winnerNext;

    @Column(name = "winner_next_is_home")
    private Boolean winnerNextIsHome;

    @ManyToOne
    @JoinColumn(name = "loser_next_tie_id")
    private BracketTie loserNext;

    @Column(name = "loser_next_is_home")
    private Boolean loserNextIsHome;

    @Column(name = "scheduled_date", nullable = false)
    private LocalDate scheduledDate;

    @Column(name = "scheduled_time", nullable = false)
    private LocalTime scheduledTime;
}
