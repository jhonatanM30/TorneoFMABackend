/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.config;

/**
 *
 * @author JhonatanAlexanderCue
 */
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();

        // 🔹 Aquí defines qué orígenes pueden acceder al backend
        List<String> allowedOrigins = Arrays.asList(
            "http://127.0.0.1:5500",  // 🔹 Para pruebas locales con Live Server
            "http://localhost:5500"  // 🔹 Para otro localhost (por si acaso)           
        );

        config.setAllowedOrigins(allowedOrigins); 
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(Arrays.asList("*"));
        config.setAllowCredentials(true); 

        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}
