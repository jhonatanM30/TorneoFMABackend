package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.UserCreateRequestDTO;
import com.marin.mas_que_amigos.dto.UserDTO;
import com.marin.mas_que_amigos.dto.UserTeamRequestDTO;
import com.marin.mas_que_amigos.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
@Validated
@Tag(name = "Usuarios", description = "Cuentas de usuario (solo administradores): crear cuentas y asignar el equipo de un DT")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "Crear una cuenta",
            description = "Crea una cuenta con usuario, contraseña y, opcionalmente, rol (ADMIN o DT; por defecto DT) y equipo "
                    + "(solo DT). El usuario se guarda en minúsculas. La respuesta nunca incluye la contraseña.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cuenta creada",
                content = @Content(schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos, usuario repetido, rol inexistente o equipo inexistente u ocupado", content = @Content),
        @ApiResponse(responseCode = "401", description = "Sin sesión o sesión vencida", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solo un administrador puede crear cuentas", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserDTO createUser(@Valid @RequestBody UserCreateRequestDTO request) {
        return userService.createUser(request);
    }

    @Operation(summary = "Listar cuentas", description = "Devuelve todas las cuentas ordenadas por usuario, con su rol y su equipo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de cuentas",
                content = @Content(schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "401", description = "Sin sesión o sesión vencida", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solo un administrador puede listar cuentas", content = @Content)
    })
    @GetMapping
    public List<UserDTO> listUsers() {
        return userService.listUsers();
    }

    @Operation(summary = "Asignar, cambiar o quitar el equipo de un DT",
            description = "Un DT tiene como máximo un equipo y un equipo como máximo un DT. teamId nulo quita el equipo. "
                    + "Las cuentas ADMIN no pueden tener equipo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Equipo actualizado",
                content = @Content(schema = @Schema(implementation = UserDTO.class))),
        @ApiResponse(responseCode = "400", description = "Equipo inexistente, equipo con otro DT o cuenta ADMIN", content = @Content),
        @ApiResponse(responseCode = "401", description = "Sin sesión o sesión vencida", content = @Content),
        @ApiResponse(responseCode = "403", description = "Solo un administrador puede asignar equipos", content = @Content),
        @ApiResponse(responseCode = "404", description = "La cuenta no existe", content = @Content)
    })
    @PutMapping("/{id}/team")
    public UserDTO assignTeam(
            @Parameter(description = "Id de la cuenta") @PathVariable @Min(1) Long id,
            @RequestBody UserTeamRequestDTO request) {
        return userService.assignTeam(id, request.getTeamId());
    }
}
