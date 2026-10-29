package com.marin.mas_que_amigos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

class JwtTokenServiceTest {

    private static final String SECRET = "unit-test-secret-unit-test-secret-0123456789";

    private MutableClock clock;
    private JwtTokenService service;

    @BeforeEach
    void setUp() {
        clock = new MutableClock(Instant.parse("2026-10-21T10:00:00Z"));
        service = new JwtTokenService(properties(SECRET, Duration.ofMinutes(3)), clock);
    }

    private static SecurityProperties properties(String secret, Duration ttl) {
        SecurityProperties properties = new SecurityProperties();
        properties.getJwt().setSecret(secret);
        properties.getJwt().setTtl(ttl);
        return properties;
    }

    @Test
    void issue_putsUserIdentityAndRoleInTheClaims() {
        JwtTokenService.IssuedToken issued = service.issue(7L, "joan", "DT");

        Jwt jwt = service.decode(issued.getToken());

        assertThat(jwt.getSubject()).isEqualTo("joan");
        assertThat(((Number) jwt.getClaim("uid")).longValue()).isEqualTo(7L);
        assertThat(jwt.<String>getClaim("role")).isEqualTo("DT");
        assertThat(jwt.getIssuedAt()).isEqualTo(clock.instant());
        assertThat(jwt.getExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(3)));
        assertThat(jwt.getHeaders()).containsEntry("alg", "HS256");
    }

    @Test
    void issue_reportsTheConfiguredLifetimeInSeconds() {
        assertThat(service.issue(1L, "admin", "ADMIN").getExpiresInSeconds()).isEqualTo(180L);

        JwtTokenService shortLived = new JwtTokenService(properties(SECRET, Duration.ofSeconds(10)), clock);
        assertThat(shortLived.issue(1L, "admin", "ADMIN").getExpiresInSeconds()).isEqualTo(10L);
    }

    @Test
    void decode_acceptsTheTokenBeforeItExpires() {
        String token = service.issue(1L, "admin", "ADMIN").getToken();

        clock.advance(Duration.ofSeconds(179));

        assertThat(service.decode(token).getSubject()).isEqualTo("admin");
    }

    @Test
    void decode_rejectsTheTokenRightAfterItsLifetimeWithoutClockSkewTolerance() {
        String token = service.issue(1L, "admin", "ADMIN").getToken();

        // Spring tolerates 60 s of clock skew by default, which would keep this
        // token alive until minute 4. The service must expire it at minute 3.
        clock.advance(Duration.ofSeconds(181));

        assertThatThrownBy(() -> service.decode(token))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void decode_rejectsATamperedToken() {
        String token = service.issue(1L, "joan", "DT").getToken();
        String[] parts = token.split("\\.");
        // Swap the payload for another user's, keeping the original signature.
        String forgedPayload = service.issue(1L, "admin", "ADMIN").getToken().split("\\.")[1];
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThatThrownBy(() -> service.decode(tampered)).isInstanceOf(JwtException.class);
    }

    @Test
    void decode_rejectsATokenSignedWithAnotherSecret() {
        JwtTokenService other = new JwtTokenService(
                properties("another-secret-another-secret-0123456789", Duration.ofMinutes(3)), clock);
        String foreignToken = other.issue(1L, "admin", "ADMIN").getToken();

        assertThatThrownBy(() -> service.decode(foreignToken)).isInstanceOf(JwtException.class);
    }

    @Test
    void decode_rejectsMalformedTokens() {
        assertThatThrownBy(() -> service.decode("not-a-token")).isInstanceOf(JwtException.class);
    }

    @Test
    void constructor_failsWhenTheSecretIsMissingBlankOrShorterThan32Bytes() {
        assertThatThrownBy(() -> new JwtTokenService(properties(null, Duration.ofMinutes(3)), clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.security.jwt.secret");
        assertThatThrownBy(() -> new JwtTokenService(properties("   ", Duration.ofMinutes(3)), clock))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtTokenService(properties("too-short", Duration.ofMinutes(3)), clock))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    /** Clock the tests can move forward, so expiry is checked without sleeping. */
    private static final class MutableClock extends Clock {
        private Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        void advance(Duration duration) {
            now = now.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }
}
