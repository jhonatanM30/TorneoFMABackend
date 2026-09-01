package com.marin.mas_que_amigos.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.marin.mas_que_amigos.dto.AlineacionDTO;
import com.marin.mas_que_amigos.model.Alineacion;
import org.junit.jupiter.api.Test;

class AlineacionMapperTest {

    private final AlineacionMapper mapper = new AlineacionMapper();

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
}
