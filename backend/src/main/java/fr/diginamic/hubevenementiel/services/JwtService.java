package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private static final String ROLE_CLAIM = "role";
    private static final String ID_CLAIM = "id";
    private static final String CONFIRMATION_CLAIM = "cnf";
    private static final String KEY_THUMBPRINT_FIELD = "jkt";

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generateToken(AppUser appUser) {
        return generateToken(appUser, null);
    }

    public String generateToken(AppUser appUser, String keyThumbprint) {
        JwtBuilder builder = Jwts.builder()
                .subject(appUser.getEmail())
                .claim(ROLE_CLAIM, appUser.getRole().name())
                .claim(ID_CLAIM, String.valueOf(appUser.getId()))
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs));
        if (keyThumbprint != null) {
            builder.claim(CONFIRMATION_CLAIM, Map.of(KEY_THUMBPRINT_FIELD, keyThumbprint));
        }
        return builder.signWith(key).compact();
    }

    public String extractKeyThumbprint(String token) {
        Object confirmation = extractClaims(token).get(CONFIRMATION_CLAIM);
        if (confirmation instanceof Map<?, ?> fields && fields.get(KEY_THUMBPRINT_FIELD) instanceof String thumbprint) {
            return thumbprint;
        }
        return null;
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public String extractRole(String token) {
        return extractClaims(token).get(ROLE_CLAIM, String.class);
    }

    public Long extractUserId(String token) {
        return Long.parseLong(extractClaims(token).get(ID_CLAIM, String.class));
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
