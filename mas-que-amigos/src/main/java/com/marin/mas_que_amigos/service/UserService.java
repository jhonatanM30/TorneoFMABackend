package com.marin.mas_que_amigos.service;

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
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final AppUserRepository userRepository;
    private final EquipoRepository equipoRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository userRepository, EquipoRepository equipoRepository,
            PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.equipoRepository = equipoRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public UserDTO createUser(UserCreateRequestDTO request) {
        String username = normalizeUsername(request.getUsername());
        UserRole role = parseRole(request.getRole());

        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("El usuario '" + username + "' ya existe.");
        }

        Equipo team = null;
        if (request.getTeamId() != null) {
            requireDt(role);
            team = resolveFreeTeam(request.getTeamId(), null);
        }

        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRole(role);
        user.setTeam(team);
        user.setCreatedAt(LocalDateTime.now());

        return toDTO(save(user));
    }

    public List<UserDTO> listUsers() {
        return userRepository.findAllByOrderByUsernameAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Assigns, changes or removes (null) the team of a DT account. A team can
     * belong to one account only; the previous team of the account is simply freed.
     */
    public UserDTO assignTeam(Long userId, Long teamId) {
        AppUser user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(
                        "Fuera de juego! No se encontró la cuenta con Id " + userId + "."));

        if (teamId == null) {
            user.setTeam(null);
        } else {
            requireDt(user.getRole());
            user.setTeam(resolveFreeTeam(teamId, user.getId()));
        }
        return toDTO(save(user));
    }

    private AppUser save(AppUser user) {
        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException ex) {
            // Safety net for a race with another request: the UNIQUE constraints are the last word.
            throw new BusinessException("No se pudo guardar la cuenta: el usuario o el equipo ya están en uso.");
        }
    }

    private Equipo resolveFreeTeam(Long teamId, Long currentUserId) {
        Equipo team = equipoRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException("Fuera de juego! No se ha seleccionado un o unos equipos existentes."));
        userRepository.findByTeamId(teamId)
                .filter(owner -> !Objects.equals(owner.getId(), currentUserId))
                .ifPresent(owner -> {
                    throw new BusinessException("El equipo '" + team.getNombre()
                            + "' ya tiene un director técnico con cuenta asignada.");
                });
        return team;
    }

    private void requireDt(UserRole role) {
        if (role != UserRole.DT) {
            throw new BusinessException("Un administrador no puede tener equipo asignado.");
        }
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private UserRole parseRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            return UserRole.DT;
        }
        try {
            return UserRole.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BusinessException("El rol '" + role + "' no es válido. Valores permitidos: ADMIN, DT.");
        }
    }

    private UserDTO toDTO(AppUser user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setRole(user.getRole());
        dto.setCreatedAt(user.getCreatedAt());
        if (user.getTeam() != null) {
            dto.setTeamId(user.getTeam().getId());
            dto.setTeamName(user.getTeam().getNombre());
        }
        return dto;
    }
}
