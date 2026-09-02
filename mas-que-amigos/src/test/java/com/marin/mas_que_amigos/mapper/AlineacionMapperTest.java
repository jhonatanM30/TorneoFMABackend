package com.marin.mas_que_amigos.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.marin.mas_que_amigos.dto.AlineacionDTO;
import com.marin.mas_que_amigos.dto.JugadorDTO;
import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.model.Alineacion;
import com.marin.mas_que_amigos.model.AlineacionId;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.model.Partido;
import org.junit.jupiter.api.Test;

class AlineacionMapperTest {

    private final PartidoMapper partidoMapper = mock(PartidoMapper.class);
    private final JugadorMapper jugadorMapper = mock(JugadorMapper.class);
    private final AlineacionMapper mapper = new AlineacionMapper(partidoMapper, jugadorMapper);

    @Test
    void toEntity_construyeIdCompuestoCorrectamente() {
        AlineacionDTO dto = new AlineacionDTO();
        dto.setIdPartido(7L);
        dto.setIdJugador(3L);
        dto.setTitular(true);

        Alineacion entidad = mapper.toEntity(dto);

        assertThat(entidad.getId()).isNotNull();
        assertThat(entidad.getId().getIdPartido()).isEqualTo(7L);
        assertThat(entidad.getId().getIdJugador()).isEqualTo(3L);
        assertThat(entidad.getTitular()).isTrue();
    }

    @Test
    void toDTO_enriqueceConDetalleDePartidoYJugador() {
        Alineacion entidad = new Alineacion();
        entidad.setId(new AlineacionId(7L, 3L));
        entidad.setTitular(true);

        Partido partido = new Partido();
        partido.setId(7L);
        entidad.setPartido(partido);

        Jugador jugador = new Jugador();
        jugador.setId(3L);
        entidad.setJugador(jugador);

        PartidoDTO partidoDTO = new PartidoDTO();
        JugadorDTO jugadorDTO = new JugadorDTO();

        when(partidoMapper.toDTO(partido)).thenReturn(partidoDTO);
        when(jugadorMapper.toDTO(jugador)).thenReturn(jugadorDTO);

        AlineacionDTO dto = mapper.toDTO(entidad);

        assertThat(dto.getIdPartido()).isEqualTo(7L);
        assertThat(dto.getIdJugador()).isEqualTo(3L);
        assertThat(dto.isTitular()).isTrue();
        assertThat(dto.getPartido()).isSameAs(partidoDTO);
        assertThat(dto.getJugador()).isSameAs(jugadorDTO);
        assertThat(dto.getIndicadorRespuesta()).isEqualTo("Success");
    }
}
