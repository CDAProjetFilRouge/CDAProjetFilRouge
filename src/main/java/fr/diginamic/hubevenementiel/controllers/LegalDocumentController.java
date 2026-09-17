package fr.diginamic.hubevenementiel.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.diginamic.hubevenementiel.dtos.legalDocument.LegalDocumentRequestDto;
import fr.diginamic.hubevenementiel.dtos.legalDocument.LegalDocumentResponseDto;
import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.enums.DocumentType;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.mappers.LegalDocumentMapper;
import fr.diginamic.hubevenementiel.services.LegalDocumentService;

@RestController
@RequestMapping("/legal-documents")
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;
    private final LegalDocumentMapper legalDocumentMapper;

    public LegalDocumentController(LegalDocumentService legalDocumentService, LegalDocumentMapper legalDocumentMapper) {
        this.legalDocumentService = legalDocumentService;
        this.legalDocumentMapper = legalDocumentMapper;
    }

    @GetMapping("/{type}")
    public LegalDocumentResponseDto getLatest(@PathVariable DocumentType type) throws HttpException {
        return legalDocumentMapper.toDto(legalDocumentService.findLatestByType(type));
    }

    @Secured("ROLE_ADMINISTRATOR")
    @PostMapping
    public ResponseEntity<LegalDocumentResponseDto> createNewVersion(@RequestBody LegalDocumentRequestDto requestDto)
            throws HttpException {
        LegalDocument document = legalDocumentMapper.toEntity(requestDto);
        LegalDocument created = legalDocumentService.createNewVersion(document);
        return ResponseEntity.status(HttpStatus.CREATED).body(legalDocumentMapper.toDto(created));
    }
}
