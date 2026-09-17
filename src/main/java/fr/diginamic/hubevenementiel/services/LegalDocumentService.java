package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.enums.DocumentType;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.LegalDocumentRepo;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class LegalDocumentService {

    private final LegalDocumentRepo legalDocumentRepo;

    public LegalDocumentService(LegalDocumentRepo legalDocumentRepo) {
        this.legalDocumentRepo = legalDocumentRepo;
    }

    public List<LegalDocument> getAllDocuments() {
        return legalDocumentRepo.findAll();
    }

    /**
     *
     * @param id id of the document to find
     * @return object of type LegalDocument
     * @throws HttpException
     */
    public LegalDocument getDocumentById(Long id) throws HttpException {
        Optional<LegalDocument> document = legalDocumentRepo.findById(id);

        if (document.isEmpty()) {
            throw new NotFoundException("No legal document found with id " + id);
        }

        return document.get();
    }

    public List<LegalDocument> getDocumentByType(DocumentType type, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<LegalDocument> documents = legalDocumentRepo.findByDocumentType(type, pageable).getContent();

        if (documents.isEmpty()) {
            throw new NotFoundException("No documents found of type: " + type);
        }

        return documents;
    }

    public List<LegalDocument> getDocumentByDate(LocalDateTime dateMin, LocalDateTime dateMax, int page, int size)
            throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<LegalDocument> documents = legalDocumentRepo.findByUpdateDateBetween(dateMin, dateMax, pageable)
                .getContent();

        if (documents.isEmpty()) {
            throw new NotFoundException("No document found with an update date between " + dateMin + " and " + dateMax);
        }

        return documents;
    }

    public List<LegalDocument> getDocumentUserId(Long id, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<LegalDocument> documents = legalDocumentRepo.findByUserId(id, pageable).getContent();

        if (documents.isEmpty()) {
            throw new NotFoundException("No legal document found with user id: " + id);
        }

        return documents;
    }

    /**
     *
     * @param type type to find documents by
     * @return object of type LegalDocument
     * @throws HttpException
     */
    public LegalDocument findLatestByType(DocumentType type) throws HttpException {
        Optional<LegalDocument> document = legalDocumentRepo.findFirstByDocumentTypeOrderByVersionDesc(type);

        if (document.isEmpty()) {
            throw new NotFoundException("Aucun document trouvé pour ce type.");
        }

        return document.get();
    }

    // pas de create/update classique ici : le MLD a une contrainte unique sur
    // (type, version),
    // donc chaque modif doit ajouter une nouvelle ligne, jamais ecraser l'ancienne.
    // createNewVersion
    // gere les deux cas (premiere creation = version 1, sinon version = derniere +
    // 1).

    /**
     *
     * @param document document to save in the DB
     * @return document saved in the DB
     * @throws HttpException
     */
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

    @Transactional
    public void deleteDocument(Long id) throws HttpException {
        LegalDocument document = getDocumentById(id);

        legalDocumentRepo.delete(document);
    }

    /**
     *
     * @param document document to perform the check on
     * @return true if the checks passed else false
     * @throws HttpException
     */
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
