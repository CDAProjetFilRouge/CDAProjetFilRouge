package fr.diginamic.hubevenementiel.entities;

import fr.diginamic.hubevenementiel.enums.TokenType;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "token")
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "value", nullable = false, length = 64)
    private String value;
    @Column(name = "creation_date_time", nullable = false)
    private LocalDateTime creationDateTime;
    @Column(name = "expiration_date_time", nullable = false)
    private LocalDateTime expirationDateTime;
    @Column(name = "use_date", nullable = false)
    private LocalDateTime useDate;
    @Column(name = "pending_data", nullable = false)
    private String pendingData;
    @Column(name = "type", nullable = false, length = 30)
    @Enumerated(EnumType.STRING)
    private TokenType tokenType;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private AppUser user;


    public Token() {
    }


    public Token(Long id, String value, LocalDateTime creationDateTime, LocalDateTime expirationDateTime, LocalDateTime useDate, String pendingData) {
        this.id = id;
        this.value = value;
        this.creationDateTime = creationDateTime;
        this.expirationDateTime = expirationDateTime;
        this.useDate = useDate;
        this.pendingData = pendingData;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public LocalDateTime getCreationDateTime() {
        return creationDateTime;
    }

    public void setCreationDateTime(LocalDateTime creationDateTime) {
        this.creationDateTime = creationDateTime;
    }

    public LocalDateTime getExpirationDateTime() {
        return expirationDateTime;
    }

    public void setExpirationDateTime(LocalDateTime expirationDateTime) {
        this.expirationDateTime = expirationDateTime;
    }

    public LocalDateTime getUseDate() {
        return useDate;
    }

    public void setUseDate(LocalDateTime useDate) {
        this.useDate = useDate;
    }

    public String getPendingData() {
        return pendingData;
    }

    public void setPendingData(String pendingData) {
        this.pendingData = pendingData;
    }
}
