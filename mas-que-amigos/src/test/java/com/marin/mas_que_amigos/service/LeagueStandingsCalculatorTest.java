package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marin.mas_que_amigos.dto.StandingDTO;
import com.marin.mas_que_amigos.service.LeagueStandingsCalculator.MatchResult;
import com.marin.mas_que_amigos.service.LeagueStandingsCalculator.TeamRef;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class LeagueStandingsCalculatorTest {

    private final LeagueStandingsCalculator calculator = new LeagueStandingsCalculator();

    private TeamRef team(long id, String name) {
        return new TeamRef(id, name);
    }

    private MatchResult result(long home, long away, int homeGoals, int awayGoals) {
        return new MatchResult(home, away, homeGoals, awayGoals);
    }

    private List<StandingDTO> calculate(List<TeamRef> teams, List<MatchResult> results) {
        return calculator.calculate(teams, results, Collections.<Long, Integer>emptyMap());
    }

    private StandingDTO row(List<StandingDTO> table, String teamName) {
        return table.stream().filter(r -> r.getTeamName().equals(teamName)).findFirst().orElseThrow(AssertionError::new);
    }

    private List<String> names(List<StandingDTO> table) {
        return table.stream().map(StandingDTO::getTeamName).collect(java.util.stream.Collectors.toList());
    }

    // ---------------------------------------------------------------- columns and points

    @Test
    void threeTeamExample_computesEveryColumn() {
        List<StandingDTO> table = calculate(
                Arrays.asList(team(1, "A"), team(2, "B"), team(3, "C")),
                Arrays.asList(result(1, 2, 2, 0), result(1, 3, 1, 1)));

        StandingDTO a = row(table, "A");
        assertThat(a.getPlayed()).isEqualTo(2);
        assertThat(a.getWon()).isEqualTo(1);
        assertThat(a.getDrawn()).isEqualTo(1);
        assertThat(a.getLost()).isZero();
        assertThat(a.getGoalsFor()).isEqualTo(3);
        assertThat(a.getGoalsAgainst()).isEqualTo(1);
        assertThat(a.getGoalDifference()).isEqualTo(2);
        assertThat(a.getPoints()).isEqualTo(4);

        StandingDTO b = row(table, "B");
        assertThat(b.getPlayed()).isEqualTo(1);
        assertThat(b.getLost()).isEqualTo(1);
        assertThat(b.getGoalsFor()).isZero();
        assertThat(b.getGoalsAgainst()).isEqualTo(2);
        assertThat(b.getGoalDifference()).isEqualTo(-2);
        assertThat(b.getPoints()).isZero();

        StandingDTO c = row(table, "C");
        assertThat(c.getPlayed()).isEqualTo(1);
        assertThat(c.getDrawn()).isEqualTo(1);
        assertThat(c.getGoalsFor()).isEqualTo(1);
        assertThat(c.getGoalsAgainst()).isEqualTo(1);
        assertThat(c.getGoalDifference()).isZero();
        assertThat(c.getPoints()).isEqualTo(1);
    }

    @Test
    void teamWithoutMatches_appearsWithEverythingAtZero() {
        List<StandingDTO> table = calculate(
                Arrays.asList(team(1, "A"), team(2, "B"), team(3, "Idle")),
                Collections.singletonList(result(1, 2, 1, 0)));

        StandingDTO idle = row(table, "Idle");
        assertThat(idle.getPlayed()).isZero();
        assertThat(idle.getWon()).isZero();
        assertThat(idle.getDrawn()).isZero();
        assertThat(idle.getLost()).isZero();
        assertThat(idle.getGoalsFor()).isZero();
        assertThat(idle.getGoalsAgainst()).isZero();
        assertThat(idle.getGoalDifference()).isZero();
        assertThat(idle.getPoints()).isZero();
        assertThat(idle.getFairPlayPoints()).isZero();
    }

    @Test
    void winIsWorthThree_drawOne_lossZero() {
        List<StandingDTO> table = calculate(
                Arrays.asList(team(1, "A"), team(2, "B"), team(3, "C"), team(4, "D"), team(5, "E"), team(6, "F")),
                Arrays.asList(result(1, 2, 3, 1), result(3, 4, 0, 0), result(5, 6, 0, 2)));

        assertThat(row(table, "A").getPoints()).isEqualTo(3);
        assertThat(row(table, "F").getPoints()).isEqualTo(3);
        assertThat(row(table, "C").getPoints()).isEqualTo(1);
        assertThat(row(table, "D").getPoints()).isEqualTo(1);
        assertThat(row(table, "B").getPoints()).isZero();
        assertThat(row(table, "E").getPoints()).isZero();
    }

    @Test
    void fairPlayPoints_areCopiedFromTheInputMap() {
        Map<Long, Integer> fairPlay = new HashMap<>();
        fairPlay.put(1L, 5);

        List<StandingDTO> table = calculator.calculate(
                Arrays.asList(team(1, "A"), team(2, "B")), Collections.<MatchResult>emptyList(), fairPlay);

        assertThat(row(table, "A").getFairPlayPoints()).isEqualTo(5);
        assertThat(row(table, "B").getFairPlayPoints()).isZero();
    }

    // ---------------------------------------------------------------- tie-breakers

    @Test
    void tieOfTwo_headToHeadBeatsABetterOverallGoalDifference() {
        // A and B end with 4 points. A beat B, but B has the better overall goal difference.
        List<StandingDTO> table = calculate(
                Arrays.asList(team(1, "A"), team(2, "B"), team(3, "X"), team(4, "Y")),
                Arrays.asList(result(1, 2, 1, 0), result(1, 3, 0, 0), result(2, 4, 5, 0), result(2, 3, 0, 0)));

        assertThat(row(table, "A").getPoints()).isEqualTo(4);
        assertThat(row(table, "B").getPoints()).isEqualTo(4);
        assertThat(row(table, "B").getGoalDifference()).isGreaterThan(row(table, "A").getGoalDifference());
        assertThat(names(table)).containsExactly("A", "B", "X", "Y");
    }

    @Test
    void tieOfThree_usesTheMiniTableOfTheTiedTeams() {
        // B, A and C all end with 6 points; among themselves B beat A and C, and A beat C.
        // X and Y end with 3 points.
        List<StandingDTO> table = calculate(
                Arrays.asList(team(1, "A"), team(2, "B"), team(3, "C"), team(4, "X"), team(5, "Y")),
                Arrays.asList(
                        result(2, 1, 1, 0), result(2, 3, 1, 0), result(1, 3, 1, 0),
                        result(1, 4, 1, 0), result(3, 4, 1, 0), result(3, 5, 1, 0),
                        result(4, 2, 1, 0), result(5, 2, 1, 0)));

        assertThat(row(table, "A").getPoints()).isEqualTo(6);
        assertThat(row(table, "B").getPoints()).isEqualTo(6);
        assertThat(row(table, "C").getPoints()).isEqualTo(6);
        // By overall goal difference A would be first, so this proves the mini table decides.
        assertThat(row(table, "A").getGoalDifference()).isGreaterThan(row(table, "B").getGoalDifference());
        assertThat(names(table)).containsExactly("B", "A", "C", "Y", "X");
    }

    @Test
    void headToHeadDraw_fallsBackToOverallGoalDifference() {
        List<StandingDTO> table = calculate(
                Arrays.asList(team(1, "A"), team(2, "B"), team(3, "X")),
                Arrays.asList(result(1, 2, 1, 1), result(1, 3, 1, 0), result(2, 3, 3, 0)));

        assertThat(row(table, "A").getPoints()).isEqualTo(4);
        assertThat(row(table, "B").getPoints()).isEqualTo(4);
        assertThat(names(table)).containsExactly("B", "A", "X");
    }

    @Test
    void headToHeadNotPlayedYet_fallsBackToOverallGoalDifference() {
        List<StandingDTO> table = calculate(
                Arrays.asList(team(1, "A"), team(2, "B"), team(3, "X"), team(4, "Y")),
                Arrays.asList(result(1, 3, 1, 0), result(2, 4, 4, 0)));

        assertThat(names(table).subList(0, 2)).containsExactly("B", "A");
    }

    @Test
    void sameGoalDifference_moreGoalsForWins() {
        List<StandingDTO> table = calculate(
                Arrays.asList(team(1, "A"), team(2, "B"), team(3, "X"), team(4, "Y")),
                Arrays.asList(result(1, 3, 3, 2), result(2, 4, 1, 0)));

        assertThat(row(table, "A").getGoalDifference()).isEqualTo(row(table, "B").getGoalDifference());
        assertThat(names(table)).containsExactly("A", "B", "X", "Y");
    }

    @Test
    void sameEverything_fewerFairPlayPointsWins() {
        Map<Long, Integer> fairPlay = new HashMap<>();
        fairPlay.put(1L, 3);
        fairPlay.put(2L, 1);
        fairPlay.put(3L, 2);
        fairPlay.put(4L, 0);

        List<StandingDTO> table = calculator.calculate(
                Arrays.asList(team(1, "A"), team(2, "B"), team(3, "X"), team(4, "Y")),
                Arrays.asList(result(1, 3, 1, 0), result(2, 4, 1, 0)), fairPlay);

        assertThat(names(table)).containsExactly("B", "A", "Y", "X");
    }

    @Test
    void totalTie_isDecidedByTeamName() {
        // The team with the alphabetically first name goes first, whatever its id.
        List<StandingDTO> table = calculate(
                Arrays.asList(team(1, "Bravo"), team(2, "Alpha")), Collections.<MatchResult>emptyList());

        assertThat(names(table)).containsExactly("Alpha", "Bravo");
    }

    // ---------------------------------------------------------------- positions

    @Test
    void positions_areConsecutiveAndUnique() {
        List<StandingDTO> table = calculate(
                Arrays.asList(team(1, "A"), team(2, "B"), team(3, "C"), team(4, "D")),
                Arrays.asList(result(1, 2, 1, 1), result(3, 4, 1, 1)));

        assertThat(table).extracting(StandingDTO::getPosition).containsExactly(1, 2, 3, 4);
    }

    @Test
    void noMatches_allZeroAndSortedByName() {
        List<StandingDTO> table = calculate(
                Arrays.asList(team(1, "Charly"), team(2, "Alpha"), team(3, "Bravo")),
                Collections.<MatchResult>emptyList());

        assertThat(names(table)).containsExactly("Alpha", "Bravo", "Charly");
        assertThat(table).extracting(StandingDTO::getPoints).containsOnly(0);
        assertThat(table).extracting(StandingDTO::getPosition).containsExactly(1, 2, 3);
    }
}
