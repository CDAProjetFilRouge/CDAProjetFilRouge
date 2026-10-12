package fr.diginamic.hubevenementiel.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.security.KeyPair;
import java.time.Instant;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.UnauthorizedException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Jwks;
import io.jsonwebtoken.security.PublicJwk;

class DpopProofVerifierTest {

    private static final String URL = "http://localhost:8080/auth/refresh";

    private DpopProofVerifier verifier;
    private KeyPair keyPair;

    @BeforeEach
    void setUp() {
        verifier = new DpopProofVerifier();
        keyPair = Jwts.SIG.ES256.keyPair().build();
    }

    private String proof(KeyPair signer, String method, String url, long issuedAt, String id) {
        PublicJwk<?> jwk = Jwks.builder().key(signer.getPublic()).build();
        return Jwts.builder()
                .header().type("dpop+jwt").jwk(jwk).and()
                .id(id)
                .claim("htm", method)
                .claim("htu", url)
                .claim("iat", issuedAt)
                .signWith(signer.getPrivate(), Jwts.SIG.ES256)
                .compact();
    }

    private String validProof() {
        return proof(keyPair, "POST", URL, Instant.now().getEpochSecond(), UUID.randomUUID().toString());
    }

    @Test
    void verify_validProof_returnsTheThumbprintOfTheKey() throws Exception {
        String thumbprint = Jwks.builder().key(keyPair.getPublic()).build().thumbprint().toString();

        DpopProofVerifier.Proof proof = verifier.verify(validProof(), "POST", URL);

        assertThat(proof.thumbprint()).isEqualTo(thumbprint);
    }

    @Test
    void verify_proofsFromTwoKeys_haveDifferentThumbprints() throws Exception {
        String mine = verifier.verify(validProof(), "POST", URL).thumbprint();
        KeyPair other = Jwts.SIG.ES256.keyPair().build();
        String theirs = verifier.verify(
                proof(other, "POST", URL, Instant.now().getEpochSecond(), UUID.randomUUID().toString()), "POST", URL)
                .thumbprint();

        assertThat(mine).isNotEqualTo(theirs);
    }

    @Test
    void verify_sameProofTwice_isRefused() throws Exception {
        String proof = validProof();
        verifier.verify(proof, "POST", URL);

        assertThatThrownBy(() -> verifier.verify(proof, "POST", URL)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verify_wrongMethod_isRefused() {
        assertThatThrownBy(() -> verifier.verify(validProof(), "GET", URL)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verify_wrongUrl_isRefused() {
        assertThatThrownBy(() -> verifier.verify(validProof(), "POST", "http://localhost:8080/auth/logout"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verify_tooOldProof_isRefused() {
        String old = proof(keyPair, "POST", URL, Instant.now().getEpochSecond() - 120, UUID.randomUUID().toString());

        assertThatThrownBy(() -> verifier.verify(old, "POST", URL)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verify_proofFromTheFuture_isRefused() {
        String future = proof(keyPair, "POST", URL, Instant.now().getEpochSecond() + 120, UUID.randomUUID().toString());

        assertThatThrownBy(() -> verifier.verify(future, "POST", URL)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verify_tamperedSignature_isRefused() {
        String proof = validProof();
        String tampered = proof.substring(0, proof.length() - 4) + "AAAA";

        assertThatThrownBy(() -> verifier.verify(tampered, "POST", URL)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verify_symmetricAlgorithm_isRefused() {
        SecretKey secret = Jwts.SIG.HS256.key().build();
        String hmac = Jwts.builder()
                .header().type("dpop+jwt").and()
                .id(UUID.randomUUID().toString())
                .claim("htm", "POST")
                .claim("htu", URL)
                .claim("iat", Instant.now().getEpochSecond())
                .signWith(secret)
                .compact();

        assertThatThrownBy(() -> verifier.verify(hmac, "POST", URL)).isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void verify_missingOrBlankProof_isRefused() {
        assertThatThrownBy(() -> verifier.verify(null, "POST", URL)).isInstanceOf(HttpException.class);
        assertThatThrownBy(() -> verifier.verify(" ", "POST", URL)).isInstanceOf(HttpException.class);
    }
}
