package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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

    @InjectMocks
    private InscriptionService inscriptionService;

    private AppUser user;
    private Event publishedFutureEvent;

    @BeforeEach
    void setUp() {
        user = new AppUser();
        user.setId(6L);
        user.setEmail("alice@example.com");
        user.setClubs(List.of());

        publishedFutureEvent = new Event();
        publishedFutureEvent.setId(2L);
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
        when(eventRepo.findByIdForUpdate(1L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> inscriptionService.isEventFull(1L));
    }

    @Test
    void isEventFull_belowCapacity_returnsFalse() throws HttpException {
        publishedFutureEvent.setMaxCapacity(10);
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.countByEventIdAndStatus(2L, InscriptionStatus.CONFIRMED)).thenReturn(9L);

        assertThat(inscriptionService.isEventFull(2L)).isFalse();
    }

    @Test
    void isEventFull_exactlyAtCapacity_returnsTrue() throws HttpException {
        publishedFutureEvent.setMaxCapacity(10);
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.countByEventIdAndStatus(2L, InscriptionStatus.CONFIRMED)).thenReturn(10L);

        assertThat(inscriptionService.isEventFull(2L)).isTrue();
    }

    // ---------------------------------------------------------------
    // register
    // ---------------------------------------------------------------

    @Test
    void register_draftEvent_throwsConflict() throws HttpException {
        publishedFutureEvent.setStatus(EventStatus.DRAFT);
        when(appUserService.findById(6L)).thenReturn(user);
        when(eventService.findById(2L)).thenReturn(publishedFutureEvent);

        assertThrows(ConflictException.class, () -> inscriptionService.register(6L, 2L));
    }

    @Test
    void register_cancelledEvent_throwsConflict() throws HttpException {
        publishedFutureEvent.setStatus(EventStatus.CANCELLED);
        when(appUserService.findById(6L)).thenReturn(user);
        when(eventService.findById(2L)).thenReturn(publishedFutureEvent);

        assertThrows(ConflictException.class, () -> inscriptionService.register(6L, 2L));
    }

    @Test
    void register_finishedEvent_throwsConflict() throws HttpException {
        publishedFutureEvent.setStatus(EventStatus.FINISHED);
        when(appUserService.findById(6L)).thenReturn(user);
        when(eventService.findById(2L)).thenReturn(publishedFutureEvent);

        assertThrows(ConflictException.class, () -> inscriptionService.register(6L, 2L));
    }

    @Test
    void register_pastPublishedEvent_throwsConflict() throws HttpException {
        publishedFutureEvent.setStartDateTime(LocalDateTime.now().minusDays(1));
        when(appUserService.findById(6L)).thenReturn(user);
        when(eventService.findById(2L)).thenReturn(publishedFutureEvent);

        assertThrows(ConflictException.class, () -> inscriptionService.register(6L, 2L));
    }

    @Test
    void register_alreadyRegistered_throwsConflict() throws HttpException {
        when(appUserService.findById(6L)).thenReturn(user);
        when(eventService.findById(2L)).thenReturn(publishedFutureEvent);
        when(inscriptionRepo.existsByUserIdAndEventIdAndStatusNot(6L, 2L, InscriptionStatus.CANCELED))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> inscriptionService.register(6L, 2L));
    }

    @Test
    void register_previouslyCancelledInscription_allowsReRegistration() throws HttpException {
        when(appUserService.findById(6L)).thenReturn(user);
        when(eventService.findById(2L)).thenReturn(publishedFutureEvent);
        when(inscriptionRepo.existsByUserIdAndEventIdAndStatusNot(6L, 2L, InscriptionStatus.CANCELED))
                .thenReturn(false);
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.countByEventIdAndStatus(2L, InscriptionStatus.CONFIRMED)).thenReturn(0L);
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertDoesNotThrow(() -> inscriptionService.register(6L, 2L));
    }

    @Test
    void register_affiliatedClubWithNullEndValidityDate_appliesAffiliatePrice() throws HttpException {
        Club club = new Club();
        club.setEndValidityDate(null);
        user.setClubs(List.of(club));
        mockSuccessfulRegisterPath();

        Inscription result = inscriptionService.register(6L, 2L);

        assertThat(result.getPrice()).isEqualByComparingTo(publishedFutureEvent.getAffiliatePrice());
    }

    @Test
    void register_clubEndValidityDateInFuture_appliesAffiliatePrice() throws HttpException {
        Club club = new Club();
        club.setEndValidityDate(LocalDate.now().plusDays(1));
        user.setClubs(List.of(club));
        mockSuccessfulRegisterPath();

        Inscription result = inscriptionService.register(6L, 2L);

        assertThat(result.getPrice()).isEqualByComparingTo(publishedFutureEvent.getAffiliatePrice());
    }

    @Test
    void register_clubEndValidityDateInPast_appliesNonAffiliatePrice() throws HttpException {
        Club club = new Club();
        club.setEndValidityDate(LocalDate.now().minusDays(1));
        user.setClubs(List.of(club));
        mockSuccessfulRegisterPath();

        Inscription result = inscriptionService.register(6L, 2L);

        assertThat(result.getPrice()).isEqualByComparingTo(publishedFutureEvent.getNonAffiliatePrice());
    }

    @Test
    void register_noClub_appliesNonAffiliatePrice() throws HttpException {
        mockSuccessfulRegisterPath();

        Inscription result = inscriptionService.register(6L, 2L);

        assertThat(result.getPrice()).isEqualByComparingTo(publishedFutureEvent.getNonAffiliatePrice());
    }

    @Test
    void register_eventNotFull_statusConfirmed() throws HttpException {
        mockSuccessfulRegisterPath();
        when(inscriptionRepo.countByEventIdAndStatus(2L, InscriptionStatus.CONFIRMED)).thenReturn(0L);

        Inscription result = inscriptionService.register(6L, 2L);

        assertThat(result.getStatus()).isEqualTo(InscriptionStatus.CONFIRMED);
    }

    @Test
    void register_eventExactlyFull_statusWaitingList() throws HttpException {
        publishedFutureEvent.setMaxCapacity(1);
        mockSuccessfulRegisterPath();
        when(inscriptionRepo.countByEventIdAndStatus(2L, InscriptionStatus.CONFIRMED)).thenReturn(1L);

        Inscription result = inscriptionService.register(6L, 2L);

        assertThat(result.getStatus()).isEqualTo(InscriptionStatus.WAITING_LIST);
    }

    @Test
    void register_unknownUser_throwsNotFound() throws HttpException {
        when(appUserService.findById(999L)).thenThrow(new NotFoundException("User not found with id: 999"));

        assertThrows(NotFoundException.class, () -> inscriptionService.register(999L, 2L));
    }

    private void mockSuccessfulRegisterPath() throws HttpException {
        when(appUserService.findById(6L)).thenReturn(user);
        when(eventService.findById(2L)).thenReturn(publishedFutureEvent);
        when(inscriptionRepo.existsByUserIdAndEventIdAndStatusNot(6L, 2L, InscriptionStatus.CANCELED))
                .thenReturn(false);
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------------------------------------------------------------
    // cancelByMember / cancelByOrganizer
    // ---------------------------------------------------------------

    private Inscription confirmedInscription() {
        Inscription inscription = new Inscription();
        inscription.setId(1L);
        inscription.setUser(user);
        inscription.setEvent(publishedFutureEvent);
        inscription.setStatus(InscriptionStatus.CONFIRMED);
        return inscription;
    }

    @Test
    void cancelByMember_alreadyCancelled_throwsConflict() {
        Inscription inscription = confirmedInscription();
        inscription.setStatus(InscriptionStatus.CANCELED);
        when(inscriptionRepo.findById(1L)).thenReturn(Optional.of(inscription));

        assertThrows(ConflictException.class, () -> inscriptionService.cancelByMember(1L));
    }

    @Test
    void cancelByMember_confirmedInscription_promotesNextInWaitingList() throws HttpException {
        Inscription inscription = confirmedInscription();
        when(inscriptionRepo.findById(1L)).thenReturn(Optional.of(inscription));
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.findFirstByEventIdAndStatusOrderByInscriptionDateAsc(2L, InscriptionStatus.WAITING_LIST))
                .thenReturn(Optional.empty());

        inscriptionService.cancelByMember(1L);

        verify(eventRepo).findByIdForUpdate(2L);
    }

    @Test
    void cancelByMember_waitingListInscription_doesNotTriggerPromotion() throws HttpException {
        Inscription inscription = confirmedInscription();
        inscription.setStatus(InscriptionStatus.WAITING_LIST);
        when(inscriptionRepo.findById(1L)).thenReturn(Optional.of(inscription));
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        inscriptionService.cancelByMember(1L);

        verify(eventRepo, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void cancelByOrganizer_blankMotif_throwsBadRequest() throws HttpException {
        assertThrows(BadRequestException.class, () -> inscriptionService.cancelByOrganizer(1L, " "));
        verify(inscriptionRepo, never()).findById(anyLong());
    }

    @Test
    void cancelByOrganizer_nullMotif_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> inscriptionService.cancelByOrganizer(1L, null));
    }

    @Test
    void cancelByOrganizer_storesMotifInCancelObject() throws HttpException {
        Inscription inscription = confirmedInscription();
        when(inscriptionRepo.findById(1L)).thenReturn(Optional.of(inscription));
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.findFirstByEventIdAndStatusOrderByInscriptionDateAsc(2L, InscriptionStatus.WAITING_LIST))
                .thenReturn(Optional.empty());

        Inscription result = inscriptionService.cancelByOrganizer(1L, "Évènement annulé faute de participants");

        assertThat(result.getCancelObject()).isEqualTo("Évènement annulé faute de participants");
    }

    @Test
    void cancelByOrganizer_sendsCancellationEmail() throws HttpException {
        Inscription inscription = confirmedInscription();
        when(inscriptionRepo.findById(1L)).thenReturn(Optional.of(inscription));
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.findFirstByEventIdAndStatusOrderByInscriptionDateAsc(2L, InscriptionStatus.WAITING_LIST))
                .thenReturn(Optional.empty());

        inscriptionService.cancelByOrganizer(1L, "Motif");

        verify(emailService).sendInscriptionCancellationEmail(eq(user.getEmail()),
                eq(publishedFutureEvent.getTitle()), eq("Motif"));
    }

    // Bug documente : canceledById n'est jamais reellement renseigne (Inscription.cancel
    // passe toujours null quel que soit l'appelant). Ce test prouve le comportement actuel.
    @Test
    void cancelByOrganizer_canceledByIdIsNeverActuallySet() throws HttpException {
        Inscription inscription = confirmedInscription();
        when(inscriptionRepo.findById(1L)).thenReturn(Optional.of(inscription));
        when(inscriptionRepo.save(any(Inscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.findFirstByEventIdAndStatusOrderByInscriptionDateAsc(2L, InscriptionStatus.WAITING_LIST))
                .thenReturn(Optional.empty());

        Inscription result = inscriptionService.cancelByOrganizer(1L, "Motif");

        assertThat(result.getCanceledById()).isNull();
    }

    // ---------------------------------------------------------------
    // promoteNextInWaitingList
    // ---------------------------------------------------------------

    @Test
    void promoteNextInWaitingList_emptyList_noOpWithoutException() {
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.findFirstByEventIdAndStatusOrderByInscriptionDateAsc(2L, InscriptionStatus.WAITING_LIST))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> inscriptionService.promoteNextInWaitingList(2L));
        verify(inscriptionRepo, never()).save(any());
    }

    @Test
    void promoteNextInWaitingList_promotesFirstInFifoOrder() throws HttpException {
        Inscription next = new Inscription();
        next.setStatus(InscriptionStatus.WAITING_LIST);
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(publishedFutureEvent));
        when(inscriptionRepo.findFirstByEventIdAndStatusOrderByInscriptionDateAsc(2L, InscriptionStatus.WAITING_LIST))
                .thenReturn(Optional.of(next));

        inscriptionService.promoteNextInWaitingList(2L);

        assertThat(next.getStatus()).isEqualTo(InscriptionStatus.CONFIRMED);
        verify(inscriptionRepo, times(1)).save(next);
    }
}
