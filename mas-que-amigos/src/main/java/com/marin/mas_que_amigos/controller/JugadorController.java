package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.JugadorDTO;
import com.marin.mas_que_amigos.service.JugadorService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Min;

@RestController
@RequestMapping("/api/jugadores")
@Validated
@Tag(name = "Jugadores", description = "Gestión de los jugadores asociados a un equipo")
public class JugadorController {

    private final JugadorService jugadorService;

    public JugadorController(JugadorService jugadorService) {
        this.jugadorService = jugadorService;
    }

    @Operation(summary = "Listar jugadores", description = "Devuelve todos los jugadores registrados.")
    @ApiResponse(responseCode = "200", description = "Listado de jugadores",
            content = @Content(schema = @Schema(implementation = JugadorDTO.class)))
    @GetMapping
    public List<JugadorDTO> obtenerJugadores() {
        return jugadorService.listarJugadores();
    }

    @Operation(summary = "Buscar jugadores por nombre", description = "Busca jugadores cuyo nombre contenga el texto indicado (sin distinguir mayúsculas/minúsculas).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Jugadores encontrados",
                content = @Content(schema = @Schema(implementation = JugadorDTO.class))),
        @ApiResponse(responseCode = "400", description = "No se encontraron jugadores con ese nombre", content = @Content)
    })
    @GetMapping("/{nombre}")
    public ResponseEntity<List<JugadorDTO>> obtenerJugador(
            @Parameter(description = "Texto a buscar dentro del nombre del jugador") @PathVariable String nombre) {
        List<JugadorDTO> rspjugadores = jugadorService.obtenerJugadorPorNombre(nombre);
        return ResponseEntity.ok(rspjugadores);
    }

    @Operation(summary = "Crear jugador", description = "Registra un nuevo jugador asociado a un equipo existente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Jugador creado",
                content = @Content(schema = @Schema(implementation = JugadorDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos, equipo inexistente o dorsal duplicado", content = @Content)
    })
    @PostMapping
    public JugadorDTO crearJugador(@Valid @RequestBody JugadorDTO jugador) {
        return jugadorService.guardarJugador(jugador);
    }

    @Operation(summary = "Editar jugador", description = "Actualiza los datos de un jugador existente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Jugador actualizado",
                content = @Content(schema = @Schema(implementation = JugadorDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos, jugador/equipo inexistente o dorsal duplicado", content = @Content)
    })
    @PutMapping
    public JugadorDTO editarJugador(@Valid @RequestBody JugadorDTO jugador) {
        return jugadorService.actualizarJugador(jugador);
    }

    @Operation(summary = "Eliminar jugador", description = "Elimina un jugador por su id.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Jugador eliminado", content = @Content),
        @ApiResponse(responseCode = "404", description = "El jugador no existe", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarJugador(
            @Parameter(description = "Id del jugador a eliminar") @PathVariable @Min(1) Long id) {
        jugadorService.eliminarJugador(id);
        return ResponseEntity.noContent().build();
    }
}
