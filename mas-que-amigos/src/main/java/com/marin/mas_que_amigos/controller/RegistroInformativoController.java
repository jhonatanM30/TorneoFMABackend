package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.RegistroInformativoDTO;
import com.marin.mas_que_amigos.service.RegistroInformativoService;

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
 * Fase 6 - Configuracion: CRUD (crear/listar/editar/eliminar) de registros
 * informativos (tipo blog) mostrados en la pagina de Inicio.
 *
 * @author JhonatanAlexanderCue
 */
@RestController
@RequestMapping("/api/registros-informativos")
@RequiredArgsConstructor
@Validated
@Tag(name = "Registros informativos", description = "Publicaciones tipo blog administradas desde Configuracion y mostradas en Inicio")
public class RegistroInformativoController {

    private final RegistroInformativoService registroInformativoService;

    @Operation(summary = "Listar registros informativos", description = "Devuelve todos los registros informativos, del mas reciente al mas antiguo.")
    @ApiResponse(responseCode = "200", description = "Listado de registros informativos",
            content = @Content(schema = @Schema(implementation = RegistroInformativoDTO.class)))
    @GetMapping
    public ResponseEntity<List<RegistroInformativoDTO>> listarTodos() {
        return ResponseEntity.ok(registroInformativoService.listarTodos());
    }

    @Operation(summary = "Crear registro informativo", description = "Crea una nueva publicacion informativa para la pagina de Inicio. La fecha de publicacion la asigna el servidor.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registro informativo creado",
                content = @Content(schema = @Schema(implementation = RegistroInformativoDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos invalidos", content = @Content)
    })
    @PostMapping
    public ResponseEntity<RegistroInformativoDTO> guardar(@Valid @RequestBody RegistroInformativoDTO registroDTO) {
        return ResponseEntity.ok(registroInformativoService.guardar(registroDTO));
    }

    // Hallazgo post-despliegue (fase9): "deberia permitir editar". Se edita
    // solo titulo y contenido; la fecha de publicacion original se conserva.
    @Operation(summary = "Editar registro informativo", description = "Edita el titulo y contenido de un registro informativo existente. La fecha de publicacion original no cambia.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Registro informativo actualizado",
                content = @Content(schema = @Schema(implementation = RegistroInformativoDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos invalidos", content = @Content),
        @ApiResponse(responseCode = "404", description = "El registro informativo no existe", content = @Content)
    })
    @PutMapping("/{id}")
    public ResponseEntity<RegistroInformativoDTO> actualizar(
            @Parameter(description = "Id del registro informativo a editar") @PathVariable @Min(1) Long id,
            @Valid @RequestBody RegistroInformativoDTO registroDTO) {
        return ResponseEntity.ok(registroInformativoService.actualizar(id, registroDTO));
    }

    @Operation(summary = "Eliminar registro informativo", description = "Elimina un registro informativo por su id.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Registro informativo eliminado", content = @Content),
        @ApiResponse(responseCode = "400", description = "El registro informativo no existe", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Id del registro informativo a eliminar") @PathVariable @Min(1) Long id) {
        registroInformativoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
