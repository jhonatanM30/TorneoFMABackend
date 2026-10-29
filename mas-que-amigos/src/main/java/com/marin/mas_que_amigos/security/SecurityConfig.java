package com.marin.mas_que_amigos.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Central authorization table, evaluated in this order. The rules live here, not in
 * method annotations, so that a denied call is answered by the 401/403 handlers: an
 * AccessDeniedException thrown inside a controller would be caught by the generic
 * handler of GlobalExceptionHandler and turned into a 500.
 *
 * The last /api/** rule is the safety net: any write endpoint added in the future
 * requires ADMIN until it is explicitly opened here.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenService tokenService,
            JsonAuthenticationEntryPoint entryPoint, JsonAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                // Reuses the CorsFilter bean of CorsConfig, so the allowed origins stay in one place.
                .cors().and()
                // Bearer tokens in a header, no cookies: there is no CSRF surface.
                .csrf().disable()
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS).and()
                .exceptionHandling()
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler).and()
                .authorizeRequests()
                        .antMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .antMatchers("/error").permitAll()
                        .antMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .antMatchers(HttpMethod.POST, "/api/auth/refresh").authenticated()
                        .antMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                        .antMatchers("/api/users", "/api/users/**").hasRole("ADMIN")
                        .antMatchers(HttpMethod.POST, "/api/transfers").hasAnyRole("ADMIN", "DT")
                        .antMatchers(HttpMethod.POST, "/api/transfers/*/confirm", "/api/transfers/*/reject").hasRole("ADMIN")
                        .antMatchers(HttpMethod.GET, "/api/**", "/uploads/**", "/v3/api-docs", "/v3/api-docs/**",
                                "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .antMatchers("/api/**").hasRole("ADMIN")
                        .anyRequest().denyAll().and()
                .oauth2ResourceServer()
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler)
                        .jwt()
                                .decoder(tokenService.jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter());
        return http.build();
    }

    // The "role" claim (ADMIN or DT) becomes the authority ROLE_ADMIN or ROLE_DT.
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }
}
