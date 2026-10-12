package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaitingListServiceTest {

    @Mock
    private EventRepo eventRepo;
    @Mock
    private InscriptionRepo inscriptionRepo;

    @InjectMocks
    private WaitingListService waitingListService;

    private Event event;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(2L);
        event.setStartDateTime(LocalDateTime.now().plusDays(5));
    }

    private Inscription waitingInscription(AccountStatus userStatus) {
        AppUser user = new AppUser();
        user.setStatus(userStatus);
        Inscription inscription = new Inscription();
        inscription.setUser(user);
        inscription.setStatus(InscriptionStatus.WAITING_LIST);
        return inscription;
    }

    private void givenWaitingList(Inscription... inscriptions) {
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(event));
        when(inscriptionRepo.findByEventIdAndStatusOrderByInscriptionDateAsc(2L, InscriptionStatus.WAITING_LIST))
                .thenReturn(List.of(inscriptions));
    }

    @Test
    void promoteNext_unknownEvent_throwsNotFound() {
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> waitingListService.promoteNext(2L));
    }

    @Test
    void promoteNext_emptyList_noOpWithoutException() {
        givenWaitingList();

        assertDoesNotThrow(() -> waitingListService.promoteNext(2L));
        verify(inscriptionRepo, never()).save(any());
    }

    @Test
    void promoteNext_promotesFirstInFifoOrder() throws NotFoundException {
        Inscription first = waitingInscription(AccountStatus.ACTIVE);
        Inscription second = waitingInscription(AccountStatus.ACTIVE);
        givenWaitingList(first, second);

        waitingListService.promoteNext(2L);

        assertThat(first.getStatus()).isEqualTo(InscriptionStatus.CONFIRMED);
        assertThat(second.getStatus()).isEqualTo(InscriptionStatus.WAITING_LIST);
        verify(inscriptionRepo, times(1)).save(first);
    }

    @Test
    void promoteNext_suspendedFirst_promotesNextOne() throws NotFoundException {
        Inscription suspended = waitingInscription(AccountStatus.SUSPENDED);
        Inscription active = waitingInscription(AccountStatus.ACTIVE);
        givenWaitingList(suspended, active);

        waitingListService.promoteNext(2L);

        assertThat(suspended.getStatus()).isEqualTo(InscriptionStatus.WAITING_LIST);
        assertThat(active.getStatus()).isEqualTo(InscriptionStatus.CONFIRMED);
        verify(inscriptionRepo, never()).save(suspended);
    }

    @Test
    void promoteNext_anonymizedFirst_promotesNextOne() throws NotFoundException {
        Inscription anonymized = waitingInscription(AccountStatus.ANONYMIZE);
        Inscription active = waitingInscription(AccountStatus.ACTIVE);
        givenWaitingList(anonymized, active);

        waitingListService.promoteNext(2L);

        assertThat(anonymized.getStatus()).isEqualTo(InscriptionStatus.WAITING_LIST);
        assertThat(active.getStatus()).isEqualTo(InscriptionStatus.CONFIRMED);
    }

    @Test
    void promoteNext_onlySuspendedOrAnonymized_promotesNobody() throws NotFoundException {
        givenWaitingList(waitingInscription(AccountStatus.SUSPENDED), waitingInscription(AccountStatus.ANONYMIZE));

        waitingListService.promoteNext(2L);

        verify(inscriptionRepo, never()).save(any());
    }

    @Test
    void promoteNext_eventAlreadyStarted_promotesNobody() throws NotFoundException {
        event.setStartDateTime(LocalDateTime.now().minusHours(1));
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(event));

        waitingListService.promoteNext(2L);

        verify(inscriptionRepo, never()).findByEventIdAndStatusOrderByInscriptionDateAsc(any(), any());
        verify(inscriptionRepo, never()).save(any());
    }
}
