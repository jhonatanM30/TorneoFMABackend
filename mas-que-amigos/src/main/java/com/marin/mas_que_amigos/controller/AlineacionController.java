/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.AlineacionDTO;
import com.marin.mas_que_amigos.service.AlineacionService;

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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author JhonatanAlexanderCue
 */
@RestController
@RequestMapping("/api/alineaciones")
@RequiredArgsConstructor
@Validated
@Tag(name = "Alineaciones", description = "Alineación de jugadores en un partido (titulares y suplentes)")
public class AlineacionController {

    private final AlineacionService alineacionService;

    @Operation(summary = "Registrar alineación", description = "Alinea a un jugador en un partido, validando que el partido y el jugador existan, "
            + "que el jugador no esté repetido en el mismo partido y que un equipo no supere 11 titulares.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Alineación registrada",
                content = @Content(schema = @Schema(implementation = AlineacionDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos, partido/jugador inexistente, jugador duplicado o cupo de titulares superado", content = @Content)
    })
    @PostMapping
    public ResponseEntity<AlineacionDTO> crearAlineacion(@Valid @RequestBody AlineacionDTO alineacionDTO) {
        return ResponseEntity.ok(alineacionService.guardarAlineacion(alineacionDTO));
    }

    @Operation(summary = "Listar alineación de un partido", description = "Devuelve los jugadores alineados (titulares y suplentes) de un partido.")
    @ApiResponse(responseCode = "200", description = "Alineación del partido",
            content = @Content(schema = @Schema(implementation = AlineacionDTO.class)))
    @GetMapping("/partido/{idPartido}")
    public ResponseEntity<List<AlineacionDTO>> obtenerPorPartido(
            @Parameter(description = "Id del partido") @PathVariable @Min(1) Long idPartido) {
        return ResponseEntity.ok(alineacionService.obtenerAlineacionPorPartido(idPartido));
    }

    // Hallazgo post-despliegue (fase9): "se deberia permitir editar la
    // alineacion un suplente pase a titular y viceversa". Restringido a
    // partidos que aún no han iniciado (ver AlineacionService).
    @Operation(summary = "Editar alineación de un jugador", description = "Cambia a un jugador ya alineado de titular a suplente o viceversa. "
            + "Solo permitido mientras el partido no ha iniciado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Alineación actualizada",
                content = @Content(schema = @Schema(implementation = AlineacionDTO.class))),
        @ApiResponse(responseCode = "400", description = "El partido ya inició o se supera el cupo de titulares", content = @Content),
        @ApiResponse(responseCode = "404", description = "El jugador no está alineado en este partido", content = @Content)
    })
    @PutMapping("/{idPartido}/{idJugador}")
    public ResponseEntity<AlineacionDTO> actualizarAlineacion(
            @Parameter(description = "Id del partido") @PathVariable @Min(1) Long idPartido,
            @Parameter(description = "Id del jugador") @PathVariable @Min(1) Long idJugador,
            @Valid @RequestBody AlineacionDTO alineacionDTO) {
        return ResponseEntity.ok(alineacionService.actualizarAlineacion(idPartido, idJugador, alineacionDTO));
    }

    // Hallazgo post-despliegue (fase9): "eliminar un jugador de la
    // alineación si el partido no ha iniciado".
    @Operation(summary = "Eliminar jugador de la alineación", description = "Quita a un jugador de la alineación de un partido. "
            + "Solo permitido mientras el partido no ha iniciado.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Jugador eliminado de la alineación", content = @Content),
        @ApiResponse(responseCode = "400", description = "El partido ya inició", content = @Content),
        @ApiResponse(responseCode = "404", description = "El jugador no está alineado en este partido", content = @Content)
    })
    @DeleteMapping("/{idPartido}/{idJugador}")
    public ResponseEntity<Void> eliminarAlineacion(
            @Parameter(description = "Id del partido") @PathVariable @Min(1) Long idPartido,
            @Parameter(description = "Id del jugador") @PathVariable @Min(1) Long idJugador) {
        alineacionService.eliminarAlineacion(idPartido, idJugador);
        return ResponseEntity.noContent().build();
    }
}
