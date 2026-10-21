package fr.diginamic.hubevenementiel.controllers;

import java.util.UUID;
import java.util.List;
import java.util.Map;

import fr.diginamic.hubevenementiel.dtos.event.*;
import fr.diginamic.hubevenementiel.mappers.EventDetailsMapper;
import fr.diginamic.hubevenementiel.mappers.EventMainMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.mappers.EventMapper;
import fr.diginamic.hubevenementiel.mappers.EventSummaryMapper;
import fr.diginamic.hubevenementiel.openapi.EventApi;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import fr.diginamic.hubevenementiel.services.AppUserService;
import fr.diginamic.hubevenementiel.services.EventService;
import org.springframework.security.core.context.SecurityContextHolder;

@RestController
@RequestMapping("/events")
public class EventController implements EventApi {

    private final EventService eventService;
    private final EventMapper eventMapper;
    private final EventMainMapper eventMainMapper;
    private final EventSummaryMapper eventSummaryMapper;
    private final EventDetailsMapper eventDetailsMapper;
    private final AppUserService appUserService;

    public EventController(EventService eventService, EventMapper eventMapper, EventSummaryMapper eventSummaryMapper,
            AppUserService appUserService, EventMainMapper eventMainMapper, EventDetailsMapper eventDetailsMapper) {
        this.eventService = eventService;
        this.eventMapper = eventMapper;
        this.eventSummaryMapper = eventSummaryMapper;
        this.appUserService = appUserService;
        this.eventMainMapper = eventMainMapper;
        this.eventDetailsMapper = eventDetailsMapper;
    }

    @Override
    @GetMapping
    public Page<EventResponseMainDto> getEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
            ) throws HttpException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        AppUserPrincipal principal = null;

        if(authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof  AppUserPrincipal) {
            principal = (AppUserPrincipal) authentication.getPrincipal();
        }
        return toMainDtos(eventService.findAllEventMain(page, size, principal));
    }

    private Page<EventResponseMainDto> toMainDtos(Page<Event> events) {
        Map<UUID, Integer> remainingSpots = eventService.remainingSpots(events.getContent());

        return events.map(event -> eventMainMapper.toDto(event, remainingSpots.get(event.getId())));
    }

    @Override
    @GetMapping("/summary")
    public List<EventSummaryResponseDto> getEventsSummary(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(required = false) EventStatus status) throws HttpException {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return eventService.search(page, size, category, startDate, endDate, minPrice, maxPrice, status, principal).stream()
                .map(eventSummaryMapper::toDto)
                .toList();
    }

    @Override
    @Secured({"ROLE_ORGANIZER", "ROLE_ADMINISTRATOR"})
    @GetMapping("/mine")
    public Page<EventResponseMainDto> getMyEvents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) EventStatus status) throws HttpException {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return toMainDtos(eventService.findOrganizedBy(page, size, status, principal));
    }

    @Override
    @GetMapping("/filter")
    public Page<EventResponseMainDto> getEventSearch(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) String clubName,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) List<EventStatus> status,
            @RequestParam(defaultValue = "false") boolean hideFinished,
            @RequestParam(defaultValue = "ASC") Sort.Direction sort) throws HttpException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        AppUserPrincipal principal = null;

        if(authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof  AppUserPrincipal) {
            principal = (AppUserPrincipal) authentication.getPrincipal();
        }
        LocalDateTime startDateTime = startDate == null ? null : startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate == null ? null : endDate.atTime(LocalTime.MAX);
        return toMainDtos(eventService.searchFilter(page, size, keyword, category, clubName, city,
                startDateTime, endDateTime, status, hideFinished, sort, principal));

    }

    /**
    @Override
    @GetMapping("/{id}")
    public EventResponseDto getById(@PathVariable UUID id) throws HttpException {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return eventMapper.toDto(eventService.findVisibleById(id, principal));
    }
    **/
    @Override
    @GetMapping("/{id}")
    public EventDetailsReponseDto getById(@PathVariable UUID id) throws HttpException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        AppUserPrincipal principal = null;

        if(authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() instanceof  AppUserPrincipal) {
            principal = (AppUserPrincipal) authentication.getPrincipal();
        }

        Event event = eventService.findVisibleById(id, principal);

        Map <UUID, Integer> remainingSpots = eventService.remainingSpots(List.of(event));

        return eventDetailsMapper.toDto(event, remainingSpots.get(event.getId()));
    }

    @Override
    @GetMapping("/search")
    public EventResponseDto getByName(@RequestParam String name) throws HttpException {
        return eventMapper.toDto(eventService.findByTitle(name));
    }

    @Override
    @Secured({"ROLE_ORGANIZER", "ROLE_ADMINISTRATOR"})
    @PostMapping
    public ResponseEntity<EventResponseDto> create(@RequestBody EventRequestDto requestDto) throws HttpException {
        Event event = eventMapper.toEntity(requestDto);

        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        AppUser organizer = appUserService.findById(principal.id());
        event.setOrganizer(organizer);

        Event created = eventService.createEvent(event, requestDto.getIdClub());
        return ResponseEntity.status(HttpStatus.CREATED).body(eventMapper.toDto(created));
    }

    @Override
    @Secured({"ROLE_ORGANIZER", "ROLE_ADMINISTRATOR"})
    @PutMapping("/{id}")
    public EventResponseDto update(@PathVariable UUID id, @RequestBody EventRequestDto requestDto) throws HttpException {
        Event event = eventMapper.toEntity(requestDto);
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Event updated = eventService.updateEvent(id, event, principal);
        return eventMapper.toDto(updated);
    }

    @Override
    @Secured({"ROLE_ORGANIZER", "ROLE_ADMINISTRATOR"})
    @PatchMapping("/{id}/status")
    public EventResponseDto changeStatus(@PathVariable UUID id, @RequestBody EventStatusRequestDto requestDto) throws HttpException {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Event updated = eventService.changeStatus(id, requestDto.getStatus(), principal);
        return eventMapper.toDto(updated);
    }

    @Override
    @Secured({"ROLE_ORGANIZER", "ROLE_ADMINISTRATOR"})
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) throws HttpException {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        eventService.deleteEvent(id, principal);
        return ResponseEntity.noContent().build();
    }
}
