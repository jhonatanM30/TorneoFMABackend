package com.marin.mas_que_amigos.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.marin.mas_que_amigos.dto.ErrorResponseDTO;
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

    @Test
    void handleAuthenticationFailed_returns401WithTheStandardErrorBody() {
        AuthenticationFailedException ex = new AuthenticationFailedException("Usuario o contraseña incorrectos");

        ResponseEntity<ErrorResponseDTO> response = handler.handleAuthenticationFailed(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody().getIndicadorRespuesta()).isEqualTo("No autorizado");
        assertThat(response.getBody().getMensaje()).isEqualTo("Usuario o contraseña incorrectos");
    }

    @Test
    void handleForbiddenOperation_returns403WithTheStandardErrorBody() {
        ForbiddenOperationException ex = new ForbiddenOperationException("Solo puedes solicitar traspasos hacia tu equipo.");

        ResponseEntity<ErrorResponseDTO> response = handler.handleForbiddenOperation(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody().getIndicadorRespuesta()).isEqualTo("Prohibido");
        assertThat(response.getBody().getMensaje()).isEqualTo("Solo puedes solicitar traspasos hacia tu equipo.");
    }

    @Test
    void handleUserNotFound_returns404WithMessage() {
        UserNotFoundException ex = new UserNotFoundException("La cuenta no existe");

        ResponseEntity<Map<String, String>> response = handler.handleUserNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "La cuenta no existe");
    }

    @Test
    void handleTransferNotFound_returns404WithMessage() {
        TransferNotFoundException ex = new TransferNotFoundException("El traspaso no existe");

        ResponseEntity<Map<String, String>> response = handler.handleTransferNotFound(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "El traspaso no existe");
    }
}
