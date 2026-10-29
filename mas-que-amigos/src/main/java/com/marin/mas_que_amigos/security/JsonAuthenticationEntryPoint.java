package com.marin.mas_que_amigos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marin.mas_que_amigos.dto.ErrorResponseDTO;
import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/**
 * 401 when the token is missing, invalid or expired, with the same
 * {indicadorRespuesta, mensaje} body the API already used for access errors.
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    static final String MESSAGE_LOGIN_REQUIRED = "Debes iniciar sesión para realizar esta acción.";
    static final String MESSAGE_SESSION_EXPIRED = "Tu sesión expiró. Inicia sesión de nuevo.";

    private final ObjectMapper objectMapper;

    public JsonAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
            AuthenticationException authException) throws IOException {
        String message = isExpiredToken(authException) ? MESSAGE_SESSION_EXPIRED : MESSAGE_LOGIN_REQUIRED;
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponseDTO("No autorizado", message)));
    }

    // The JWT decoder reports an expired token only through the text of its error.
    private boolean isExpiredToken(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            String text = current.getMessage();
            if (text != null && text.toLowerCase().contains("expired")) {
                return true;
            }
        }
        return false;
    }
}
