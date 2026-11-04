package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.marin.mas_que_amigos.model.BracketTieKind;
import com.marin.mas_que_amigos.service.BracketBuilder.Node;
import com.marin.mas_que_amigos.service.BracketBuilder.Pairing;
import com.marin.mas_que_amigos.service.BracketBuilder.Plan;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class BracketBuilderTest {

    private final BracketBuilder builder = new BracketBuilder();

    /** Pairings (1,2), (3,4), ... for 2 * pairs teams. */
    private List<Pairing> firstRound(int pairs) {
        List<Pairing> pairings = new ArrayList<>();
        for (int i = 0; i < pairs; i++) {
            pairings.add(new Pairing(2L * i + 1, 2L * i + 2));
        }
        return pairings;
    }

    private Pairing pairing(long home, long away) {
        return new Pairing(home, away);
    }

    // ---------------------------------------------------------------- shape

    @Test
    void eightTeams_threeRoundsWithFourTwoAndOneTies() {
        Plan plan = builder.build(firstRound(4), true);

        assertThat(plan.getRounds()).hasSize(3);
        assertThat(plan.getRounds().get(0)).hasSize(4);
        assertThat(plan.getRounds().get(1)).hasSize(2);
        assertThat(plan.getRounds().get(2)).hasSize(1);
        assertThat(plan.getRounds().get(2).get(0).getKind()).isEqualTo(BracketTieKind.MAIN);
    }

    @Test
    void onlyTheFirstRoundHasTeams_andTheyAreTheGivenPairingsInOrder() {
        Plan plan = builder.build(Arrays.asList(pairing(1, 2), pairing(3, 4), pairing(5, 6), pairing(7, 8)), true);

        List<Node> first = plan.getRounds().get(0);
        assertThat(first).extracting(Node::getHomeTeamId).containsExactly(1L, 3L, 5L, 7L);
        assertThat(first).extracting(Node::getAwayTeamId).containsExactly(2L, 4L, 6L, 8L);
        for (Node node : plan.getRounds().get(1)) {
            assertThat(node.getHomeTeamId()).isNull();
            assertThat(node.getAwayTeamId()).isNull();
        }
        assertThat(plan.getRounds().get(2).get(0).getHomeTeamId()).isNull();
    }

    @Test
    void positionsAndRoundNumbersAreConsecutiveFromOneAndZero() {
        Plan plan = builder.build(firstRound(4), true);

        for (int round = 0; round < plan.getRounds().size(); round++) {
            for (int position = 0; position < plan.getRounds().get(round).size(); position++) {
                Node node = plan.getRounds().get(round).get(position);
                assertThat(node.getRound()).isEqualTo(round + 1);
                assertThat(node.getPosition()).isEqualTo(position);
            }
        }
    }

    // ---------------------------------------------------------------- winner links

    @Test
    void winnersOfConsecutiveTiesFeedTheSameNextTie_theFirstAsHomeAndTheSecondAsAway() {
        Plan plan = builder.build(firstRound(4), true);
        List<Node> quarters = plan.getRounds().get(0);
        List<Node> semis = plan.getRounds().get(1);

        assertThat(quarters.get(0).getWinnerNext()).isSameAs(semis.get(0));
        assertThat(quarters.get(0).isWinnerNextIsHome()).isTrue();
        assertThat(quarters.get(1).getWinnerNext()).isSameAs(semis.get(0));
        assertThat(quarters.get(1).isWinnerNextIsHome()).isFalse();
        assertThat(quarters.get(2).getWinnerNext()).isSameAs(semis.get(1));
        assertThat(quarters.get(2).isWinnerNextIsHome()).isTrue();
        assertThat(quarters.get(3).getWinnerNext()).isSameAs(semis.get(1));
        assertThat(quarters.get(3).isWinnerNextIsHome()).isFalse();
        assertThat(semis.get(0).getWinnerNext()).isSameAs(plan.getRounds().get(2).get(0));
        assertThat(semis.get(1).getWinnerNext()).isSameAs(plan.getRounds().get(2).get(0));
    }

    @Test
    void theFinalHasNoNextTie() {
        Plan plan = builder.build(firstRound(4), true);

        assertThat(plan.getRounds().get(2).get(0).getWinnerNext()).isNull();
        assertThat(plan.getRounds().get(2).get(0).getLoserNext()).isNull();
    }

    // ---------------------------------------------------------------- third place

    @Test
    void withThirdPlace_theLosersOfTheSemifinalsFeedTheThirdPlaceTie() {
        Plan plan = builder.build(firstRound(4), true);
        List<Node> semis = plan.getRounds().get(1);

        assertThat(plan.getThirdPlace()).isNotNull();
        assertThat(plan.getThirdPlace().getKind()).isEqualTo(BracketTieKind.THIRD_PLACE);
        assertThat(plan.getThirdPlace().getRound()).isEqualTo(3);
        assertThat(semis.get(0).getLoserNext()).isSameAs(plan.getThirdPlace());
        assertThat(semis.get(0).isLoserNextIsHome()).isTrue();
        assertThat(semis.get(1).getLoserNext()).isSameAs(plan.getThirdPlace());
        assertThat(semis.get(1).isLoserNextIsHome()).isFalse();
    }

    @Test
    void withoutThirdPlace_thereIsNoThirdPlaceTieAndLosersGoNowhere() {
        Plan plan = builder.build(firstRound(4), false);

        assertThat(plan.getThirdPlace()).isNull();
        assertThat(plan.getRounds().get(1).get(0).getLoserNext()).isNull();
        assertThat(plan.getRounds().get(1).get(1).getLoserNext()).isNull();
    }

    @Test
    void fourTeams_semifinalsAreTheFirstRound() {
        Plan plan = builder.build(firstRound(2), true);

        assertThat(plan.getRounds()).hasSize(2);
        assertThat(plan.getRounds().get(0)).hasSize(2);
        assertThat(plan.getThirdPlace()).isNotNull();
        assertThat(plan.getRounds().get(0).get(0).getLoserNext()).isSameAs(plan.getThirdPlace());
    }

    @Test
    void twoTeams_aSingleRoundWithTheFinalAndNoThirdPlace() {
        Plan plan = builder.build(firstRound(1), true);

        assertThat(plan.getRounds()).hasSize(1);
        assertThat(plan.getRounds().get(0)).hasSize(1);
        assertThat(plan.getThirdPlace()).isNull();
        assertThat(plan.getRounds().get(0).get(0).getWinnerNext()).isNull();
    }

    @Test
    void aNumberOfPairingsThatIsNotAPowerOfTwo_isRejected() {
        assertThatThrownBy(() -> builder.build(firstRound(3), true)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> builder.build(Collections.<Pairing>emptyList(), true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // ---------------------------------------------------------------- group qualifiers

    private List<Long> group(long first, Long second) {
        return second == null ? Collections.singletonList(first) : Arrays.asList(first, second);
    }

    /** Team ids: 1st of group A = 11, 2nd of A = 12, 1st of B = 21, 2nd of B = 22, and so on. */
    @Test
    void twoGroupsWithTwoQualifiers_crossFirstAgainstSecondOfTheOtherGroup() {
        List<Pairing> pairings = builder.groupPairings(Arrays.asList(group(11, 12L), group(21, 22L)));

        assertThat(pairings).containsExactly(pairing(11, 22), pairing(21, 12));
    }

    @Test
    void fourGroupsWithTwoQualifiers_firstTheFirstsOfEachPairOfGroupsThenTheSeconds() {
        List<Pairing> pairings = builder.groupPairings(
                Arrays.asList(group(11, 12L), group(21, 22L), group(31, 32L), group(41, 42L)));

        // 1A-2B, 1C-2D, 1B-2A, 1D-2C
        assertThat(pairings).containsExactly(pairing(11, 22), pairing(31, 42), pairing(21, 12), pairing(41, 32));
    }

    @Test
    void fourGroupsWithOneQualifier_firstsOfTheGroupPairs() {
        List<Pairing> pairings = builder.groupPairings(
                Arrays.asList(group(11, null), group(21, null), group(31, null), group(41, null)));

        assertThat(pairings).containsExactly(pairing(11, 21), pairing(31, 41));
    }

    @Test
    void twoGroupsWithOneQualifier_theTwoFirstsMeet() {
        List<Pairing> pairings = builder.groupPairings(Arrays.asList(group(11, null), group(21, null)));

        assertThat(pairings).containsExactly(pairing(11, 21));
    }

    @Test
    void teamsOfTheSameGroupEndUpInOppositeHalvesOfTheBracket() {
        // 4 groups, 2 qualifiers: 8 teams. 1st and 2nd of each group must be in different halves.
        List<Pairing> pairings = builder.groupPairings(
                Arrays.asList(group(11, 12L), group(21, 22L), group(31, 32L), group(41, 42L)));
        Plan plan = builder.build(pairings, true);

        List<Node> quarters = plan.getRounds().get(0);
        // Halves: quarters 0-1 feed semifinal 0, quarters 2-3 feed semifinal 1.
        for (long[] group : new long[][]{{11, 12}, {21, 22}, {31, 32}, {41, 42}}) {
            int firstHalf = halfOf(quarters, group[0]);
            int secondHalf = halfOf(quarters, group[1]);
            assertThat(firstHalf).as("halves of group %d", group[0]).isNotEqualTo(secondHalf);
        }
    }

    private int halfOf(List<Node> quarters, long teamId) {
        for (int i = 0; i < quarters.size(); i++) {
            Node node = quarters.get(i);
            if (node.getHomeTeamId() == teamId || node.getAwayTeamId() == teamId) {
                return i / 2;
            }
        }
        throw new AssertionError("team not in the first round: " + teamId);
    }

    @Test
    void anOddNumberOfGroups_isRejected() {
        assertThatThrownBy(() -> builder.groupPairings(
                Arrays.asList(group(11, 12L), group(21, 22L), group(31, 32L))))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
