package fr.diginamic.hubevenementiel.dtos.event;

import java.util.UUID;
import java.time.LocalDateTime;

import fr.diginamic.hubevenementiel.enums.EventStatus;

public class EventSummaryResponseDto {

    private UUID id;
    private String title;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String clubName;
    private EventStatus status;

    public EventSummaryResponseDto() {
    }

    public EventSummaryResponseDto(UUID id, String title, LocalDateTime startDateTime, LocalDateTime endDateTime) {
        this.id = id;
        this.title = title;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
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

}
