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
}
