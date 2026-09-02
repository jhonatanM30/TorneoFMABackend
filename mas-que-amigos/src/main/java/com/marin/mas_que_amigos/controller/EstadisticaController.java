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
import javax.validation.Valid;
import javax.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

    @Operation(summary = "Registrar estadística", description = "Registra el rendimiento de un jugador (goles, tarjetas, asistencias) en un partido.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estadística registrada",
                content = @Content(schema = @Schema(implementation = EstadisticaDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content)
    })
    @PostMapping
    public ResponseEntity<EstadisticaDTO> guardar(@Valid @RequestBody EstadisticaDTO estadisticaDTO) {
        return ResponseEntity.ok(estadisticaService.guardar(estadisticaDTO));
    }

    @Operation(summary = "Eliminar estadística", description = "Elimina una estadística por su id.")
    @ApiResponse(responseCode = "204", description = "Estadística eliminada", content = @Content)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Id de la estadística a eliminar") @PathVariable @Min(1) Long id) {
        estadisticaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
