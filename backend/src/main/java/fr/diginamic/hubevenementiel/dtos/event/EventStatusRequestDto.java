package fr.diginamic.hubevenementiel.dtos.event;

import fr.diginamic.hubevenementiel.enums.EventStatus;

public class EventStatusRequestDto {

    private EventStatus status;

    public EventStatusRequestDto() {
    }

    public EventStatusRequestDto(EventStatus status) {
        this.status = status;
    }

    public EventStatus getStatus() {
        return status;
    }

    public void setStatus(EventStatus status) {
        this.status = status;
    }
}
