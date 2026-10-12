package fr.diginamic.hubevenementiel.services;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.RefreshToken;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.UnauthorizedException;
import fr.diginamic.hubevenementiel.repositories.RefreshTokenRepo;

@Service
public class RefreshTokenService {

    private static final long SLIDING_DAYS = 7;
    private static final long ABSOLUTE_DAYS = 30;
    private static final long GRACE_SECONDS = 10;

    public record Rotation(AppUser user, String refreshToken, String keyThumbprint) {
    }

    private final RefreshTokenRepo refreshTokenRepo;
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepo refreshTokenRepo) {
        this.refreshTokenRepo = refreshTokenRepo;
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional
    public Rotation rotate(String rawToken, String keyThumbprint) throws HttpException {
        LocalDateTime now = LocalDateTime.now();

        RefreshToken current = refreshTokenRepo.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new UnauthorizedException("Session invalide."));

        if (current.getRevokedAt() != null) {
            throw new UnauthorizedException("Session invalide.");
        }

        if (current.getKeyThumbprint() != null && !current.getKeyThumbprint().equals(keyThumbprint)) {
            refreshTokenRepo.revokeFamily(current.getFamilyId(), now);
            throw new UnauthorizedException("Session invalide.");
        }

        if (current.getUsedAt() != null && current.getUsedAt().plusSeconds(GRACE_SECONDS).isBefore(now)) {
            refreshTokenRepo.revokeFamily(current.getFamilyId(), now);
            throw new UnauthorizedException("Session invalide.");
        }

        LocalDateTime absoluteLimit = current.getFamilyCreatedAt().plusDays(ABSOLUTE_DAYS);
        if (current.getExpiresAt().isBefore(now) || absoluteLimit.isBefore(now)) {
            throw new UnauthorizedException("Session expirée.");
        }

        AppUser user = current.getUser();
        if (user.getStatus() != AccountStatus.ACTIVE) {
            refreshTokenRepo.revokeFamily(current.getFamilyId(), now);
            throw new UnauthorizedException("Session invalide.");
        }

        if (current.getUsedAt() == null) {
            current.setUsedAt(now);
        }

        LocalDateTime expiresAt = now.plusDays(SLIDING_DAYS);
        if (expiresAt.isAfter(absoluteLimit)) {
            expiresAt = absoluteLimit;
        }

        String raw = generateRawToken();
        refreshTokenRepo.save(new RefreshToken(hash(raw), user, current.getFamilyId(),
                current.getFamilyCreatedAt(), expiresAt, current.getKeyThumbprint()));

        return new Rotation(user, raw, current.getKeyThumbprint());
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokenRepo.findByTokenHash(hash(rawToken))
                .ifPresent(token -> refreshTokenRepo.revokeFamily(token.getFamilyId(), LocalDateTime.now()));
    }

    @Transactional
    public String issueForLogin(AppUser user, String keyThumbprint) {
        LocalDateTime now = LocalDateTime.now();
        String raw = generateRawToken();
        refreshTokenRepo.save(new RefreshToken(hash(raw), user, UUID.randomUUID().toString(),
                now, now.plusDays(SLIDING_DAYS), keyThumbprint));
        return raw;
    }

}