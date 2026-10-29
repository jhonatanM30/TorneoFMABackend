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

class LoginRequestDTOValidationTest {

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

    private Set<String> messages(String username, String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername(username);
        request.setPassword(password);
        return validator.validate(request).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void completeRequest_hasNoViolations() {
        assertThat(messages("admin", "anything")).isEmpty();
    }

    @Test
    void missingUsername_isRejectedInSpanish() {
        assertThat(messages("", "pw")).containsExactly("El usuario es obligatorio.");
        assertThat(messages(null, "pw")).containsExactly("El usuario es obligatorio.");
    }

    @Test
    void missingPassword_isRejectedInSpanish() {
        assertThat(messages("admin", " ")).containsExactly("La contraseña es obligatoria.");
        assertThat(messages("admin", null)).containsExactly("La contraseña es obligatoria.");
    }

    @Test
    void bothMissing_reportsBothFields() {
        assertThat(messages(null, null)).containsExactlyInAnyOrder(
                "El usuario es obligatorio.", "La contraseña es obligatoria.");
    }
}
