package fr.diginamic.hubevenementiel.entities;

import fr.diginamic.hubevenementiel.enums.RequestStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "ANONYMIZATION_DEMAND")
public class AnonymizationDemand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "request_status")
    @Enumerated(EnumType.STRING)
    private RequestStatus requestStatus;

    @Column(name = "demand_date")
    private LocalDateTime demandDate;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    @ManyToOne
    @JoinColumn(name = "requester_id")
    @Column(name = "requester")
    private User requester;

    @ManyToOne
    @JoinColumn(name = "administrator_id", nullable = true)
    private User admin;

    public AnonymizationDemand(){};

    public AnonymizationDemand(User admin, User requester, LocalDateTime approvedDate, LocalDateTime demandDate, RequestStatus requestStatus, Long id) {
        this.admin = admin;
        this.requester = requester;
        this.approvedDate = approvedDate;
        this.demandDate = demandDate;
        this.requestStatus = requestStatus;
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getAdmin() {
        return admin;
    }

    public void setAdmin(User admin) {
        this.admin = admin;
    }

    public User getRequester() {
        return requester;
    }

    public void setRequester(User requester) {
        this.requester = requester;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDateTime approvedDate) {
        this.approvedDate = approvedDate;
    }

    public LocalDateTime getDemandDate() {
        return demandDate;
    }

    public void setDemandDate(LocalDateTime demandDate) {
        this.demandDate = demandDate;
    }

    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }
}
