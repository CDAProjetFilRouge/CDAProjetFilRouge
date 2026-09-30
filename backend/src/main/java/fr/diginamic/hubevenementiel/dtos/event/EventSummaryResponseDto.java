package fr.diginamic.hubevenementiel.dtos.event;

import java.time.LocalDateTime;

public class EventSummaryResponseDto {

    private Long id;
    private String title;
    private LocalDateTime startDateTime;

    public EventSummaryResponseDto() {
    }

    public EventSummaryResponseDto(Long id, String title, LocalDateTime startDateTime) {
        this.id = id;
        this.title = title;
        this.startDateTime = startDateTime;
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

    public LocalDateTime getStartDateTime() {
        return startDateTime;
    }

    public void setStartDateTime(LocalDateTime startDateTime) {
        this.startDateTime = startDateTime;
    }

}
