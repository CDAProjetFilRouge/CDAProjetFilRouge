package fr.diginamic.hubevenementiel.dtos.inscription;

import fr.diginamic.hubevenementiel.dtos.appUser.AppUserSummaryResponseDto;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class InscriptionEventResponseDto {

    private Long id;
    private AppUserSummaryResponseDto user;
    private LocalDateTime inscriptionDate;
    private InscriptionStatus status;
    private BigDecimal price;

    public InscriptionEventResponseDto() {
    }

    public InscriptionEventResponseDto(Long id, AppUserSummaryResponseDto user, LocalDateTime inscriptionDate, InscriptionStatus status, BigDecimal price) {
        this.id = id;
        this.user = user;
        this.inscriptionDate = inscriptionDate;
        this.status = status;
        this.price = price;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppUserSummaryResponseDto getUser() {
        return user;
    }

    public void setUser(AppUserSummaryResponseDto user) {
        this.user = user;
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
}
