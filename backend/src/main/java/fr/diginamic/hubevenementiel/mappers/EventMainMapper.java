package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.event.EventResponseMainDto;
import fr.diginamic.hubevenementiel.entities.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class EventMainMapper {

    @Autowired
    private ImageGaleryMapper imageGaleryMapper;

    @Autowired
    private AddressMapper addressMapper;


    public EventResponseMainDto toDto(Event event) {
        if (event == null){
            return null;
        }

        EventResponseMainDto dto = new EventResponseMainDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setLocation(addressMapper.toDto(event.getLocation()));
        dto.setCategory(event.getCategory());
        dto.setStartDateTime(event.getStartDateTime());
        dto.setEndDateTime(event.getEndDateTime());
        dto.setAffiliatePrice(event.getAffiliatePrice());
        dto.setNonAffiliatePrice(event.getNonAffiliatePrice());
        dto.setMaxCapacity(event.getMaxCapacity());
        dto.setRemainingSpots(event.getRemainingPlace());
        dto.setImageGallery(event.getImageGallery().stream().map(imageGaleryMapper::toDto).collect(Collectors.toList()));
        dto.setStatus(event.getStatus());
        dto.setOrganizerFirstName(event.getOrganizer().getFirstName());
        dto.setOrganizerLastName(event.getOrganizer().getLastName());

        return dto;
    }
}
