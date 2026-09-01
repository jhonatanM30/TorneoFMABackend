package com.marin.mas_que_amigos.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.model.Partido;
import org.junit.jupiter.api.Test;

class PartidoMapperTest {

    private final EquipoMapper equipoMapper = mock(EquipoMapper.class);
    private final PartidoMapper partidoMapper = new PartidoMapper(equipoMapper);

    @Test
    void toEntity_respetaFaseEnviada() {
        PartidoDTO dto = new PartidoDTO();
        dto.setIdEquipoLocal(1L);
        dto.setIdEquipoVisitante(2L);
        dto.setFase("ELIMINACION_DIRECTA");

        Partido partido = partidoMapper.toEntity(dto);

        assertThat(partido.getFase()).isEqualTo(Partido.Fase.ELIMINACION_DIRECTA);
    }

    @Test
    void toEntity_faseNulaUsaFaseDeGruposPorDefecto() {
        PartidoDTO dto = new PartidoDTO();
        dto.setIdEquipoLocal(1L);
        dto.setIdEquipoVisitante(2L);

        Partido partido = partidoMapper.toEntity(dto);

        assertThat(partido.getFase()).isEqualTo(Partido.Fase.FASE_DE_GRUPOS);
    }

    @Test
    void toEntity_faseInvalidaLanzaBusinessException() {
        PartidoDTO dto = new PartidoDTO();
        dto.setIdEquipoLocal(1L);
        dto.setIdEquipoVisitante(2L);
        dto.setFase("SEMIFINAL");

        assertThatThrownBy(() -> partidoMapper.toEntity(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("SEMIFINAL");
    }
}
