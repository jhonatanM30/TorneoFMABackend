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
public class GroupsRequestDTO {

    // Power of 2 (2, 4, 8...), checked by the service together with the team count.
    @NotNull(message = "La cantidad de grupos es obligatoria.")
    private Integer groupCount;

    // 1 or 2 teams qualify from each group.
    @NotNull(message = "Los clasificados por grupo son obligatorios.")
    private Integer qualifiersPerGroup;

    // Date of the first round of every group; each following round is intervalDays later.
    @NotNull(message = "La fecha de inicio es obligatoria.")
    private LocalDate startDate;

    @NotNull(message = "La hora de los partidos es obligatoria.")
    private LocalTime time;

    // Optional, 7 days when missing.
    @Min(value = 1, message = "El intervalo entre jornadas debe estar entre 1 y 30 días.")
    @Max(value = 30, message = "El intervalo entre jornadas debe estar entre 1 y 30 días.")
    private Integer intervalDays;

    // Optional: the composition of each group chosen by hand. Without it the
    // teams are drawn at random.
    @Valid
    private List<GroupCompositionDTO> groups;
}
