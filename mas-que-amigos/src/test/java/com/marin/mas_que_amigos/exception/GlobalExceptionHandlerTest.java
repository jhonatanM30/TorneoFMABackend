package com.marin.mas_que_amigos.exception;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    // Antes de la corrección, este método del handler tenía el parámetro
    // tipado como NotFoundException, que NO es compatible con
    // JugadorNotFoundException (ambas heredan directo de RuntimeException).
    // Esta prueba no compilaría contra el código anterior; sirve tanto de
    // regresión como de evidencia del bug corregido.
    @Test
    void handleJugadorNotFound_devuelve404ConMensaje() {
        JugadorNotFoundException ex = new JugadorNotFoundException("Jugador no encontrado");

        ResponseEntity<Map<String, String>> response = handler.handleJugadorNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Jugador no encontrado");
    }
}
