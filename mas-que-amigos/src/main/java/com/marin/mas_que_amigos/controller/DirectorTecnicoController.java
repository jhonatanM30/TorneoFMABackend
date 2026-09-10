package com.marin.mas_que_amigos.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Hallazgo post-despliegue (tercera ronda, fase8-13): antes, la unica forma
 * de saber si la clave de Director Tecnico ingresada era correcta era
 * intentar una escritura real (crear/editar/eliminar algo) y enterarse
 * recien con un 401 si estaba mal (ver hallazgo 8-9, manejo reactivo en
 * apiErrors.js). El usuario pidio poder validarla en el momento de
 * ingresarla, sin dejarlo "pasar" a modo Director Tecnico a menos que sea
 * correcta.
 *
 * Este endpoint no valida nada por su cuenta: al ser un metodo POST,
 * DirectorTecnicoInterceptor ya exige y compara el header
 * X-Director-Tecnico-Key ANTES de que la peticion llegue aca (interceptor
 * registrado sobre todo /api/** en DirectorTecnicoWebConfig). Si esta
 * linea llega a ejecutarse es porque el interceptor ya valido la clave; si
 * la clave fuera incorrecta o faltara, el interceptor corta la peticion
 * con 401 y este metodo ni se llega a invocar. Se evita asi duplicar en
 * dos lugares la logica de comparacion de la clave.
 *
 * @author JhonatanAlexanderCue
 */
@RestController
@RequestMapping("/api/director-tecnico")
@Tag(name = "Director Tecnico", description = "Validacion de la clave de Director Tecnico (FRONTEND_VISION.md Fase7 + hallazgo post-despliegue)")
public class DirectorTecnicoController {

    @Operation(summary = "Validar la clave de Director Tecnico",
            description = "No hace nada por si mismo: si la peticion llega hasta aca es porque "
                    + "DirectorTecnicoInterceptor ya valido el header X-Director-Tecnico-Key. Se usa "
                    + "para confirmar la clave en el momento de ingresarla, antes de dejar pasar al "
                    + "usuario a modo Director Tecnico en el frontend.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "La clave enviada en X-Director-Tecnico-Key es correcta", content = @Content),
        @ApiResponse(responseCode = "401", description = "La clave enviada es incorrecta o falta (respondido por DirectorTecnicoInterceptor)", content = @Content)
    })
    @PostMapping("/validar")
    public ResponseEntity<Void> validar() {
        return ResponseEntity.ok().build();
    }
}
