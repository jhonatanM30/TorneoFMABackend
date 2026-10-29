package com.marin.mas_que_amigos.dto;

import com.marin.mas_que_amigos.model.UserRole;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Account as exposed by the API. It never carries the password or its hash.
 */
@Getter
@Setter
@NoArgsConstructor
public class UserDTO {

    private Long id;
    private String username;
    private UserRole role;
    private Long teamId;
    private String teamName;
    private LocalDateTime createdAt;
}
