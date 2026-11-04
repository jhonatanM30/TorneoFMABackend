package com.marin.mas_que_amigos.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BracketRequestDTO {

    // Date of the first round; each following round is intervalDays later.
    @NotNull(message = "La fecha de inicio es obligatoria.")
    private LocalDate startDate;

    // Same time for every match of the bracket.
    @NotNull(message = "La hora de los partidos es obligatoria.")
    private LocalTime time;

    // Optional, 7 days when missing.
    @Min(value = 1, message = "El intervalo entre rondas debe estar entre 1 y 30 días.")
    @Max(value = 30, message = "El intervalo entre rondas debe estar entre 1 y 30 días.")
    private Integer intervalDays;

    // Optional, true when missing: include the match for third place.
    private Boolean thirdPlaceMatch;

    // Optional, KNOCKOUT only: first round pairings chosen by hand. Without
    // them the pairings are drawn at random.
    @Valid
    private List<PairingRequestDTO> pairings;
}
