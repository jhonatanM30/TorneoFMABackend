package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.JugadorDTO;
import com.marin.mas_que_amigos.service.JugadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Min;

@RestController
@RequestMapping("/api/jugadores")
@Validated
public class JugadorController {

    private final JugadorService jugadorService;

    public JugadorController(JugadorService jugadorService) {
        this.jugadorService = jugadorService;
    }

    @GetMapping
    public List<JugadorDTO> obtenerJugadores() {
        return jugadorService.listarJugadores();
    }

    @GetMapping("/{nombre}")
    public ResponseEntity<List<JugadorDTO>> obtenerJugador(@PathVariable String nombre) {
        List<JugadorDTO> rspjugadores = jugadorService.obtenerJugadorPorNombre(nombre);
        return ResponseEntity.ok(rspjugadores);
    }

    @PostMapping
    public JugadorDTO crearJugador(@Valid @RequestBody JugadorDTO jugador) {
        return jugadorService.guardarJugador(jugador);
    }

    @PutMapping
    public JugadorDTO editarJugador(@Valid @RequestBody JugadorDTO jugador) {
        return jugadorService.actualizarJugador(jugador);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarJugador(@PathVariable @Min(1) Long id) {
        jugadorService.eliminarJugador(id);
        return ResponseEntity.noContent().build();
    }
}
