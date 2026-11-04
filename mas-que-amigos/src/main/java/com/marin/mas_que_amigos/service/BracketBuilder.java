package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.model.BracketTieKind;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;

/**
 * Lays out a knockout bracket from the pairings of its first round. Pure logic
 * with no Spring or database dependencies.
 *
 * The winners of two consecutive ties of a round meet in the next round: the
 * winner of tie {@code 2k} is the home team and the winner of tie {@code 2k + 1}
 * the away team of tie {@code k} of the next round. The losers of the two
 * semifinals feed the optional third place tie.
 */
public class BracketBuilder {

    @Value
    public static class Pairing {

        long homeTeamId;
        long awayTeamId;
    }

    /** A tie of the bracket. Only the first round has teams when the plan is built. */
    @Getter
    @Setter
    public static class Node {

        private BracketTieKind kind;
        /** 1-based round number. */
        private int round;
        /** 0-based position inside the round. */
        private int position;
        private Long homeTeamId;
        private Long awayTeamId;
        private Node winnerNext;
        private boolean winnerNextIsHome;
        private Node loserNext;
        private boolean loserNextIsHome;
    }

    @Getter
    public static class Plan {

        private final List<List<Node>> rounds;
        private final Node thirdPlace;

        Plan(List<List<Node>> rounds, Node thirdPlace) {
            this.rounds = rounds;
            this.thirdPlace = thirdPlace;
        }
    }

    /**
     * @param firstRound the pairings of the first round, in bracket order; their
     *        count must be a power of 2 (the teams are twice that)
     * @param thirdPlace whether to include the third place tie (ignored with fewer than 4 teams)
     */
    public Plan build(List<Pairing> firstRound, boolean thirdPlace) {
        int pairs = firstRound.size();
        if (pairs < 1 || Integer.bitCount(pairs) != 1) {
            throw new IllegalArgumentException("The number of first round pairings must be a power of 2: " + pairs);
        }

        List<List<Node>> rounds = new ArrayList<>();
        int roundNumber = 1;
        for (int size = pairs; size >= 1; size /= 2) {
            List<Node> round = new ArrayList<>();
            for (int position = 0; position < size; position++) {
                Node node = new Node();
                node.setKind(BracketTieKind.MAIN);
                node.setRound(roundNumber);
                node.setPosition(position);
                if (roundNumber == 1) {
                    node.setHomeTeamId(firstRound.get(position).getHomeTeamId());
                    node.setAwayTeamId(firstRound.get(position).getAwayTeamId());
                }
                round.add(node);
            }
            rounds.add(round);
            roundNumber++;
        }

        for (int r = 0; r < rounds.size() - 1; r++) {
            List<Node> round = rounds.get(r);
            List<Node> next = rounds.get(r + 1);
            for (int position = 0; position < round.size(); position++) {
                round.get(position).setWinnerNext(next.get(position / 2));
                round.get(position).setWinnerNextIsHome(position % 2 == 0);
            }
        }

        Node thirdPlaceNode = null;
        if (thirdPlace && rounds.size() >= 2) {
            thirdPlaceNode = new Node();
            thirdPlaceNode.setKind(BracketTieKind.THIRD_PLACE);
            thirdPlaceNode.setRound(rounds.size());
            thirdPlaceNode.setPosition(0);
            List<Node> semifinals = rounds.get(rounds.size() - 2);
            for (int position = 0; position < semifinals.size(); position++) {
                semifinals.get(position).setLoserNext(thirdPlaceNode);
                semifinals.get(position).setLoserNextIsHome(position == 0);
            }
        }
        return new Plan(rounds, thirdPlaceNode);
    }

    /**
     * First round pairings from the qualified teams of each group, in group
     * order (A, B, C...), each list ordered 1st, 2nd. Groups are paired two by
     * two (A with B, C with D...). With two qualifiers the first half of the
     * bracket lists "1st of X vs 2nd of Y" for every pair of groups and the
     * second half lists "1st of Y vs 2nd of X", so two teams of the same group
     * end up in opposite halves. With one qualifier the firsts of each pair of
     * groups meet.
     */
    public List<Pairing> groupPairings(List<List<Long>> qualifiersByGroup) {
        int groups = qualifiersByGroup.size();
        if (groups < 2 || groups % 2 != 0) {
            throw new IllegalArgumentException("The groups are paired two by two, so their number must be even: " + groups);
        }
        int qualifiers = qualifiersByGroup.get(0).size();

        List<Pairing> pairings = new ArrayList<>();
        if (qualifiers == 1) {
            for (int group = 0; group < groups; group += 2) {
                pairings.add(new Pairing(qualifiersByGroup.get(group).get(0), qualifiersByGroup.get(group + 1).get(0)));
            }
            return pairings;
        }
        if (qualifiers != 2) {
            throw new IllegalArgumentException("Only 1 or 2 qualifiers per group are supported: " + qualifiers);
        }
        for (int group = 0; group < groups; group += 2) {
            pairings.add(new Pairing(qualifiersByGroup.get(group).get(0), qualifiersByGroup.get(group + 1).get(1)));
        }
        for (int group = 0; group < groups; group += 2) {
            pairings.add(new Pairing(qualifiersByGroup.get(group + 1).get(0), qualifiersByGroup.get(group).get(1)));
        }
        return pairings;
    }
}
