package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Partido;
import java.util.Optional;
import lombok.Value;

/**
 * Who won a match that must have a winner. The team with more goals wins and,
 * with the score tied, the team with more penalties. Penalties never override a
 * decisive score.
 */
public final class MatchWinner {

    @Value
    public static class Result {

        Equipo winner;
        Equipo loser;
    }

    private MatchWinner() {
    }

    /**
     * @return the winner and the loser, or empty when the match is still tied
     *         (equal score and missing or equal penalties)
     */
    public static Optional<Result> of(Partido match) {
        if (match.getGolesLocal() != match.getGolesVisitante()) {
            boolean homeWins = match.getGolesLocal() > match.getGolesVisitante();
            return Optional.of(homeWins
                    ? new Result(match.getEquipoLocal(), match.getEquipoVisitante())
                    : new Result(match.getEquipoVisitante(), match.getEquipoLocal()));
        }
        Integer home = match.getPenalesLocal();
        Integer away = match.getPenalesVisitante();
        if (home == null || away == null || home.equals(away)) {
            return Optional.empty();
        }
        return Optional.of(home > away
                ? new Result(match.getEquipoLocal(), match.getEquipoVisitante())
                : new Result(match.getEquipoVisitante(), match.getEquipoLocal()));
    }
}
