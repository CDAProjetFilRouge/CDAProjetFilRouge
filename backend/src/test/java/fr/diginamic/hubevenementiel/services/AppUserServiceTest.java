package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Club;
import fr.diginamic.hubevenementiel.entities.Token;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.enums.TokenType;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import fr.diginamic.hubevenementiel.repositories.TokenRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    private EmailService emailService;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AppUserService appUserService;

    private AppUser validUser;

    @BeforeEach
    void setUp() {
        appUserService = new AppUserService(userRepo, tokenRepo, clubRepo, emailService, passwordEncoder);
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
    // appUserChecker : format du téléphone
    // ---------------------------------------------------------------

    @Test
    void appUserChecker_malformedPhone_throwsBadRequest() {
        validUser.setPhone("123456");
        assertThrows(BadRequestException.class, () -> appUserService.appUserChecker(validUser, false, false));
    }

    @Test
    void appUserChecker_malformedPhone_alsoRefusedWhenPhoneIsRequired() {
        validUser.setPhone("06ABCDEFGH");
        assertThrows(BadRequestException.class, () -> appUserService.appUserChecker(validUser, true, false));
    }

    @Test
    void appUserChecker_validPhone_passes() throws HttpException {
        validUser.setPhone("05 61 23 45 67");
        assertThat(appUserService.appUserChecker(validUser, true, false)).isTrue();
    }

    @Test
    void appUserChecker_noPhoneWhenOptional_passes() throws HttpException {
        validUser.setPhone(null);
        assertThat(appUserService.appUserChecker(validUser, false, false)).isTrue();
    }

    @Test
    void appUserChecker_blankPhoneWhenOptional_passes() throws HttpException {
        validUser.setPhone("  ");
        assertThat(appUserService.appUserChecker(validUser, false, false)).isTrue();
    }
}
