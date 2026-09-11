package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.AnonymizationDemand;
import fr.diginamic.hubevenementiel.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnonymizationDemandRepo extends JpaRepository<AnonymizationDemand, Long> {

    List<AnonymizationDemand> findByRequestStatus(RequestStatus status);

    List<AnonymizationDemand> findByDemandDateBetween(LocalDateTime min, LocalDateTime max);

    List<AnonymizationDemand> findByApprovedDateBetween(LocalDateTime min, LocalDateTime max);

    Page<AnonymizationDemand> findByRequesterId(Long id, Pageable pageable);

    Page<AnonymizationDemand> findByRequesterLastName(String lastName, Pageable pageable);

    Page<AnonymizationDemand> findByRequesterFirstName(String firstName, Pageable pageable);

    Page<AnonymizationDemand> findByRequesterEmail(String email, Pageable pageable);

    Page<AnonymizationDemand> findByAdminId(Long id, Pageable pageable);

    Page<AnonymizationDemand> findByAdminLastName(String lastName, Pageable pageable);

    Page<AnonymizationDemand> findByAdminFirstName(String firstName, Pageable pageable);

    Page<AnonymizationDemand> findByAdminEmail(String email, Pageable pageable);


}
