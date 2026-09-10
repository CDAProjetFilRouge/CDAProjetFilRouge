package fr.diginamic.hubevenementiel.entities;

import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "event")
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    @Column(name = "description", nullable = false, length = 65535)
    private String description;
    @Column(name = "location", nullable = false)
    private String location;
    @Column(name = "category", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private Category category;
    @Column(name = "start_date_time", nullable = false)
    private LocalDateTime startDateTime;
    @Column(name = "end_date_time", nullable = false)
    private LocalDateTime endDateTime;
    @Column(name = "affiliate_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal affiliatePrice;
    @Column(name = "non_affiliate_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal nonAffiliatePrice;
    @Column(name = "max_capacity", nullable = false, length = 10)
    private int maxCapacity;

    @OneToMany(mappedBy = "event")
    private List<Image> imageGallery = new ArrayList<>();

    @Column(name = "status", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private EventStatus status;

    //@ManyToOne
    //@JoinColumn(name = "organizer_id", nullable = false)
    //private User organizer;



    public Event() {
    }

    public Event(Long id, String title, String description, String location, Category category, LocalDateTime startDateTime, LocalDateTime endDateTime, BigDecimal affiliatePrice, BigDecimal nonAffiliatePrice, int maxCapacity, List<Image> imageGallery, EventStatus status) {
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
        this.imageGallery = imageGallery;
        this.status = status;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
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

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public List<Image> getImageGallery() {
        return imageGallery;
    }

    public void setImageGallery(List<Image> imageGallery) {
        this.imageGallery = imageGallery;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }


}
