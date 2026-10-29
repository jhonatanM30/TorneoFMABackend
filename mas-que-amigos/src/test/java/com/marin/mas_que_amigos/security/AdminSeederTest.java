package com.marin.mas_que_amigos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.marin.mas_que_amigos.model.AppUser;
import com.marin.mas_que_amigos.model.UserRole;
import com.marin.mas_que_amigos.repository.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminSeederTest {

    @Mock
    private AppUserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    private final SecurityProperties properties = new SecurityProperties();
    private final ListAppender<ILoggingEvent> logs = new ListAppender<>();
    private Logger seederLogger;

    @BeforeEach
    void setUp() {
        seederLogger = (Logger) LoggerFactory.getLogger(AdminSeeder.class);
        logs.start();
        seederLogger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        seederLogger.detachAppender(logs);
    }

    private AdminSeeder seeder() {
        return new AdminSeeder(properties, userRepository, passwordEncoder);
    }

    @Test
    void run_createsTheAdminWhenItDoesNotExist() throws Exception {
        properties.getAdmin().setUsername("Admin");
        properties.getAdmin().setPassword("Admin-pass-123");
        when(userRepository.existsByUsername("admin")).thenReturn(false);
        when(passwordEncoder.encode("Admin-pass-123")).thenReturn("$2a$hash");

        seeder().run(null);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(userRepository).save(captor.capture());
        AppUser saved = captor.getValue();
        assertThat(saved.getUsername()).isEqualTo("admin");
        assertThat(saved.getRole()).isEqualTo(UserRole.ADMIN);
        assertThat(saved.getPasswordHash()).isEqualTo("$2a$hash").isNotEqualTo("Admin-pass-123");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getTeam()).isNull();
    }

    @Test
    void run_doesNotCreateNorChangeAnythingWhenTheAdminAlreadyExists() throws Exception {
        properties.getAdmin().setUsername("admin");
        properties.getAdmin().setPassword("another-password-456");
        when(userRepository.existsByUsername("admin")).thenReturn(true);

        seeder().run(null);

        verify(userRepository, never()).save(any(AppUser.class));
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void run_withIncompleteConfigurationCreatesNothingAndLogsAWarning() throws Exception {
        properties.getAdmin().setUsername("admin");
        properties.getAdmin().setPassword("  ");

        seeder().run(null);

        verifyNoInteractions(userRepository, passwordEncoder);
        assertThat(logs.list).anySatisfy(event -> {
            assertThat(event.getLevel()).isEqualTo(Level.WARN);
            assertThat(event.getFormattedMessage()).contains("APP_SECURITY_ADMIN_USERNAME");
        });
    }

    @Test
    void run_withoutAnyConfigurationCreatesNothingAndLogsAWarning() throws Exception {
        seeder().run(null);

        verifyNoInteractions(userRepository, passwordEncoder);
        assertThat(logs.list).anySatisfy(event -> assertThat(event.getLevel()).isEqualTo(Level.WARN));
    }
}
