package fr.diginamic.hubevenementiel.dtos.event;

import fr.diginamic.hubevenementiel.dtos.address.AddressResponseDto;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserSummaryResponseDto;
import fr.diginamic.hubevenementiel.dtos.image.ImageSummaryResponseDto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventResponseDto {

    private Long id;
    private String title;
    private String description;
    private AddressResponseDto location;
    private Category category;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private BigDecimal affiliatePrice;
    private BigDecimal nonAffiliatePrice;
    private Integer maxCapacity;
    private Integer remainingSpots;
    private List<ImageSummaryResponseDto> imageGallery = new ArrayList<>();
    private EventStatus status;
    private AppUserSummaryResponseDto organizer;

    public EventResponseDto() {
    }

    public EventResponseDto(Long id, String title, String description, AddressResponseDto location, Category category, LocalDateTime startDateTime, LocalDateTime endDateTime, BigDecimal affiliatePrice, BigDecimal nonAffiliatePrice, Integer maxCapacity, Integer remainingSpots, List<ImageSummaryResponseDto> imageGallery, EventStatus status, AppUserSummaryResponseDto organizer) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.location = location;
        this.category = category;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.affiliatePrice = affiliatePrice;
        this.nonAffiliatePrice = nonAffiliatePrice;
        this.maxCapacity = maxCapacity;
        this.remainingSpots = remainingSpots;
        this.imageGallery = imageGallery;
        this.status = status;
        this.organizer = organizer;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public AddressResponseDto getLocation() {
        return location;
    }

    public void setLocation(AddressResponseDto location) {
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

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Integer getRemainingSpots() {
        return remainingSpots;
    }

    public void setRemainingSpots(Integer remainingSpots) {
        this.remainingSpots = remainingSpots;
    }

    public List<ImageSummaryResponseDto> getImageGallery() {
        return imageGallery;
    }

    public void setImageGallery(List<ImageSummaryResponseDto> imageGallery) {
        this.imageGallery = imageGallery;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public AppUserSummaryResponseDto getOrganizer() {
        return organizer;
    }

    public void setOrganizer(AppUserSummaryResponseDto organizer) {
        this.organizer = organizer;
    }
}

