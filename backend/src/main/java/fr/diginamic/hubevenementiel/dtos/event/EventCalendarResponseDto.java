package fr.diginamic.hubevenementiel.dtos.event;

import java.time.LocalDateTime;
import java.util.UUID;

import fr.diginamic.hubevenementiel.enums.EventStatus;

public class EventCalendarResponseDto {

    private UUID id;
    private String title;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private EventStatus status;
    private Integer maxCapacity;
    private long confirmedCount;
    private String clubName;

    public EventCalendarResponseDto() {
    }

    public EventCalendarResponseDto(UUID id, String title, LocalDateTime startDateTime, LocalDateTime endDateTime,
            EventStatus status, Integer maxCapacity, long confirmedCount, String clubName) {
        this.id = id;
        this.title = title;
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.status = status;
        this.maxCapacity = maxCapacity;
        this.confirmedCount = confirmedCount;
        this.clubName = clubName;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public long getConfirmedCount() {
        return confirmedCount;
    }

    public void setConfirmedCount(long confirmedCount) {
        this.confirmedCount = confirmedCount;
    }

    public String getClubName() {
        return clubName;
    }

    public void setClubName(String clubName) {
        this.clubName = clubName;
    }
}
