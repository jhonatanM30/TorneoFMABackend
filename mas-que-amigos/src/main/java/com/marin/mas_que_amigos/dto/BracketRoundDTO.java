package com.marin.mas_que_amigos.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BracketRoundDTO {

    private int round;
    // Spanish name of the round: octavos de final, cuartos de final, semifinales, final...
    private String name;
    private List<BracketTieDTO> ties;
}
