package com.marin.mas_que_amigos.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AuthResponseDTO {

    private String token;
    private String tokenType = "Bearer";
    private long expiresInSeconds;
    private UserSummaryDTO user;
}
