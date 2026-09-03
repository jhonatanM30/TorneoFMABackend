package com.marin.mas_que_amigos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.io.File;

// FRONTEND_VISION.md Fase1: "boton de carga que me permita subir imagenes
// desde el dispositivo y que esta se guarde donde estan las otras
// existentes para el escudo del equipo". No existia ninguna capacidad de
// archivos en el backend (ni multipart ni servido de estaticos) antes de
// este hallazgo: se agrega aqui, sirviendo el directorio configurado en
// app.uploads.dir bajo la ruta publica /uploads/**. Ver
// EquipoController#subirImagenEquipo / EquipoService#actualizarImagenEquipo.
@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String rutaAbsoluta = new File(uploadsDir).getAbsolutePath();
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + rutaAbsoluta + File.separator);
    }
}
