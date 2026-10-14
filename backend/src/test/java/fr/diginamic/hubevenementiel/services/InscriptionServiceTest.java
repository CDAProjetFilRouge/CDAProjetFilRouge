package fr.diginamic.hubevenementiel.services;

import static fr.diginamic.hubevenementiel.TestIds.uuid;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.ForbiddenException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.context.ApplicationEventPublisher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InscriptionServiceTest {

    @Mock
    private InscriptionRepo inscriptionRepo;
    @Mock
    private EventRepo eventRepo;
    @Mock
    private AppUserService appUserService;
    @Mock
    private EventService eventService;
    @Mock
    private EmailService emailService;

    private InscriptionService inscriptionService;

    private AppUser user;
    private Event publishedFutureEvent;

    private static final AppUserPrincipal ADMIN = new AppUserPrincipal(uuid(1), "admin@example.com", "ADMINISTRATOR");

    @BeforeEach
    void setUp() {
        inscriptionService = new InscriptionService(inscriptionRepo, eventRepo, appUserService, eventService,
                emailService, new WaitingListService(eventRepo, inscriptionRepo, mock(ApplicationEventPublisher.class)));

        user = new AppUser();
        user.setId(uuid(6));
        user.setEmail("alice@example.com");
        user.setClubs(List.of());

        publishedFutureEvent = new Event();
        publishedFutureEvent.setId(uuid(2));
        publishedFutureEvent.setTitle("Marathon");
        publishedFutureEvent.setStatus(EventStatus.PUBLISHED);
        publishedFutureEvent.setStartDateTime(LocalDateTime.now().plusDays(5));
        publishedFutureEvent.setMaxCapacity(100);
        publishedFutureEvent.setAffiliatePrice(BigDecimal.TEN);
        publishedFutureEvent.setNonAffiliatePrice(BigDecimal.valueOf(20));
    }

    // ---------------------------------------------------------------
    // isEventFull
    // ---------------------------------------------------------------

    @Test
    void isEventFull_unknownEvent_throwsNotFound() {
        when(eventRepo.findByIdForUpdate(uuid(1))).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> inscriptionService.isEventFull(uuid(1)));
    }

    @Test
    void isEventFull_belowCapacity_returnsFalse() throws HttpException {
        publishedFutureEvent.setMaxCapacity(10);
        when(eventRepo.findByIdForUpdate(uuid(2))).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.countByEventIdAndStatus(uuid(2), InscriptionStatus.CONFIRMED)).thenReturn(9L);

        assertThat(inscriptionService.isEventFull(uuid(2))).isFalse();
    }

    @Test
    void isEventFull_exactlyAtCapacity_returnsTrue() throws HttpException {
        publishedFutureEvent.setMaxCapacity(10);
        when(eventRepo.findByIdForUpdate(uuid(2))).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.countByEventIdAndStatus(uuid(2), InscriptionStatus.CONFIRMED)).thenReturn(10L);

        assertThat(inscriptionService.isEventFull(uuid(2))).isTrue();
    }

    // ---------------------------------------------------------------
    // register
    // ---------------------------------------------------------------

    @Test
    void register_draftEvent_throwsConflict() throws HttpException {
        publishedFutureEvent.setStatus(EventStatus.DRAFT);
        when(appUserService.findById(uuid(6))).thenReturn(user);
        when(eventService.findById(uuid(2))).thenReturn(publishedFutureEvent);

        assertThrows(ConflictException.class, () -> inscriptionService.register(uuid(6), uuid(2)));
    }

    @Test
    void register_cancelledEvent_throwsConflict() throws HttpException {
        publishedFutureEvent.setStatus(EventStatus.CANCELLED);
        when(appUserService.findById(uuid(6))).thenReturn(user);
        when(eventService.findById(uuid(2))).thenReturn(publishedFutureEvent);

        assertThrows(ConflictException.class, () -> inscriptionService.register(uuid(6), uuid(2)));
    }

    @Test
    void register_finishedEvent_throwsConflict() throws HttpException {
        publishedFutureEvent.setStatus(EventStatus.FINISHED);
        when(appUserService.findById(uuid(6))).thenReturn(user);
        when(eventService.findById(uuid(2))).thenReturn(publishedFutureEvent);

        assertThrows(ConflictException.class, () -> inscriptionService.register(uuid(6), uuid(2)));
    }

    @Test
    void register_pastPublishedEvent_throwsConflict() throws HttpException {
        publishedFutureEvent.setStartDateTime(LocalDateTime.now().minusDays(1));
        when(appUserService.findById(uuid(6))).thenReturn(user);
        when(eventService.findById(uuid(2))).thenReturn(publishedFutureEvent);

        assertThrows(ConflictException.class, () -> inscriptionService.register(uuid(6), uuid(2)));
    }

    @Test
    void register_alreadyRegistered_throwsConflict() throws HttpException {
        when(appUserService.findById(uuid(6))).thenReturn(user);
        when(eventService.findById(uuid(2))).thenReturn(publishedFutureEvent);
        when(inscriptionRepo.existsByUserIdAndEventIdAndStatusNot(uuid(6), uuid(2), InscriptionStatus.CANCELED))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> inscriptionService.register(uuid(6), uuid(2)));
    }

    @Test
    void register_previouslyCancelledInscription_allowsReRegistration() throws HttpException {
        when(appUserService.findById(uuid(6))).thenReturn(user);
        when(eventService.findById(uuid(2))).thenReturn(publishedFutureEvent);
        when(inscriptionRepo.existsByUserIdAndEventIdAndStatusNot(uuid(6), uuid(2), InscriptionStatus.CANCELED))
                .thenReturn(false);
        when(eventRepo.findByIdForUpdate(uuid(2))).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.countByEventIdAndStatus(uuid(2), InscriptionStatus.CONFIRMED)).thenReturn(0L);
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> inscriptionService.register(uuid(6), uuid(2)));
    }

    @Test
    void register_affiliatedClubWithNullEndValidityDate_appliesAffiliatePrice() throws HttpException {
        Club club = new Club();
        club.setEndValidityDate(null);
        user.setClubs(List.of(club));
        mockSuccessfulRegisterPath();

        Inscription result = inscriptionService.register(uuid(6), uuid(2));

        assertThat(result.getPrice()).isEqualByComparingTo(publishedFutureEvent.getAffiliatePrice());
    }

    @Test
    void register_clubEndValidityDateInFuture_appliesAffiliatePrice() throws HttpException {
        Club club = new Club();
        club.setEndValidityDate(LocalDate.now().plusDays(1));
        user.setClubs(List.of(club));
        mockSuccessfulRegisterPath();

        Inscription result = inscriptionService.register(uuid(6), uuid(2));

        assertThat(result.getPrice()).isEqualByComparingTo(publishedFutureEvent.getAffiliatePrice());
    }

    @Test
    void register_clubEndValidityDateInPast_appliesNonAffiliatePrice() throws HttpException {
        Club club = new Club();
        club.setEndValidityDate(LocalDate.now().minusDays(1));
        user.setClubs(List.of(club));
        mockSuccessfulRegisterPath();

        Inscription result = inscriptionService.register(uuid(6), uuid(2));

        assertThat(result.getPrice()).isEqualByComparingTo(publishedFutureEvent.getNonAffiliatePrice());
    }

    @Test
    void register_noClub_appliesNonAffiliatePrice() throws HttpException {
        mockSuccessfulRegisterPath();

        Inscription result = inscriptionService.register(uuid(6), uuid(2));

        assertThat(result.getPrice()).isEqualByComparingTo(publishedFutureEvent.getNonAffiliatePrice());
    }

    @Test
    void register_eventNotFull_statusConfirmed() throws HttpException {
        mockSuccessfulRegisterPath();
        when(inscriptionRepo.countByEventIdAndStatus(uuid(2), InscriptionStatus.CONFIRMED)).thenReturn(0L);

        Inscription result = inscriptionService.register(uuid(6), uuid(2));

        assertThat(result.getStatus()).isEqualTo(InscriptionStatus.CONFIRMED);
    }

    @Test
    void register_eventExactlyFull_statusWaitingList() throws HttpException {
        publishedFutureEvent.setMaxCapacity(1);
        mockSuccessfulRegisterPath();
        when(inscriptionRepo.countByEventIdAndStatus(uuid(2), InscriptionStatus.CONFIRMED)).thenReturn(1L);

        Inscription result = inscriptionService.register(uuid(6), uuid(2));

        assertThat(result.getStatus()).isEqualTo(InscriptionStatus.WAITING_LIST);
    }

    // ---------------------------------------------------------------
    // contrôles d'identité (audit sécurité)
    // ---------------------------------------------------------------

    @Test
    void waitingListPosition_confirmedInscription_isNull() {
        Inscription confirmed = new Inscription();
        confirmed.setStatus(InscriptionStatus.CONFIRMED);

        assertThat(inscriptionService.waitingListPosition(confirmed)).isNull();
        verify(inscriptionRepo, never()).countAheadInQueue(any(UUID.class), any(), any(), any(UUID.class));
    }

    @Test
    void waitingListPosition_isOnePlusTheNumberOfWaitingInscriptionsAhead() {
        Inscription waiting = new Inscription();
        waiting.setId(uuid(30));
        waiting.setEvent(publishedFutureEvent);
        waiting.setStatus(InscriptionStatus.WAITING_LIST);
        waiting.setInscriptionDate(LocalDateTime.now());
        when(inscriptionRepo.countAheadInQueue(eq(uuid(2)), eq(InscriptionStatus.WAITING_LIST), any(), eq(uuid(30))))
                .thenReturn(2L);

        assertThat(inscriptionService.waitingListPosition(waiting)).isEqualTo(3);
    }

    @Test
    void waitingListPosition_firstInQueueIsOne() {
        Inscription waiting = new Inscription();
        waiting.setId(uuid(10));
        waiting.setEvent(publishedFutureEvent);
        waiting.setStatus(InscriptionStatus.WAITING_LIST);
        waiting.setInscriptionDate(LocalDateTime.now());
        when(inscriptionRepo.countAheadInQueue(eq(uuid(2)), eq(InscriptionStatus.WAITING_LIST), any(), eq(uuid(10))))
                .thenReturn(0L);

        assertThat(inscriptionService.waitingListPosition(waiting)).isEqualTo(1);
    }

    @Test
    void checkSelf_sameUser_passes() {
        assertDoesNotThrow(() -> inscriptionService.checkSelf(new AppUserPrincipal(uuid(6), "a@example.com", "MEMBER"), uuid(6)));
    }

    @Test
    void checkSelf_otherUser_throwsForbidden() {
        assertThrows(ForbiddenException.class,
                () -> inscriptionService.checkSelf(new AppUserPrincipal(uuid(7), "b@example.com", "MEMBER"), uuid(6)));
    }

    @Test
    void checkSelf_administratorCannotRegisterSomeoneElse() {
        assertThrows(ForbiddenException.class,
                () -> inscriptionService.checkSelf(new AppUserPrincipal(uuid(1), "admin@example.com", "ADMINISTRATOR"), uuid(6)));
    }

    @Test
    void findByUser_otherMember_throwsForbidden() {
        AppUserPrincipal other = new AppUserPrincipal(uuid(7), "b@example.com", "MEMBER");

        assertThrows(ForbiddenException.class, () -> inscriptionService.findByUser(other, uuid(6), 0, 20));
        verify(inscriptionRepo, never()).findByUserId(any(UUID.class), any());
    }

    @Test
    void findByUser_selfAndAdministrator_areAllowed() throws HttpException {
        when(inscriptionRepo.findByUserId(eq(uuid(6)), any())).thenReturn(org.springframework.data.domain.Page.empty());

        assertDoesNotThrow(() -> inscriptionService.findByUser(new AppUserPrincipal(uuid(6), "a@example.com", "MEMBER"), uuid(6), 0, 20));
        assertDoesNotThrow(() -> inscriptionService.findByUser(new AppUserPrincipal(uuid(1), "admin@example.com", "ADMINISTRATOR"), uuid(6), 0, 20));
    }

    @Test
    void findByUser_invalidPaging_throwsBadRequest() {
        AppUserPrincipal self = new AppUserPrincipal(uuid(6), "a@example.com", "MEMBER");

        assertThrows(BadRequestException.class, () -> inscriptionService.findByUser(self, uuid(6), -1, 20));
        assertThrows(BadRequestException.class, () -> inscriptionService.findByUser(self, uuid(6), 0, 0));
        assertThrows(BadRequestException.class, () -> inscriptionService.findByUser(self, uuid(6), 0, 201));
    }

    @Test
    void findByEvent_otherOrganizer_throwsForbidden() throws HttpException {
        AppUser owner = new AppUser();
        owner.setId(uuid(8));
        publishedFutureEvent.setOrganizer(owner);
        when(eventService.findById(uuid(2))).thenReturn(publishedFutureEvent);

        assertThrows(ForbiddenException.class,
                () -> inscriptionService.findByEvent(new AppUserPrincipal(uuid(9), "o@example.com", "ORGANIZER"), uuid(2), 0, 20));
        verify(inscriptionRepo, never()).findByEventId(any(UUID.class), any());
    }

    @Test
    void findByEvent_ownerAndAdministrator_areAllowed() throws HttpException {
        AppUser owner = new AppUser();
        owner.setId(uuid(8));
        publishedFutureEvent.setOrganizer(owner);
        when(eventService.findById(uuid(2))).thenReturn(publishedFutureEvent);
        when(inscriptionRepo.findByEventId(eq(uuid(2)), any())).thenReturn(org.springframework.data.domain.Page.empty());

        assertDoesNotThrow(() -> inscriptionService.findByEvent(new AppUserPrincipal(uuid(8), "o@example.com", "ORGANIZER"), uuid(2), 0, 20));
        assertDoesNotThrow(() -> inscriptionService.findByEvent(new AppUserPrincipal(uuid(1), "admin@example.com", "ADMINISTRATOR"), uuid(2), 0, 20));
    }

    @Test
    void register_unknownUser_throwsNotFound() throws HttpException {
        when(appUserService.findById(uuid(999))).thenThrow(new NotFoundException("User not found with id: 999"));

        assertThrows(NotFoundException.class, () -> inscriptionService.register(uuid(999), uuid(2)));
    }

    private void mockSuccessfulRegisterPath() throws HttpException {
        when(appUserService.findById(uuid(6))).thenReturn(user);
        when(eventService.findById(uuid(2))).thenReturn(publishedFutureEvent);
        when(inscriptionRepo.existsByUserIdAndEventIdAndStatusNot(uuid(6), uuid(2), InscriptionStatus.CANCELED))
                .thenReturn(false);
        when(eventRepo.findByIdForUpdate(uuid(2))).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------------------------------------------------------
    // cancelByMember / cancelByOrganizer
    // ---------------------------------------------------------------

    private Inscription confirmedInscription() {
        Inscription inscription = new Inscription();
        inscription.setId(uuid(1));
        inscription.setUser(user);
        inscription.setEvent(publishedFutureEvent);
        inscription.setStatus(InscriptionStatus.CONFIRMED);
        return inscription;
    }

    @Test
    void cancelByMember_alreadyCancelled_throwsConflict() {
        Inscription inscription = confirmedInscription();
        inscription.setStatus(InscriptionStatus.CANCELED);
        when(inscriptionRepo.findById(uuid(1))).thenReturn(Optional.of(inscription));
        AppUserPrincipal principal = new AppUserPrincipal(uuid(6), "alice@example.com", "MEMBER");

        assertThrows(ConflictException.class, () -> inscriptionService.cancelByMember(uuid(1), principal));
    }

    @Test
    void cancelByMember_confirmedInscription_promotesNextInWaitingList() throws HttpException {
        Inscription inscription = confirmedInscription();
        when(inscriptionRepo.findById(uuid(1))).thenReturn(Optional.of(inscription));
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepo.findByIdForUpdate(uuid(2))).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.findByEventIdAndStatusOrderByInscriptionDateAsc(uuid(2), InscriptionStatus.WAITING_LIST))
                .thenReturn(List.of());
        AppUserPrincipal principal = new AppUserPrincipal(uuid(6), "alice@example.com", "MEMBER");

        inscriptionService.cancelByMember(uuid(1), principal);

        verify(eventRepo).findByIdForUpdate(uuid(2));
    }

    @Test
    void cancelByMember_waitingListInscription_doesNotTriggerPromotion() throws HttpException {
        Inscription inscription = confirmedInscription();
        inscription.setStatus(InscriptionStatus.WAITING_LIST);
        when(inscriptionRepo.findById(uuid(1))).thenReturn(Optional.of(inscription));
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppUserPrincipal principal = new AppUserPrincipal(uuid(6), "alice@example.com", "MEMBER");

        inscriptionService.cancelByMember(uuid(1), principal);

        verify(eventRepo, never()).findByIdForUpdate(any(UUID.class));
    }

    @Test
    void cancelByOrganizer_foreignOrganizer_throwsForbiddenAndSendsNoEmail() throws HttpException {
        AppUser owner = new AppUser();
        owner.setId(uuid(8));
        publishedFutureEvent.setOrganizer(owner);
        Inscription inscription = confirmedInscription();
        when(inscriptionRepo.findById(uuid(1))).thenReturn(Optional.of(inscription));

        assertThrows(ForbiddenException.class, () -> inscriptionService.cancelByOrganizer(uuid(1), "Motif",
                new AppUserPrincipal(uuid(9), "o@example.com", "ORGANIZER")));

        assertThat(inscription.getStatus()).isEqualTo(InscriptionStatus.CONFIRMED);
        verify(inscriptionRepo, never()).save(any(Inscription.class));
        verify(emailService, never()).sendInscriptionCancellationEmail(anyString(), anyString(), anyString());
    }

    @Test
    void cancelByOrganizer_ownerIsAllowed() throws HttpException {
        AppUser owner = new AppUser();
        owner.setId(uuid(8));
        publishedFutureEvent.setOrganizer(owner);
        Inscription inscription = confirmedInscription();
        when(inscriptionRepo.findById(uuid(1))).thenReturn(Optional.of(inscription));
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepo.findByIdForUpdate(uuid(2))).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.findByEventIdAndStatusOrderByInscriptionDateAsc(uuid(2), InscriptionStatus.WAITING_LIST))
                .thenReturn(List.of());

        Inscription result = inscriptionService.cancelByOrganizer(uuid(1), "Motif",
                new AppUserPrincipal(uuid(8), "o@example.com", "ORGANIZER"));

        assertThat(result.getStatus()).isEqualTo(InscriptionStatus.CANCELED);
    }

    @Test
    void cancelByOrganizer_blankMotif_throwsBadRequest() throws HttpException {
        assertThrows(BadRequestException.class, () -> inscriptionService.cancelByOrganizer(uuid(1), " ", ADMIN));
        verify(inscriptionRepo, never()).findById(any(UUID.class));
    }

    @Test
    void cancelByOrganizer_nullMotif_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> inscriptionService.cancelByOrganizer(uuid(1), null, ADMIN));
    }

    @Test
    void cancelByOrganizer_storesMotifInCancelObject() throws HttpException {
        Inscription inscription = confirmedInscription();
        when(inscriptionRepo.findById(uuid(1))).thenReturn(Optional.of(inscription));
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepo.findByIdForUpdate(uuid(2))).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.findByEventIdAndStatusOrderByInscriptionDateAsc(uuid(2), InscriptionStatus.WAITING_LIST))
                .thenReturn(List.of());

        Inscription result = inscriptionService.cancelByOrganizer(uuid(1), "Évènement annulé faute de participants", ADMIN);

        assertThat(result.getCancelObject()).isEqualTo("Évènement annulé faute de participants");
    }

    @Test
    void cancelByOrganizer_sendsCancellationEmail() throws HttpException {
        Inscription inscription = confirmedInscription();
        when(inscriptionRepo.findById(uuid(1))).thenReturn(Optional.of(inscription));
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepo.findByIdForUpdate(uuid(2))).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.findByEventIdAndStatusOrderByInscriptionDateAsc(uuid(2), InscriptionStatus.WAITING_LIST))
                .thenReturn(List.of());

        inscriptionService.cancelByOrganizer(uuid(1), "Motif", ADMIN);

        verify(emailService).sendInscriptionCancellationEmail(eq(user.getEmail()),
                eq(publishedFutureEvent.getTitle()), eq("Motif"));
    }

    // Bug documente : canceledById n'est jamais reellement renseigne (Inscription.cancel
    // passe toujours null quel que soit l'appelant). Ce test prouve le comportement actuel.
    @Test
    void cancelByOrganizer_canceledByIdIsNeverActuallySet() throws HttpException {
        Inscription inscription = confirmedInscription();
        when(inscriptionRepo.findById(uuid(1))).thenReturn(Optional.of(inscription));
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepo.findByIdForUpdate(uuid(2))).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.findByEventIdAndStatusOrderByInscriptionDateAsc(uuid(2), InscriptionStatus.WAITING_LIST))
                .thenReturn(List.of());

        Inscription result = inscriptionService.cancelByOrganizer(uuid(1), "Motif", ADMIN);

        assertThat(result.getCanceledById()).isNull();
    }

}
