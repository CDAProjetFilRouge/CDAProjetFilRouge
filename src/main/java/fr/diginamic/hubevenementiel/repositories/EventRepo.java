package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;


public interface EventRepo extends JpaRepository<Event, Long> {

    Optional<Event> findByTitle(String title);

    Page<Event> findByLocationId(Long id, Pageable pageable);

    Page<Event> findByLocationPostalCode(String code, Pageable pageable);

    Page<Event> findByLocationCity(String city, Pageable pageable);

    Page<Event> findByLocationCountry(String country, Pageable pageable);

    Page<Event> findByCategory(Category category, Pageable pageable);

    Page<Event> findByStartDateTimeBetween(LocalDateTime dateMin, LocalDate dateMax);

    Page<Event> findByEndDateTimeBetween(LocalDate dateMin, LocalDateTime dateMax);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Page<Event> findByStartDateTimeGreaterThanEqualAndEndDateTimeLessThanEqual(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<Event> findByNonAffiliatePriceGreaterThanEqualAndNonAffiliatePriceLessThanEqual(BigDecimal lowerPrice, BigDecimal higherPrice, Pageable pageable);

    boolean existsByTitle(String title);

}
