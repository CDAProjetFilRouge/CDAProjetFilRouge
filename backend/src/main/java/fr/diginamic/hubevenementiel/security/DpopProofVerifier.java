package fr.diginamic.hubevenementiel.security;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import io.jsonwebtoken.security.PublicJwk;

@Component
public class DpopProofVerifier {

    private static final String PROOF_TYPE = "dpop+jwt";
    private static final String PROOF_ALGORITHM = "ES256";
    private static final long MAX_AGE_SECONDS = 60;
    private static final long MEMORY_SECONDS = 120;

    private final Map<String, Instant> seenIds = new ConcurrentHashMap<>();

    public record Proof(String thumbprint, String accessTokenHash) {
    }

    private UnauthorizedException invalid() {
        return new UnauthorizedException("Preuve DPoP invalide.");
    }

    private void checkFreshness(Claims claims) throws HttpException {
        Number issuedAt = claims.get("iat", Number.class);
        if (issuedAt == null) {
            throw invalid();
        }
        long age = Instant.now().getEpochSecond() - issuedAt.longValue();
        if (Math.abs(age) > MAX_AGE_SECONDS) {
            throw invalid();
        }
    }

    private void rememberId(String id) throws HttpException {
        if (id == null || id.isBlank()) {
            throw invalid();
        }
        Instant now = Instant.now();
        seenIds.values().removeIf(seenAt -> seenAt.isBefore(now.minusSeconds(MEMORY_SECONDS)));
        if (seenIds.putIfAbsent(id, now) != null) {
            throw invalid();
        }
    }

    public static String hashOfAccessToken(String accessToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(accessToken.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public Proof verify(String proofJwt, String method, String url) throws HttpException {
        if (proofJwt == null || proofJwt.isBlank()) {
            throw invalid();
        }

        Claims claims;
        PublicJwk<?> jwk;
        try {
            var jws = Jwts.parser()
                    .keyLocator(new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(JwsHeader header) {
                            return header.getJwk().toKey();
                        }
                    })
                    .build()
                    .parseSignedClaims(proofJwt);

            if (!PROOF_TYPE.equals(jws.getHeader().getType())
                    || !PROOF_ALGORITHM.equals(jws.getHeader().getAlgorithm())) {
                throw invalid();
            }
            claims = jws.getPayload();
            jwk = jws.getHeader().getJwk();
        } catch (JwtException | IllegalArgumentException | NullPointerException exception) {
            throw invalid();
        }

        if (!method.equals(claims.get("htm", String.class)) || !url.equals(claims.get("htu", String.class))) {
            throw invalid();
        }

        checkFreshness(claims);
        rememberId(claims.getId());

        return new Proof(jwk.thumbprint().toString(), claims.get("ath", String.class));
    }

}
