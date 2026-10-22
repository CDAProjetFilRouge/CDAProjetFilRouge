package fr.diginamic.hubevenementiel.dtos.event;

import java.util.UUID;
import fr.diginamic.hubevenementiel.dtos.address.AddressRequestDto;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class EventRequestDto {

    private String title;
    private String description;
    private AddressRequestDto location;
    private Category category;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private BigDecimal affiliatePrice;
    private BigDecimal nonAffiliatePrice;
    private Integer maxCapacity;
    private Boolean commentsClosedAfterEnd;
    private EventStatus status;
    private UUID idClub;

    public EventRequestDto() {
    }

    public EventRequestDto(String title, String description, AddressRequestDto location, Category category, LocalDateTime startDateTime,
                           LocalDateTime endDateTime, BigDecimal affiliatePrice, BigDecimal nonAffiliatePrice,
                           Integer maxCapacity, EventStatus status, UUID idClub) {
        this.title = title;
        this.description = description;
        this.location = location;
        this.category = category;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.affiliatePrice = affiliatePrice;
        this.nonAffiliatePrice = nonAffiliatePrice;
        this.maxCapacity = maxCapacity;
        this.status = status;
        this.idClub = idClub;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AddressRequestDto getLocation() {
        return location;
    }

    public void setLocation(AddressRequestDto location) {
        this.location = location;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

    public LocalDateTime getEndDateTime() {
        return endDateTime;
    }

    public void setEndDateTime(LocalDateTime endDateTime) {
        this.endDateTime = endDateTime;
    }

    public BigDecimal getAffiliatePrice() {
        return affiliatePrice;
    }

    public void setAffiliatePrice(BigDecimal affiliatePrice) {
        this.affiliatePrice = affiliatePrice;
    }

    public BigDecimal getNonAffiliatePrice() {
        return nonAffiliatePrice;
    }

    public void setNonAffiliatePrice(BigDecimal nonAffiliatePrice) {
        this.nonAffiliatePrice = nonAffiliatePrice;
    }

    public Boolean getCommentsClosedAfterEnd() {
        return commentsClosedAfterEnd;
    }

    public void setCommentsClosedAfterEnd(Boolean commentsClosedAfterEnd) {
        this.commentsClosedAfterEnd = commentsClosedAfterEnd;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public UUID getIdClub() { return idClub; }

    public void setIdClub(UUID idClub) { this.idClub = idClub; }
}
