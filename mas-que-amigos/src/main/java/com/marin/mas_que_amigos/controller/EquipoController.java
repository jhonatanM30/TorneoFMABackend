package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.service.EquipoService;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import org.springframework.validation.annotation.Validated;


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
    public ResponseEntity<EquipoDTO> obtenerEquipo(@Validated @PathVariable String nombre) {
        return ResponseEntity.ok(equipoService.obtenerEquipoPorNombre(nombre)); 
    }

    @PostMapping
    public EquipoDTO crearEquipo(@RequestBody EquipoDTO equipo) {
        return equipoService.guardarEquipo(equipo);
    }
    
    @PutMapping
    public EquipoDTO editarEquipo(@RequestBody EquipoDTO equipo) {
        return equipoService.actualizarEquipo(equipo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEquipo(@PathVariable Long id) {
        equipoService.eliminarEquipo(id);
        return ResponseEntity.noContent().build();
    }
}
