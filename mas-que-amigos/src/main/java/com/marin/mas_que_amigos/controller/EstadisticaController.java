/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.EstadisticaDTO;
import com.marin.mas_que_amigos.service.EstadisticaService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/estadisticas")
@RequiredArgsConstructor
public class EstadisticaController {

    private final EstadisticaService estadisticaService;

    @GetMapping
    public ResponseEntity<List<EstadisticaDTO>> listarTodas() {
        return ResponseEntity.ok(estadisticaService.listarTodas());
    }

    @GetMapping("/{id}")
    public ResponseEntity<EstadisticaDTO> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(estadisticaService.buscarPorId(id));
    }

    @PostMapping
    public ResponseEntity<EstadisticaDTO> guardar(@RequestBody EstadisticaDTO estadisticaDTO) {
        return ResponseEntity.ok(estadisticaService.guardar(estadisticaDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        estadisticaService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
