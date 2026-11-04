package com.marin.mas_que_amigos.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A tie of the bracket. The teams and the match are null until they are known.
 */
@Getter
@Setter
@NoArgsConstructor
public class BracketTieDTO {

    private Long id;
    private int position;
    private TeamRefDTO homeTeam;
    private TeamRefDTO awayTeam;
    private Long matchId;
    private String matchStatus;
    private Integer homeGoals;
    private Integer awayGoals;
    private Integer homePenalties;
    private Integer awayPenalties;
    private Long winnerTeamId;
    private LocalDate date;
    private LocalTime time;
}
