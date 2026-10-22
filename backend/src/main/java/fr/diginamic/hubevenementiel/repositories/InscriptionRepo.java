package fr.diginamic.hubevenementiel.repositories;

import java.util.UUID;
import java.util.Collection;
import java.util.List;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface InscriptionRepo extends JpaRepository<Inscription, UUID> {

    /**
     *
     * @param id id of the users you want to find inscriptions associated with
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of inscription with pagination info
     */
    // L'évènement et son club sont chargés avec la page : le calendrier affiche le club de chaque inscription.
    @EntityGraph(attributePaths = {"event", "event.club"})
    Page<Inscription> findByUserId(UUID id, Pageable pageable);

    Page<Inscription> findByUserIdAndEventStatusNot(UUID id, EventStatus eventStatus, Pageable pageable);

    /**
     * Historique d'un utilisateur : ses inscriptions confirmées à des évènements dont la date de fin est atteinte,
     * ou qui ont été annulés, du plus récent au plus ancien.
     */
    @EntityGraph(attributePaths = {"event", "event.club"})
    @Query("select i from Inscription i where i.user.id = :userId and i.status = :status "
            + "and (i.event.endDateTime <= :now or i.event.status = :cancelled) order by i.event.startDateTime desc")
    Page<Inscription> findHistory(@Param("userId") UUID userId, @Param("status") InscriptionStatus status,
            @Param("now") LocalDateTime now, @Param("cancelled") EventStatus cancelled, Pageable pageable);

    /**
     *
     * @param id id of the events you want to find inscriptions associated with
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of inscription with pagination info
     */
    Page<Inscription> findByEventId(UUID id, Pageable pageable);

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
    Page<Inscription> findByCanceledById(UUID id, Pageable pageable);

    /**
     *
     * @param eventId event id you want to find inscription associated with
     * @param status status you want to find inscription associated with
     * @return the number of inscriptions with parameters
     */
    long countByEventIdAndStatus(UUID eventId, InscriptionStatus status);

    /**
     *
     * @param eventIds ids of the events you want the number of inscriptions for
     * @param status status of the inscriptions to count
     * @return one entry per event having at least one inscription with this status
     */
    @Query("select i.event.id as eventId, count(i) as total from Inscription i "
            + "where i.event.id in :eventIds and i.status = :status group by i.event.id")
    List<EventInscriptionCount> countByEventIdsAndStatus(@Param("eventIds") Collection<UUID> eventIds,
            @Param("status") InscriptionStatus status);

    /**
     *
     * @param userId id of the user you want to find inscription associated with
     * @param eventId id of the event you want to find inscription associated with
     * @param status status of the inscription you want to find
     * @return true if an inscription exists with there paramters and false if not
     */
    boolean existsByUserIdAndEventIdAndStatusNot(UUID userId, UUID eventId, InscriptionStatus status);

    /**
     *
     * @param eventId id of event you want to find inscription associated with
     * @param status status of the inscription you want to find
     * @return an optional of type inscription
     */
    List<Inscription> findByEventIdAndStatusOrderByInscriptionDateAsc(UUID eventId, InscriptionStatus status);

    /**
     *
     * @param eventId id of the event
     * @param statuses statuses to keep
     * @return the event's inscriptions having one of the given statuses
     */
    List<Inscription> findByEventIdAndStatusIn(UUID eventId, Collection<InscriptionStatus> statuses);

    /**
     * Removes every inscription of an event, whatever its status.
     *
     * @param eventId id of the event
     */
    void deleteByEventId(UUID eventId);

    /**
     * Nombre d'inscriptions de même statut arrivées avant celle donnée (date, puis identifiant pour départager).
     * Sert à calculer le rang dans la liste d'attente, dans l'ordre de promotion.
     */
    @Query("select count(i) from Inscription i where i.event.id = :eventId and i.status = :status "
            + "and (i.inscriptionDate < :date or (i.inscriptionDate = :date and i.id < :id))")
    long countAheadInQueue(@Param("eventId") UUID eventId, @Param("status") InscriptionStatus status,
            @Param("date") LocalDateTime date, @Param("id") UUID id);

    /**
     *
     * @param userId id of the user you want to find every inscription for
     * @return all the inscriptions of the user, whatever their status
     */
    List<Inscription> findAllByUserId(UUID userId);

    /**
     *
     * @param userId id of the user
     * @param status status of the inscriptions to count
     * @param from lower bound (inclusive) of the event start date
     * @return the number of inscriptions of the user for events starting from this date
     */
    long countByUserIdAndStatusAndEventStartDateTimeGreaterThanEqual(UUID userId, InscriptionStatus status,
            LocalDateTime from);

    /**
     *
     * @param userId id of the user
     * @param status status of the inscriptions to count
     * @param to upper bound (exclusive) of the event start date
     * @return the number of inscriptions of the user for events started before this date
     */
    long countByUserIdAndStatusAndEventStartDateTimeLessThan(UUID userId, InscriptionStatus status,
            LocalDateTime to);

    long countByUserIdAndStatusAndEventStatusNotAndEventStartDateTimeGreaterThanEqual(UUID userId,
            InscriptionStatus status, EventStatus eventStatus, LocalDateTime from);

    long countByUserIdAndStatusAndEventStatusNotAndEventStartDateTimeLessThan(UUID userId,
            InscriptionStatus status, EventStatus eventStatus, LocalDateTime to);

    /**
     *
     * @param organizerId id of the organizer
     * @param eventStatus status of the events of the organizer
     * @param status status of the inscriptions to count
     * @param from lower bound (inclusive) of the event start date
     * @return the number of inscriptions received by the upcoming events of the organizer
     */
    @Query("SELECT COUNT(i) FROM Inscription i WHERE i.event.organizer.id = :organizerId "
            + "AND i.event.status = :eventStatus AND i.status = :status AND i.event.startDateTime >= :from")
    long countForOrganizerEvents(@Param("organizerId") UUID organizerId,
            @Param("eventStatus") EventStatus eventStatus,
            @Param("status") InscriptionStatus status, @Param("from") LocalDateTime from);
}
