package com.marin.mas_que_amigos.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * One row of a league table.
 */
@Getter
@Setter
@NoArgsConstructor
public class StandingDTO {

    private int position;
    private Long teamId;
    private String teamName;
    private int played;
    private int won;
    private int drawn;
    private int lost;
    private int goalsFor;
    private int goalsAgainst;
    private int goalDifference;
    private int points;
    // Fair play tie-breaker: yellow cards x 1 + red cards x 2 (fewer is better).
    private int fairPlayPoints;
}
