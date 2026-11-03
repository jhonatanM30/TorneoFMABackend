package com.marin.mas_que_amigos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ScheduleRequestDTOValidationTest {

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

    private Set<String> messages(LocalDate startDate, LocalTime time, Integer intervalDays) {
        ScheduleRequestDTO request = new ScheduleRequestDTO();
        request.setStartDate(startDate);
        request.setTime(time);
        request.setIntervalDays(intervalDays);
        return validator.validate(request).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    @Test
    void completeRequest_hasNoViolations() {
        assertThat(messages(LocalDate.of(2027, 3, 6), LocalTime.of(15, 0), 7)).isEmpty();
    }

    @Test
    void intervalIsOptional() {
        assertThat(messages(LocalDate.of(2027, 3, 6), LocalTime.of(15, 0), null)).isEmpty();
    }

    @Test
    void missingStartDate_isRejectedInSpanish() {
        assertThat(messages(null, LocalTime.of(15, 0), 7)).containsExactly("La fecha de inicio es obligatoria.");
    }

    @Test
    void missingTime_isRejectedInSpanish() {
        assertThat(messages(LocalDate.of(2027, 3, 6), null, 7)).containsExactly("La hora de los partidos es obligatoria.");
    }

    @Test
    void intervalOutOfRange_isRejectedInSpanish() {
        assertThat(messages(LocalDate.of(2027, 3, 6), LocalTime.of(15, 0), 0))
                .containsExactly("El intervalo entre jornadas debe estar entre 1 y 30 días.");
        assertThat(messages(LocalDate.of(2027, 3, 6), LocalTime.of(15, 0), 31))
                .containsExactly("El intervalo entre jornadas debe estar entre 1 y 30 días.");
    }
}
