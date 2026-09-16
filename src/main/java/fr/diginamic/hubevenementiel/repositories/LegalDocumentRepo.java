package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface LegalDocumentRepo extends JpaRepository<LegalDocument, Long> {

    /**
     *
     * @param type type of the document you want to find
     * @param pageable status of the inscription you want to find
     * @return a list of document with pagination
     */
    Page<LegalDocument> findByDocumentType(DocumentType type, Pageable pageable);

    /**
     *
     * @param dateMin starting date you want to find documents for
     * @param dateMax maximum date you want to find documents for
     * @param pageable status of the inscription you want to find
     * @return a list of document with pagination
     */
    Page<LegalDocument> findByUpdateDateBetween(LocalDateTime dateMin, LocalDateTime dateMax, Pageable pageable);

    /**
     *
     * @param id id of the user you want to find documents associated with
     * @param pageable status of the inscription you want to find
     * @return a list of document with pagination
     */
    Page<LegalDocument> findByUserId(Long id, Pageable pageable);

    /**
     *
     * @param type type of document you want to find
     * @return an optional of type LegalDocument
     */
    Optional<LegalDocument> findFirstByDocumentTypeOrderByVersionDesc(DocumentType type);
}
