package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.AnonymizationDemand;
import fr.diginamic.hubevenementiel.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface AnonymizationDemandRepo extends JpaRepository<AnonymizationDemand, Long> {

    /**
     *
     * @param status status of the request (e.g. ACCEPTED)
     * @param pageable
     * @return a list of anonymization demand by status with pagination info
     */
    Page<AnonymizationDemand> findByRequestStatus(RequestStatus status, Pageable pageable);

    /**
     *
     * @param dateMin starting date at which to do the search
     * @param dateMax ending date at which to do the search
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymization demand requests by a date between a min data and max date with pagination info
     */
    List<AnonymizationDemand> findByDemandDateBetween(LocalDateTime dateMin, LocalDateTime dateMax, Pageable pageable);

    /**
     *
     * @param dateMin starting date at which to do the search
     * @param dateMax ending date at which to do the search
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymization demand approved by a date between a min data and max date with pagination info
     */
    List<AnonymizationDemand> findByApprovedDateBetween(LocalDateTime dateMin, LocalDateTime dateMax, Pageable pageable);

    /**
     *
     * @param id id of the requester you want to find all the anonymization demand for
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific requester with pagination info
     */
    Page<AnonymizationDemand> findByRequesterId(Long id, Pageable pageable);

    /**
     *
     * @param lastName last name of the requester you want to find all the anonymization demand for
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific requester with pagination info
     */
    Page<AnonymizationDemand> findByRequesterLastName(String lastName, Pageable pageable);

    /**
     *
     * @param firstName first name of the requester you want to find all the anonymization demand for
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific requester with pagination info
     */
    Page<AnonymizationDemand> findByRequesterFirstName(String firstName, Pageable pageable);

    /**
     *
     * @param email email of the requester you want to find all the anonymization demand for
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific requester with pagination info
     */
    Page<AnonymizationDemand> findByRequesterEmail(String email, Pageable pageable);

    /**
     *
     * @param id id of the admin you want to find all the anomization demand for
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific admin with pagination info
     */
    Page<AnonymizationDemand> findByAdminId(Long id, Pageable pageable);

    /**
     *
     * @param lastName last name of the admin you want to find all the anomization demand for
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific admin with pagination info
     */
    Page<AnonymizationDemand> findByAdminLastName(String lastName, Pageable pageable);

    /**
     *
     * @param firstName first name of the admin you want to find all the anomization demand for
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific admin with pagination info
     */
    Page<AnonymizationDemand> findByAdminFirstName(String firstName, Pageable pageable);

    /**
     *
     * @param email email of the admin you want to find all the anomization demand for
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific admin with pagination info
     */
    Page<AnonymizationDemand> findByAdminEmail(String email, Pageable pageable);


}
