package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marin.mas_que_amigos.dto.UserCreateRequestDTO;
import com.marin.mas_que_amigos.dto.UserDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.UserNotFoundException;
import com.marin.mas_que_amigos.model.AppUser;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.UserRole;
import com.marin.mas_que_amigos.repository.AppUserRepository;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, equipoRepository, passwordEncoder);
    }

    private UserCreateRequestDTO request(String username, String password, String role, Long teamId) {
        UserCreateRequestDTO request = new UserCreateRequestDTO();
        request.setUsername(username);
        request.setPassword(password);
        request.setRole(role);
        request.setTeamId(teamId);
        return request;
    }

    private Equipo team(Long id, String name) {
        Equipo team = new Equipo();
        team.setId(id);
        team.setNombre(name);
        return team;
    }

    private AppUser user(Long id, String username, UserRole role, Equipo team) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash("hash");
        user.setRole(role);
        user.setTeam(team);
        user.setCreatedAt(LocalDateTime.of(2026, 10, 21, 10, 0));
        return user;
    }

    private void stubSave() {
        when(userRepository.save(any(AppUser.class))).thenAnswer(invocation -> {
            AppUser saved = invocation.getArgument(0);
            saved.setId(50L);
            return saved;
        });
    }

    // ------------------------------------------------------------------ createUser

    @Test
    void createUser_withoutRole_createsADtAccountWithLowercaseUsernameAndHashedPassword() {
        when(userRepository.existsByUsername("joan")).thenReturn(false);
        when(passwordEncoder.encode("secret-123")).thenReturn("$2a$hash");
        stubSave();

        UserDTO result = service.createUser(request("  Joan ", "secret-123", null, null));

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("joan");
        assertThat(saved.getRole()).isEqualTo(UserRole.DT);
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$hash").isNotEqualTo("secret-123");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getTeam()).isNull();

        assertThat(result.getId()).isEqualTo(50L);
        assertThat(result.getUsername()).isEqualTo("joan");
        assertThat(result.getRole()).isEqualTo(UserRole.DT);
        assertThat(result.getTeamId()).isNull();
        assertThat(result.getCreatedAt()).isNotNull();
    }

    @Test
    void createUser_withAdminRole_createsAnAdminAccount() {
        when(userRepository.existsByUsername("boss")).thenReturn(false);
        when(passwordEncoder.encode("secret-123")).thenReturn("hash");
        stubSave();

        UserDTO result = service.createUser(request("boss", "secret-123", "admin", null));

        assertThat(result.getRole()).isEqualTo(UserRole.ADMIN);
    }

    @Test
    void createUser_withUnknownRole_throwsBusinessException() {
        assertThatThrownBy(() -> service.createUser(request("boss", "secret-123", "SUPERUSER", null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ADMIN, DT");
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void createUser_whenTheUsernameExistsIgnoringCase_throwsBusinessException() {
        when(userRepository.existsByUsername("entrenador1")).thenReturn(true);

        assertThatThrownBy(() -> service.createUser(request("Entrenador1", "secret-123", null, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya existe");
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void createUser_withAFreeTeam_linksTheTeam() {
        Equipo macalister = team(1L, "Macalister");
        when(userRepository.existsByUsername("joan")).thenReturn(false);
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(macalister));
        when(userRepository.findByTeamId(1L)).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secret-123")).thenReturn("hash");
        stubSave();

        UserDTO result = service.createUser(request("joan", "secret-123", "DT", 1L));

        assertThat(result.getTeamId()).isEqualTo(1L);
        assertThat(result.getTeamName()).isEqualTo("Macalister");
    }

    @Test
    void createUser_withAMissingTeam_throwsBusinessException() {
        when(userRepository.existsByUsername("joan")).thenReturn(false);
        when(equipoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createUser(request("joan", "secret-123", "DT", 99L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("equipos existentes");
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void createUser_withATeamThatAlreadyHasADt_throwsBusinessException() {
        when(userRepository.existsByUsername("juan")).thenReturn(false);
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(team(1L, "Macalister")));
        when(userRepository.findByTeamId(1L)).thenReturn(Optional.of(user(9L, "joan", UserRole.DT, team(1L, "Macalister"))));

        assertThatThrownBy(() -> service.createUser(request("juan", "secret-123", "DT", 1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya tiene");
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void createUser_adminWithTeam_throwsBusinessException() {
        when(userRepository.existsByUsername("boss")).thenReturn(false);

        assertThatThrownBy(() -> service.createUser(request("boss", "secret-123", "ADMIN", 1L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("administrador");
        verify(userRepository, never()).save(any(AppUser.class));
    }

    // ------------------------------------------------------------------ listUsers

    @Test
    void listUsers_returnsEveryAccountWithItsTeamAndNeverExposesThePassword() {
        List<AppUser> sorted = Arrays.asList(
                user(1L, "admin", UserRole.ADMIN, null),
                user(2L, "joan", UserRole.DT, team(1L, "Macalister")));
        when(userRepository.findAllByOrderByUsernameAsc()).thenReturn(sorted);

        List<UserDTO> result = service.listUsers();

        assertThat(result).extracting(UserDTO::getUsername).containsExactly("admin", "joan");
        assertThat(result.get(1).getTeamId()).isEqualTo(1L);
        assertThat(result.get(1).getTeamName()).isEqualTo("Macalister");
        assertThat(UserDTO.class.getDeclaredFields()).extracting("name")
                .doesNotContain("password", "passwordHash");
    }

    // ------------------------------------------------------------------ assignTeam

    @Test
    void assignTeam_givesAFreeTeamToADtWithoutTeam() {
        AppUser joan = user(2L, "joan", UserRole.DT, null);
        Equipo macalister = team(1L, "Macalister");
        when(userRepository.findById(2L)).thenReturn(Optional.of(joan));
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(macalister));
        when(userRepository.findByTeamId(1L)).thenReturn(Optional.empty());
        when(userRepository.save(joan)).thenReturn(joan);

        UserDTO result = service.assignTeam(2L, 1L);

        assertThat(joan.getTeam()).isSameAs(macalister);
        assertThat(result.getTeamId()).isEqualTo(1L);
    }

    @Test
    void assignTeam_changesTheTeamOfADtThatAlreadyHadOne() {
        AppUser joan = user(2L, "joan", UserRole.DT, team(1L, "Macalister"));
        Equipo real = team(5L, "Real");
        when(userRepository.findById(2L)).thenReturn(Optional.of(joan));
        when(equipoRepository.findById(5L)).thenReturn(Optional.of(real));
        when(userRepository.findByTeamId(5L)).thenReturn(Optional.empty());
        when(userRepository.save(joan)).thenReturn(joan);

        service.assignTeam(2L, 5L);

        assertThat(joan.getTeam()).isSameAs(real);
    }

    @Test
    void assignTeam_withANullTeam_removesTheTeam() {
        AppUser joan = user(2L, "joan", UserRole.DT, team(1L, "Macalister"));
        when(userRepository.findById(2L)).thenReturn(Optional.of(joan));
        when(userRepository.save(joan)).thenReturn(joan);

        UserDTO result = service.assignTeam(2L, null);

        assertThat(joan.getTeam()).isNull();
        assertThat(result.getTeamId()).isNull();
    }

    @Test
    void assignTeam_toTheSameTeamTheDtAlreadyHas_isAccepted() {
        Equipo macalister = team(1L, "Macalister");
        AppUser joan = user(2L, "joan", UserRole.DT, macalister);
        when(userRepository.findById(2L)).thenReturn(Optional.of(joan));
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(macalister));
        when(userRepository.findByTeamId(1L)).thenReturn(Optional.of(joan));
        when(userRepository.save(joan)).thenReturn(joan);

        assertThat(service.assignTeam(2L, 1L).getTeamId()).isEqualTo(1L);
    }

    @Test
    void assignTeam_toAnAdminAccount_throwsBusinessException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user(1L, "admin", UserRole.ADMIN, null)));

        assertThatThrownBy(() -> service.assignTeam(1L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("administrador");
        verify(userRepository, never()).save(any(AppUser.class));
    }

    @Test
    void assignTeam_toAnUnknownAccount_throwsUserNotFoundException() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignTeam(99L, 1L)).isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void assignTeam_withAMissingTeam_throwsBusinessException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(user(2L, "joan", UserRole.DT, null)));
        when(equipoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.assignTeam(2L, 99L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("equipos existentes");
    }

    @Test
    void assignTeam_withATeamOfAnotherDt_throwsBusinessException() {
        AppUser joan = user(2L, "joan", UserRole.DT, null);
        AppUser juan = user(3L, "juan", UserRole.DT, team(1L, "Macalister"));
        when(userRepository.findById(2L)).thenReturn(Optional.of(joan));
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(team(1L, "Macalister")));
        when(userRepository.findByTeamId(1L)).thenReturn(Optional.of(juan));

        assertThatThrownBy(() -> service.assignTeam(2L, 1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya tiene");
        verify(userRepository, never()).save(any(AppUser.class));
    }
}
