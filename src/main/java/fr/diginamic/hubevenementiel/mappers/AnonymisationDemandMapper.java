package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.anonymizerDemand.AnonymizationDemandResponseDto;
import fr.diginamic.hubevenementiel.entities.AnonymizationDemand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AnonymisationDemandMapper {

    @Autowired
    private AppUserSummaryMapper appUserSummaryMapper;

    public AnonymizationDemandResponseDto toDto(AnonymizationDemand demand) {
        if (demand == null) {
            return null;
        }
        AnonymizationDemandResponseDto dto = new AnonymizationDemandResponseDto();
        dto.setId(demand.getId());
        dto.setRequestStatus(demand.getRequestStatus());
        dto.setDemandDate(demand.getDemandDate());
        dto.setApprovedDate(demand.getApprovedDate());
        dto.setRequester(appUserSummaryMapper.toDto(demand.getRequester()));
        dto.setAdmin(appUserSummaryMapper.toDto(demand.getAdmin()));

        return dto;
    }
}
