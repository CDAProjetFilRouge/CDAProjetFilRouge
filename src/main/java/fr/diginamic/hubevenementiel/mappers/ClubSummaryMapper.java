package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.club.ClubSummaryResponseDto;
import fr.diginamic.hubevenementiel.entities.Club;
import org.springframework.stereotype.Component;

@Component
public class ClubSummaryMapper {

    public ClubSummaryResponseDto toDto(Club club) {
        if (club == null) {
            return null;
        }

        ClubSummaryResponseDto dto = new ClubSummaryResponseDto();
        dto.setId(club.getId());
        dto.setName(club.getName());
        dto.setCategory(club.getCategory());

        return dto;
    }
}
