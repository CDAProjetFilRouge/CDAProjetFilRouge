package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public interface EventRepo extends JpaRepository<Event, Long> {

    /**
     *
     * @param title title of the event
     * @return an optional of type event
     */
    Optional<Event> findByTitle(String title);

    Page<Event> findByLocationId(Long id, Pageable pageable);

    Page<Event> findByLocationPostalCode(String code, Pageable pageable);

    Page<Event> findByLocationCity(String city, Pageable pageable);

    Page<Event> findByLocationCountry(String country, Pageable pageable);

    /**
     *
     * @param category
     * @param pageable
     * @return
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

    @Query("SELECT e FROM Event e WHERE " +
            "(:category IS NULL OR e.category = :category) AND " +
            "(:startDate IS NULL OR e.startDateTime >= :startDate) AND " +
            "(:endDate IS NULL OR e.endDateTime <= :endDate) AND " +
            "(:minPrice IS NULL OR e.nonAffiliatePrice >= :minPrice) AND " +
            "(:maxPrice IS NULL OR e.nonAffiliatePrice <= :maxPrice) AND " +
            "(:status IS NULL OR e.status = :status)")
    Page<Event> search(@Param("category") Category category,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("minPrice") BigDecimal minPrice,
            @Param("maxPrice") BigDecimal maxPrice,
            @Param("status") EventStatus status,
            Pageable pageable);

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
    Optional<Event> findByIdForUpdate(@Param("id") Long id);

}
