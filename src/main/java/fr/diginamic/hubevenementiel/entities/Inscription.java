package fr.diginamic.hubevenementiel.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

// canceledById reste une simple cle etrangere (pas de @ManyToOne) : il n'y a
// pas d'identite de l'annulateur portee par les methodes cancelPerMember() /
// cancelPerOrganizer(String motif) du diagramme.
@Entity
public class Inscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false)
    private LocalDateTime inscriptionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InscriptionStatus status;

    @Column(nullable = false)
    private BigDecimal price;

    private LocalDateTime cancellationDate;

    private String cancelObject;

    private Long canceledById;

    public Inscription() {
    }

    public Inscription(User user, Event event, BigDecimal price, InscriptionStatus status) {
        this.user = user;
        this.event = event;
        this.price = price;
        this.status = status;
        this.inscriptionDate = LocalDateTime.now();
    }

    public void confirm() {
        this.status = InscriptionStatus.CONFIRMED;
    }

    public void cancelPerMember() {
        cancel(null, null);
    }

    public void cancelPerOrganizer(String motif) {
        cancel(motif, null);
    }

    private void cancel(String motif, Long canceledById) {
        this.status = InscriptionStatus.CANCELED;
        this.cancellationDate = LocalDateTime.now();
        this.cancelObject = motif;
        this.canceledById = canceledById;
    }

    public boolean isWaiting() {
        return this.status == InscriptionStatus.WAITING_LIST;
    }

    public boolean isActive() {
        return this.status == InscriptionStatus.CONFIRMED || this.status == InscriptionStatus.WAITING_LIST;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    public LocalDateTime getInscriptionDate() {
        return inscriptionDate;
    }

    public void setInscriptionDate(LocalDateTime inscriptionDate) {
        this.inscriptionDate = inscriptionDate;
    }

    public InscriptionStatus getStatus() {
        return status;
    }

    public void setStatus(InscriptionStatus status) {
        this.status = status;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getCancellationDate() {
        return cancellationDate;
    }

    public void setCancellationDate(LocalDateTime cancellationDate) {
        this.cancellationDate = cancellationDate;
    }

    public String getCancelObject() {
        return cancelObject;
    }

    public void setCancelObject(String cancelObject) {
        this.cancelObject = cancelObject;
    }

    public Long getCanceledById() {
        return canceledById;
    }

    public void setCanceledById(Long canceledById) {
        this.canceledById = canceledById;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Inscription other)) {
            return false;
        }
        return id != null && id.equals(other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
