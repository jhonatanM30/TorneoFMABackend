package com.marin.mas_que_amigos.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

/**
 * Splits the teams of a tournament into groups. Pure logic with no Spring or
 * database dependencies; the {@link Random} is injectable so the draw can be
 * repeated in tests.
 */
public class GroupDraw {

    private final Random random;

    public GroupDraw(Random random) {
        this.random = random;
    }

    /**
     * Shuffles the teams and deals them one by one to the groups, so the group
     * sizes differ by at most 1 (the first groups get the extra teams).
     */
    public List<List<Long>> draw(List<Long> teamIds, int groupCount) {
        List<Long> shuffled = new ArrayList<>(teamIds);
        Collections.shuffle(shuffled, random);

        List<List<Long>> groups = new ArrayList<>();
        for (int i = 0; i < groupCount; i++) {
            groups.add(new ArrayList<Long>());
        }
        for (int i = 0; i < shuffled.size(); i++) {
            groups.get(i % groupCount).add(shuffled.get(i));
        }
        return groups;
    }

    /** "A" for index 0, "B" for 1... */
    public static String groupName(int index) {
        return String.valueOf((char) ('A' + index));
    }

    /**
     * Checks a composition chosen by hand: the requested number of groups,
     * every enrolled team exactly once and no team that is not enrolled.
     *
     * @return the reason, in Spanish, or empty when the composition is valid
     */
    public static Optional<String> validateManual(List<List<Long>> groups, Collection<Long> enrolledTeamIds, int groupCount) {
        if (groups.size() != groupCount) {
            return Optional.of("La cantidad de grupos indicada (" + groups.size()
                    + ") no coincide con la cantidad de grupos pedida (" + groupCount + ").");
        }
        Set<Long> seen = new HashSet<>();
        for (List<Long> group : groups) {
            for (Long teamId : group) {
                if (!enrolledTeamIds.contains(teamId)) {
                    return Optional.of("El equipo con Id " + teamId + " no está inscrito en el torneo.");
                }
                if (!seen.add(teamId)) {
                    return Optional.of("La composición repite al equipo con Id " + teamId + ".");
                }
            }
        }
        if (seen.size() != enrolledTeamIds.size()) {
            return Optional.of("La composición debe incluir a todos los equipos inscritos, cada uno una sola vez.");
        }
        return Optional.empty();
    }
}
