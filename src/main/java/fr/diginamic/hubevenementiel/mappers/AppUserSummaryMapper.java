package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.appUser.AppUserSummaryResponseDto;
import fr.diginamic.hubevenementiel.entities.AppUser;
import org.springframework.stereotype.Component;

@Component
public class AppUserSummaryMapper {

    public AppUserSummaryResponseDto toDto(AppUser user) {
        if (user == null) {
            return null;
        }

        AppUserSummaryResponseDto dto = new AppUserSummaryResponseDto();
        dto.setId(user.getId());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEmail(user.getEmail());

        return dto;
    }
}
