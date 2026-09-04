package com.marin.mas_que_amigos.config;

import com.marin.mas_que_amigos.security.DirectorTecnicoInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el DirectorTecnicoInterceptor (FRONTEND_VISION.md Fase 7) sobre
 * todo /api/**. La clave se lee de app.director-tecnico.clave, que a su
 * vez se puede sobreescribir con la variable de entorno
 * APP_DIRECTOR_TECNICO_CLAVE (relajado de Spring Boot, igual que las
 * variables SPRING_DATASOURCE_* de application.properties), para no
 * dejarla escrita en el codigo fuente en un despliegue real. Se deja un
 * valor por defecto solo para que el entorno de desarrollo local funcione
 * sin configuracion adicional.
 *
 * @author JhonatanAlexanderCue
 */
@Configuration
public class DirectorTecnicoWebConfig implements WebMvcConfigurer {

    @Value("${app.director-tecnico.clave:director2025}")
    private String claveDirectorTecnico;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new DirectorTecnicoInterceptor(claveDirectorTecnico))
                .addPathPatterns("/api/**");
    }
}
