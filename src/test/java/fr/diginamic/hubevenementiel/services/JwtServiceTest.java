package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "ceci-est-un-secret-de-test-suffisamment-long-pour-hmac-sha256";

    private JwtService jwtService;
    private AppUser user;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000L);

        user = new AppUser();
        user.setId(6L);
        user.setEmail("alice.martin@example.com");
        user.setRole(Role.ORGANIZER);
    }

    @Test
    void generateAndExtractEmail_roundTrip_matchesOriginal() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractEmail(token)).isEqualTo(user.getEmail());
    }

    @Test
    void generateAndExtractRole_roundTrip_matchesOriginal() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractRole(token)).isEqualTo("ORGANIZER");
    }

    @Test
    void generateAndExtractUserId_roundTrip_matchesOriginal() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUserId(token)).isEqualTo(6L);
    }

    @Test
    void isTokenValid_validToken_returnsTrue() {
        String token = jwtService.generateToken(user);

        assertThat(jwtService.isTokenValid(token)).isTrue();
    }

    @Test
    void isTokenValid_expiredToken_returnsFalse() throws InterruptedException {
        JwtService shortLivedService = new JwtService(SECRET, 1L);
        String token = shortLivedService.generateToken(user);
        Thread.sleep(20);

        assertThat(shortLivedService.isTokenValid(token)).isFalse();
    }

    @Test
    void isTokenValid_tamperedSignature_returnsFalse() {
        String token = jwtService.generateToken(user);
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThat(jwtService.isTokenValid(tampered)).isFalse();
    }

    @Test
    void isTokenValid_malformedToken_returnsFalse() {
        assertThat(jwtService.isTokenValid("ceci-n-est-pas-un-jwt")).isFalse();
    }
}
