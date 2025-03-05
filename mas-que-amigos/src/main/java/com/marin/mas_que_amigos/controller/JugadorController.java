package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.JugadorDTO;
import com.marin.mas_que_amigos.service.JugadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/jugadores")
public class JugadorController {

    private final JugadorService jugadorService;

    public JugadorController(JugadorService jugadorService) {
        this.jugadorService = jugadorService;
    }

    @GetMapping
    public List<JugadorDTO> obtenerJugadores() {
        return jugadorService.listarJugadores();
    }

    @GetMapping("/{id}")
    public ResponseEntity<JugadorDTO> obtenerJugador(@PathVariable int dorsal) {
        JugadorDTO jugador = jugadorService.obtenerJugadorPorId(dorsal);
        return ResponseEntity.ok(jugador);
    }

    @PostMapping
    public JugadorDTO crearJugador(@RequestBody JugadorDTO jugador) {
        return jugadorService.guardarJugador(jugador);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarJugador(@PathVariable Long id) {
        jugadorService.eliminarJugador(id);
        return ResponseEntity.noContent().build();
    }
}
