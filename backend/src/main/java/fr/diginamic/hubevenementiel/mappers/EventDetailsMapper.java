package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.event.EventDetailsReponseDto;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Image;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.stream.Collectors;

public class EventDetailsMapper {

    @Autowired
    private ImageGaleryMapper imageGaleryMapper;

    public EventDetailsReponseDto toDto (Event event, int remainingSpots) {
        if (event == null){
            return null;
        }
        EventDetailsReponseDto dto = new EventDetailsReponseDto();
        dto.setId(event.getId());
        dto.setTitle(event.getTitle());
        dto.setDescription(event.getDescription());
        dto.setStreet1(event.getLocation().getStreet1());
        dto.setStreet2(event.getLocation().getStreet2());
        dto.setCity(event.getLocation().getCity());
        dto.setPostalCode(event.getLocation().getPostalCode());
        dto.setCountry(event.getLocation().getCountry());
        dto.setCategory(event.getCategory());
        dto.setStartDate(event.getStartDateTime());
        dto.setEndDate(event.getEndDateTime());
        dto.setAffiliatePrice(event.getAffiliatePrice());
        dto.setNonAffiliatePrice(event.getNonAffiliatePrice());
        dto.setMaxCapacity(event.getMaxCapacity());
        dto.setRemainingSpots(remainingSpots);
        dto.setImageGallery(event.getImageGallery().stream().map(imageGaleryMapper::toDto).collect(Collectors.toList()));
        dto.setStatus(event.getStatus());
        dto.setClubName(event.getClub().getName());
        dto.setOrganizerFirstName(event.getOrganizer().getFirstName());
        dto.setOrganizerLastname(event.getOrganizer().getLastName());

        return dto;
    }
}
