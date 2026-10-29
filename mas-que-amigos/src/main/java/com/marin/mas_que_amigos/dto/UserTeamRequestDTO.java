package com.marin.mas_que_amigos.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserTeamRequestDTO {

    // Null removes the team from the account.
    private Long teamId;
}
