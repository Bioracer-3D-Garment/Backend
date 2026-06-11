package Bioracer.BachelorProject.Backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.util.ReflectionTestUtils;

import Bioracer.BachelorProject.Backend.config.JwtProperties;
import Bioracer.BachelorProject.Backend.model.Role;
import Bioracer.BachelorProject.Backend.model.User;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

    @Mock
    private JwtEncoder jwtEncoder;

    @Mock
    private JwtDecoder jwtDecoder;

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties("secret",
                new JwtProperties.Token("Bioracer_Backend", Duration.ofHours(8)));
        jwtService = new JwtService(jwtProperties, jwtEncoder, jwtDecoder);
    }

    private Jwt jwtWith(String subject, String scope, Long id) {
        return Jwt.withTokenValue("encoded-token")
                .header("alg", "HS256")
                .subject(subject)
                .claim("scope", scope)
                .claim("id", id)
                .build();
    }

    @Test
    void generateTokenEncodesExpectedClaims() {
        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwtWith("jane@example.com", "ROLE_USER", 1000L));

        String token = jwtService.generateToken("jane@example.com", 1000L, Role.USER);

        assertThat(token).isEqualTo("encoded-token");

        ArgumentCaptor<JwtEncoderParameters> parametersCaptor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(parametersCaptor.capture());

        var claims = parametersCaptor.getValue().getClaims().getClaims();
        assertThat(claims.get("iss")).hasToString("Bioracer_Backend");
        assertThat(claims.get("sub")).isEqualTo("jane@example.com");
        assertThat(claims.get("scope")).isEqualTo("ROLE_USER");
        assertThat(claims.get("id")).isEqualTo(1000L);

        Instant issuedAt = (Instant) claims.get("iat");
        Instant expiresAt = (Instant) claims.get("exp");
        assertThat(Duration.between(issuedAt, expiresAt)).isEqualTo(Duration.ofHours(8));
    }

    @Test
    void generateTokenForUserUsesEmailIdAndRole() {
        User user = new User("Jane", "Doe", "jane@example.com", "hashed", Role.ADMIN);
        ReflectionTestUtils.setField(user, "id", 1000L);

        when(jwtEncoder.encode(any(JwtEncoderParameters.class)))
                .thenReturn(jwtWith("jane@example.com", "ROLE_ADMIN", 1000L));

        String token = jwtService.generateToken(user);

        assertThat(token).isEqualTo("encoded-token");

        ArgumentCaptor<JwtEncoderParameters> parametersCaptor = ArgumentCaptor.forClass(JwtEncoderParameters.class);
        verify(jwtEncoder).encode(parametersCaptor.capture());

        var claims = parametersCaptor.getValue().getClaims().getClaims();
        assertThat(claims.get("sub")).isEqualTo("jane@example.com");
        assertThat(claims.get("scope")).isEqualTo("ROLE_ADMIN");
        assertThat(claims.get("id")).isEqualTo(1000L);
    }

    @Test
    void extractEmailReturnsSubject() {
        when(jwtDecoder.decode("token")).thenReturn(jwtWith("jane@example.com", "ROLE_USER", 1000L));

        assertThat(jwtService.extractEmail("token")).isEqualTo("jane@example.com");
    }

    @Test
    void extractRoleReturnsScopeClaim() {
        when(jwtDecoder.decode("token")).thenReturn(jwtWith("jane@example.com", "ROLE_USER", 1000L));

        assertThat(jwtService.extractRole("token")).isEqualTo("ROLE_USER");
    }

    @Test
    void extractIdReturnsIdClaim() {
        when(jwtDecoder.decode("token")).thenReturn(jwtWith("jane@example.com", "ROLE_USER", 1000L));

        assertThat(jwtService.extractId("token")).isEqualTo(1000L);
    }
}
