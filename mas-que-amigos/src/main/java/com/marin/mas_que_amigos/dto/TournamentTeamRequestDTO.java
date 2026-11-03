package com.marin.mas_que_amigos.dto;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TournamentTeamRequestDTO {

    @NotNull(message = "El equipo a inscribir es obligatorio.")
    private Long teamId;
}
