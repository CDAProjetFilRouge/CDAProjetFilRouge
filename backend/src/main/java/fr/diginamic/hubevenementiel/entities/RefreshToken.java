package fr.diginamic.hubevenementiel.entities;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "refresh_token", indexes = @Index(name = "idx_refresh_token_family", columnList = "family_id"))
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "key_thumbprint", length = 64)
    private String keyThumbprint;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;
    @Column(name = "family_created_at", nullable = false)
    private LocalDateTime familyCreatedAt;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "used_at", nullable = true)
    private LocalDateTime usedAt;
    @Column(name = "revoked_at", nullable = true)
    private LocalDateTime revokedAt;

    public RefreshToken() {

    }

    public RefreshToken(String tokenHash, AppUser user, String familyId, LocalDateTime familyCreatedAt,
            LocalDateTime expiresAt, String keyThumbprint) {
        this.tokenHash = tokenHash;
        this.user = user;
        this.familyId = familyId;
        this.familyCreatedAt = familyCreatedAt;
        this.expiresAt = expiresAt;
        this.keyThumbprint = keyThumbprint;
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public AppUser getUser() {
        return user;
    }

    public String getFamilyId() {
        return familyId;
    }

    public LocalDateTime getFamilyCreatedAt() {
        return familyCreatedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public LocalDateTime getUsedAt() {
        return usedAt;
    }

    public void setUsedAt(LocalDateTime usedAt) {
        this.usedAt = usedAt;
    }

    public LocalDateTime getRevokedAt() {
        return revokedAt;
    }

    public void setRevokedAt(LocalDateTime revokedAt) {
        this.revokedAt = revokedAt;
    }

    public String getKeyThumbprint() {
        return keyThumbprint;
    }

}
