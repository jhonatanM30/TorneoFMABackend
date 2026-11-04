package com.marin.mas_que_amigos.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GroupDTO {

    private Long id;
    // A, B, C...
    private String name;
    private List<TeamRefDTO> teams;
    // Teams that qualify from this group to the knockout stage.
    private int qualifiersCount;
    // Table of the group: only finished group stage matches among its teams count.
    private List<StandingDTO> standings;
}
