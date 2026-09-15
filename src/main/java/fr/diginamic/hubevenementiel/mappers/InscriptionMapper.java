package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.inscription.InscriptionEventResponseDto;
import fr.diginamic.hubevenementiel.dtos.inscription.InscriptionResponseDto;
import fr.diginamic.hubevenementiel.entities.Inscription;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class InscriptionMapper {

    @Autowired
    private EventSummaryMapper eventSummaryMapper;
    @Autowired
    private AppUserSummaryMapper appUserSummaryMapper;

    public InscriptionResponseDto toDto(Inscription inscription) {
        if (inscription == null) {
            return null;
        }

        InscriptionResponseDto dto = new InscriptionResponseDto();
        dto.setId(inscription.getId());
        dto.setEvent(eventSummaryMapper.toDto(inscription.getEvent()));
        dto.setInscriptionDate(inscription.getInscriptionDate());
        dto.setStatus(inscription.getStatus());
        dto.setPrice(inscription.getPrice());
        dto.setCancellationDate(inscription.getCancellationDate());
        dto.setCancelObject(inscription.getCancelObject());

        return dto;
    }

    public InscriptionEventResponseDto toEventDto(Inscription inscription) {
        if (inscription == null) {
            return null;
        }

        InscriptionEventResponseDto dto = new InscriptionEventResponseDto();
        dto.setId(inscription.getId());
        dto.setUser(appUserSummaryMapper.toDto(inscription.getUser()));
        dto.setInscriptionDate(inscription.getInscriptionDate());
        dto.setStatus(inscription.getStatus());
        dto.setPrice(inscription.getPrice());

        return dto;
    }

}

