package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.service.EquipoService;

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
@RequestMapping("/api/equipos")
@Validated
@Tag(name = "Equipos", description = "Gestión de los equipos que participan en la liga")
public class EquipoController {

    private final EquipoService equipoService;

    // Solo inyectamos el servicio
    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @Operation(summary = "Listar equipos", description = "Devuelve todos los equipos registrados, incluyendo sus jugadores.")
    @ApiResponse(responseCode = "200", description = "Listado de equipos",
            content = @Content(schema = @Schema(implementation = EquipoDTO.class)))
    @GetMapping
    public List<EquipoDTO> obtenerEquipos() {
        return equipoService.listarEquipos();
    }

    @Operation(summary = "Obtener equipo por nombre", description = "Busca un equipo por su nombre exacto (sin distinguir mayúsculas/minúsculas).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Equipo encontrado",
                content = @Content(schema = @Schema(implementation = EquipoDTO.class))),
        @ApiResponse(responseCode = "404", description = "No existe un equipo con ese nombre", content = @Content)
    })
    @GetMapping("/{nombre}")
    public ResponseEntity<EquipoDTO> obtenerEquipo(
            @Parameter(description = "Nombre del equipo a buscar") @PathVariable String nombre) {
        return ResponseEntity.ok(equipoService.obtenerEquipoPorNombre(nombre));
    }

    @Operation(summary = "Crear equipo", description = "Registra un nuevo equipo en la base de datos.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Equipo creado",
                content = @Content(schema = @Schema(implementation = EquipoDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos o equipo ya existente", content = @Content)
    })
    @PostMapping
    public EquipoDTO crearEquipo(@Valid @RequestBody EquipoDTO equipo) {
        return equipoService.guardarEquipo(equipo);
    }

    @Operation(summary = "Editar equipo", description = "Actualiza los datos de un equipo existente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Equipo actualizado",
                content = @Content(schema = @Schema(implementation = EquipoDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos", content = @Content),
        @ApiResponse(responseCode = "404", description = "El equipo no existe", content = @Content)
    })
    @PutMapping
    public EquipoDTO editarEquipo(@Valid @RequestBody EquipoDTO equipo) {
        return equipoService.actualizarEquipo(equipo);
    }

    @Operation(summary = "Eliminar equipo", description = "Elimina un equipo (y en cascada a sus jugadores) por su id.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Equipo eliminado", content = @Content),
        @ApiResponse(responseCode = "404", description = "El equipo no existe", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEquipo(
            @Parameter(description = "Id del equipo a eliminar") @PathVariable @Min(1) Long id) {
        equipoService.eliminarEquipo(id);
        return ResponseEntity.noContent().build();
    }
}
