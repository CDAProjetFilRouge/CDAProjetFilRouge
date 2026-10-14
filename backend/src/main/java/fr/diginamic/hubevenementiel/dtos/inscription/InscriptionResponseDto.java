package fr.diginamic.hubevenementiel.dtos.inscription;

import java.util.UUID;
import fr.diginamic.hubevenementiel.dtos.event.EventSummaryResponseDto;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InscriptionResponseDto {

    private UUID id;
    private EventSummaryResponseDto event;
    private LocalDateTime inscriptionDate;
    private InscriptionStatus status;
    private BigDecimal price;
    private LocalDateTime cancellationDate;
    private String cancelObject;
    private Integer waitingListPosition;

    public InscriptionResponseDto() {
    }

    public InscriptionResponseDto(UUID id, EventSummaryResponseDto event, LocalDateTime inscriptionDate, InscriptionStatus status, BigDecimal price, LocalDateTime cancellationDate, String cancelObject) {
        this.id = id;
        this.event = event;
        this.inscriptionDate = inscriptionDate;
        this.status = status;
        this.price = price;
        this.cancellationDate = cancellationDate;
        this.cancelObject = cancelObject;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public EventSummaryResponseDto getEvent() {
        return event;
    }

    public void setEvent(EventSummaryResponseDto event) {
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

    /** Rang dans la liste d'attente (1 = prochain promu), null si l'inscription n'est pas en liste d'attente. */
    public Integer getWaitingListPosition() {
        return waitingListPosition;
    }

    public void setWaitingListPosition(Integer waitingListPosition) {
        this.waitingListPosition = waitingListPosition;
    }
}
