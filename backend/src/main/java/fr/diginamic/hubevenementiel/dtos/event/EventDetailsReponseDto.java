package fr.diginamic.hubevenementiel.dtos.event;

import fr.diginamic.hubevenementiel.dtos.image.ImageSummaryResponseDto;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class EventDetailsReponseDto {

    //region Properties
    private long id;
    private String title;
    private String description;
    private String street1;
    private String street2;
    private String city;
    private String postalCode;
    private String country;
    private Category category;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private BigDecimal affiliatePrice;
    private BigDecimal nonAffiliatePrice;
    private Integer maxCapacity;
    private Integer remainingSpots;
    private List<ImageSummaryResponseDto> imageGallery = new ArrayList<>();
    private EventStatus status;
    private String clubName;
    private String organizerFirstName;
    private String organizerLastname;
    //endregion

    //region Constructor
    public EventDetailsReponseDto(){};

    public EventDetailsReponseDto(long id, String title, String description, String street1, String street2,
                                  String city, String postalCode, String country, Category category, LocalDateTime startDate,
                                  LocalDateTime endDate, BigDecimal affiliatePrice, BigDecimal nonAffiliatePrice, Integer maxCapacity,
                                  Integer remainingSpots, List<ImageSummaryResponseDto> imageGallery, EventStatus status, String clubName,
                                  String organizerFirstName, String organizerLastname) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.street1 = street1;
        this.street2 = street2;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
        this.category = category;
        this.startDate = startDate;
        this.endDate = endDate;
        this.affiliatePrice = affiliatePrice;
        this.nonAffiliatePrice = nonAffiliatePrice;
        this.maxCapacity = maxCapacity;
        this.remainingSpots = remainingSpots;
        this.imageGallery = imageGallery;
        this.status = status;
        this.clubName = clubName;
        this.organizerFirstName = organizerFirstName;
        this.organizerLastname = organizerLastname;
    }
    //endregion

    //region Getters and Setters

    public long getId() {
        return id;
    }

    public void setId(long id) {
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

    public String getStreet1() {
        return street1;
    }

    public void setStreet1(String street1) {
        this.street1 = street1;
    }

    public String getStreet2() {
        return street2;
    }

    public void setStreet2(String street2) {
        this.street2 = street2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public LocalDateTime getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDateTime startDate) {
        this.startDate = startDate;
    }

    public LocalDateTime getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDateTime endDate) {
        this.endDate = endDate;
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

    public List<ImageSummaryResponseDto> getImageGallery() {
        return imageGallery;
    }

    public void setImageGallery(List<ImageSummaryResponseDto> imageGallery) {
        this.imageGallery = imageGallery;
    }

    public Integer getRemainingSpots() {
        return remainingSpots;
    }

    public void setRemainingSpots(Integer remainingSpots) {
        this.remainingSpots = remainingSpots;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public String getOrganizerFirstName() {
        return organizerFirstName;
    }

    public void setOrganizerFirstName(String organizerFirstName) {
        this.organizerFirstName = organizerFirstName;
    }

    public String getOrganizerLastname() {
        return organizerLastname;
    }

    public void setOrganizerLastname(String organizerLastname) {
        this.organizerLastname = organizerLastname;
    }
    //endregion
}
