package com.marin.mas_que_amigos.security;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Security settings bound from {@code app.security.*}. There are deliberately no
 * defaults for the secret or the seeded admin credentials: they come from
 * environment variables (APP_SECURITY_JWT_SECRET, APP_SECURITY_ADMIN_USERNAME,
 * APP_SECURITY_ADMIN_PASSWORD).
 */
@Component
@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {

    private final Jwt jwt = new Jwt();
    private final Admin admin = new Admin();

    public Jwt getJwt() {
        return jwt;
    }

    public Admin getAdmin() {
        return admin;
    }

    public static class Jwt {

        private String secret;
        private Duration ttl = Duration.ofMinutes(3);

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public Duration getTtl() {
            return ttl;
        }

        public void setTtl(Duration ttl) {
            this.ttl = ttl;
        }
    }

    public static class Admin {

        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
