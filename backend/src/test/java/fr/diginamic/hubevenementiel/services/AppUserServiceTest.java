package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.entities.Token;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.enums.TokenType;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.AnonymizationDemandRepo;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import fr.diginamic.hubevenementiel.repositories.CommentRepo;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import fr.diginamic.hubevenementiel.repositories.LegalDocumentRepo;
import fr.diginamic.hubevenementiel.repositories.TokenRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

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
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AppUserService appUserService;

    private AppUser validUser;

    @BeforeEach
    void setUp() {
        validUser = new AppUser();
        validUser.setLastName("Martin");
        validUser.setFirstName("Alice");
        validUser.setEmail("alice.martin@example.com");
        validUser.setHashedPassword("motdepasse12");
    }

    // ---------------------------------------------------------------
    // appUserChecker
    // ---------------------------------------------------------------

    @Test
    void appUserChecker_nullUser_throwsBadRequest() {
        assertThrows(BadRequestException.class, () -> appUserService.appUserChecker(null, false, false));
    }

    @Test
    void appUserChecker_blankLastName_throwsBadRequest() {
        validUser.setLastName(" ");
        assertThrows(BadRequestException.class, () -> appUserService.appUserChecker(validUser, false, false));
    }

    @Test
    void appUserChecker_nullFirstName_throwsBadRequest() {
        validUser.setFirstName(null);
        assertThrows(BadRequestException.class, () -> appUserService.appUserChecker(validUser, false, false));
    }

    @Test
    void appUserChecker_blankEmail_throwsBadRequest() {
        validUser.setEmail("");
        assertThrows(BadRequestException.class, () -> appUserService.appUserChecker(validUser, false, false));
    }

    @Test
    void appUserChecker_malformedEmail_throwsBadRequest() {
        validUser.setEmail("pas-un-email");
        assertThrows(BadRequestException.class, () -> appUserService.appUserChecker(validUser, false, false));
    }

    @Test
    void appUserChecker_passwordRequired_nullPassword_throwsBadRequest() {
        validUser.setHashedPassword(null);
        assertThrows(BadRequestException.class, () -> appUserService.appUserChecker(validUser, false, true));
    }

    @Test
    void appUserChecker_passwordRequired_tooShort_throwsBadRequest() {
        validUser.setHashedPassword("a".repeat(11));
        assertThrows(BadRequestException.class, () -> appUserService.appUserChecker(validUser, false, true));
    }

    @Test
    void appUserChecker_passwordRequired_exactly12Chars_passes() throws HttpException {
        validUser.setHashedPassword("a".repeat(12));
        assertThat(appUserService.appUserChecker(validUser, false, true)).isTrue();
    }

    @Test
    void appUserChecker_passwordNotRequired_nullPassword_passes() throws HttpException {
        validUser.setHashedPassword(null);
        assertThat(appUserService.appUserChecker(validUser, false, false)).isTrue();
    }

    @Test
    void appUserChecker_phoneRequired_blankPhone_throwsBadRequest() {
        validUser.setPhone(" ");
        assertThrows(BadRequestException.class, () -> appUserService.appUserChecker(validUser, true, false));
    }

    @Test
    void appUserChecker_phoneNotRequired_nullPhone_passes() throws HttpException {
        validUser.setPhone(null);
        assertThat(appUserService.appUserChecker(validUser, false, false)).isTrue();
    }

    // ---------------------------------------------------------------
    // createAccount
    // ---------------------------------------------------------------

    @Test
    void createAccount_happyPath_setsInactiveStatusMemberRoleAndSendsVerificationEmail() throws HttpException {
        when(userRepo.existsByEmail(validUser.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepo.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.createAccount(validUser);

        assertThat(result.getStatus()).isEqualTo(AccountStatus.INACTIVE);
        assertThat(result.getRole()).isEqualTo(Role.MEMBER);
        assertThat(result.getCreationDate()).isNotNull();

        ArgumentCaptor<Token> tokenCaptor = ArgumentCaptor.forClass(Token.class);
        verify(tokenRepo).save(tokenCaptor.capture());
        Token savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getTokenType()).isEqualTo(TokenType.ENABLE_ACCOUNT);
        assertThat(savedToken.getExpirationDateTime()).isAfter(LocalDateTime.now().plusHours(23));

        verify(emailService).sendVerificationEmail(eq(validUser.getEmail()), anyString());
    }

    @Test
    void createAccount_emailAlreadyExists_throwsConflictAndNeverSaves() {
        when(userRepo.existsByEmail(validUser.getEmail())).thenReturn(true);

        assertThrows(ConflictException.class, () -> appUserService.createAccount(validUser));

        verify(userRepo, never()).save(any());
    }

    // ---------------------------------------------------------------
    // createAccountByAdmin
    // ---------------------------------------------------------------

    @Test
    void createAccountByAdmin_happyPath_setsRequestedRole() throws HttpException {
        validUser.setPhone("0600000000");
        when(userRepo.existsByEmail(validUser.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");
        when(userRepo.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.createAccountByAdmin(validUser, Role.ORGANIZER, List.of());

        assertThat(result.getStatus()).isEqualTo(AccountStatus.PENDING_ACTIVATION);
        assertThat(result.getRole()).isEqualTo(Role.ORGANIZER);
    }

    @Test
    void createAccountByAdmin_missingPhone_throwsBadRequest() {
        validUser.setPhone(null);

        assertThrows(BadRequestException.class, () -> appUserService.createAccountByAdmin(validUser, Role.ORGANIZER, List.of()));
    }

    // ---------------------------------------------------------------
    // updateOwnAccount / updateAccountByAdmin
    // ---------------------------------------------------------------

    @Test
    void updateOwnAccount_unknownId_throwsNotFound() {
        when(userRepo.findById(1L)).thenReturn(Optional.empty());
        AppUserPrincipal principal = new AppUserPrincipal(1L, "alice@example.com", "MEMBER");

        assertThrows(NotFoundException.class, () -> appUserService.updateOwnAccount(1L, validUser, principal));
    }

    @Test
    void updateOwnAccount_emailChangedToExistingOne_throwsConflict() {
        AppUser existing = new AppUser();
        existing.setEmail("ancien@example.com");
        when(userRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepo.existsByEmail(validUser.getEmail())).thenReturn(true);
        AppUserPrincipal principal = new AppUserPrincipal(1L, "alice@example.com", "MEMBER");

        assertThrows(ConflictException.class, () -> appUserService.updateOwnAccount(1L, validUser, principal));
    }

    @Test
    void updateOwnAccount_emailUnchangedCaseInsensitive_doesNotCheckDuplicate() throws HttpException {
        AppUser existing = new AppUser();
        existing.setEmail(validUser.getEmail().toUpperCase());
        when(userRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepo.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppUserPrincipal principal = new AppUserPrincipal(1L, "alice@example.com", "MEMBER");

        appUserService.updateOwnAccount(1L, validUser, principal);

        verify(userRepo, never()).existsByEmail(anyString());
    }

    @Test
    void updateOwnAccount_neverTouchesRoleOrClubs() throws HttpException {
        AppUser existing = new AppUser();
        existing.setEmail(validUser.getEmail());
        existing.setRole(Role.MEMBER);
        existing.setClubs(List.of(new Club()));
        when(userRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepo.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AppUserPrincipal principal = new AppUserPrincipal(1L, "alice@example.com", "MEMBER");

        AppUser result = appUserService.updateOwnAccount(1L, validUser, principal);

        assertThat(result.getRole()).isEqualTo(Role.MEMBER);
        assertThat(result.getClubs()).hasSize(1);
    }

    @Test
    void updateAccountByAdmin_nullClubIds_resultsInEmptyList() throws HttpException {
        AppUser existing = new AppUser();
        existing.setEmail(validUser.getEmail());
        validUser.setPhone("0600000000");
        when(userRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(userRepo.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.updateAccountByAdmin(1L, validUser, null);

        assertThat(result.getClubs()).isEmpty();
        verify(clubRepo, never()).findAllById(any());
    }

    @Test
    void updateAccountByAdmin_unknownClubIds_areSilentlyIgnored() throws HttpException {
        AppUser existing = new AppUser();
        existing.setEmail(validUser.getEmail());
        validUser.setPhone("0600000000");
        when(userRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(clubRepo.findAllById(List.of(999L))).thenReturn(List.of());
        when(userRepo.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser result = appUserService.updateAccountByAdmin(1L, validUser, List.of(999L));

        assertThat(result.getClubs()).isEmpty();
    }

    // ---------------------------------------------------------------
    // Mot de passe / tokens
    // ---------------------------------------------------------------

    @Test
    void requestPasswordChange_wrongCurrentPassword_throwsBadRequestAndNoTokenCreated() {
        AppUser existing = new AppUser();
        existing.setHashedPassword("hashActuel");
        when(userRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("mauvaisMdp", "hashActuel")).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> appUserService.requestPasswordChange(1L, "mauvaisMdp", "nouveauMdp12"));

        verify(tokenRepo, never()).save(any());
    }

    @Test
    void requestPasswordReset_unknownEmail_returnsSilentlyWithoutException() {
        when(userRepo.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        appUserService.requestPasswordReset("inconnu@example.com");

        verify(tokenRepo, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void confirmPasswordReset_wrongTokenType_throwsBadRequest() {
        Token token = new Token();
        token.setTokenType(TokenType.ENABLE_ACCOUNT);
        when(tokenRepo.findByValue("abc")).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class, () -> appUserService.confirmPasswordReset("abc"));
    }

    @Test
    void confirmPasswordReset_alreadyUsedToken_throwsBadRequest() {
        Token token = new Token();
        token.setTokenType(TokenType.CHANGE_PWD);
        token.setUseDate(LocalDateTime.now().minusMinutes(5));
        when(tokenRepo.findByValue("abc")).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class, () -> appUserService.confirmPasswordReset("abc"));
    }

    @Test
    void confirmPasswordReset_expiredToken_throwsBadRequest() {
        Token token = new Token();
        token.setTokenType(TokenType.CHANGE_PWD);
        token.setExpirationDateTime(LocalDateTime.now().minusMinutes(1));
        when(tokenRepo.findByValue("abc")).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class, () -> appUserService.confirmPasswordReset("abc"));
    }

    @Test
    void confirmAccountVerification_happyPath_setsStatusActive() throws HttpException {
        AppUser user = new AppUser();
        user.setStatus(AccountStatus.INACTIVE);
        Token token = new Token();
        token.setTokenType(TokenType.ENABLE_ACCOUNT);
        token.setExpirationDateTime(LocalDateTime.now().plusHours(1));
        token.setUser(user);
        when(tokenRepo.findByValue("abc")).thenReturn(Optional.of(token));

        appUserService.confirmAccountVerification("abc");

        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(userRepo).save(user);
    }

    // ---------------------------------------------------------------
    // anonymizeAccount (RG31)
    // ---------------------------------------------------------------

    @Test
    void anonymizeAccount_randomizesPersonalDataAndClearsAddress() {
        AppUser user = new AppUser();
        user.setLastName("Martin");
        user.setFirstName("Alice");
        user.setEmail("alice.martin@example.com");
        user.setPhone("0600000000");
        user.setHashedPassword("hash");
        user.setAddress(new fr.diginamic.hubevenementiel.entities.Address());

        appUserService.anonymizeAccount(user);

        assertThat(user.getLastName()).startsWith("ANONYME-");
        assertThat(user.getFirstName()).startsWith("ANONYME-");
        assertThat(user.getEmail()).contains("@anonymise.local");
        assertThat(user.getStatus()).isEqualTo(AccountStatus.ANONYMIZE);
        assertThat(user.getAddress()).isNull();
        verify(userRepo).save(user);
    }

    // ---------------------------------------------------------------
<<<<<<< HEAD
    // deleteAccount
    // ---------------------------------------------------------------

    @Test
    void deleteAccount_noActivity_deletesTokensAndUser() throws HttpException {
        AppUser user = new AppUser();
        user.setId(5L);
        when(userRepo.findById(5L)).thenReturn(Optional.of(user));
        when(inscriptionRepo.findAllByUserId(5L)).thenReturn(List.of());

        appUserService.deleteAccount(5L, false);

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

        ConflictException ex = assertThrows(ConflictException.class, () -> appUserService.deleteAccount(5L, false));

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
        when(inscriptionRepo.findFirstByEventIdAndStatusOrderByInscriptionDateAsc(9L, InscriptionStatus.WAITING_LIST))
                .thenReturn(Optional.of(waiting));

        appUserService.deleteAccount(5L, false);

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

        appUserService.deleteAccount(5L, false);

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

        ConflictException ex = assertThrows(ConflictException.class, () -> appUserService.deleteAccount(5L, false));

        assertThat(ex.getCode()).isEqualTo(AppUserService.ACCOUNT_HAS_COMMENTS);
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

        appUserService.deleteAccount(5L, true);

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

        ConflictException ex = assertThrows(ConflictException.class, () -> appUserService.deleteAccount(5L, true));

        assertThat(ex.getCode()).isNull();
        assertThat(ex.getMessage()).contains("organise des évènements").contains("commentaires");
        verify(commentRepo, never()).deleteByAuthorId(any());
        verify(userRepo, never()).delete(any(AppUser.class));
=======
    // explainLoginRefusal
    // ---------------------------------------------------------------

    @Test
    void explainLoginRefusal_suspendedWithEndDate_correctPassword_mentionsTheDate() {
        AppUser user = userWithStatus(AccountStatus.SUSPENDED);
        user.setSuspensionEndDate(LocalDateTime.of(2026, 10, 10, 12, 0));
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bonMotDePasse", "hash")).thenReturn(true);

        Optional<String> result = appUserService.explainLoginRefusal("alice.martin@example.com", "bonMotDePasse");

        assertThat(result).contains("Votre compte est suspendu jusqu'au 10/10/2026.");
    }

    @Test
    void explainLoginRefusal_suspendedWithoutEndDate_correctPassword_saysSuspended() {
        AppUser user = userWithStatus(AccountStatus.SUSPENDED);
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bonMotDePasse", "hash")).thenReturn(true);

        Optional<String> result = appUserService.explainLoginRefusal("alice.martin@example.com", "bonMotDePasse");

        assertThat(result).contains("Votre compte est suspendu.");
    }

    @Test
    void explainLoginRefusal_inactive_correctPassword_asksToActivate() {
        AppUser user = userWithStatus(AccountStatus.INACTIVE);
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bonMotDePasse", "hash")).thenReturn(true);

        Optional<String> result = appUserService.explainLoginRefusal("alice.martin@example.com", "bonMotDePasse");

        assertThat(result).isPresent();
        assertThat(result.get()).contains("pas encore activé");
    }

    @Test
    void explainLoginRefusal_suspended_wrongPassword_revealsNothing() {
        AppUser user = userWithStatus(AccountStatus.SUSPENDED);
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mauvaisMotDePasse", "hash")).thenReturn(false);

        Optional<String> result = appUserService.explainLoginRefusal("alice.martin@example.com", "mauvaisMotDePasse");

        assertThat(result).isEmpty();
    }

    @Test
    void explainLoginRefusal_unknownEmail_revealsNothing() {
        when(userRepo.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        Optional<String> result = appUserService.explainLoginRefusal("inconnu@example.com", "nImporteQuoi");

        assertThat(result).isEmpty();
    }

    @Test
    void explainLoginRefusal_anonymized_correctPassword_revealsNothing() {
        AppUser user = userWithStatus(AccountStatus.ANONYMIZE);
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bonMotDePasse", "hash")).thenReturn(true);

        Optional<String> result = appUserService.explainLoginRefusal("alice.martin@example.com", "bonMotDePasse");

        assertThat(result).isEmpty();
    }

    private AppUser userWithStatus(AccountStatus status) {
        AppUser user = new AppUser();
        user.setEmail("alice.martin@example.com");
        user.setHashedPassword("hash");
        user.setStatus(status);
        return user;
>>>>>>> 2e14ef58f542545c116eb557500c839cdd78c3d1
    }
}
