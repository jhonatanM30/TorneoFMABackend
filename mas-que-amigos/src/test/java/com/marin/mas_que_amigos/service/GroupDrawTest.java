package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class GroupDrawTest {

    private List<Long> teams(int count) {
        List<Long> ids = new ArrayList<>();
        for (long id = 1; id <= count; id++) {
            ids.add(id);
        }
        return ids;
    }

    private List<Integer> sizes(List<List<Long>> groups) {
        return groups.stream().map(List::size).collect(Collectors.toList());
    }

    @Test
    void eightTeamsInTwoGroups_fourAndFour() {
        List<List<Long>> groups = new GroupDraw(new Random(1)).draw(teams(8), 2);

        assertThat(sizes(groups)).containsExactly(4, 4);
    }

    @Test
    void nineTeamsInTwoGroups_fiveAndFour() {
        List<List<Long>> groups = new GroupDraw(new Random(1)).draw(teams(9), 2);

        assertThat(sizes(groups)).containsExactly(5, 4);
    }

    @Test
    void tenTeamsInFourGroups_threeThreeTwoTwo() {
        List<List<Long>> groups = new GroupDraw(new Random(1)).draw(teams(10), 4);

        assertThat(sizes(groups)).containsExactly(3, 3, 2, 2);
    }

    @Test
    void everyTeamAppearsExactlyOnce() {
        List<List<Long>> groups = new GroupDraw(new Random(7)).draw(teams(11), 4);

        List<Long> all = groups.stream().flatMap(List::stream).collect(Collectors.toList());
        assertThat(all).hasSize(11);
        assertThat(new HashSet<>(all)).hasSize(11);
    }

    @Test
    void theSameSeedRepeatsTheDraw_andAnotherSeedChangesIt() {
        List<List<Long>> first = new GroupDraw(new Random(42)).draw(teams(16), 4);
        List<List<Long>> same = new GroupDraw(new Random(42)).draw(teams(16), 4);
        boolean someDifferent = false;
        for (long seed = 1; seed <= 10; seed++) {
            if (!new GroupDraw(new Random(seed)).draw(teams(16), 4).equals(first)) {
                someDifferent = true;
            }
        }

        assertThat(same).isEqualTo(first);
        assertThat(someDifferent).isTrue();
    }

    @Test
    void groupNamesAreLettersFromA() {
        assertThat(GroupDraw.groupName(0)).isEqualTo("A");
        assertThat(GroupDraw.groupName(1)).isEqualTo("B");
        assertThat(GroupDraw.groupName(7)).isEqualTo("H");
    }

    // ---------------------------------------------------------------- manual composition

    private final Set<Long> enrolled = new HashSet<>(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L));

    @Test
    void validManualComposition_hasNoError() {
        Optional<String> error = GroupDraw.validateManual(
                Arrays.asList(Arrays.asList(1L, 2L, 3L), Arrays.asList(4L, 5L, 6L)), enrolled, 2);

        assertThat(error).isEmpty();
    }

    @Test
    void manualCompositionThatOmitsATeam_isRejected() {
        Optional<String> error = GroupDraw.validateManual(
                Arrays.asList(Arrays.asList(1L, 2L), Arrays.asList(4L, 5L, 6L)), enrolled, 2);

        assertThat(error).isPresent();
        assertThat(error.get()).contains("todos los equipos inscritos");
    }

    @Test
    void manualCompositionWithARepeatedTeam_isRejected() {
        Optional<String> error = GroupDraw.validateManual(
                Arrays.asList(Arrays.asList(1L, 2L, 3L), Arrays.asList(3L, 5L, 6L)), enrolled, 2);

        assertThat(error).isPresent();
        assertThat(error.get()).contains("repite");
    }

    @Test
    void manualCompositionWithATeamThatIsNotEnrolled_isRejected() {
        Optional<String> error = GroupDraw.validateManual(
                Arrays.asList(Arrays.asList(1L, 2L, 3L), Arrays.asList(4L, 5L, 99L)), enrolled, 2);

        assertThat(error).isPresent();
        assertThat(error.get()).contains("no está inscrito");
    }

    @Test
    void manualCompositionWithAnotherNumberOfGroups_isRejected() {
        Optional<String> error = GroupDraw.validateManual(
                Arrays.asList(Arrays.asList(1L, 2L, 3L, 4L, 5L, 6L)), enrolled, 2);

        assertThat(error).isPresent();
        assertThat(error.get()).contains("cantidad de grupos");
    }
}
