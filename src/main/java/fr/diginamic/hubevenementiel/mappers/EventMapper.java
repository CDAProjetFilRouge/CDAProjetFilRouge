package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.event.EventRequestDto;
import fr.diginamic.hubevenementiel.dtos.event.EventResponseDto;
import fr.diginamic.hubevenementiel.entities.Event;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class EventMapper {

    @Autowired
    private AppUserSummaryMapper appUserSummaryMapper;
    @Autowired
    private ImageGaleryMapper imageGaleryMapper;
    @Autowired
    private AddressMapper addressMapper;

    public EventResponseDto toDto(Event event) {
        if (event == null) {
            return null;
        }

        EventResponseDto dto = new EventResponseDto();
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
        dto.setImageGallery(event.getImageGallery().stream().map(imageGaleryMapper::toDto).collect(Collectors.toList()));
        dto.setStatus(event.getStatus());
        dto.setOrganizer(appUserSummaryMapper.toDto(event.getOrganizer()));

        return dto;
    }

    public Event toEntity(EventRequestDto dto) {
        Event entity = new Event();
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        if (dto.getLocation() != null) {
            entity.setLocation(addressMapper.toEntity(dto.getLocation()));
        }
        entity.setCategory(dto.getCategory());
        entity.setStartDateTime(dto.getStartDateTime());
        entity.setEndDateTime(dto.getEndDateTime());
        entity.setAffiliatePrice(dto.getAffiliatePrice());
        entity.setNonAffiliatePrice(dto.getNonAffiliatePrice());
        entity.setMaxCapacity(dto.getMaxCapacity());
        entity.setStatus(dto.getStatus());

        return entity;
    }
}
