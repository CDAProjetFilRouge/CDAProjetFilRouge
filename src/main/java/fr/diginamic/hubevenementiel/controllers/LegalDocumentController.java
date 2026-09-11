package fr.diginamic.hubevenementiel.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.enums.DocumentType;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.services.LegalDocumentService;

@RestController
@RequestMapping("/legal-documents")
public class LegalDocumentController {

    private final LegalDocumentService legalDocumentService;

    public LegalDocumentController(LegalDocumentService legalDocumentService) {
        this.legalDocumentService = legalDocumentService;
    }

    @GetMapping("/{type}")
    public LegalDocument getLatest(@PathVariable DocumentType type) throws HttpException {
        return legalDocumentService.findLatestByType(type);
    }

    @PostMapping
    public ResponseEntity<LegalDocument> createNewVersion(@RequestBody LegalDocument document) throws HttpException {
        LegalDocument created = legalDocumentService.createNewVersion(document);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
