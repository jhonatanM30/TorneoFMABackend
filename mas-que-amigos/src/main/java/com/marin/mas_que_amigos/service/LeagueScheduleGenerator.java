package com.marin.mas_que_amigos.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.Value;

/**
 * Builds a single round-robin schedule (every pair meets once) with the circle
 * method. Pure logic, with no Spring or database dependencies.
 *
 * With an odd number of teams a dummy "rest" slot is added, so in every round
 * one team does not play. Each round has every team playing at most once.
 *
 * Home advantage comes from a cyclic orientation of the pairs, independent of
 * the round: with N slots (N odd, counting the dummy slot when needed), slot i
 * is home against the next (N - 1) / 2 slots and away against the rest. Every
 * team then has the same number of home and away games, or differs by one
 * when the real team count is even (the missing game is against the dummy).
 */
public class LeagueScheduleGenerator {

    @Value
    public static class Pairing {

        long homeTeamId;
        long awayTeamId;
    }

    /**
     * @param teamIds the teams, in the order that decides the first pairings
     * @return the rounds, each one with its matches; empty with fewer than 2 teams
     */
    public List<List<Pairing>> generate(List<Long> teamIds) {
        if (teamIds.size() < 2) {
            return Collections.emptyList();
        }

        // null is the dummy rest slot used when the number of teams is odd.
        List<Long> slots = new ArrayList<>(teamIds);
        if (slots.size() % 2 != 0) {
            slots.add(null);
        }
        int size = slots.size();
        int rounds = size - 1;

        Map<Long, Integer> slotOf = new HashMap<>();
        for (int index = 0; index < teamIds.size(); index++) {
            slotOf.put(teamIds.get(index), index);
        }
        // Odd number of slots, so the cyclic orientation below is exact.
        int cycle = teamIds.size() % 2 == 0 ? teamIds.size() + 1 : teamIds.size();

        List<List<Pairing>> schedule = new ArrayList<>();
        for (int round = 0; round < rounds; round++) {
            List<Pairing> matches = new ArrayList<>();
            for (int i = 0; i < size / 2; i++) {
                Long first = slots.get(i);
                Long second = slots.get(size - 1 - i);
                if (first == null || second == null) {
                    continue;
                }
                int distance = Math.floorMod(slotOf.get(second) - slotOf.get(first), cycle);
                boolean firstIsHome = distance <= (cycle - 1) / 2;
                matches.add(firstIsHome ? new Pairing(first, second) : new Pairing(second, first));
            }
            schedule.add(matches);

            // The first slot stays fixed and the others rotate one place.
            slots.add(1, slots.remove(size - 1));
        }
        return schedule;
    }
}
