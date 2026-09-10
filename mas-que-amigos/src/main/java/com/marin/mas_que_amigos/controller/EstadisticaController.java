/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.EstadisticaDTO;
import com.marin.mas_que_amigos.service.EstadisticaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Optional;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 *
 * @author JhonatanAlexanderCue
 */
@RestController
@RequestMapping("/api/estadisticas")
@RequiredArgsConstructor
@Validated
@Tag(name = "Estadísticas", description = "Estadísticas de jugadores por partido (goles, tarjetas, asistencias)")
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    @Operation(summary = "Listar estadísticas", description = "Devuelve todas las estadísticas registradas.")
    @ApiResponse(responseCode = "200", description = "Listado de estadísticas",
            content = @Content(schema = @Schema(implementation = EstadisticaDTO.class)))
    @GetMapping
    public ResponseEntity<List<EstadisticaDTO>> listarTodas() {
        return ResponseEntity.ok(estadisticaService.listarTodas());
    }

    @Operation(summary = "Obtener estadística por id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estadística encontrada",
                content = @Content(schema = @Schema(implementation = EstadisticaDTO.class))),
        @ApiResponse(responseCode = "404", description = "La estadística no existe", content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<EstadisticaDTO> obtenerPorId(
            @Parameter(description = "Id de la estadística") @PathVariable @Min(1) Long id) {
        return ResponseEntity.ok(estadisticaService.buscarPorId(id));
    }

    // Hallazgo post-despliegue (fase9): permite al frontend, antes de abrir
    // el modal de "registrar estadistica", averiguar si ya existe un
    // registro para ese jugador+partido y, si es asi, abrir el modal en
    // modo edicion en vez de crear un segundo registro duplicado.
    @Operation(summary = "Buscar estadística existente de un jugador en un partido",
            description = "Devuelve el registro de estadística de ese jugador en ese partido si ya existe, para editarlo en vez de crear uno duplicado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Ya existe un registro para ese jugador y partido",
                content = @Content(schema = @Schema(implementation = EstadisticaDTO.class))),
        @ApiResponse(responseCode = "204", description = "No existe ningún registro todavía para ese jugador y partido", content = @Content)
    })
    @GetMapping("/buscar")
    public ResponseEntity<EstadisticaDTO> buscarExistente(
            @Parameter(description = "Id del jugador") @RequestParam @Min(1) Long idJugador,
            @Parameter(description = "Id del partido") @RequestParam @Min(1) Long idPartido) {
        Optional<EstadisticaDTO> existente = estadisticaService.buscarExistente(idJugador, idPartido);
        return existente.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @Operation(summary = "Registrar estadística", description = "Registra el rendimiento de un jugador (goles, tarjetas, asistencias) en un partido que ya haya iniciado. "
            + "Si ya existe un registro para ese jugador y partido, se rechaza (usa PUT /{id} para editarlo).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estadística registrada",
                content = @Content(schema = @Schema(implementation = EstadisticaDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos, partido no iniciado o registro duplicado", content = @Content)
    })
    @PostMapping
    public ResponseEntity<EstadisticaDTO> guardar(@Valid @RequestBody EstadisticaDTO estadisticaDTO) {
        return ResponseEntity.ok(estadisticaService.guardar(estadisticaDTO));
    }

    @Operation(summary = "Editar estadística", description = "Edita los contadores (goles/tarjetas/asistencias/minuto) de un registro de estadística ya existente. "
            + "El cambio se refleja automáticamente en los goles del partido.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estadística actualizada",
                content = @Content(schema = @Schema(implementation = EstadisticaDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o intento de reasignar el registro a otro jugador/partido", content = @Content),
        @ApiResponse(responseCode = "404", description = "La estadística no existe", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<EstadisticaDTO> actualizar(
            @Parameter(description = "Id de la estadística a editar") @PathVariable @Min(1) Long id,
            @Valid @RequestBody EstadisticaDTO estadisticaDTO) {
        return ResponseEntity.ok(estadisticaService.actualizar(id, estadisticaDTO));
    }

    @Operation(summary = "Eliminar estadística", description = "Elimina una estadística por su id. El cambio se refleja automáticamente en los goles del partido.")
    @ApiResponse(responseCode = "204", description = "Estadística eliminada", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Id de la estadística a eliminar") @PathVariable @Min(1) Long id) {
        estadisticaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
