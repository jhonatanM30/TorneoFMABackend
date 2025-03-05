/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.service.PartidoService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.*;


/**
 *
 * @author JhonatanAlexanderCue
 */
@RestController
@RequestMapping("/api/partidos")
@RequiredArgsConstructor
public class PartidoController {

    private final PartidoService partidoService;

    @GetMapping
    public ResponseEntity<List<PartidoDTO>> listarTodos() {
        return ResponseEntity.ok(partidoService.listarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<List<PartidoDTO>> obtenerPorFecha(@PathVariable LocalDate fecha) {
        return ResponseEntity.ok(partidoService.buscarPorFecha(fecha));
    }

    @PostMapping
    public ResponseEntity<PartidoDTO> guardar(@RequestBody PartidoDTO partidoDTO) {
        return ResponseEntity.ok(partidoService.guardar(partidoDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        partidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}