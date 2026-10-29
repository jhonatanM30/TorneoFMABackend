package com.marin.mas_que_amigos.security;

import com.marin.mas_que_amigos.model.AppUser;
import com.marin.mas_que_amigos.model.UserRole;
import com.marin.mas_que_amigos.repository.AppUserRepository;
import java.time.LocalDateTime;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Creates the initial administrator on startup from configuration. It never
 * touches an existing account (so a changed password in the environment does not
 * overwrite the stored one) and, when the configuration is missing, the app still
 * starts (read-only) and a warning is logged.
 */
@Component
public class AdminSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminSeeder.class);

    private final SecurityProperties properties;
    private final AppUserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminSeeder(SecurityProperties properties, AppUserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        String username = properties.getAdmin().getUsername();
        String password = properties.getAdmin().getPassword();

        if (isBlank(username) || isBlank(password)) {
            log.warn("Initial admin not created: define APP_SECURITY_ADMIN_USERNAME and APP_SECURITY_ADMIN_PASSWORD. "
                    + "Until an admin exists the API is read-only.");
            return;
        }

        String normalizedUsername = username.trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByUsername(normalizedUsername)) {
            log.info("Initial admin '{}' already exists; leaving it untouched.", normalizedUsername);
            return;
        }

        AppUser admin = new AppUser();
        admin.setUsername(normalizedUsername);
        admin.setPasswordHash(passwordEncoder.encode(password));
        admin.setRole(UserRole.ADMIN);
        admin.setCreatedAt(LocalDateTime.now());
        userRepository.save(admin);
        log.info("Initial admin '{}' created.", normalizedUsername);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
