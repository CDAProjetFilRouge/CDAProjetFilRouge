package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Address;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.ForbiddenException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventInscriptionCount;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.EventSpecifications;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService {

    /** Plus grand tarif accepté : la colonne est un decimal(10,2). */
    static final BigDecimal MAX_PRICE = new BigDecimal("99999999.99");

    /** Longueur maximale de la description (la colonne est un TEXT de 65 535 octets). */
    static final int MAX_DESCRIPTION_LENGTH = 10_000;

    /** Nombre de places maximum pour un évènement. */
    static final int MAX_CAPACITY = 100_000;

    private final EventRepo eventRepository;
    private final InscriptionRepo inscriptionRepo;
    private final ClubService clubService;
    private final WaitingListService waitingListService;


    public EventService(EventRepo eventRepository, InscriptionRepo inscriptionRepo,
            WaitingListService waitingListService, ClubService clubService) {
        this.eventRepository = eventRepository;
        this.inscriptionRepo = inscriptionRepo;
        this.waitingListService = waitingListService;
        this.clubService = clubService;
    }

    /**
     *
     * @param events events of a page for which the remaining spots are needed
     * @return the remaining spots by event id (maximum capacity minus confirmed inscriptions, never below zero)
     */
    public Map<Long, Integer> remainingSpots(List<Event> events) {
        if (events.isEmpty()) {
            return Map.of();
        }

        List<Long> eventIds = events.stream().map(Event::getId).toList();

        Map<Long, Long> confirmedByEvent = inscriptionRepo
                .countByEventIdsAndStatus(eventIds, InscriptionStatus.CONFIRMED).stream()
                .collect(Collectors.toMap(EventInscriptionCount::getEventId, EventInscriptionCount::getTotal));

        return events.stream().collect(Collectors.toMap(
                Event::getId,
                event -> Math.max(0, event.getMaxCapacity() - confirmedByEvent.getOrDefault(event.getId(), 0L).intValue())));
    }

    /**
     *
     * @param eventId id of the event to find
     * @return an object of type Event
     * @throws HttpException
     */
    public Event findById(Long eventId) throws HttpException {
        Optional<Event> optionalEvent = eventRepository.findById(eventId);

        if (optionalEvent.isEmpty()) {
            throw new NotFoundException("Aucun évènement trouvé avec cet identifiant.");
        }

        return optionalEvent.get();
    }

    /**
     * Recherche par id unique : pas de Specification ici (reservee aux listes), juste
     * la meme regle RG14/RG15 verifiee en Java sur l'unique ligne chargee.
     *
     * @param eventId id of the event to find
     * @param principal the authenticated caller (id/role used to check draft visibility)
     * @return an object of type Event, only if visible to this caller
     * @throws HttpException
     */
    public Event findVisibleById(Long eventId, AppUserPrincipal principal) throws HttpException {
        Event event = findById(eventId);

        if(event == null){
            throw new NotFoundException("Aucun évènement trouvé avec cet identifiant");
        }

        if(principal != null){
            boolean isAdmin = "ADMINISTRATOR".equals(principal.role());
            boolean isOwner = event.getOrganizer() != null && event.getOrganizer().getId().equals(principal.id());

            if (event.getStatus() == EventStatus.DRAFT && !isAdmin && !isOwner) {
                throw new NotFoundException("Vous n'avez pas les accès nécessaire pour consulter cette ressource");
            }
            return event;

        }else if(event.getStatus() != EventStatus.DRAFT){
            return event;
        }else{
            throw new NotFoundException("Vous n'avez pas les accès nécessaire pour consulter cette ressource");
        }
    }

    /**
     *
     * @param eventTitle title of the events to do the search on
     * @return a list of Events
     * @throws HttpException
     */
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

    public List<Event> findByDates(int page, int size, LocalDateTime startDate, LocalDateTime endDate)
            throws HttpException {

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("La date de début ne peut pas être postérieure à la date de fin.");
        }

        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByStartDateTimeGreaterThanEqualAndEndDateTimeLessThanEqual(
                startDate, endDate, pageable).getContent();
    }

    public List<Event> findByPrice(int page, int size, int lowerPrice, int higherPrice) throws HttpException {

        if (lowerPrice > higherPrice) {
            throw new BadRequestException("Le prix minimum ne peut pas être supérieur au prix maximum.");
        }

        BigDecimal lower = BigDecimal.valueOf(lowerPrice);
        BigDecimal higher = BigDecimal.valueOf(higherPrice);

        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByNonAffiliatePriceGreaterThanEqualAndNonAffiliatePriceLessThanEqual(
                lower, higher, pageable).getContent();
    }

    public List<Event> findByStatus(int page, int size, EventStatus EventStatus) {
        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findByStatus(EventStatus, pageable).getContent();
    }

    /**
     *
     * @param page starting page
     * @param size number of entries per page
     * @param category category to filter events by, or null to not filter on it
     * @param startDate minimum start date to filter events by, or null to not filter on it
     * @param endDate maximum end date to filter events by, or null to not filter on it
     * @param minPrice minimum non-affiliate price to filter events by, or null to not filter on it
     * @param maxPrice maximum non-affiliate price to filter events by, or null to not filter on it
     * @param status status to filter events by, or null to not filter on it
     * @param principal the authenticated caller (used to include their own DRAFT events)
     * @return a list of events matching the given filters and visible to the caller
     * @throws HttpException
     */
    public List<Event> search(int page, int size, Category category, LocalDateTime startDate, LocalDateTime endDate,
            Integer minPrice, Integer maxPrice, EventStatus status, AppUserPrincipal principal) throws HttpException {

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("La date de début ne peut pas être postérieure à la date de fin.");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw new BadRequestException("Le prix minimum ne peut pas être supérieur au prix maximum.");
        }

        BigDecimal minPriceValue = minPrice != null ? BigDecimal.valueOf(minPrice) : null;
        BigDecimal maxPriceValue = maxPrice != null ? BigDecimal.valueOf(maxPrice) : null;

        Specification<Event> spec = EventSpecifications.visibleTo(principal);
        for (Specification<Event> filter : Arrays.asList(
                EventSpecifications.hasCategory(category),
                EventSpecifications.startsOnOrAfter(startDate),
                EventSpecifications.endsOnOrBefore(endDate),
                EventSpecifications.nonAffiliatePriceAtLeast(minPriceValue),
                EventSpecifications.nonAffiliatePriceAtMost(maxPriceValue),
                EventSpecifications.hasStatus(status))) {
            if (filter != null) {
                spec = spec.and(filter);
            }
        }

        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findAll(spec, pageable).getContent();
    }

    public Page<Event> findAllEventMain(int page, int size, AppUserPrincipal principal) throws HttpException {

        if(size > 20){
            throw new BadRequestException("Taille maximale d'éléments à afficher dépassée");
        }

        Specification<Event> spec = EventSpecifications.visibleTo(principal);
        Pageable pageable = PageRequest.of(page, size);

        return eventRepository.findAll(spec, pageable);
    }

    /**
     * Évènements dont l'utilisateur est l'organisateur, tous statuts, du plus proche
     * dans le temps au plus lointain.
     *
     * @param status filtre facultatif sur le statut
     */
    public Page<Event> findOrganizedBy(int page, int size, EventStatus status, AppUserPrincipal principal)
            throws HttpException {
        if (size > 20) {
            throw new BadRequestException("Taille maximale d'éléments à afficher dépassée");
        }

        Specification<Event> spec = EventSpecifications.organizedBy(principal.id());
        Specification<Event> statusFilter = EventSpecifications.hasStatus(status);
        if (statusFilter != null) {
            spec = spec.and(statusFilter);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "startDateTime"));
        return eventRepository.findAll(spec, pageable);
    }

    public Page<Event> searchFilter(int page, int size, String keyword, Category category, String clubName, String city,
            LocalDateTime startDate, LocalDateTime endDate, EventStatus status, AppUserPrincipal principal) throws HttpException {

        if(size > 20){
            throw new BadRequestException("Taille maximale d'éléments à afficher dépassée");
        }

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("La date de début ne peut pas être postérieure à la date de fin.");
        }

        Specification<Event> spec = EventSpecifications.visibleTo(principal);
        for (Specification<Event> filter : Arrays.asList(
                EventSpecifications.titleOrDescription(keyword),
                EventSpecifications.hasCategory(category),
                EventSpecifications.organizerClubNameContains(clubName),
                EventSpecifications.hasCity(city),
                EventSpecifications.startsOnOrAfter(startDate),
                EventSpecifications.endsOnOrBefore(endDate),
                EventSpecifications.hasStatus(status))) {
            if (filter != null) {
                spec = spec.and(filter);
            }
        }

        Pageable pageable = PageRequest.of(page, size);
        return eventRepository.findAll(spec, pageable);
    }

    /**
     *
     * @param event Event to save in the DB
     * @return Event saved
     * @throws HttpException
     */
    @Transactional
    public Event createEvent(Event event, Long clubId) throws HttpException {

        event.setStatus(EventStatus.DRAFT);
        eventChecker(event);

        Club club = clubService.findById(clubId);
        checkClubMembership(event.getOrganizer(), club);

        event.setClub(club);

        if (eventRepository.existsByTitle(event.getTitle())) {
            throw new ConflictException("Un évènement avec ce titre existe déjà.");
        }

        return eventRepository.save(event);
    }

    /**
     *
     * @param eventId id of the event to update
     * @param modifiedEvent updated Event information
     * @param principal the authenticated caller, must be the event's organizer or an admin
     * @return updated Event
     * @throws HttpException
     */
    @Transactional
    public Event updateEvent(Long eventId, Event modifiedEvent, AppUserPrincipal principal) throws HttpException {

        Event eventToBeModified = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun évènement n'a été trouvé avec cet identifiant."));

        checkOwnership(eventToBeModified, principal);

        if (eventToBeModified.getStartDateTime().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Un évènement passé ne peut plus être modifié.");
        }

        eventChecker(modifiedEvent);

        if (!eventToBeModified.getTitle().equalsIgnoreCase(modifiedEvent.getTitle())
                && eventRepository.existsByTitle(modifiedEvent.getTitle())) {
            throw new ConflictException("Un évènement avec ce titre existe déjà.");
        }

        Integer previousCapacity = eventToBeModified.getMaxCapacity();

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

        Event saved = eventRepository.save(eventToBeModified);

        if (previousCapacity != null && saved.getMaxCapacity() != null && saved.getMaxCapacity() > previousCapacity) {
            waitingListService.fillFreeSpots(eventId);
        }

        return saved;
    }

    /**
     *
     * @param eventId id of the event to delete
     * @param principal the authenticated caller, must be the event's organizer or an admin
     * @throws HttpException
     */
    @Transactional
    public void deleteEvent(Long eventId, AppUserPrincipal principal) throws HttpException {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun évènement n'a été trouvé avec cet identifiant."));

        checkOwnership(event, principal);

        if (!event.getStartDateTime().isAfter(LocalDateTime.now())) {
            throw new ConflictException("Seul un évènement futur peut être supprimé.");
        }

        eventRepository.delete(event);
    }

    /**
     * RG15 : seul le propriétaire de l'évènement ou un administrateur peut le modifier/supprimer.
     */
    private void checkOwnership(Event event, AppUserPrincipal principal) throws HttpException {
        boolean isAdmin = "ADMINISTRATOR".equals(principal.role());
        boolean isOwner = event.getOrganizer() != null && event.getOrganizer().getId().equals(principal.id());

        if (!isAdmin && !isOwner) {
            throw new ForbiddenException("Seul le propriétaire de cet évènement peut le modifier ou le supprimer.");
        }
    }


    /**
     *
     * @param event event to perform the checks on
     * @return true if the event passed
     * @throws HttpException
     */
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
        if (description.length() > MAX_DESCRIPTION_LENGTH) {
            throw new BadRequestException("La description ne peut pas dépasser 10 000 caractères.");
        }

        Address location = event.getLocation();
        if (location == null) {
            throw new BadRequestException("L'évènement doit avoir une localisation.");
        }
        checkLocation(location);

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

        if (!start.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("La date de début doit être dans le futur.");
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
        if (maxCapacity > MAX_CAPACITY) {
            throw new BadRequestException("La capacité maximale ne peut pas dépasser 100 000 places.");
        }

        BigDecimal affiliatePrice = event.getAffiliatePrice();
        if (affiliatePrice == null) {
            throw new BadRequestException("Vous devez déterminer un prix pour les personnes affiliées au club.");
        }
        if (affiliatePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Le prix pour les personnes affiliées ne peut pas être négatif.");
        }
        if (affiliatePrice.compareTo(MAX_PRICE) > 0) {
            throw new BadRequestException("Le prix pour les personnes affiliées ne peut pas dépasser 99 999 999,99 €.");
        }

        BigDecimal nonAffiliatePrice = event.getNonAffiliatePrice();
        if (nonAffiliatePrice == null) {
            throw new BadRequestException("Vous devez déterminer un prix pour les personnes non affiliées au club.");
        }
        if (nonAffiliatePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("Le prix pour les personnes non affiliées ne peut pas être négatif.");
        }
        if (nonAffiliatePrice.compareTo(MAX_PRICE) > 0) {
            throw new BadRequestException("Le prix pour les personnes non affiliées ne peut pas dépasser 99 999 999,99 €.");
        }

        if (event.getStatus() == null) {
            throw new BadRequestException("Vous devez définir un statut pour l'évènement.");
        }

        return true;
    }

    /**
     * Garde-fous alignés sur les colonnes de la table address : un champ obligatoire
     * absent ou trop long partirait en base et reviendrait en erreur 500.
     */
    private void checkLocation(Address location) throws BadRequestException {
        requireLocationField(location.getStreet1(), 255, "L'adresse");
        if (location.getStreet2() != null && location.getStreet2().length() > 255) {
            throw new BadRequestException("Le complément d'adresse ne peut pas dépasser 255 caractères.");
        }
        requireLocationField(location.getPostalCode(), 10, "Le code postal");
        if (!location.getPostalCode().matches("\\d{4,5}")) {
            throw new BadRequestException("Le code postal doit être composé de 4 ou 5 chiffres.");
        }
        requireLocationField(location.getCity(), 100, "La ville");
        requireLocationField(location.getCountry(), 100, "Le pays");
    }

    private void requireLocationField(String value, int maxLength, String label) throws BadRequestException {
        if (value == null || value.isBlank()) {
            throw new BadRequestException(label + " est obligatoire.");
        }
        if (value.length() > maxLength) {
            throw new BadRequestException(label + " ne peut pas dépasser " + maxLength + " caractères.");
        }
    }

    /**
     * Un organisateur ne crée un évènement que pour un club dont il est membre ;
     * un administrateur peut choisir n'importe quel club.
     */
    private void checkClubMembership(AppUser organizer, Club club) throws ForbiddenException {
        if (organizer == null || organizer.getRole() == Role.ADMINISTRATOR) {
            return;
        }
        boolean isMember = organizer.getClubs().stream()
                .anyMatch(c -> c.getId() != null && c.getId().equals(club.getId()));
        if (!isMember) {
            throw new ForbiddenException("Vous ne pouvez créer un évènement que pour un club dont vous êtes membre.");
        }
    }

}
