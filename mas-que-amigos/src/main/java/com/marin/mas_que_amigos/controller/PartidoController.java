/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.service.PartidoService;

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
import org.springframework.web.bind.annotation.*;


/**
 *
 * @author JhonatanAlexanderCue
 */
@RestController
@RequestMapping("/api/partidos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Partidos", description = "Gestión de los partidos programados entre equipos")
public class PartidoController {

    private final PartidoService partidoService;

    @Operation(summary = "Listar partidos", description = "Devuelve todos los partidos programados.")
    @ApiResponse(responseCode = "200", description = "Listado de partidos",
            content = @Content(schema = @Schema(implementation = PartidoDTO.class)))
    @GetMapping
    public ResponseEntity<List<PartidoDTO>> listarPartidos() {
        return ResponseEntity.ok(partidoService.listarPartidos());
    }

    @Operation(summary = "Buscar partidos por equipo", description = "Devuelve los partidos (local o visitante) de un equipo, ordenados por fecha y hora.")
    @ApiResponse(responseCode = "200", description = "Partidos del equipo",
            content = @Content(schema = @Schema(implementation = PartidoDTO.class)))
    @GetMapping("/{nombre}")
    public ResponseEntity<List<PartidoDTO>> buscarPartidoPorEquipo(
            @Parameter(description = "Nombre del equipo") @PathVariable String nombre) {
        return ResponseEntity.ok(partidoService.buscarPartidoPorEquipo(nombre));
    }

    @Operation(summary = "Crear partido", description = "Programa un partido entre dos equipos existentes en una fecha disponible.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Partido creado",
                content = @Content(schema = @Schema(implementation = PartidoDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos, equipos inexistentes, equipos repetidos o fecha ya ocupada", content = @Content)
    })
    @PostMapping
    public ResponseEntity<PartidoDTO> crearPartido(@Valid @RequestBody PartidoDTO partidoDTO) {
        return ResponseEntity.ok(partidoService.guardar(partidoDTO));
    }

    @Operation(summary = "Eliminar partido", description = "Elimina un partido por su id.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Partido eliminado", content = @Content),
        @ApiResponse(responseCode = "404", description = "El partido no existe", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Id del partido a eliminar") @PathVariable @Min(1) Long id) {
        partidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
