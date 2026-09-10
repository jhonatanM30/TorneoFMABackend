package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.ImagenGaleriaDTO;
import com.marin.mas_que_amigos.service.ImagenGaleriaService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import javax.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Hallazgo post-despliegue (fase9): galeria de fotos de la pagina de
 * Inicio, administrada desde Configuracion (subir/listar/eliminar),
 * reemplazando las 5 imagenes fijas que antes estaban codificadas
 * directamente en index.html.
 *
 * @author JhonatanAlexanderCue
 */
@RestController
@RequestMapping("/api/galeria")
@RequiredArgsConstructor
@Validated
@Tag(name = "Galería", description = "Fotos del carrusel de la página de Inicio, administradas desde Configuración")
public class ImagenGaleriaController {

    private final ImagenGaleriaService imagenGaleriaService;

    @Operation(summary = "Listar fotos de la galería", description = "Devuelve todas las fotos de la galería, de la más reciente a la más antigua.")
    @ApiResponse(responseCode = "200", description = "Listado de fotos",
            content = @Content(schema = @Schema(implementation = ImagenGaleriaDTO.class)))
    @GetMapping
    public ResponseEntity<List<ImagenGaleriaDTO>> listarTodas() {
        return ResponseEntity.ok(imagenGaleriaService.listarTodas());
    }

    @Operation(summary = "Subir foto a la galería", description = "Sube una imagen (jpg, png, webp o gif) desde el dispositivo del usuario y la agrega al carrusel de Inicio.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Imagen agregada a la galería",
                content = @Content(schema = @Schema(implementation = ImagenGaleriaDTO.class))),
        @ApiResponse(responseCode = "400", description = "Archivo faltante o formato no soportado", content = @Content)
    })
    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ImagenGaleriaDTO> subirImagen(
            @Parameter(description = "Archivo de imagen (jpg, png, webp, gif)") @RequestParam("imagen") MultipartFile imagen) {
        return ResponseEntity.ok(imagenGaleriaService.subirImagen(imagen));
    }

    @Operation(summary = "Eliminar foto de la galería", description = "Elimina una foto de la galería por su id.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Foto eliminada", content = @Content),
        @ApiResponse(responseCode = "404", description = "La foto no existe", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(
            @Parameter(description = "Id de la foto a eliminar") @PathVariable @Min(1) Long id) {
        imagenGaleriaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
