package com.marin.mas_que_amigos.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BracketDTO {

    // Empty while the bracket has not been generated.
    private List<BracketRoundDTO> rounds;
    private BracketTieDTO thirdPlace;
    // Null until the final is finished.
    private TeamRefDTO champion;
}
