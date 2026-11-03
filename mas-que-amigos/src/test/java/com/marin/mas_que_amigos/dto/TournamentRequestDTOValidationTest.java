package com.marin.mas_que_amigos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class TournamentRequestDTOValidationTest {

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

    private Set<String> messages(String name, String format) {
        TournamentRequestDTO request = new TournamentRequestDTO();
        request.setName(name);
        request.setFormat(format);
        return validator.validate(request).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void completeRequest_hasNoViolations() {
        assertThat(messages("Copa Apertura", "LEAGUE")).isEmpty();
    }

    @Test
    void missingName_isRejectedInSpanish() {
        assertThat(messages(null, "LEAGUE")).containsExactly("El nombre del torneo es obligatorio.");
        assertThat(messages("   ", "LEAGUE")).containsExactly("El nombre del torneo es obligatorio.");
    }

    @Test
    void shortName_isRejectedInSpanish() {
        assertThat(messages("ab", "LEAGUE")).containsExactly("El nombre del torneo debe tener entre 3 y 100 caracteres.");
    }

    @Test
    void longName_isRejectedInSpanish() {
        String name = new String(new char[101]).replace('\0', 'a');
        assertThat(messages(name, "LEAGUE")).containsExactly("El nombre del torneo debe tener entre 3 y 100 caracteres.");
    }

    @Test
    void missingFormat_isRejectedInSpanish() {
        assertThat(messages("Copa Apertura", null)).containsExactly("El formato del torneo es obligatorio.");
        assertThat(messages("Copa Apertura", " ")).containsExactly("El formato del torneo es obligatorio.");
    }
}
