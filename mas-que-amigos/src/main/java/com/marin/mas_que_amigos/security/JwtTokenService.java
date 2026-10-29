package com.marin.mas_que_amigos.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

/**
 * Issues and validates the short-lived session tokens (HS256 JWT).
 *
 * The expiry check uses a zero clock-skew tolerance: Spring's default validator
 * tolerates 60 seconds, which would turn a 3-minute session into a 4-minute one.
 */
@Service
public class JwtTokenService {

    private static final int MIN_SECRET_BYTES = 32;

    private final Clock clock;
    private final Duration ttl;
    private final JwtEncoder encoder;
    private final NimbusJwtDecoder decoder;

    @Autowired
    public JwtTokenService(SecurityProperties properties) {
        this(properties, Clock.systemUTC());
    }

    // Package-private so tests can move the clock instead of sleeping.
    JwtTokenService(SecurityProperties properties, Clock clock) {
        SecretKey key = new SecretKeySpec(requireSecret(properties.getJwt().getSecret()), "HmacSHA256");
        this.clock = clock;
        this.ttl = properties.getJwt().getTtl();
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));

        JwtTimestampValidator timestampValidator = new JwtTimestampValidator(Duration.ZERO);
        timestampValidator.setClock(clock);
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        this.decoder.setJwtValidator(timestampValidator);
    }

    public IssuedToken issue(Long userId, String username, String role) {
        Instant now = clock.instant();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(username)
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .claim("uid", userId)
                .claim("role", role)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new IssuedToken(token, ttl.getSeconds());
    }

    /** @throws org.springframework.security.oauth2.jwt.JwtException if the token is invalid, altered or expired */
    public Jwt decode(String token) {
        return decoder.decode(token);
    }

    /** Decoder used by the resource-server filter of the security chain. */
    public JwtDecoder jwtDecoder() {
        return decoder;
    }

    private static byte[] requireSecret(String secret) {
        byte[] bytes = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (secret == null || secret.trim().isEmpty() || bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException("app.security.jwt.secret (variable APP_SECURITY_JWT_SECRET) must be defined "
                    + "and have at least " + MIN_SECRET_BYTES + " characters.");
        }
        return bytes;
    }

    public static final class IssuedToken {

        private final String token;
        private final long expiresInSeconds;

        IssuedToken(String token, long expiresInSeconds) {
            this.token = token;
            this.expiresInSeconds = expiresInSeconds;
        }

        public String getToken() {
            return token;
        }

        public long getExpiresInSeconds() {
            return expiresInSeconds;
        }
    }
}
