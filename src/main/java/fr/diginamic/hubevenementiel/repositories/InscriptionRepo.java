package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface InscriptionRepo extends JpaRepository<Inscription, Long> {

    Page<Inscription> findByUserId(Long id, Pageable pageable);

    Page<Inscription> findByEventId(Long id, Pageable pageable);

    Page<Inscription> findByInscriptionDateBetween(LocalDateTime dateMin, LocalDateTime dateMax);

    Page<Inscription> findByStatus(InscriptionStatus status, Pageable pageable);

    Page<Inscription> findByCancellationDate(LocalDateTime dateMin, LocalDateTime dateMax);

    Page<Inscription> findByCanceledById(Long id, Pageable pageable);
}
