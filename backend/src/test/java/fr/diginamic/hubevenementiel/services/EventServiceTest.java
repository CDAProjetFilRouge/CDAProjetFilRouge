package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.entities.Address;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.ForbiddenException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventInscriptionCount;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepo eventRepository;

    @Mock
    private InscriptionRepo inscriptionRepo;

    @Mock
    private WaitingListService waitingListService;

    @Mock
    private ClubService clubService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private EventService eventService;

    private Event validEvent;
    private AppUser organizer;

    @BeforeEach
    void setUp() {
        organizer = new AppUser();
        organizer.setId(8L);

        validEvent = new Event();
        validEvent.setTitle("Marathon de Rennes");
        validEvent.setDescription("Une belle course.");
        Address address = new Address();
        address.setStreet1("12 rue de la Paix");
        address.setPostalCode("75002");
        address.setCity("Paris");
        address.setCountry("France");
        validEvent.setLocation(address);
        validEvent.setCategory(Category.SPORT);
        validEvent.setStartDateTime(LocalDateTime.now().plusDays(10));
        validEvent.setEndDateTime(LocalDateTime.now().plusDays(10).plusHours(3));
        validEvent.setMaxCapacity(100);
        validEvent.setAffiliatePrice(BigDecimal.TEN);
        validEvent.setNonAffiliatePrice(BigDecimal.valueOf(20));
        validEvent.setStatus(EventStatus.DRAFT);
        validEvent.setOrganizer(organizer);
    }

    // ---------------------------------------------------------------
    // eventChecker
    // ---------------------------------------------------------------

    @Test
    void eventChecker_nullEvent_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(null));
    }

    @Test
    void eventChecker_blankTitle_throwsBadRequest() {
        validEvent.setTitle(" ");
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_titleTooLong_throwsBadRequest() {
        validEvent.setTitle("a".repeat(201));
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_titleExactly200Chars_passes() throws HttpException {
        validEvent.setTitle("a".repeat(200));
        assertThat(eventService.eventChecker(validEvent)).isTrue();
    }

    @Test
    void eventChecker_nullDescription_throwsBadRequest() {
        validEvent.setDescription(null);
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_nullLocation_throwsBadRequest() {
        validEvent.setLocation(null);
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_nullCategory_throwsBadRequest() {
        validEvent.setCategory(null);
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_nullStartDate_throwsBadRequest() {
        validEvent.setStartDateTime(null);
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_nullEndDate_throwsBadRequest() {
        validEvent.setEndDateTime(null);
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_endDateEqualsStartDate_throwsBadRequest() {
        LocalDateTime same = LocalDateTime.now().plusDays(1);
        validEvent.setStartDateTime(same);
        validEvent.setEndDateTime(same);
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_endDateOneSecondAfterStart_passes() throws HttpException {
        LocalDateTime start = LocalDateTime.now().plusDays(1);
        validEvent.setStartDateTime(start);
        validEvent.setEndDateTime(start.plusSeconds(1));
        assertThat(eventService.eventChecker(validEvent)).isTrue();
    }

    @Test
    void eventChecker_nullMaxCapacity_throwsBadRequest() {
        validEvent.setMaxCapacity(null);
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_zeroMaxCapacity_throwsBadRequest() {
        validEvent.setMaxCapacity(0);
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_negativeMaxCapacity_throwsBadRequest() {
        validEvent.setMaxCapacity(-1);
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_negativeAffiliatePrice_throwsBadRequest() {
        validEvent.setAffiliatePrice(BigDecimal.valueOf(-1));
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_zeroAffiliatePrice_passes() throws HttpException {
        validEvent.setAffiliatePrice(BigDecimal.ZERO);
        assertThat(eventService.eventChecker(validEvent)).isTrue();
    }

    @Test
    void eventChecker_negativeNonAffiliatePrice_throwsBadRequest() {
        validEvent.setNonAffiliatePrice(BigDecimal.valueOf(-1));
        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    // ---------------------------------------------------------------
    // eventChecker : règles décidées avec le PO (places, code postal, date passée)
    // ---------------------------------------------------------------

    @Test
    void eventChecker_capacityAboveLimit_throwsBadRequest() {
        validEvent.setMaxCapacity(100_001);

        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_capacityAtLimit_isAccepted() throws HttpException {
        validEvent.setMaxCapacity(100_000);

        assertThat(eventService.eventChecker(validEvent)).isTrue();
    }

    @Test
    void eventChecker_postalCodeWithLetters_throwsBadRequest() {
        validEvent.getLocation().setPostalCode("AB12");

        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_postalCodeWithSixDigits_throwsBadRequest() {
        validEvent.getLocation().setPostalCode("750020");

        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_postalCodeWithFourDigits_isAccepted() throws HttpException {
        validEvent.getLocation().setPostalCode("1000");

        assertThat(eventService.eventChecker(validEvent)).isTrue();
    }

    @Test
    void eventChecker_startInThePast_throwsBadRequest() {
        validEvent.setStartDateTime(LocalDateTime.now().minusHours(1));
        validEvent.setEndDateTime(LocalDateTime.now().plusHours(1));

        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void createEvent_startInThePast_isRejectedAndNothingIsSaved() {
        validEvent.setStartDateTime(LocalDateTime.now().minusDays(1));
        validEvent.setEndDateTime(LocalDateTime.now().plusDays(1));

        assertThrows(BadRequestException.class, () -> eventService.createEvent(validEvent, 1L));

        verify(eventRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // eventChecker : longueurs alignées sur les colonnes
    // ---------------------------------------------------------------

    @Test
    void eventChecker_descriptionTooLong_throwsBadRequest() {
        validEvent.setDescription("a".repeat(10_001));

        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_descriptionAtLimit_isAccepted() throws HttpException {
        validEvent.setDescription("a".repeat(10_000));

        assertThat(eventService.eventChecker(validEvent)).isTrue();
    }

    @Test
    void eventChecker_cityTooLong_throwsBadRequest() {
        validEvent.getLocation().setCity("v".repeat(101));

        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_missingStreet_throwsBadRequest() {
        validEvent.getLocation().setStreet1(" ");

        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    // ---------------------------------------------------------------
    // eventChecker : plafond des tarifs (decimal(10,2))
    // ---------------------------------------------------------------

    @Test
    void eventChecker_affiliatePriceAboveColumnLimit_throwsBadRequest() {
        validEvent.setAffiliatePrice(new BigDecimal("100000000"));

        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_nonAffiliatePriceAboveColumnLimit_throwsBadRequest() {
        validEvent.setNonAffiliatePrice(new BigDecimal("100000000"));

        assertThrows(BadRequestException.class, () -> eventService.eventChecker(validEvent));
    }

    @Test
    void eventChecker_priceAtColumnLimit_isAccepted() throws HttpException {
        validEvent.setAffiliatePrice(new BigDecimal("99999999.99"));
        validEvent.setNonAffiliatePrice(new BigDecimal("99999999.99"));

        assertThat(eventService.eventChecker(validEvent)).isTrue();
    }

    // ---------------------------------------------------------------
    // createEvent
    // ---------------------------------------------------------------

    @Test
    void createEvent_forcesDraftStatusRegardlessOfInput() throws HttpException {
        Long clubId = 1L;
        Club club = new Club();
        club.setId(clubId);
        organizer.setClubs(new ArrayList<>(List.of(club)));
        when(clubService.findById(clubId)).thenReturn(club);

        validEvent.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.existsByTitle(anyString())).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = eventService.createEvent(validEvent, clubId);

        assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void createEvent_duplicateTitle_throwsConflict() throws HttpException {
        Long clubId = 1L;
        Club club = new Club();
        club.setId(clubId);
        organizer.setClubs(new ArrayList<>(List.of(club)));
        when(clubService.findById(clubId)).thenReturn(club);

        when(eventRepository.existsByTitle(validEvent.getTitle())).thenReturn(true);

        assertThrows(ConflictException.class, () -> eventService.createEvent(validEvent, clubId));

        verify(eventRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // createEvent : l'organisateur doit appartenir au club
    // ---------------------------------------------------------------

    @Test
    void createEvent_organizerNotMemberOfTheClub_throwsForbidden() throws HttpException {
        Club ownClub = new Club();
        ownClub.setId(4L);
        Club otherClub = new Club();
        otherClub.setId(9L);
        organizer.setClubs(new ArrayList<>(List.of(ownClub)));
        when(clubService.findById(9L)).thenReturn(otherClub);

        assertThrows(ForbiddenException.class, () -> eventService.createEvent(validEvent, 9L));

        verify(eventRepository, never()).save(any());
    }

    @Test
    void createEvent_organizerWithoutAnyClub_throwsForbidden() throws HttpException {
        Club club = new Club();
        club.setId(4L);
        organizer.setClubs(new ArrayList<>());
        when(clubService.findById(4L)).thenReturn(club);

        assertThrows(ForbiddenException.class, () -> eventService.createEvent(validEvent, 4L));
    }

    @Test
    void createEvent_administratorCanChooseAnyClub() throws HttpException {
        Club club = new Club();
        club.setId(9L);
        organizer.setRole(Role.ADMINISTRATOR);
        organizer.setClubs(new ArrayList<>());
        when(clubService.findById(9L)).thenReturn(club);
        when(eventRepository.existsByTitle(anyString())).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = eventService.createEvent(validEvent, 9L);

        assertThat(result.getClub()).isSameAs(club);
    }

    // ---------------------------------------------------------------
    // findOrganizedBy : « mes évènements »
    // ---------------------------------------------------------------

    @Test
    void findOrganizedBy_sizeAboveTwenty_throwsBadRequest() {
        AppUserPrincipal principal = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        assertThrows(BadRequestException.class, () -> eventService.findOrganizedBy(0, 21, null, principal));

        verify(eventRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void findOrganizedBy_sortsByStartDateDescending() throws HttpException {
        AppUserPrincipal principal = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");
        when(eventRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(validEvent)));

        Page<Event> result = eventService.findOrganizedBy(2, 10, EventStatus.DRAFT, principal);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(eventRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(10);
        assertThat(pageable.getValue().getSort().getOrderFor("startDateTime").getDirection())
                .isEqualTo(Sort.Direction.DESC);
        assertThat(result.getContent()).containsExactly(validEvent);
    }

    // ---------------------------------------------------------------
    // updateEvent / deleteEvent : id inconnu
    // ---------------------------------------------------------------

    @Test
    void updateEvent_unknownId_throwsNotFound() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());
        AppUserPrincipal principal = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        assertThrows(NotFoundException.class, () -> eventService.updateEvent(1L, validEvent, principal));
    }

    @Test
    void deleteEvent_unknownId_throwsNotFound() {
        when(eventRepository.findById(1L)).thenReturn(Optional.empty());
        AppUserPrincipal principal = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        assertThrows(NotFoundException.class, () -> eventService.deleteEvent(1L, principal));
    }

    @Test
    void updateEvent_titleUnchangedCaseInsensitive_doesNotCheckDuplicate() throws HttpException {
        validEvent.setStartDateTime(LocalDateTime.now().plusDays(1));
        validEvent.setEndDateTime(LocalDateTime.now().plusDays(1).plusHours(1));
        Event existing = cloneFutureEvent();
        existing.setTitle(validEvent.getTitle().toUpperCase());
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppUserPrincipal principal = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        eventService.updateEvent(1L, validEvent, principal);

        verify(eventRepository, never()).existsByTitle(anyString());
    }

    // ---------------------------------------------------------------
    // RG15 : checkOwnership (updateEvent / deleteEvent)
    // ---------------------------------------------------------------

    @Test
    void updateEvent_ownerPrincipal_isAllowed() throws HttpException {
        Event existing = cloneFutureEvent();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppUserPrincipal owner = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        assertDoesNotThrow(() -> eventService.updateEvent(1L, validEvent, owner));
    }

    @Test
    void updateEvent_adminPrincipal_isAllowedEvenIfNotOwner() throws HttpException {
        Event existing = cloneFutureEvent();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppUserPrincipal admin = new AppUserPrincipal(99L, "emma@example.com", "ADMINISTRATOR");

        assertDoesNotThrow(() -> eventService.updateEvent(1L, validEvent, admin));
    }

    @Test
    void updateEvent_nonOwnerOrganizer_throwsForbidden() {
        Event existing = cloneFutureEvent();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        AppUserPrincipal stranger = new AppUserPrincipal(42L, "bob@example.com", "ORGANIZER");

        assertThrows(ForbiddenException.class, () -> eventService.updateEvent(1L, validEvent, stranger));
        verify(eventRepository, never()).save(any());
    }

    @Test
    void deleteEvent_nonOwnerOrganizer_throwsForbidden() {
        Event existing = cloneFutureEvent();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        AppUserPrincipal stranger = new AppUserPrincipal(42L, "bob@example.com", "ORGANIZER");

        assertThrows(ForbiddenException.class, () -> eventService.deleteEvent(1L, stranger));
        verify(eventRepository, never()).delete(any(Event.class));
    }

    // ---------------------------------------------------------------
    // RG16 : updateEvent bloqué si l'évènement est déjà passé
    // ---------------------------------------------------------------

    @Test
    void updateEvent_pastEvent_throwsConflictEvenForOwner() {
        Event existing = new Event();
        existing.setOrganizer(organizer);
        existing.setStartDateTime(LocalDateTime.now().minusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        AppUserPrincipal owner = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        assertThrows(ConflictException.class, () -> eventService.updateEvent(1L, validEvent, owner));
        verify(eventRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // RG17 : deleteEvent bloqué si l'évènement n'est plus futur
    // ---------------------------------------------------------------

    @Test
    void deleteEvent_pastEvent_throwsConflict() {
        Event existing = new Event();
        existing.setOrganizer(organizer);
        existing.setStartDateTime(LocalDateTime.now().minusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        AppUserPrincipal owner = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        assertThrows(ConflictException.class, () -> eventService.deleteEvent(1L, owner));
        verify(eventRepository, never()).delete(any(Event.class));
    }

    @Test
    void deleteEvent_futureEvent_deletesSuccessfully() throws HttpException {
        Event existing = cloneFutureEvent();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        AppUserPrincipal owner = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        eventService.deleteEvent(1L, owner);

        verify(eventRepository).delete(existing);
    }

    @Test
    void deleteEvent_withRegistrations_notifiesEveryoneAndRemovesTheirInscriptions() throws HttpException {
        Event existing = cloneFutureEvent();
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        AppUser confirmed = new AppUser();
        confirmed.setEmail("confirme@example.com");
        AppUser waiting = new AppUser();
        waiting.setEmail("attente@example.com");
        when(inscriptionRepo.findByEventIdAndStatusIn(1L, List.of(InscriptionStatus.CONFIRMED, InscriptionStatus.WAITING_LIST)))
                .thenReturn(List.of(
                        new Inscription(confirmed, existing, BigDecimal.TEN, InscriptionStatus.CONFIRMED),
                        new Inscription(waiting, existing, BigDecimal.TEN, InscriptionStatus.WAITING_LIST)));
        AppUserPrincipal owner = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        eventService.deleteEvent(1L, owner);

        verify(inscriptionRepo).deleteByEventId(1L);
        ArgumentCaptor<EventDeletedEvent> captor = ArgumentCaptor.forClass(EventDeletedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue().emails()).containsExactly("confirme@example.com", "attente@example.com");
        assertThat(captor.getValue().eventTitle()).isEqualTo(existing.getTitle());
    }

    @Test
    void deleteEvent_pastEvent_notifiesNobody() {
        Event past = cloneFutureEvent();
        past.setStartDateTime(LocalDateTime.now().minusDays(1));
        when(eventRepository.findById(1L)).thenReturn(Optional.of(past));
        AppUserPrincipal owner = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        assertThrows(ConflictException.class, () -> eventService.deleteEvent(1L, owner));

        verify(inscriptionRepo, never()).deleteByEventId(any());
        verify(eventPublisher, never()).publishEvent(any(Object.class));
    }

    // ---------------------------------------------------------------
    // RG14/RG15 : findVisibleById
    // ---------------------------------------------------------------

    @Test
    void findVisibleById_draftEvent_visibleToOwner() throws HttpException {
        Event draft = cloneFutureEvent();
        draft.setStatus(EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(draft));
        AppUserPrincipal owner = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        assertThat(eventService.findVisibleById(1L, owner)).isEqualTo(draft);
    }

    @Test
    void findVisibleById_draftEvent_visibleToAdmin() throws HttpException {
        Event draft = cloneFutureEvent();
        draft.setStatus(EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(draft));
        AppUserPrincipal admin = new AppUserPrincipal(99L, "emma@example.com", "ADMINISTRATOR");

        assertThat(eventService.findVisibleById(1L, admin)).isEqualTo(draft);
    }

    @Test
    void findVisibleById_draftEvent_hiddenFromStranger_throwsNotFound() {
        Event draft = cloneFutureEvent();
        draft.setStatus(EventStatus.DRAFT);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(draft));
        AppUserPrincipal stranger = new AppUserPrincipal(42L, "bob@example.com", "MEMBER");

        assertThrows(NotFoundException.class, () -> eventService.findVisibleById(1L, stranger));
    }

    @Test
    void findVisibleById_publishedEvent_visibleToEveryone() throws HttpException {
        Event published = cloneFutureEvent();
        published.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(published));
        AppUserPrincipal stranger = new AppUserPrincipal(42L, "bob@example.com", "MEMBER");

        assertThat(eventService.findVisibleById(1L, stranger)).isEqualTo(published);
    }

    // ---------------------------------------------------------------
    // search / filtres (validations en amont de la Specification)
    // ---------------------------------------------------------------

    @Test
    void search_startAfterEnd_throwsBadRequest() {
        AppUserPrincipal principal = new AppUserPrincipal(1L, "a@example.com", "MEMBER");
        LocalDateTime start = LocalDateTime.now().plusDays(2);
        LocalDateTime end = LocalDateTime.now().plusDays(1);

        assertThrows(BadRequestException.class,
                () -> eventService.search(0, 20, null, start, end, null, null, null, principal));
    }

    @Test
    void search_minPriceGreaterThanMaxPrice_throwsBadRequest() {
        AppUserPrincipal principal = new AppUserPrincipal(1L, "a@example.com", "MEMBER");

        assertThrows(BadRequestException.class,
                () -> eventService.search(0, 20, null, null, null, 50, 10, null, principal));
    }

    // ---------------------------------------------------------------
    // remainingSpots
    // ---------------------------------------------------------------

    @Test
    void remainingSpots_subtractsConfirmedInscriptionsFromTheCapacity() {
        Event busy = eventWith(1L, 10);
        Event empty = eventWith(2L, 5);
        when(inscriptionRepo.countByEventIdsAndStatus(List.of(1L, 2L), InscriptionStatus.CONFIRMED))
                .thenReturn(List.of(count(1L, 3L)));

        Map<Long, Integer> result = eventService.remainingSpots(List.of(busy, empty));

        assertThat(result).containsEntry(1L, 7).containsEntry(2L, 5);
    }

    @Test
    void remainingSpots_neverGoesBelowZero() {
        Event overbooked = eventWith(1L, 2);
        when(inscriptionRepo.countByEventIdsAndStatus(List.of(1L), InscriptionStatus.CONFIRMED))
                .thenReturn(List.of(count(1L, 3L)));

        Map<Long, Integer> result = eventService.remainingSpots(List.of(overbooked));

        assertThat(result).containsEntry(1L, 0);
    }

    @Test
    void remainingSpots_noEvents_returnsEmptyMapWithoutQuerying() {
        Map<Long, Integer> result = eventService.remainingSpots(List.of());

        assertThat(result).isEmpty();
        verify(inscriptionRepo, never()).countByEventIdsAndStatus(any(), any());
    }

    private Event eventWith(Long id, int maxCapacity) {
        Event event = new Event();
        event.setId(id);
        event.setMaxCapacity(maxCapacity);
        return event;
    }

    private EventInscriptionCount count(Long eventId, Long total) {
        return new EventInscriptionCount() {
            @Override
            public Long getEventId() {
                return eventId;
            }

            @Override
            public Long getTotal() {
                return total;
            }
        };
    }

    // ---------------------------------------------------------------
    // updateEvent : capacité augmentée -> liste d'attente promue (US-303)
    // ---------------------------------------------------------------

    @Test
    void updateEvent_capacityRaised_fillsFreeSpotsFromWaitingList() throws HttpException {
        Event existing = cloneFutureEvent();
        existing.setMaxCapacity(10);
        validEvent.setMaxCapacity(15);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppUserPrincipal owner = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        eventService.updateEvent(1L, validEvent, owner);

        verify(waitingListService).fillFreeSpots(1L);
    }

    @Test
    void updateEvent_capacityUnchanged_doesNotTouchWaitingList() throws HttpException {
        Event existing = cloneFutureEvent();
        existing.setMaxCapacity(100);
        validEvent.setMaxCapacity(100);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppUserPrincipal owner = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        eventService.updateEvent(1L, validEvent, owner);

        verify(waitingListService, never()).fillFreeSpots(any());
    }

    @Test
    void updateEvent_capacityLowered_doesNotTouchWaitingList() throws HttpException {
        Event existing = cloneFutureEvent();
        existing.setMaxCapacity(100);
        validEvent.setMaxCapacity(50);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppUserPrincipal owner = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        eventService.updateEvent(1L, validEvent, owner);

        verify(waitingListService, never()).fillFreeSpots(any());
    }

    @Test
    void updateEvent_capacityBelowConfirmedCount_throwsConflict() {
        Event existing = cloneFutureEvent();
        existing.setMaxCapacity(20);
        validEvent.setMaxCapacity(5);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(inscriptionRepo.countByEventIdAndStatus(1L, InscriptionStatus.CONFIRMED)).thenReturn(12L);
        AppUserPrincipal owner = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        assertThrows(ConflictException.class, () -> eventService.updateEvent(1L, validEvent, owner));

        verify(eventRepository, never()).save(any(Event.class));
    }

    @Test
    void updateEvent_capacityEqualToConfirmedCount_isAccepted() throws HttpException {
        Event existing = cloneFutureEvent();
        existing.setMaxCapacity(20);
        validEvent.setMaxCapacity(12);
        when(eventRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inscriptionRepo.countByEventIdAndStatus(1L, InscriptionStatus.CONFIRMED)).thenReturn(12L);
        AppUserPrincipal owner = new AppUserPrincipal(8L, "david@example.com", "ORGANIZER");

        Event result = eventService.updateEvent(1L, validEvent, owner);

        assertThat(result.getMaxCapacity()).isEqualTo(12);
    }

    private Event cloneFutureEvent() {
        Event event = new Event();
        event.setOrganizer(organizer);
        event.setTitle(validEvent.getTitle());
        event.setStartDateTime(LocalDateTime.now().plusDays(5));
        event.setEndDateTime(LocalDateTime.now().plusDays(5).plusHours(2));
        event.setStatus(EventStatus.PUBLISHED);
        return event;
    }
}
