package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Inscription;
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

    @Test
    void promoteNext_unknownEvent_throwsNotFound() {
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> waitingListService.promoteNext(2L));
    }

    @Test
    void promoteNext_emptyList_noOpWithoutException() {
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(event));
        when(inscriptionRepo.findFirstByEventIdAndStatusOrderByInscriptionDateAsc(2L, InscriptionStatus.WAITING_LIST))
                .thenReturn(Optional.empty());

        assertDoesNotThrow(() -> waitingListService.promoteNext(2L));
        verify(inscriptionRepo, never()).save(any());
    }

    @Test
    void promoteNext_promotesFirstInFifoOrder() throws NotFoundException {
        Inscription next = new Inscription();
        next.setStatus(InscriptionStatus.WAITING_LIST);
        when(eventRepo.findByIdForUpdate(2L)).thenReturn(Optional.of(event));
        when(inscriptionRepo.findFirstByEventIdAndStatusOrderByInscriptionDateAsc(2L, InscriptionStatus.WAITING_LIST))
                .thenReturn(Optional.of(next));

        waitingListService.promoteNext(2L);

        assertThat(next.getStatus()).isEqualTo(InscriptionStatus.CONFIRMED);
        verify(inscriptionRepo, times(1)).save(next);
    }
}
