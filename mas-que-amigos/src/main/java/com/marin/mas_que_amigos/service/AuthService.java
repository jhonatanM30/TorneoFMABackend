package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.AuthResponseDTO;
import com.marin.mas_que_amigos.dto.LoginRequestDTO;
import com.marin.mas_que_amigos.dto.UserSummaryDTO;
import com.marin.mas_que_amigos.exception.AuthenticationFailedException;
import com.marin.mas_que_amigos.model.AppUser;
import com.marin.mas_que_amigos.repository.AppUserRepository;
import com.marin.mas_que_amigos.security.JwtTokenService;
import java.util.Locale;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS = "Usuario o contraseña incorrectos";
    private static final String SESSION_NO_LONGER_VALID = "La sesión ya no es válida. Inicia sesión de nuevo.";

    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService tokenService;

    // Hash of a random password, only used to spend the same time on unknown users.
    private final String dummyHash;

    public AuthService(AppUserRepository userRepository, PasswordEncoder passwordEncoder,
            JwtTokenService tokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        AppUser user = userRepository.findByUsername(normalize(request.getUsername())).orElse(null);

        if (user == null) {
            // Compare anyway so the response time does not reveal whether the user exists.
            passwordEncoder.matches(request.getPassword(), dummyHash);
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AuthenticationFailedException(INVALID_CREDENTIALS);
        }
        return buildResponse(user);
    }

    /**
     * The username comes from a still-valid token; the account is read again so the
     * new token carries the role stored today, not the one the old token had.
     */
    public AuthResponseDTO refresh(String username) {
        return buildResponse(currentUser(username));
    }

    public UserSummaryDTO me(String username) {
        return toSummary(currentUser(username));
    }

    private AppUser currentUser(String username) {
        return userRepository.findByUsername(normalize(username))
                .orElseThrow(() -> new AuthenticationFailedException(SESSION_NO_LONGER_VALID));
    }

    private AuthResponseDTO buildResponse(AppUser user) {
        JwtTokenService.IssuedToken issued = tokenService.issue(user.getId(), user.getUsername(), user.getRole().name());
        AuthResponseDTO response = new AuthResponseDTO();
        response.setToken(issued.getToken());
        response.setExpiresInSeconds(issued.getExpiresInSeconds());
        response.setUser(toSummary(user));
        return response;
    }

    private UserSummaryDTO toSummary(AppUser user) {
        UserSummaryDTO summary = new UserSummaryDTO();
        summary.setId(user.getId());
        summary.setUsername(user.getUsername());
        summary.setRole(user.getRole());
        if (user.getTeam() != null) {
            summary.setTeamId(user.getTeam().getId());
            summary.setTeamName(user.getTeam().getNombre());
        }
        return summary;
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }
}
