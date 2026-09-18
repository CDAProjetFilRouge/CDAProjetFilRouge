package fr.diginamic.hubevenementiel.dtos.anonymizerDemand;

import fr.diginamic.hubevenementiel.enums.RequestStatus;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserSummaryResponseDto;


import java.time.LocalDateTime;

public class AnonymizationDemandResponseDto {

    private Long id;
    private RequestStatus requestStatus;
    private LocalDateTime demandDate;
    private LocalDateTime approvedDate;
    private AppUserSummaryResponseDto requester;
    private AppUserSummaryResponseDto admin;

    public AnonymizationDemandResponseDto() {
    }

    public AnonymizationDemandResponseDto(Long id, RequestStatus requestStatus, LocalDateTime demandDate, LocalDateTime approvedDate, AppUserSummaryResponseDto requester, AppUserSummaryResponseDto admin) {
        this.id = id;
        this.requestStatus = requestStatus;
        this.demandDate = demandDate;
        this.approvedDate = approvedDate;
        this.requester = requester;
        this.admin = admin;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public RequestStatus getRequestStatus() {
        return requestStatus;
    }

    public void setRequestStatus(RequestStatus requestStatus) {
        this.requestStatus = requestStatus;
    }

    public LocalDateTime getDemandDate() {
        return demandDate;
    }

    public void setDemandDate(LocalDateTime demandDate) {
        this.demandDate = demandDate;
    }

    public LocalDateTime getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDateTime approvedDate) {
        this.approvedDate = approvedDate;
    }

    public AppUserSummaryResponseDto getRequester() {
        return requester;
    }

    public void setRequester(AppUserSummaryResponseDto requester) {
        this.requester = requester;
    }

    public AppUserSummaryResponseDto getAdmin() {
        return admin;
    }

    public void setAdmin(AppUserSummaryResponseDto admin) {
        this.admin = admin;
    }
}
