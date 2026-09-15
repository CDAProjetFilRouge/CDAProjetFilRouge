package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.enums.DocumentType;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.LegalDocumentRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class LegalDocumentService {

    private final LegalDocumentRepo legalDocumentRepo;

    public LegalDocumentService(LegalDocumentRepo legalDocumentRepo) {
        this.legalDocumentRepo = legalDocumentRepo;
    }

    public LegalDocument findLatestByType(DocumentType type) throws HttpException {
        Optional<LegalDocument> document = legalDocumentRepo.findFirstByDocumentTypeOrderByVersionDesc(type);

        if (document.isEmpty()) {
            throw new NotFoundException("Aucun document trouvé pour ce type.");
        }

        return document.get();
    }

    @Transactional
    public LegalDocument createNewVersion(LegalDocument document) throws HttpException {
        legalDocumentChecker(document);

        int previousVersion = legalDocumentRepo.findFirstByDocumentTypeOrderByVersionDesc(document.getDocumentType())
                .map(LegalDocument::getVersion)
                .orElse(0);

        document.setVersion(previousVersion + 1);
        document.setUpdateDate(LocalDateTime.now());

        return legalDocumentRepo.save(document);
    }

    public boolean legalDocumentChecker(LegalDocument document) throws HttpException {

        if (document == null) {
            throw new BadRequestException("Le document ne peut pas être nul.");
        }

        if (document.getDocumentType() == null) {
            throw new BadRequestException("Vous devez choisir un type de document.");
        }

        String content = document.getContent();
        if (content == null || content.isBlank()) {
            throw new BadRequestException("Le contenu du document ne peut pas être vide.");
        }

        return true;
    }
}
