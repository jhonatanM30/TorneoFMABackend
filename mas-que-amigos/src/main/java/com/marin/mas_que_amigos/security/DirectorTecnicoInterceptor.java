package com.marin.mas_que_amigos.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.marin.mas_que_amigos.dto.ErrorResponseDTO;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * FRONTEND_VISION.md Fase 7 (hallazgo unico, marcado "#CONSTANTE
 * INTENCIONAL#"): mientras no exista un login real que distinga
 * Director Tecnico de usuarios consultivos, se valida una clave fija
 * (ver DirectorTecnicoWebConfig / app.director-tecnico.clave) en TODOS
 * los metodos que no son de solo consulta (GET/HEAD/OPTIONS quedan
 * libres; POST/PUT/DELETE/PATCH exigen el header X-Director-Tecnico-Key).
 *
 * Se implementa como un interceptor central (no anotacion por
 * controlador) para que ningun endpoint de escritura nuevo quede
 * desprotegido por olvido: aplica a todo /api/** por configuracion
 * (ver DirectorTecnicoWebConfig#addInterceptors), no por cada
 * controlador.
 *
 * @author JhonatanAlexanderCue
 */
public class DirectorTecnicoInterceptor implements HandlerInterceptor {

    public static final String HEADER_CLAVE = "X-Director-Tecnico-Key";

    private static final Set<String> METODOS_DE_CONSULTA = new HashSet<>(
            Arrays.asList("GET", "HEAD", "OPTIONS"));

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String claveConfigurada;

    public DirectorTecnicoInterceptor(String claveConfigurada) {
        this.claveConfigurada = claveConfigurada;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (METODOS_DE_CONSULTA.contains(request.getMethod())) {
            return true;
        }

        String claveRecibida = request.getHeader(HEADER_CLAVE);
        if (claveConfigurada != null && !claveConfigurada.isEmpty() && claveConfigurada.equals(claveRecibida)) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        ErrorResponseDTO error = new ErrorResponseDTO(
                "No autorizado",
                "Esta accion requiere la clave de Director Tecnico. Solo puedes consultar informacion.");
        response.getWriter().write(objectMapper.writeValueAsString(error));
        return false;
    }
}
