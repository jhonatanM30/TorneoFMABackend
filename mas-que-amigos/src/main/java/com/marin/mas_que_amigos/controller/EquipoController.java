package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.service.EquipoService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/equipos")
public class EquipoController {

    private final EquipoService equipoService;

    // Solo inyectamos el servicio
    public EquipoController(EquipoService equipoService) {
        this.equipoService = equipoService;
    }

    @GetMapping
    public List<EquipoDTO> obtenerEquipos() {
        return equipoService.listarEquipos();
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<EquipoDTO> obtenerEquipo(@PathVariable String nombre) {
        EquipoDTO equipo = equipoService.obtenerEquipoPorNombre(nombre);
        return ResponseEntity.ok(equipo);  // Retorna 200 OK con el equipo encontrado
    }

    @PostMapping
    public EquipoDTO crearEquipo(@RequestBody EquipoDTO equipo) {
        return equipoService.guardarEquipo(equipo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEquipo(@PathVariable Long id) {
        equipoService.eliminarEquipo(id);
        return ResponseEntity.noContent().build();
    }
}
