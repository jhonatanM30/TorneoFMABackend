package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.CambioJugadorDTO;
import com.marin.mas_que_amigos.service.CambioJugadorService;

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
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * FRONTEND_VISION.md Fase3-09/Fase3-10: cambios de jugador (sustituciones)
 * de un partido en curso, con minuto. Anidado bajo /api/partidos/{idPartido}
 * porque un cambio no tiene sentido fuera del contexto de un partido.
 *
 * @author JhonatanAlexanderCue
 */
@RestController
@RequiredArgsConstructor
@Validated
@Tag(name = "Cambios de jugador", description = "Sustituciones (titular sale, suplente entra) registradas durante un partido en curso, con el minuto")
public class CambioJugadorController {

    private final CambioJugadorService cambioJugadorService;

    @Operation(summary = "Listar cambios de un partido", description = "Devuelve los cambios de jugador de un partido, ordenados por minuto.")
    @ApiResponse(responseCode = "200", description = "Listado de cambios",
            content = @Content(schema = @Schema(implementation = CambioJugadorDTO.class)))
    @GetMapping("/api/partidos/{idPartido}/cambios")
    public List<CambioJugadorDTO> listarPorPartido(
            @Parameter(description = "Id del partido") @PathVariable @Min(1) Long idPartido) {
        return cambioJugadorService.listarPorPartido(idPartido);
    }

    @Operation(summary = "Registrar un cambio de jugador", description = "Registra que un jugador titular sale y un suplente del mismo equipo entra, en el minuto indicado. El partido debe estar en curso.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cambio registrado",
                content = @Content(schema = @Schema(implementation = CambioJugadorDTO.class))),
        @ApiResponse(responseCode = "400", description = "Partido no encontrado, no está en curso, o jugadores inválidos", content = @Content)
    })
    @PostMapping("/api/partidos/{idPartido}/cambios")
    public CambioJugadorDTO registrar(
            @Parameter(description = "Id del partido") @PathVariable @Min(1) Long idPartido,
            @Valid @RequestBody CambioJugadorDTO cambioDTO) {
        return cambioJugadorService.registrar(idPartido, cambioDTO);
    }
}
