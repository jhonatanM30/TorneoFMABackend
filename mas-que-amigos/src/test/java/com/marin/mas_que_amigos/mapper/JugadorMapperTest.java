package com.marin.mas_que_amigos.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.marin.mas_que_amigos.dto.JugadorDTO;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Jugador;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class JugadorMapperTest {

    private final EquipoMapper equipoMapper = new EquipoMapper(null);
    private final JugadorMapper mapper = new JugadorMapper(equipoMapper);

    @Test
    void toEntity_copiesCostWhenProvided() {
        JugadorDTO dto = new JugadorDTO();
        dto.setCosto(new BigDecimal("1500.50"));

        Jugador entity = mapper.toEntity(dto);

        assertThat(entity.getCosto()).isEqualByComparingTo("1500.50");
    }

    @Test
    void toEntity_withoutCost_defaultsToZero() {
        Jugador entity = mapper.toEntity(new JugadorDTO());

        assertThat(entity.getCosto()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void toDTO_includesCost() {
        Equipo equipo = new Equipo();
        equipo.setId(1L);
        Jugador jugador = new Jugador();
        jugador.setEquipo(equipo);
        jugador.setCosto(new BigDecimal("300.00"));

        JugadorDTO dto = mapper.toDTO(jugador);

        assertThat(dto.getCosto()).isEqualByComparingTo("300.00");
    }
}
