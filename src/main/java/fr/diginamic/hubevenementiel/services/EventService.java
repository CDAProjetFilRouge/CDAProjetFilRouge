package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Address;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepo eventRepository;


    public EventService(EventRepo eventRepository) {
        this.eventRepository = eventRepository;
    }

    public List<Event> findAllEvents(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return eventRepository.findAll(pageable).getContent();
    }

    public Event findById(Long eventId) throws HttpException {
        Optional<Event> optionalEvent = eventRepository.findById(eventId);

        if (optionalEvent.isEmpty()) {
            throw new NotFoundException("Aucun évènement trouvé avec cet identifiant.");
        }

        return optionalEvent.get();
    }

    public Event findByTitle(String eventTitle) throws HttpException {
        Optional<Event> optionalEvent = eventRepository.findByTitle(eventTitle);

        if (optionalEvent.isEmpty()) {
            throw new NotFoundException("Aucun évènement trouvé avec ce nom.");
        }

        return optionalEvent.get();
    }

    public List<Event> findByCategory(int page, int size, Category category) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByCategory(category, pageable).getContent();
    }

    public List<Event> findByDates(int page, int size, LocalDateTime startDate, LocalDateTime endDate) throws HttpException {

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("La date de début ne peut pas être postérieure à la date de fin.");
        }

        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByStartDateTimeGreaterThanEqualAndEndDateTimeLessThanEqual(
                startDate, endDate, pageable
        ).getContent();
    }

    public List<Event> findByPrice(int page, int size, int lowerPrice, int higherPrice) throws HttpException {

        if (lowerPrice > higherPrice) {
            throw new BadRequestException("Le prix minimum ne peut pas être supérieur au prix maximum.");
        }

        BigDecimal lower = BigDecimal.valueOf(lowerPrice);
        BigDecimal higher = BigDecimal.valueOf(higherPrice);

        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByNonAffiliatePriceGreaterThanEqualAndNonAffiliatePriceLessThanEqual(
                lower, higher, pageable
        ).getContent();
    }

    public List<Event> findByStatus(int page, int size, EventStatus EventStatus) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByStatus(EventStatus, pageable).getContent();
    }

    @Transactional
    public Event createEvent(Event event) throws HttpException {

        event.setStatus(EventStatus.DRAFT);
        eventChecker(event);

        if (eventRepository.existsByTitle(event.getTitle())) {
            throw new ConflictException("Un évènement avec ce titre existe déjà.");
        }

        return eventRepository.save(event);
    }

    @Transactional
    public Event updateEvent(Long eventId, Event modifiedEvent) throws HttpException {

        Event eventToBeModified = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun évènement n'a été trouvé avec cet identifiant."));

        eventChecker(modifiedEvent);

        if (!eventToBeModified.getTitle().equalsIgnoreCase(modifiedEvent.getTitle())
                && eventRepository.existsByTitle(modifiedEvent.getTitle())) {
            throw new ConflictException("Un évènement avec ce titre existe déjà.");
        }

        eventToBeModified.setTitle(modifiedEvent.getTitle());
        eventToBeModified.setDescription(modifiedEvent.getDescription());
        eventToBeModified.setLocation(modifiedEvent.getLocation());
        eventToBeModified.setCategory(modifiedEvent.getCategory());
        eventToBeModified.setStartDateTime(modifiedEvent.getStartDateTime());
        eventToBeModified.setEndDateTime(modifiedEvent.getEndDateTime());
        eventToBeModified.setAffiliatePrice(modifiedEvent.getAffiliatePrice());
        eventToBeModified.setNonAffiliatePrice(modifiedEvent.getNonAffiliatePrice());
        eventToBeModified.setMaxCapacity(modifiedEvent.getMaxCapacity());
        eventToBeModified.setStatus(modifiedEvent.getStatus());

        return eventRepository.save(eventToBeModified);
    }

    @Transactional
    public void deleteEvent(Long eventId) throws HttpException {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun évènement n'a été trouvé avec cet identifiant."));

        eventRepository.delete(event);
    }


    public boolean eventChecker(Event event) throws HttpException {

        if (event == null) {
            throw new BadRequestException("L'évènement ne peut pas être nul.");
        }

        String title = event.getTitle();
        if (title == null || title.isBlank()) {
            throw new BadRequestException("Le titre de l'évènement doit contenir au moins un caractère.");
        }
        if (title.length() > 200) {
            throw new BadRequestException("Le titre ne peut pas dépasser 200 caractères.");
        }

        String description = event.getDescription();
        if (description == null || description.isBlank()) {
            throw new BadRequestException("L'évènement doit avoir une description.");
        }

        Address location = event.getLocation();
        if (location == null) {
            throw new BadRequestException("L'évènement doit avoir une localisation.");
        }

        if (event.getCategory() == null) {
            throw new BadRequestException("Vous devez choisir une catégorie pour l'évènement.");
        }

        LocalDateTime start = event.getStartDateTime();
        if (start == null) {
            throw new BadRequestException("L'évènement doit avoir une date de début.");
        }

        LocalDateTime end = event.getEndDateTime();
        if (end == null) {
            throw new BadRequestException("L'évènement doit avoir une date de fin.");
        }

        if (!end.isAfter(start)) {
            throw new BadRequestException("La date de fin doit être postérieure à la date de début.");
        }

        Integer maxCapacity = event.getMaxCapacity();
        if (maxCapacity == null) {
            throw new BadRequestException("Vous devez définir une capacité maximale pour l'évènement.");
        }
        if (maxCapacity <= 0) {
            throw new BadRequestException("La capacité maximale doit être supérieure à zéro.");
        }

        BigDecimal affiliatePrice = event.getAffiliatePrice();
        if (affiliatePrice == null) {
            throw new BadRequestException("Vous devez déterminer un prix pour les personnes affiliées au club.");
        }
        if (affiliatePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Le prix pour les personnes affiliées ne peut pas être négatif.");
        }

        BigDecimal nonAffiliatePrice = event.getNonAffiliatePrice();
        if (nonAffiliatePrice == null) {
            throw new BadRequestException("Vous devez déterminer un prix pour les personnes non affiliées au club.");
        }
        if (nonAffiliatePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Le prix pour les personnes non affiliées ne peut pas être négatif.");
        }

        if (event.getStatus() == null) {
            throw new BadRequestException("Vous devez définir un statut pour l'évènement.");
        }

        return true;
    }


}
