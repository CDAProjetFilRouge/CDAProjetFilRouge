package fr.diginamic.hubevenementiel.repositories;

import java.util.UUID;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface EventRepo extends JpaRepository<Event, UUID>, JpaSpecificationExecutor<Event> {

    /**
     *
     * @param title title of the event
     * @return an optional of type event
     */
    Optional<Event> findByTitle(String title);

    /**
     *
     * @param spec filters to apply
     * @param pageable settings for the pagination
     * @return a page of events with their location and organizer loaded in the same query
     */
    @Override
    @EntityGraph(attributePaths = {"location", "organizer"})
    Page<Event> findAll(Specification<Event> spec, Pageable pageable);

    Page<Event> findByLocationId(UUID id, Pageable pageable);

    Page<Event> findByLocationPostalCode(String code, Pageable pageable);

    Page<Event> findByLocationCity(String city, Pageable pageable);

    Page<Event> findByLocationCountry(String country, Pageable pageable);

    /**
     *
     * @param category category of the event you want to do the search on
     * @param pageable settings for the pagination, create a pageable object using PageRequest.of()
     * @return a list of events with pagination info
     */
    Page<Event> findByCategory(Category category, Pageable pageable);

    Page<Event> findByStartDateTimeBetween(Pageable pageable, LocalDateTime dateMin, LocalDate dateMax);

    Page<Event> findByEndDateTimeBetween(Pageable pageable, LocalDate dateMin, LocalDateTime dateMax);

    /**
     *
     * @param status status of the event you want to do the search on
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of event with pagination info
     */
    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    /**
     *
     * @param start starting date of the event you want to do the search on
     * @param end maximum date of the event you want to do the search on
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of events with pagination info
     */
    Page<Event> findByStartDateTimeGreaterThanEqualAndEndDateTimeLessThanEqual(LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     *
     * @param lowerPrice starting price of the event you want to search on
     * @param higherPrice maximum price of the event you want to search on
     * @param pageable settings for the pagination, create a peagble object using PageRequest.of()
     * @return a list of events with pagination info
     */
    Page<Event> findByNonAffiliatePriceGreaterThanEqualAndNonAffiliatePriceLessThanEqual(BigDecimal lowerPrice, BigDecimal higherPrice, Pageable pageable);

    /**
     *
     * @param title title of the event you want to search
     * @return true or false depending if the event exists or not
     */
    boolean existsByTitle(String title);

    /**
     *
     * @param id id of the event you want to search
     * @return an optional of type event
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") UUID id);

    /**
     *
     * @param organizerId id of the organizer
     * @return true if the user organizes at least one event
     */
    boolean existsByOrganizerId(UUID organizerId);

    /**
     *
     * @param organizerId id of the organizer
     * @param status status of the events to count
     * @param from lower bound (inclusive) of the start date
     * @return the number of events of the organizer starting from this date
     */
    long countByOrganizerIdAndStatusAndStartDateTimeGreaterThanEqual(UUID organizerId, EventStatus status,
            LocalDateTime from);

    /**
     *
     * @param organizerId id of the organizer
     * @param status status of the events to count
     * @return the number of events of the organizer with this status
     */
    long countByOrganizerIdAndStatus(UUID organizerId, EventStatus status);

    /**
     *
     * @param status status of the events to count
     * @param from lower bound (inclusive) of the start date
     * @return the number of events with this status starting from this date
     */
    long countByStatusAndStartDateTimeGreaterThanEqual(EventStatus status, LocalDateTime from);

    /**
     *
     * @param organizerId id of the organizer
     * @param status status of the events to sum
     * @param from lower bound (inclusive) of the start date
     * @return the total capacity of the matching events, 0 if there are none
     */
    @Query("SELECT COALESCE(SUM(e.maxCapacity), 0L) FROM Event e WHERE e.organizer.id = :organizerId "
            + "AND e.status = :status AND e.startDateTime >= :from")
    long sumCapacityByOrganizer(@Param("organizerId") UUID organizerId, @Param("status") EventStatus status,
            @Param("from") LocalDateTime from);

    /**
     * Moves to a new status every event that has the given status and whose end date is before the given date.
     *
     * @param from status the events must currently have
     * @param to status to give them
     * @param date events ending strictly before this date are updated
     * @return the number of events updated
     */
    @Modifying
    @Query("UPDATE Event e SET e.status = :to WHERE e.status = :from AND e.endDateTime < :date")
    int updateStatusOfEndedEvents(@Param("from") EventStatus from, @Param("to") EventStatus to,
            @Param("date") LocalDateTime date);
}
