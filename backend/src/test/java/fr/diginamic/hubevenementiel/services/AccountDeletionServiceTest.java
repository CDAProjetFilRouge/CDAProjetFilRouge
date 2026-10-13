package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.repositories.AnonymizationDemandRepo;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import fr.diginamic.hubevenementiel.repositories.CommentRepo;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import fr.diginamic.hubevenementiel.repositories.LegalDocumentRepo;
import fr.diginamic.hubevenementiel.repositories.RefreshTokenRepo;
import fr.diginamic.hubevenementiel.repositories.TokenRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private TokenRepo tokenRepo;
    @Mock
    private ClubRepo clubRepo;
    @Mock
    private EventRepo eventRepo;
    @Mock
    private InscriptionRepo inscriptionRepo;
    @Mock
    private CommentRepo commentRepo;
    @Mock
    private LegalDocumentRepo legalDocumentRepo;
    @Mock
    private AnonymizationDemandRepo anonymizationDemandRepo;
    @Mock
    private RefreshTokenRepo refreshTokenRepo;
    @Mock
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountDeletionService accountDeletionService;

    @BeforeEach
    void setUp() {
        AppUserService appUserService = new AppUserService(userRepo, tokenRepo, clubRepo, emailService, passwordEncoder);
        WaitingListService waitingListService = new WaitingListService(eventRepo, inscriptionRepo,
                mock(ApplicationEventPublisher.class));
        AccountDeletionGuard guard = new AccountDeletionGuard(eventRepo, clubRepo, legalDocumentRepo,
                anonymizationDemandRepo, commentRepo);
        AccountDataCleaner cleaner = new AccountDataCleaner(commentRepo, tokenRepo, refreshTokenRepo,
                anonymizationDemandRepo, waitingListService);
        accountDeletionService = new AccountDeletionService(appUserService, userRepo, guard, cleaner);
    }

    @Test
    void deleteAccount_noActivity_deletesTokensAndUser() throws HttpException {
        AppUser user = new AppUser();
        user.setId(5L);
        when(userRepo.findById(5L)).thenReturn(Optional.of(user));
        when(inscriptionRepo.findAllByUserId(5L)).thenReturn(List.of());

        accountDeletionService.deleteAccount(5L, false);

        verify(tokenRepo).deleteByUser(user);
        verify(anonymizationDemandRepo).deleteByRequesterId(5L);
        verify(userRepo).delete(user);
    }

    @Test
    void deleteAccount_organizerOfEvents_throwsConflictAndDeletesNothing() {
        AppUser user = new AppUser();
        user.setId(5L);
        when(userRepo.findById(5L)).thenReturn(Optional.of(user));
        when(eventRepo.existsByOrganizerId(5L)).thenReturn(true);
        when(clubRepo.existsByOwnerId(5L)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> accountDeletionService.deleteAccount(5L, false));

        assertThat(ex.getMessage()).contains("organise des évènements").contains("anonymisation");
        verify(tokenRepo, never()).deleteByUser(any());
        verify(inscriptionRepo, never()).delete(any(Inscription.class));
        verify(userRepo, never()).delete(any(AppUser.class));
    }

    @Test
    void deleteAccount_confirmedInscriptionOnFutureEvent_removesItAndPromotesWaitingList() throws HttpException {
        AppUser user = new AppUser();
        user.setId(5L);
        Event event = new Event();
        event.setId(9L);
        event.setStartDateTime(LocalDateTime.now().plusDays(3));
        Inscription confirmed = new Inscription();
        confirmed.setUser(user);
        confirmed.setEvent(event);
        confirmed.setStatus(InscriptionStatus.CONFIRMED);
        Inscription waiting = new Inscription();
        waiting.setStatus(InscriptionStatus.WAITING_LIST);

        when(userRepo.findById(5L)).thenReturn(Optional.of(user));
        when(inscriptionRepo.findAllByUserId(5L)).thenReturn(List.of(confirmed));
        when(eventRepo.findByIdForUpdate(9L)).thenReturn(Optional.of(event));
        when(inscriptionRepo.findByEventIdAndStatusOrderByInscriptionDateAsc(9L, InscriptionStatus.WAITING_LIST))
                .thenReturn(List.of(waiting));

        accountDeletionService.deleteAccount(5L, false);

        verify(inscriptionRepo).delete(confirmed);
        assertThat(waiting.getStatus()).isEqualTo(InscriptionStatus.CONFIRMED);
        verify(userRepo).delete(user);
    }

    @Test
    void deleteAccount_inscriptionOnPastEvent_removesItWithoutPromotion() throws HttpException {
        AppUser user = new AppUser();
        user.setId(5L);
        Event event = new Event();
        event.setId(9L);
        event.setStartDateTime(LocalDateTime.now().minusDays(3));
        Inscription confirmed = new Inscription();
        confirmed.setEvent(event);
        confirmed.setStatus(InscriptionStatus.CONFIRMED);

        when(userRepo.findById(5L)).thenReturn(Optional.of(user));
        when(inscriptionRepo.findAllByUserId(5L)).thenReturn(List.of(confirmed));

        accountDeletionService.deleteAccount(5L, false);

        verify(inscriptionRepo).delete(confirmed);
        verify(eventRepo, never()).findByIdForUpdate(any());
        verify(userRepo).delete(user);
    }

    @Test
    void deleteAccount_onlyComments_throwsConflictWithCode() {
        AppUser user = new AppUser();
        user.setId(5L);
        when(userRepo.findById(5L)).thenReturn(Optional.of(user));
        when(commentRepo.existsByAuthorId(5L)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> accountDeletionService.deleteAccount(5L, false));

        assertThat(ex.getCode()).isEqualTo(AccountDeletionGuard.ACCOUNT_HAS_COMMENTS);
        verify(commentRepo, never()).deleteByAuthorId(any());
        verify(userRepo, never()).delete(any(AppUser.class));
    }

    @Test
    void deleteAccount_onlyCommentsWithDeleteComments_deletesCommentsAndUser() throws HttpException {
        AppUser user = new AppUser();
        user.setId(5L);
        when(userRepo.findById(5L)).thenReturn(Optional.of(user));
        when(commentRepo.existsByAuthorId(5L)).thenReturn(true);
        when(inscriptionRepo.findAllByUserId(5L)).thenReturn(List.of());

        accountDeletionService.deleteAccount(5L, true);

        verify(commentRepo).deleteByAuthorId(5L);
        verify(userRepo).delete(user);
    }

    @Test
    void deleteAccount_commentsAndEventsWithDeleteComments_stillRefusedWithoutCode() {
        AppUser user = new AppUser();
        user.setId(5L);
        when(userRepo.findById(5L)).thenReturn(Optional.of(user));
        when(eventRepo.existsByOrganizerId(5L)).thenReturn(true);
        when(commentRepo.existsByAuthorId(5L)).thenReturn(true);

        ConflictException ex = assertThrows(ConflictException.class, () -> accountDeletionService.deleteAccount(5L, true));

        assertThat(ex.getCode()).isNull();
        assertThat(ex.getMessage()).contains("organise des évènements").contains("commentaires");
        verify(commentRepo, never()).deleteByAuthorId(any());
        verify(userRepo, never()).delete(any(AppUser.class));
    }
}
