package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.enums.DocumentType;
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

    public LegalDocumentService(LegalDocumentRepo legalDocumentRepo){
        this.legalDocumentRepo = legalDocumentRepo;
    }

    public List<LegalDocument> getAllDocuments (){
        return legalDocumentRepo.findAll();
    }

    public LegalDocument getDocumentById(Long id) throws HttpException {
        if(legalDocumentRepo.findById(id).isEmpty()){
            throw new NotFoundException("No legal document found with id"+ id);
        }

        return legalDocumentRepo.findById(id).get();
    }

    public List<LegalDocument> getDocumentByType(DocumentType type, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if(legalDocumentRepo.findByDocumentType(type, pageable).getContent().isEmpty()){
            throw new NotFoundException("No documents found of type: "+type);
        }

        return legalDocumentRepo.findByDocumentType(type, pageable).getContent();
    }

    public List<LegalDocument> getDocumentByDate(LocalDateTime dateMin, LocalDateTime dateMax, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if(legalDocumentRepo.findByUpdateDateBetween(dateMin, dateMax, pageable).getContent().isEmpty()){
            throw new NotFoundException("No document found with an update date between "+dateMin+" and "+dateMax);
        }

        return legalDocumentRepo.findByUpdateDateBetween(dateMin, dateMax, pageable).getContent();
    }

    public List<LegalDocument> getDocumentUserId(Long id, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if(legalDocumentRepo.findByUserId(id, pageable).getContent().isEmpty()){
            throw new NotFoundException("No legal document found with user id: "+id);
        }

        return legalDocumentRepo.findByUserId(id, pageable).getContent();
    }

    @Transactional
    public void createDocument(LegalDocument legalDocument) {
        legalDocumentRepo.save(legalDocument);
    }

    @Transactional
    public void updateDocument(LegalDocument legalDocument) throws HttpException {
        Optional<LegalDocument> legalDocumentDB = legalDocumentRepo.findById(legalDocument.getId());

        if(legalDocumentDB.isEmpty()){
            throw new NotFoundException("Not legal document found with id: "+legalDocument.getId());
        }

        legalDocumentDB.get().setDocumentType(legalDocument.getDocumentType());
        legalDocumentDB.get().setContent(legalDocument.getContent());
        legalDocumentDB.get().setVersion(legalDocument.getVersion());
        legalDocumentDB.get().setUpdateDate(legalDocument.getUpdateDate());
        legalDocumentDB.get().setPdfPath(legalDocument.getPdfPath());
        legalDocumentDB.get().setUser(legalDocument.getUser());
    }

    @Transactional
    public void deleteDocument(Long id) throws HttpException {
        Optional<LegalDocument> legalDocument = legalDocumentRepo.findById(id);

        if(legalDocument.isEmpty()){
            throw new NotFoundException("No legal document found with id: "+id);
        }

        legalDocumentRepo.delete(legalDocument.get());
    }
}
