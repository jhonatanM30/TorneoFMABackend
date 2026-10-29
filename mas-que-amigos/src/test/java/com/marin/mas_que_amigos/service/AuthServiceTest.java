package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marin.mas_que_amigos.dto.AuthResponseDTO;
import com.marin.mas_que_amigos.dto.LoginRequestDTO;
import com.marin.mas_que_amigos.dto.UserSummaryDTO;
import com.marin.mas_que_amigos.exception.AuthenticationFailedException;
import com.marin.mas_que_amigos.model.AppUser;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.UserRole;
import com.marin.mas_que_amigos.repository.AppUserRepository;
import com.marin.mas_que_amigos.security.JwtTokenService;
import com.marin.mas_que_amigos.security.SecurityProperties;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String INVALID_CREDENTIALS = "Usuario o contraseña incorrectos";

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    // Real token service: the tests decode what the service issues.
    private JwtTokenService tokenService;
    private AuthService service;

    @BeforeEach
    void setUp() {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setSecret("auth-service-test-secret-0123456789-abcdef");
        properties.getJwt().setTtl(Duration.ofMinutes(3));
        tokenService = new JwtTokenService(properties);
        service = new AuthService(userRepository, passwordEncoder, tokenService);
    }

    private AppUser user(Long id, String username, UserRole role, Equipo team) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("stored-hash");
        user.setRole(role);
        user.setTeam(team);
        user.setCreatedAt(LocalDateTime.of(2026, 10, 21, 10, 0));
        return user;
    }

    private Equipo team(Long id, String name) {
        Equipo team = new Equipo();
        team.setId(id);
        team.setNombre(name);
        return team;
    }

    private LoginRequestDTO login(String username, String password) {
        LoginRequestDTO request = new LoginRequestDTO();
        request.setUsername(username);
        request.setPassword(password);
        return request;
    }

    // ------------------------------------------------------------------ login

    @Test
    void login_withCorrectCredentials_returnsATokenAndTheUser() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user(1L, "admin", UserRole.ADMIN, null)));
        when(passwordEncoder.matches("Admin-pass-123", "stored-hash")).thenReturn(true);

        AuthResponseDTO response = service.login(login("admin", "Admin-pass-123"));

        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresInSeconds()).isEqualTo(180L);
        Jwt jwt = tokenService.decode(response.getToken());
        assertThat(jwt.getSubject()).isEqualTo("admin");
        assertThat(jwt.<String>getClaim("role")).isEqualTo("ADMIN");
        assertThat(((Number) jwt.getClaim("uid")).longValue()).isEqualTo(1L);
        UserSummaryDTO user = response.getUser();
        assertThat(user.getId()).isEqualTo(1L);
        assertThat(user.getUsername()).isEqualTo("admin");
        assertThat(user.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(user.getTeamId()).isNull();
    }

    @Test
    void login_ignoresTheCaseOfTheUsername() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user(1L, "admin", UserRole.ADMIN, null)));
        when(passwordEncoder.matches("Admin-pass-123", "stored-hash")).thenReturn(true);

        AuthResponseDTO response = service.login(login("  ADMIN ", "Admin-pass-123"));

        assertThat(response.getUser().getUsername()).isEqualTo("admin");
    }

    @Test
    void login_ofADtWithATeam_includesTheTeamInTheUser() {
        AppUser joan = user(2L, "joan", UserRole.DT, team(1L, "Macalister"));
        when(userRepository.findByUsername("joan")).thenReturn(Optional.of(joan));
        when(passwordEncoder.matches("secret-123", "stored-hash")).thenReturn(true);

        UserSummaryDTO user = service.login(login("joan", "secret-123")).getUser();

        assertThat(user.getRole()).isEqualTo(UserRole.DT);
        assertThat(user.getTeamId()).isEqualTo(1L);
        assertThat(user.getTeamName()).isEqualTo("Macalister");
    }

    @Test
    void login_withAWrongPassword_throwsTheGenericMessage() {
        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(user(1L, "admin", UserRole.ADMIN, null)));
        when(passwordEncoder.matches("wrong", "stored-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.login(login("admin", "wrong")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage(INVALID_CREDENTIALS);
    }

    @Test
    void login_withAnUnknownUser_throwsTheSameMessageAndStillComparesAPassword() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(login("ghost", "whatever-123")))
                .isInstanceOf(AuthenticationFailedException.class)
                .hasMessage(INVALID_CREDENTIALS);
        // A dummy comparison keeps the response time from revealing that the user does not exist.
        verify(passwordEncoder).matches(eq("whatever-123"), any());
    }

    // ------------------------------------------------------------------ refresh

    @Test
    void refresh_issuesANewTokenWithTheRoleCurrentlyInTheDatabase() {
        // The old token said DT, but the account is an ADMIN now: the new token must say ADMIN.
        when(userRepository.findByUsername("joan")).thenReturn(Optional.of(user(2L, "joan", UserRole.ADMIN, null)));

        AuthResponseDTO response = service.refresh("joan");

        Jwt jwt = tokenService.decode(response.getToken());
        assertThat(jwt.<String>getClaim("role")).isEqualTo("ADMIN");
        assertThat(response.getExpiresInSeconds()).isEqualTo(180L);
        assertThat(response.getUser().getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void refresh_whenTheAccountNoLongerExists_throwsAuthenticationFailed() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("ghost")).isInstanceOf(AuthenticationFailedException.class);
    }

    // ------------------------------------------------------------------ me

    @Test
    void me_returnsTheCurrentAccountWithItsTeam() {
        when(userRepository.findByUsername("joan"))
                .thenReturn(Optional.of(user(2L, "joan", UserRole.DT, team(1L, "Macalister"))));

        UserSummaryDTO me = service.me("joan");

        assertThat(me.getId()).isEqualTo(2L);
        assertThat(me.getUsername()).isEqualTo("joan");
        assertThat(me.getRole()).isEqualTo(UserRole.DT);
        assertThat(me.getTeamId()).isEqualTo(1L);
    }

    @Test
    void me_whenTheAccountNoLongerExists_throwsAuthenticationFailed() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.me("ghost")).isInstanceOf(AuthenticationFailedException.class);
    }
}
