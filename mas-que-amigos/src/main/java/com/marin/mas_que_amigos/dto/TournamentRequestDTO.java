package com.marin.mas_que_amigos.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class TournamentRequestDTO {

    // @Size lets a null name through, so @NotBlank is what reports "required".
    @NotBlank(message = "El nombre del torneo es obligatorio.")
    @Size(min = 3, max = 100, message = "El nombre del torneo debe tener entre 3 y 100 caracteres.")
    private String name;

    // Kept as text so an invalid value is answered in Spanish by the service
    // instead of a generic JSON parsing error.
    @NotBlank(message = "El formato del torneo es obligatorio.")
    private String format;
}
