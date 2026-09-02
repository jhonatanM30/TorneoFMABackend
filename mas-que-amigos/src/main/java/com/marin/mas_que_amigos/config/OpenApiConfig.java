/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos generales expuestos en /v3/api-docs y /swagger-ui.html.
 *
 * @author JhonatanAlexanderCue
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "MAS-QUE-AMIGOS API",
                version = "1.0.0",
                description = "API REST para gestionar equipos, jugadores, partidos, alineaciones "
                        + "y estadísticas de una liga de fútbol amateur o semiprofesional.",
                contact = @Contact(name = "Jhonatan Alexander Cuesta")
        )
)
public class OpenApiConfig {
}
