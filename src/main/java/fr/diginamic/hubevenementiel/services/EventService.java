package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class EventService {

    private final EventRepository eventRepository;


    public EventService(EventRepository eventRepository) {
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

    public Event findByName(String eventName) throws HttpException {
        Optional<Event> optionalEvent = eventRepository.findByName(eventName);

        if (optionalEvent.isEmpty()) {
            throw new NotFoundException("Aucun évènement trouvé avec ce nom.");
        }

        return optionalEvent.get();
    }

    public List<Event> findByCategory(int page, int size, Category category) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByCategory(category, pageable).getContent();
    }

    public List<Event> findByDates(int page, int size, LocalDate startDate, LocalDate endDate) throws HttpException {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("La date de début ne peut pas être postérieure à la date de fin");
        }

        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByDates(startDate, endDate, pageable).getContent();
    }

    public List<Event> findByPrice(int page, int size, int lowerPrice, int higherPrice) {
        if (lowerPrice > higherPrice) {
            throw new BadRequestException("Le prix minimum ne peut pas être supérieur au prix maximum")
        }

        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByPrice(lowerPrice, higherPrice, pageable).getContent();
    }

    public List<Event> findByStatus(int page, int size, EventStatus EventStatus) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByStatus(EventStatus, pageable).getContent();
    }

    @Transactional
    public void createEvent(Event event) throws HttpException {
        eventChecker(event);
        eventRepository.save(event);
    }




    public boolean eventChecker(Event event) throws HttpException {


        if (event.getTitle().isBlank()) {
            throw new BadRequestException("Le titre de l'évènement doit contenir au moins un caractère.");
        }

        if (event.getDescription().isBlank()) {
            throw new BadRequestException("L'évènement doit avoir une description");
        }

        if (event.getLocation().isBlank()){
            throw new BadRequestException("L'évènement doit avoir une localisation.");
        }

        if (event.getCategory() == null) {
            throw new BadRequestException("Vous devez choisir une catégorie pur l'évènement.");
        }

        if (event.getStartDateTime() == null) {
            throw new BadRequestException("L'évènement doit avoir une date de début.");
        }

        if (event.getMaxCapacity() == null) {
            throw new BadRequestException("Vous devez définir une capacité maximale pour l'évènement");
        }

        if (event.getAffiliatePrice() == null) {
            throw new BadRequestException("Vous devez déterminer un prix pour les personnes affiliée au club");
        }

        if (event.getNonAffiliatePrice() == null) {
            throw new BadRequestException("Vous devez déterminer un prix pour les personnes non affiliée au club");
        }

        return true;
    }


}
