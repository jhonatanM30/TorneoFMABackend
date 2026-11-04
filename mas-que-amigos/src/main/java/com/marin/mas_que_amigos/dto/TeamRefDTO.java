package com.marin.mas_que_amigos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Id and name of a team, for brackets and groups.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TeamRefDTO {

    private Long id;
    private String name;
}
