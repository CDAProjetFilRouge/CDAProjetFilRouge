package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.event.EventSummaryResponseDto;
import fr.diginamic.hubevenementiel.entities.Event;
import org.springframework.stereotype.Component;

@Component
public class EventSummaryMapper {

    public EventSummaryResponseDto toDto(Event event) {
        if (event == null) {
            return null;
        }

        EventSummaryResponseDto dto = new EventSummaryResponseDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setStartDateTime(event.getStartDateTime());
        dto.setEndDateTime(event.getEndDateTime());
        dto.setClubName(event.getClub() != null ? event.getClub().getName() : null);

        return dto;
    }
}