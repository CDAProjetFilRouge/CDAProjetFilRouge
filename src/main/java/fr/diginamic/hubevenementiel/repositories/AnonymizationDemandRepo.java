package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.AnonymizationDemand;
import fr.diginamic.hubevenementiel.enums.RequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface AnonymizationDemandRepo extends JpaRepository<AnonymizationDemand, Long> {

    /**
     *
     * @param status   status of the request (e.g. ACCEPTED)
     * @param pageable settings for the pagination, create a pageable object using PageRequest.of()
     * @return a list of anonymization demand by status with pagination info
     */
    Page<AnonymizationDemand> findByRequestStatus(Pageable pageable, RequestStatus status);

    /**
     *
     * @param dateMin starting date at which to do the search
     * @param dateMax maximum date at which to do the search
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymization demand requests by a date between a min data and max date with pagination info
     */
    Page<AnonymizationDemand> findByDemandDateBetween(Pageable pageable, LocalDateTime dateMin, LocalDateTime dateMax);

    /**
     *
     * @param dateMin starting date at which to do the search
     * @param dateMax maximum date at which to do the search
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymization demand approved by a date between a min data and max date with pagination info
     */
    Page<AnonymizationDemand> findByApprovedDateBetween(Pageable pageable, LocalDateTime dateMin,
            LocalDateTime dateMax);

    /**
     *
     * @param id id of the requester you want to find all the anonymization demand associated with
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific requester with pagination info
     */
    Page<AnonymizationDemand> findByRequesterId(Pageable pageable, Long id);

    /**
     *
     * @param lastName last name of the requester you want to find all the anonymization demand associated with
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific requester with pagination info
     */
    Page<AnonymizationDemand> findByRequesterLastName(Pageable pageable, String lastName);

    /**
     *
     * @param firstName first name of the requester you want to find all the anonymization demand associated with
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific requester with pagination info
     */
    Page<AnonymizationDemand> findByRequesterFirstName(Pageable pageable, String firstName);

    /**
     *
     * @param email email of the requester you want to find all the anonymization demand associated with
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific requester with pagination info
     */
    Page<AnonymizationDemand> findByRequesterEmail(Pageable pageable, String email);

    /**
     *
     * @param id id of the admin you want to find all the anomization demand associated with
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific admin with pagination info
     */
    Page<AnonymizationDemand> findByAdminId(Pageable pageable, Long id);

    /**
     *
     * @param lastName last name of the admin you want to find all the anomization demand associated with
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific admin with pagination info
     */
    Page<AnonymizationDemand> findByAdminLastName(Pageable pageable, String lastName);

    /**
     *
     * @param firstName first name of the admin you want to find all the anomization demand associated with
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific admin with pagination info
     */
    Page<AnonymizationDemand> findByAdminFirstName(Pageable pageable, String firstName);

    /**
     *
     * @param email email of the admin you want to find all the anomization demand associated with
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of anonymmizationDemand from a specific admin with pagination info
     */
    Page<AnonymizationDemand> findByAdminEmail(Pageable pageable, String email);

}
