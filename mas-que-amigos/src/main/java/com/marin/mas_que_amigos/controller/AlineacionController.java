/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.AlineacionDTO;
import com.marin.mas_que_amigos.service.AlineacionService;
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
public class AlineacionController {

    private final AlineacionService alineacionService;

    @PostMapping
    public ResponseEntity<AlineacionDTO> crearAlineacion(@Valid @RequestBody AlineacionDTO alineacionDTO) {
        return ResponseEntity.ok(alineacionService.guardarAlineacion(alineacionDTO));
    }

    @GetMapping("/partido/{idPartido}")
    public ResponseEntity<List<AlineacionDTO>> obtenerPorPartido(@PathVariable @Min(1) Long idPartido) {
        return ResponseEntity.ok(alineacionService.obtenerAlineacionPorPartido(idPartido));
    }
}
