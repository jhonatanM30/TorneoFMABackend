package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.marin.mas_que_amigos.service.LeagueScheduleGenerator.Pairing;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LeagueScheduleGeneratorTest {

    private final LeagueScheduleGenerator generator = new LeagueScheduleGenerator();

    private List<Long> teams(int count) {
        List<Long> ids = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            ids.add(id);
        }
        return ids;
    }

    private String pairKey(Pairing pairing) {
        long low = Math.min(pairing.getHomeTeamId(), pairing.getAwayTeamId());
        long high = Math.max(pairing.getHomeTeamId(), pairing.getAwayTeamId());
        return low + "-" + high;
    }

    @Test
    void fourTeams_threeRoundsWithEveryTeamPlayingOncePerRound() {
        List<List<Pairing>> rounds = generator.generate(teams(4));

        assertThat(rounds).hasSize(3);
        Set<String> pairs = new HashSet<>();
        for (List<Pairing> round : rounds) {
            assertThat(round).hasSize(2);
            Set<Long> playing = new HashSet<>();
            for (Pairing pairing : round) {
                playing.add(pairing.getHomeTeamId());
                playing.add(pairing.getAwayTeamId());
                pairs.add(pairKey(pairing));
            }
            assertThat(playing).containsExactlyInAnyOrder(1L, 2L, 3L, 4L);
        }
        assertThat(pairs).hasSize(6);
    }

    @Test
    void fiveTeams_fiveRoundsAndEachTeamRestsExactlyOnce() {
        List<List<Pairing>> rounds = generator.generate(teams(5));

        assertThat(rounds).hasSize(5);
        Map<Long, Integer> rests = new HashMap<>();
        Set<String> pairs = new HashSet<>();
        for (List<Pairing> round : rounds) {
            assertThat(round).hasSize(2);
            Set<Long> playing = new HashSet<>();
            for (Pairing pairing : round) {
                playing.add(pairing.getHomeTeamId());
                playing.add(pairing.getAwayTeamId());
                pairs.add(pairKey(pairing));
            }
            for (long id : teams(5)) {
                if (!playing.contains(id)) {
                    rests.merge(id, 1, Integer::sum);
                }
            }
        }
        assertThat(pairs).hasSize(10);
        assertThat(rests.values()).containsOnly(1);
        assertThat(rests).hasSize(5);
    }

    @Test
    void twoTeams_oneRoundWithOneMatch() {
        List<List<Pairing>> rounds = generator.generate(teams(2));

        assertThat(rounds).hasSize(1);
        assertThat(rounds.get(0)).hasSize(1);
    }

    @Test
    void fewerThanTwoTeams_noRounds() {
        assertThat(generator.generate(Collections.<Long>emptyList())).isEmpty();
        assertThat(generator.generate(teams(1))).isEmpty();
    }

    @Test
    void everyPairPlaysExactlyOnce_forTwoToTwelveTeams() {
        for (int count = 2; count <= 12; count++) {
            Set<String> pairs = new HashSet<>();
            int matches = 0;
            for (List<Pairing> round : generator.generate(teams(count))) {
                for (Pairing pairing : round) {
                    pairs.add(pairKey(pairing));
                    matches++;
                }
            }
            assertThat(matches).as("matches for %d teams", count).isEqualTo(count * (count - 1) / 2);
            assertThat(pairs).as("distinct pairs for %d teams", count).hasSize(count * (count - 1) / 2);
        }
    }

    @Test
    void homeAwayBalance_neverDiffersByMoreThanOne_forTwoToTwelveTeams() {
        for (int count = 2; count <= 12; count++) {
            Map<Long, Integer> balance = new HashMap<>();
            for (long id : teams(count)) {
                balance.put(id, 0);
            }
            for (List<Pairing> round : generator.generate(teams(count))) {
                for (Pairing pairing : round) {
                    balance.merge(pairing.getHomeTeamId(), 1, Integer::sum);
                    balance.merge(pairing.getAwayTeamId(), -1, Integer::sum);
                }
            }
            for (Map.Entry<Long, Integer> entry : balance.entrySet()) {
                assertThat(Math.abs(entry.getValue()))
                        .as("home - away for team %d with %d teams", entry.getKey(), count)
                        .isLessThanOrEqualTo(1);
            }
        }
    }
}
