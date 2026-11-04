package com.marin.mas_que_amigos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PenaltiesRequestDTOValidationTest {

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

    private Set<String> messages(Integer home, Integer away) {
        PenaltiesRequestDTO request = new PenaltiesRequestDTO();
        request.setHomePenalties(home);
        request.setAwayPenalties(away);
        return validator.validate(request).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void validPenalties_haveNoViolations() {
        assertThat(messages(5, 4)).isEmpty();
        assertThat(messages(0, 3)).isEmpty();
    }

    @Test
    void missingPenalties_areRejectedInSpanish() {
        assertThat(messages(null, 4)).containsExactly("Los penales del equipo local son obligatorios.");
        assertThat(messages(5, null)).containsExactly("Los penales del equipo visitante son obligatorios.");
    }

    @Test
    void negativePenalties_areRejectedInSpanish() {
        assertThat(messages(-1, 4)).containsExactly("Los penales del equipo local no pueden ser negativos.");
        assertThat(messages(5, -2)).containsExactly("Los penales del equipo visitante no pueden ser negativos.");
    }

    @Test
    void jsonUsesTheSpanishPropertyNamesOfTheMatchApi() throws Exception {
        PenaltiesRequestDTO request = new ObjectMapper().readValue("{\"penalesLocal\":5,\"penalesVisitante\":4}",
                PenaltiesRequestDTO.class);

        assertThat(request.getHomePenalties()).isEqualTo(5);
        assertThat(request.getAwayPenalties()).isEqualTo(4);
    }
}
