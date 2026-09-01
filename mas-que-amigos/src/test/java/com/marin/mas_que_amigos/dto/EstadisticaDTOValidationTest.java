package com.marin.mas_que_amigos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class EstadisticaDTOValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void init() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void close() {
        factory.close();
    }

    @Test
    void estadisticaValida_sinViolaciones() {
        EstadisticaDTO dto = new EstadisticaDTO();
        dto.setIdJugador(1L);
        dto.setIdPartido(1L);
        dto.setGoles(2);
        dto.setAsistencias(1);

        Set<ConstraintViolation<EstadisticaDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones).isEmpty();
    }

    @Test
    void estadisticaConGolesNegativosYSinIds_generaViolaciones() {
        EstadisticaDTO dto = new EstadisticaDTO();
        dto.setGoles(-1);

        Set<ConstraintViolation<EstadisticaDTO>> violaciones = validator.validate(dto);

        assertThat(violaciones)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("idJugador", "idPartido", "goles");
    }
}
