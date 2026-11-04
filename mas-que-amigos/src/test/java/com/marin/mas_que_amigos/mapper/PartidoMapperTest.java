package com.marin.mas_que_amigos.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.model.Tournament;
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
    void toDTO_incluyeElIdDelTorneoDelPartido() {
        Tournament torneo = new Tournament();
        torneo.setId(7L);
        Partido partido = new Partido();
        partido.setId(50L);
        partido.setFase(Partido.Fase.FASE_DE_GRUPOS);
        partido.setEstado(Partido.Estado.PROGRAMADO);
        partido.setTorneo(torneo);

        PartidoDTO dto = partidoMapper.toDTO(partido);

        assertThat(dto.getIdTorneo()).isEqualTo(7L);
    }

    @Test
    void toDTO_incluyeLosPenalesCuandoExisten() {
        Partido partido = new Partido();
        partido.setId(50L);
        partido.setFase(Partido.Fase.FINAL);
        partido.setEstado(Partido.Estado.EN_CURSO);
        partido.setPenalesLocal(5);
        partido.setPenalesVisitante(4);

        PartidoDTO dto = partidoMapper.toDTO(partido);

        assertThat(dto.getPenalesLocal()).isEqualTo(5);
        assertThat(dto.getPenalesVisitante()).isEqualTo(4);
    }

    @Test
    void toDTO_sinPenalesLosDejaNulos() {
        Partido partido = new Partido();
        partido.setId(50L);
        partido.setFase(Partido.Fase.FASE_DE_GRUPOS);
        partido.setEstado(Partido.Estado.PROGRAMADO);

        PartidoDTO dto = partidoMapper.toDTO(partido);

        assertThat(dto.getPenalesLocal()).isNull();
        assertThat(dto.getPenalesVisitante()).isNull();
    }

    @Test
    void toEntity_noAsignaElTorneo() {
        // El torneo lo resuelve PartidoService (Historico por defecto, validaciones).
        PartidoDTO dto = new PartidoDTO();
        dto.setIdTorneo(7L);

        assertThat(partidoMapper.toEntity(dto).getTorneo()).isNull();
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
