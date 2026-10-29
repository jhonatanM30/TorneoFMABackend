package com.marin.mas_que_amigos.security;

import com.marin.mas_que_amigos.model.UserRole;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Who is making the call, as stated by a valid session token. Only identity and
 * role: anything that can change after login (such as the DT's team) must be
 * read from the account.
 */
public class AuthenticatedUser {

    private final Long id;
    private final UserRole role;

    public AuthenticatedUser(Long id, UserRole role) {
        this.id = id;
        this.role = role;
    }

    public static AuthenticatedUser fromJwt(Jwt jwt) {
        Long id = ((Number) jwt.getClaim("uid")).longValue();
        UserRole role = UserRole.valueOf(jwt.<String>getClaim("role"));
        return new AuthenticatedUser(id, role);
    }

    public Long getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }
}
