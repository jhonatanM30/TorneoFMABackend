package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.StandingDTO;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Value;

/**
 * Builds a league table from finished matches. Pure logic with no Spring or
 * database dependencies, so it can be reused by other formats (for example,
 * one table per group).
 *
 * Teams are ordered by points. Teams with the same points are ordered, in
 * this order, by: head-to-head (points, then goal difference, then goals for,
 * counting only the matches among the tied teams), overall goal difference,
 * overall goals for, fewer fair play points, and team name. Head-to-head is
 * applied in a single pass: a subgroup that stays tied is not re-evaluated.
 */
public class LeagueStandingsCalculator {

    public static final int POINTS_PER_WIN = 3;
    public static final int POINTS_PER_DRAW = 1;
    public static final int POINTS_PER_LOSS = 0;

    // Fair play tie-breaker: fewer points are better.
    public static final int YELLOW_CARD_FAIR_PLAY_POINTS = 1;
    public static final int RED_CARD_FAIR_PLAY_POINTS = 2;

    /** Fair play points of a team from its yellow and red cards. */
    public static int fairPlayPoints(long yellowCards, long redCards) {
        return (int) (yellowCards * YELLOW_CARD_FAIR_PLAY_POINTS + redCards * RED_CARD_FAIR_PLAY_POINTS);
    }

    @Value
    public static class TeamRef {

        long id;
        String name;
    }

    @Value
    public static class MatchResult {

        long homeTeamId;
        long awayTeamId;
        int homeGoals;
        int awayGoals;
    }

    /** Totals of a team, either over every match or only among a group of teams. */
    private static class Totals {

        int played;
        int won;
        int drawn;
        int lost;
        int goalsFor;
        int goalsAgainst;

        int points() {
            return won * POINTS_PER_WIN + drawn * POINTS_PER_DRAW + lost * POINTS_PER_LOSS;
        }

        int goalDifference() {
            return goalsFor - goalsAgainst;
        }

        void add(int scored, int conceded) {
            played++;
            goalsFor += scored;
            goalsAgainst += conceded;
            if (scored > conceded) {
                won++;
            } else if (scored == conceded) {
                drawn++;
            } else {
                lost++;
            }
        }
    }

    /**
     * @param teams every team of the table, including those without matches
     * @param results finished matches only
     * @param fairPlayPoints fair play points by team id (missing means 0)
     * @return the table ordered by position, with positions 1..n
     */
    public List<StandingDTO> calculate(List<TeamRef> teams, List<MatchResult> results, Map<Long, Integer> fairPlayPoints) {
        Map<Long, TeamRef> teamsById = new LinkedHashMap<>();
        for (TeamRef team : teams) {
            teamsById.put(team.getId(), team);
        }
        Map<Long, Totals> overall = totals(teamsById.keySet(), results);

        List<TeamRef> ordered = new ArrayList<>(teams);
        ordered.sort(Comparator.comparingInt((TeamRef t) -> overall.get(t.getId()).points()).reversed()
                .thenComparing(nameOrder()));

        List<TeamRef> finalOrder = new ArrayList<>();
        int start = 0;
        while (start < ordered.size()) {
            int end = start + 1;
            int points = overall.get(ordered.get(start).getId()).points();
            while (end < ordered.size() && overall.get(ordered.get(end).getId()).points() == points) {
                end++;
            }
            List<TeamRef> tied = new ArrayList<>(ordered.subList(start, end));
            if (tied.size() > 1) {
                sortTiedGroup(tied, overall, results, fairPlayPoints);
            }
            finalOrder.addAll(tied);
            start = end;
        }

        List<StandingDTO> table = new ArrayList<>();
        int position = 1;
        for (TeamRef team : finalOrder) {
            table.add(toRow(position++, team, overall.get(team.getId()), fairPlayPoints));
        }
        return table;
    }

    private void sortTiedGroup(List<TeamRef> tied, Map<Long, Totals> overall, List<MatchResult> results,
            Map<Long, Integer> fairPlayPoints) {
        Set<Long> ids = new HashSet<>();
        for (TeamRef team : tied) {
            ids.add(team.getId());
        }
        // Only the matches played among the tied teams count for head-to-head.
        List<MatchResult> amongThem = new ArrayList<>();
        for (MatchResult result : results) {
            if (ids.contains(result.getHomeTeamId()) && ids.contains(result.getAwayTeamId())) {
                amongThem.add(result);
            }
        }
        Map<Long, Totals> headToHead = totals(ids, amongThem);

        tied.sort(Comparator
                .comparingInt((TeamRef t) -> headToHead.get(t.getId()).points()).reversed()
                .thenComparing(Comparator.comparingInt((TeamRef t) -> headToHead.get(t.getId()).goalDifference()).reversed())
                .thenComparing(Comparator.comparingInt((TeamRef t) -> headToHead.get(t.getId()).goalsFor).reversed())
                .thenComparing(Comparator.comparingInt((TeamRef t) -> overall.get(t.getId()).goalDifference()).reversed())
                .thenComparing(Comparator.comparingInt((TeamRef t) -> overall.get(t.getId()).goalsFor).reversed())
                .thenComparingInt((TeamRef t) -> fairPlayPoints.getOrDefault(t.getId(), 0))
                .thenComparing(nameOrder()));
    }

    private Map<Long, Totals> totals(Iterable<Long> teamIds, List<MatchResult> results) {
        Map<Long, Totals> totals = new HashMap<>();
        for (Long id : teamIds) {
            totals.put(id, new Totals());
        }
        for (MatchResult result : results) {
            Totals home = totals.get(result.getHomeTeamId());
            Totals away = totals.get(result.getAwayTeamId());
            if (home != null) {
                home.add(result.getHomeGoals(), result.getAwayGoals());
            }
            if (away != null) {
                away.add(result.getAwayGoals(), result.getHomeGoals());
            }
        }
        return totals;
    }

    private Comparator<TeamRef> nameOrder() {
        // Case-insensitive first, then case-sensitive so the order is always deterministic.
        return Comparator.comparing(TeamRef::getName, String.CASE_INSENSITIVE_ORDER)
                .thenComparing(TeamRef::getName)
                .thenComparingLong(TeamRef::getId);
    }

    private StandingDTO toRow(int position, TeamRef team, Totals totals, Map<Long, Integer> fairPlayPoints) {
        StandingDTO row = new StandingDTO();
        row.setPosition(position);
        row.setTeamId(team.getId());
        row.setTeamName(team.getName());
        row.setPlayed(totals.played);
        row.setWon(totals.won);
        row.setDrawn(totals.drawn);
        row.setLost(totals.lost);
        row.setGoalsFor(totals.goalsFor);
        row.setGoalsAgainst(totals.goalsAgainst);
        row.setGoalDifference(totals.goalDifference());
        row.setPoints(totals.points());
        row.setFairPlayPoints(fairPlayPoints.getOrDefault(team.getId(), 0));
        return row;
    }
}
