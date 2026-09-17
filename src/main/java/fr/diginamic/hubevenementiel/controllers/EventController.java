package fr.diginamic.hubevenementiel.controllers;

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

import java.time.LocalDateTime;

import fr.diginamic.hubevenementiel.dtos.event.EventRequestDto;
import fr.diginamic.hubevenementiel.dtos.event.EventResponseDto;
import fr.diginamic.hubevenementiel.dtos.event.EventSummaryResponseDto;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.mappers.EventMapper;
import fr.diginamic.hubevenementiel.mappers.EventSummaryMapper;
import fr.diginamic.hubevenementiel.services.EventService;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final EventMapper eventMapper;
    private final EventSummaryMapper eventSummaryMapper;

    public EventController(EventService eventService, EventMapper eventMapper, EventSummaryMapper eventSummaryMapper) {
        this.eventService = eventService;
        this.eventMapper = eventMapper;
        this.eventSummaryMapper = eventSummaryMapper;
    }

    @GetMapping
    public List<EventSummaryResponseDto> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) EventStatus status) throws HttpException {
        return eventService.search(page, size, category, startDate, endDate, minPrice, maxPrice, status).stream()
                .map(eventSummaryMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public EventResponseDto getById(@PathVariable Long id) throws HttpException {
        return eventMapper.toDto(eventService.findById(id));
    }

    @GetMapping("/search")
    public EventResponseDto getByName(@RequestParam String name) throws HttpException {
        return eventMapper.toDto(eventService.findByTitle(name));
    }

    @PostMapping
    public ResponseEntity<EventResponseDto> create(@RequestBody EventRequestDto requestDto) throws HttpException {
        Event event = eventMapper.toEntity(requestDto);
        Event created = eventService.createEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventMapper.toDto(created));
    }

    @PutMapping("/{id}")
    public EventResponseDto update(@PathVariable Long id, @RequestBody EventRequestDto requestDto) throws HttpException {
        Event event = eventMapper.toEntity(requestDto);
        Event updated = eventService.updateEvent(id, event);
        return eventMapper.toDto(updated);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) throws HttpException {
        eventService.deleteEvent(id);
        return ResponseEntity.noContent().build();
    }
}
