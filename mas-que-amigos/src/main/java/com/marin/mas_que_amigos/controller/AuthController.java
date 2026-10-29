package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.AuthResponseDTO;
import com.marin.mas_que_amigos.dto.LoginRequestDTO;
import com.marin.mas_que_amigos.dto.UserSummaryDTO;
import com.marin.mas_que_amigos.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión, renovación de la sesión y consulta de la sesión actual")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Iniciar sesión",
            description = "Valida usuario y contraseña y devuelve un token de sesión de corta vida (3 minutos por defecto). "
                    + "Envíalo en el header Authorization: Bearer <token>. El usuario no distingue mayúsculas de minúsculas.")
    @SecurityRequirements
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Sesión iniciada",
                content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Usuario o contraseña vacíos", content = @Content),
        @ApiResponse(responseCode = "401", description = "Usuario o contraseña incorrectos", content = @Content)
    })
    @PostMapping("/login")
    public AuthResponseDTO login(@Valid @RequestBody LoginRequestDTO request) {
        return authService.login(request);
    }

    @Operation(summary = "Renovar la sesión",
            description = "Con un token todavía vigente devuelve uno nuevo con la vigencia completa. "
                    + "El front lo llama mientras el usuario tiene actividad en pantalla; con un token vencido no se puede renovar.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Token renovado",
                content = @Content(schema = @Schema(implementation = AuthResponseDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido", content = @Content)
    })
    @PostMapping("/refresh")
    public AuthResponseDTO refresh(@AuthenticationPrincipal Jwt jwt) {
        return authService.refresh(jwt.getSubject());
    }

    @Operation(summary = "Sesión actual",
            description = "Devuelve quién es la persona del token: identificador, usuario, rol y equipo asignado (nulo si no tiene).")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Datos de la sesión",
                content = @Content(schema = @Schema(implementation = UserSummaryDTO.class))),
        @ApiResponse(responseCode = "401", description = "Token ausente, inválido o vencido", content = @Content)
    })
    @GetMapping("/me")
    public UserSummaryDTO me(@AuthenticationPrincipal Jwt jwt) {
        return authService.me(jwt.getSubject());
    }
}
