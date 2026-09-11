package fr.diginamic.hubevenementiel.repositories;

import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import org.springframework.cglib.core.Local;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;


public interface EventRepo extends JpaRepository<Event, Long> {

    Page<Event> findByTitle(String title, Pageable pageable);

    Page<Event> findByLocationId(Long id, Pageable pageable);

    Page<Event> findByLocationPostalCode(String code, Pageable pageable);

    Page<Event> findByLocationCity(String city, Pageable pageable);

    Page<Event> findByLocationCountry(String country, Pageable pageable);

    Page<Event> findByCategory(Category category, Pageable pageable);

    Page<Event> findByStartDateTimeBetween(LocalDateTime dateMin, LocalDate dateMax);

    Page<Event> findByEndDateTimeBetween(LocalDate dateMin, LocalDateTime dateMax);

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

}
