package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Address;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.Category;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.ForbiddenException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        validEvent.setLocation(new Address());
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
    // createEvent
    // ---------------------------------------------------------------

    @Test
    void createEvent_forcesDraftStatusRegardlessOfInput() throws HttpException {
        validEvent.setStatus(EventStatus.PUBLISHED);
        when(eventRepository.existsByTitle(anyString())).thenReturn(false);
        when(eventRepository.save(any(Event.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Event result = eventService.createEvent(validEvent);

        assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test
    void createEvent_duplicateTitle_throwsConflict() {
        when(eventRepository.existsByTitle(validEvent.getTitle())).thenReturn(true);

        assertThrows(ConflictException.class, () -> eventService.createEvent(validEvent));

        verify(eventRepository, never()).save(any());
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
