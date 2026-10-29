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

class UserCreateRequestDTOValidationTest {

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

    private UserCreateRequestDTO request(String username, String password) {
        UserCreateRequestDTO request = new UserCreateRequestDTO();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    private Set<String> messages(UserCreateRequestDTO request) {
        return validator.validate(request).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void validRequest_hasNoViolations() {
        assertThat(validator.validate(request("joan.perez-1_x", "12345678"))).isEmpty();
    }

    @Test
    void usernameWithExactlyTheLimits_isAccepted() {
        assertThat(validator.validate(request("abc", "12345678"))).isEmpty();
        assertThat(validator.validate(request(repeat('a', 50), "12345678"))).isEmpty();
    }

    @Test
    void usernameTooShortTooLongOrWithForbiddenCharacters_isRejectedInSpanish() {
        String expected = "El usuario debe tener entre 3 y 50 caracteres: letras, números, punto, guion y guion bajo.";

        assertThat(messages(request("ab", "12345678"))).containsExactly(expected);
        assertThat(messages(request(repeat('a', 51), "12345678"))).containsExactly(expected);
        assertThat(messages(request("joan perez", "12345678"))).containsExactly(expected);
        assertThat(messages(request("joan@mail", "12345678"))).containsExactly(expected);
    }

    @Test
    void blankUsernameOrPassword_isRejectedInSpanish() {
        assertThat(messages(request("", "12345678"))).contains("El usuario es obligatorio.");
        assertThat(messages(request("joan", ""))).contains("La contraseña es obligatoria.");
        assertThat(messages(request(null, null)))
                .contains("El usuario es obligatorio.", "La contraseña es obligatoria.");
    }

    @Test
    void passwordShorterThan8Characters_isRejectedInSpanish() {
        assertThat(messages(request("joan", "1234567"))).containsExactly("La contraseña debe tener al menos 8 caracteres.");
        assertThat(validator.validate(request("joan", "12345678"))).isEmpty();
    }

    @Test
    void passwordLongerThanBcryptLimit_isRejectedInSpanish() {
        assertThat(messages(request("joan", repeat('x', 73))))
                .containsExactly("La contraseña no puede superar 72 caracteres.");
        assertThat(validator.validate(request("joan", repeat('x', 72)))).isEmpty();
    }

    private static String repeat(char c, int times) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < times; i++) {
            builder.append(c);
        }
        return builder.toString();
    }
}
