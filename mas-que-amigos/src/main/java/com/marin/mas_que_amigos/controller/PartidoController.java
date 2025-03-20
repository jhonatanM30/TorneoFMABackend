/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.service.PartidoService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;

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
    public ResponseEntity<List<PartidoDTO>> listarPartidos() {
        return ResponseEntity.ok(partidoService.listarPartidos());
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<List<PartidoDTO>> buscarPartidoPorEquipo(@PathVariable String nombre) {
        return ResponseEntity.ok(partidoService.buscarPartidoPorEquipo(nombre));
    }

    @PostMapping
    public ResponseEntity<PartidoDTO> crearPartido(@RequestBody PartidoDTO partidoDTO) {
        return ResponseEntity.ok(partidoService.guardar(partidoDTO));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        partidoService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}