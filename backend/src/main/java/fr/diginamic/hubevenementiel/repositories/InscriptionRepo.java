package fr.diginamic.hubevenementiel.repositories;

import java.util.Collection;
import java.util.List;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InscriptionRepo extends JpaRepository<Inscription, Long> {

    /**
     *
     * @param id id of the users you want to find inscriptions associated with
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of inscription with pagination info
     */
    Page<Inscription> findByUserId(Long id, Pageable pageable);

    /**
     *
     * @param id id of the events you want to find inscriptions associated with
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of inscription with pagination info
     */
    Page<Inscription> findByEventId(Long id, Pageable pageable);

    /**
     *
     * @param dateMin starting date at which you want to find inscription for
     * @param dateMax maximum date at which you want to find inscription for
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of inscription with pagination info
     */
    Page<Inscription> findByInscriptionDateBetween(LocalDateTime dateMin, LocalDateTime dateMax, Pageable pageable);

    /**
     *
     * @param status status of inscription you want to find
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of inscription with pagination info
     */
    Page<Inscription> findByStatus(InscriptionStatus status, Pageable pageable);

    /**
     *
     * @param dateMin starting date at which you want to find inscription for
     * @param dateMax maximum date at which you want to find inscription for
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of inscription with pagination info
     */
    Page<Inscription> findByCancellationDate(LocalDateTime dateMin, LocalDateTime dateMax, Pageable pageable);

    /**
     *
     * @param id id of the user that cancelled an inscription
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of inscription with pagination info
     */
    Page<Inscription> findByCanceledById(Long id, Pageable pageable);

    /**
     *
     * @param eventId event id you want to find inscription associated with
     * @param status status you want to find inscription associated with
     * @return the number of inscriptions with parameters
     */
    long countByEventIdAndStatus(Long eventId, InscriptionStatus status);

    /**
     *
     * @param eventIds ids of the events you want the number of inscriptions for
     * @param status status of the inscriptions to count
     * @return one entry per event having at least one inscription with this status
     */
    @Query("select i.event.id as eventId, count(i) as total from Inscription i "
            + "where i.event.id in :eventIds and i.status = :status group by i.event.id")
    List<EventInscriptionCount> countByEventIdsAndStatus(@Param("eventIds") Collection<Long> eventIds,
            @Param("status") InscriptionStatus status);

    /**
     *
     * @param userId id of the user you want to find inscription associated with
     * @param eventId id of the event you want to find inscription associated with
     * @param status status of the inscription you want to find
     * @return true if an inscription exists with there paramters and false if not
     */
    boolean existsByUserIdAndEventIdAndStatusNot(Long userId, Long eventId, InscriptionStatus status);

    /**
     *
     * @param eventId id of event you want to find inscription associated with
     * @param status status of the inscription you want to find
     * @return an optional of type inscription
     */
    Optional<Inscription> findFirstByEventIdAndStatusOrderByInscriptionDateAsc(Long eventId, InscriptionStatus status);

    /**
     *
     * @param userId id of the user you want to find every inscription for
     * @return all the inscriptions of the user, whatever their status
     */
    List<Inscription> findAllByUserId(Long userId);
}
