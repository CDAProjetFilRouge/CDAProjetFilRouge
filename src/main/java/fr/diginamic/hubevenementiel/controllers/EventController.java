package fr.diginamic.hubevenementiel.controllers;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.services.EventService;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping
    public List<Event> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) LocalDate startDate,
            @RequestParam(required = false) LocalDate endDate,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) EventStatus status) throws HttpException {

        if (category != null) {
            return eventService.findByCategory(page, size, category);
        }
        if (startDate != null || endDate != null) {
            return eventService.findByDates(page, size, startDate, endDate);
        }
        if (minPrice != null && maxPrice != null) {
            return eventService.findByPrice(page, size, minPrice, maxPrice);
        }
        if (status != null) {
            return eventService.findByStatus(page, size, status);
        }

        return eventService.findAllEvents(page, size);
    }

    @GetMapping("/{id}")
    public Event getById(@PathVariable Long id) throws HttpException {
        return eventService.findById(id);
    }

    @GetMapping("/search")
    public Event getByName(@RequestParam String name) throws HttpException {
        return eventService.findByName(name);
    }

    @PostMapping
    public ResponseEntity<Event> create(@RequestBody Event event) throws HttpException {
        Event created = eventService.createEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public Event update(@PathVariable Long id, @RequestBody Event event) throws HttpException {
        return eventService.updateEvent(id, event);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws HttpException {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
