package fr.diginamic.hubevenementiel.repositories;
import fr.diginamic.hubevenementiel.entities.LegalDocument;
import fr.diginamic.hubevenementiel.enums.DocumentType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;

import java.time.LocalDateTime;

public interface LegalDocumentRepo extends JpaRepository<LegalDocument, Long> {

    Page<LegalDocument> findByDocumentType(DocumentType type, Pageable pageable);

    Page<LegalDocument> findByUpdateDateBetween(LocalDateTime dateMin, LocalDateTime dateMax, Pageable pageable);

    Page<LegalDocument> findByUserId(Long id, Pageable pageable);
}
