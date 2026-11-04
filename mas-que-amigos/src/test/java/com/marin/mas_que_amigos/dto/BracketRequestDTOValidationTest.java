package com.marin.mas_que_amigos.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class BracketRequestDTOValidationTest {

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

    private BracketRequestDTO request(LocalDate startDate, LocalTime time, Integer intervalDays,
            List<PairingRequestDTO> pairings) {
        BracketRequestDTO request = new BracketRequestDTO();
        request.setStartDate(startDate);
        request.setTime(time);
        request.setIntervalDays(intervalDays);
        request.setPairings(pairings);
        return request;
    }

    private Set<String> messages(BracketRequestDTO request) {
        return validator.validate(request).stream().map(ConstraintViolation::getMessage).collect(Collectors.toSet());
    }

    private PairingRequestDTO pairing(Long home, Long away) {
        PairingRequestDTO pairing = new PairingRequestDTO();
        pairing.setHomeTeamId(home);
        pairing.setAwayTeamId(away);
        return pairing;
    }

    @Test
    void completeRequestWithoutOptionalFields_hasNoViolations() {
        assertThat(messages(request(LocalDate.of(2027, 5, 1), LocalTime.of(18, 0), null, null))).isEmpty();
    }

    @Test
    void completeRequestWithPairings_hasNoViolations() {
        assertThat(messages(request(LocalDate.of(2027, 5, 1), LocalTime.of(18, 0), 7,
                Collections.singletonList(pairing(1L, 2L))))).isEmpty();
    }

    @Test
    void missingStartDateAndTime_areRejectedInSpanish() {
        assertThat(messages(request(null, null, null, null))).containsExactlyInAnyOrder(
                "La fecha de inicio es obligatoria.", "La hora de los partidos es obligatoria.");
    }

    @Test
    void intervalOutOfRange_isRejectedInSpanish() {
        assertThat(messages(request(LocalDate.of(2027, 5, 1), LocalTime.of(18, 0), 0, null)))
                .containsExactly("El intervalo entre rondas debe estar entre 1 y 30 días.");
        assertThat(messages(request(LocalDate.of(2027, 5, 1), LocalTime.of(18, 0), 31, null)))
                .containsExactly("El intervalo entre rondas debe estar entre 1 y 30 días.");
    }

    @Test
    void pairingWithoutATeam_isRejectedInSpanish() {
        assertThat(messages(request(LocalDate.of(2027, 5, 1), LocalTime.of(18, 0), null,
                Collections.singletonList(pairing(null, 2L)))))
                .containsExactly("El equipo local del cruce es obligatorio.");
        assertThat(messages(request(LocalDate.of(2027, 5, 1), LocalTime.of(18, 0), null,
                Collections.singletonList(pairing(1L, null)))))
                .containsExactly("El equipo visitante del cruce es obligatorio.");
    }
}
