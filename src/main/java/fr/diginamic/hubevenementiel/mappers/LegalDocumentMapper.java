package fr.diginamic.hubevenementiel.mappers;

import fr.diginamic.hubevenementiel.dtos.legalDocument.LegalDocumentRequestDto;
import fr.diginamic.hubevenementiel.dtos.legalDocument.LegalDocumentResponseDto;
import fr.diginamic.hubevenementiel.entities.LegalDocument;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LegalDocumentMapper {

    @Autowired
    private AppUserSummaryMapper appUserSummaryMapper;

    public LegalDocumentResponseDto toDto(LegalDocument legalDocument) {
        if (legalDocument == null) {
            return null;
        }

        LegalDocumentResponseDto dto = new LegalDocumentResponseDto();
        dto.setId(legalDocument.getId());
        dto.setDocumentType(legalDocument.getDocumentType());
        dto.setContent(legalDocument.getContent());
        dto.setVersion(legalDocument.getVersion());
        dto.setUpdateDate(legalDocument.getUpdateDate());
        dto.setPdfPath(legalDocument.getPdfPath());
        dto.setUser(appUserSummaryMapper.toDto(legalDocument.getUser()));

        return dto;
    }

    public LegalDocument toEntity(LegalDocumentRequestDto dto) {
        LegalDocument entity = new LegalDocument();
        entity.setDocumentType(dto.getDocumentType());
        entity.setContent(dto.getContent());

        return entity;
    }
}
