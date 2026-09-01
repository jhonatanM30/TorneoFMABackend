package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.service.EquipoService;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Min;


@RestController
@RequestMapping("/api/equipos")
@Validated
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
        return ResponseEntity.ok(equipoService.obtenerEquipoPorNombre(nombre));
    }

    @PostMapping
    public EquipoDTO crearEquipo(@Valid @RequestBody EquipoDTO equipo) {
        return equipoService.guardarEquipo(equipo);
    }

    @PutMapping
    public EquipoDTO editarEquipo(@Valid @RequestBody EquipoDTO equipo) {
        return equipoService.actualizarEquipo(equipo);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarEquipo(@PathVariable @Min(1) Long id) {
        equipoService.eliminarEquipo(id);
        return ResponseEntity.noContent().build();
    }
}
