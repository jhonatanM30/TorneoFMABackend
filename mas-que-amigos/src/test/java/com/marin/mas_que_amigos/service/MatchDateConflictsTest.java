package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MatchDateConflictsTest {

    @Mock
    private PartidoRepository partidoRepository;

    private MatchDateConflicts conflicts;

    private Equipo team;

    @BeforeEach
    void setUp() {
        conflicts = new MatchDateConflicts(partidoRepository);
        team = new Equipo();
        team.setId(2L);
        team.setNombre("Socios.com");
    }

    @Test
    void requireFree_teamWithoutMatchThatDate_doesNothing() {
        when(partidoRepository.existePartidoEnFechaParaEquipo(LocalDate.of(2027, 3, 13), 2L)).thenReturn(false);

        assertThatCode(() -> conflicts.requireFree(LocalDate.of(2027, 3, 13), team)).doesNotThrowAnyException();
    }

    @Test
    void requireFree_teamAlreadyPlaying_isRejectedNamingTheTeamAndTheDate() {
        when(partidoRepository.existePartidoEnFechaParaEquipo(LocalDate.of(2027, 3, 13), 2L)).thenReturn(true);

        assertThatThrownBy(() -> conflicts.requireFree(LocalDate.of(2027, 3, 13), team))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Socios.com")
                .hasMessageContaining("2027-03-13");
    }
}
