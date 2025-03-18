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

    @GetMapping("/{nombre}")
    public ResponseEntity<List<JugadorDTO>> obtenerJugador(@PathVariable String nombre) {
        List<JugadorDTO> rspjugadores = jugadorService.obtenerJugadorPorNombre(nombre);
        return ResponseEntity.ok(rspjugadores);
    }

    @PostMapping
    public JugadorDTO crearJugador(@RequestBody JugadorDTO jugador) {
        return jugadorService.guardarJugador(jugador);
    }
    
    @PutMapping
    public JugadorDTO editarJugador(@RequestBody JugadorDTO jugador) {
        return jugadorService.actualizarJugador(jugador);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarJugador(@PathVariable Long id) {
        jugadorService.eliminarJugador(id);
        return ResponseEntity.noContent().build();
    }
}
