package com.marin.mas_que_amigos.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ScheduleRequestDTO {

    // Date of the first round; each following round is intervalDays later.
    @NotNull(message = "La fecha de inicio es obligatoria.")
    private LocalDate startDate;

    // Same time for every generated match.
    @NotNull(message = "La hora de los partidos es obligatoria.")
    private LocalTime time;

    // Optional, 7 days when missing.
    @Min(value = 1, message = "El intervalo entre jornadas debe estar entre 1 y 30 días.")
    @Max(value = 30, message = "El intervalo entre jornadas debe estar entre 1 y 30 días.")
    private Integer intervalDays;
}
