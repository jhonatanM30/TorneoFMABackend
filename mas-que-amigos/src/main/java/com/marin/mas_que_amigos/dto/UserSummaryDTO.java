package com.marin.mas_que_amigos.dto;

import com.marin.mas_que_amigos.model.UserRole;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Who is logged in: basic data only (no password, no hash).
 */
@Getter
@Setter
@NoArgsConstructor
public class UserSummaryDTO {

    private Long id;
    private String username;
    private UserRole role;
    // Null when the account has no team (always null for ADMIN accounts).
    private Long teamId;
    private String teamName;
}
