package com.marin.mas_que_amigos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.marin.mas_que_amigos.model.Jugador.Posicion;
import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class JugadorDTOValidationTest {

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

    private JugadorDTO validPlayer() {
        JugadorDTO dto = new JugadorDTO();
        dto.setNombre("Jhon");
        dto.setPosicion(Posicion.DELANTERO);
        dto.setEdad(20);
        dto.setDorsal(9);
        return dto;
    }

    @Test
    void withoutCost_hasNoViolations() {
        assertThat(validator.validate(validPlayer())).isEmpty();
    }

    @Test
    void zeroAndPositiveCost_haveNoViolations() {
        JugadorDTO dto = validPlayer();
        dto.setCosto(BigDecimal.ZERO);
        assertThat(validator.validate(dto)).isEmpty();

        dto.setCosto(new BigDecimal("1000000.99"));
        assertThat(validator.validate(dto)).isEmpty();
    }

    @Test
    void negativeCost_isRejectedWithSpanishMessage() {
        JugadorDTO dto = validPlayer();
        dto.setCosto(new BigDecimal("-1"));

        Set<ConstraintViolation<JugadorDTO>> violations = validator.validate(dto);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("El costo no puede ser negativo.");
    }

    @Test
    void costWithMoreThanTwoDecimals_isRejected() {
        JugadorDTO dto = validPlayer();
        dto.setCosto(new BigDecimal("10.123"));

        Set<String> messages = validator.validate(dto).stream()
                .map(ConstraintViolation::getMessage).collect(Collectors.toSet());

        assertThat(messages).containsExactly("El costo admite hasta 10 enteros y 2 decimales.");
    }
}
