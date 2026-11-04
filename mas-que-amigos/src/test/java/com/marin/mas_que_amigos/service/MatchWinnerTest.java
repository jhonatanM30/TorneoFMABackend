package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.service.MatchWinner.Result;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MatchWinnerTest {

    private final Equipo home = team(1L, "A");
    private final Equipo away = team(2L, "B");

    private static Equipo team(Long id, String name) {
        Equipo team = new Equipo();
        team.setId(id);
        team.setNombre(name);
        return team;
    }

    private Partido match(int homeGoals, int awayGoals, Integer homePenalties, Integer awayPenalties) {
        Partido match = new Partido();
        match.setEquipoLocal(home);
        match.setEquipoVisitante(away);
        match.setGolesLocal(homeGoals);
        match.setGolesVisitante(awayGoals);
        match.setPenalesLocal(homePenalties);
        match.setPenalesVisitante(awayPenalties);
        return match;
    }

    @Test
    void moreGoals_wins_withoutLookingAtPenalties() {
        Optional<Result> result = MatchWinner.of(match(2, 1, null, null));

        assertThat(result).isPresent();
        assertThat(result.get().getWinner()).isSameAs(home);
        assertThat(result.get().getLoser()).isSameAs(away);
    }

    @Test
    void awayTeamWithMoreGoals_wins() {
        Result result = MatchWinner.of(match(0, 3, null, null)).get();

        assertThat(result.getWinner()).isSameAs(away);
        assertThat(result.getLoser()).isSameAs(home);
    }

    @Test
    void goalsIgnoreThePenaltiesWhenTheyDiffer() {
        // Penalties never override a decisive score.
        Result result = MatchWinner.of(match(1, 0, 0, 5)).get();

        assertThat(result.getWinner()).isSameAs(home);
    }

    @Test
    void tiedScore_moreFirstPenaltiesWins() {
        Result result = MatchWinner.of(match(1, 1, 5, 4)).get();

        assertThat(result.getWinner()).isSameAs(home);
        assertThat(result.getLoser()).isSameAs(away);
    }

    @Test
    void tiedScore_awayWithMorePenaltiesWins() {
        Result result = MatchWinner.of(match(2, 2, 3, 4)).get();

        assertThat(result.getWinner()).isSameAs(away);
    }

    @Test
    void tiedScoreWithoutPenalties_hasNoWinner() {
        assertThat(MatchWinner.of(match(1, 1, null, null))).isEmpty();
    }

    @Test
    void tiedScoreWithEqualPenalties_hasNoWinner() {
        assertThat(MatchWinner.of(match(1, 1, 4, 4))).isEmpty();
    }

    @Test
    void tiedScoreWithOnlyOneSidePenalties_hasNoWinner() {
        assertThat(MatchWinner.of(match(1, 1, 4, null))).isEmpty();
    }
}
