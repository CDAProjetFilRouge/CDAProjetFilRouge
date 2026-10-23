package fr.diginamic.hubevenementiel.services;

import static fr.diginamic.hubevenementiel.TestIds.uuid;
import java.util.UUID;
import fr.diginamic.hubevenementiel.logging.LogCapture;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import fr.diginamic.hubevenementiel.repositories.TokenRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountStatusServiceTest {

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

    private AccountStatusService accountStatusService;

    @BeforeEach
    void setUp() {
        AppUserService appUserService = new AppUserService(userRepo, tokenRepo, clubRepo, emailService, passwordEncoder);
        accountStatusService = new AccountStatusService(appUserService, userRepo, passwordEncoder);
    }

    // ---------------------------------------------------------------
    // suspend
    // ---------------------------------------------------------------

    private AppUser userWithStatus(UUID id, AccountStatus status) {
        AppUser user = new AppUser();
        user.setId(id);
        user.setStatus(status);
        return user;
    }

    @Test
    void suspend_ownAccount_throwsBadRequestAndTouchesNothing() {
        assertThrows(BadRequestException.class, () -> accountStatusService.suspend(uuid(5), null, uuid(5)));
        verify(userRepo, never()).save(any(AppUser.class));
    }

    @Test
    void suspend_activeUser_setsSuspendedStatusAndEndDate() throws HttpException {
        AppUser target = userWithStatus(uuid(7), AccountStatus.ACTIVE);
        LocalDateTime end = LocalDateTime.now().plusDays(3);
        when(userRepo.findById(uuid(7))).thenReturn(Optional.of(target));

        accountStatusService.suspend(uuid(7), end, uuid(1));

        assertThat(target.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        assertThat(target.getSuspensionEndDate()).isEqualTo(end);
        verify(userRepo).save(target);
    }

    @Test
    void suspendAndReactivate_areLoggedWithTheAccount() throws HttpException {
        AppUser target = userWithStatus(uuid(7), AccountStatus.ACTIVE);
        when(userRepo.findById(uuid(7))).thenReturn(Optional.of(target));

        try (LogCapture logs = new LogCapture()) {
            accountStatusService.suspend(uuid(7), null, uuid(1));
            accountStatusService.reactivate(uuid(7));

            assertThat(logs.messages()).containsExactly(
                    "Compte " + uuid(7) + " suspendu (fin : définitive)",
                    "Compte " + uuid(7) + " réactivé");
        }
    }

    @Test
    void suspend_endDateInThePast_throwsBadRequest() {
        AppUser target = userWithStatus(uuid(7), AccountStatus.ACTIVE);
        when(userRepo.findById(uuid(7))).thenReturn(Optional.of(target));

        assertThrows(BadRequestException.class,
                () -> accountStatusService.suspend(uuid(7), LocalDateTime.now().minusDays(1), uuid(1)));
    }

    @Test
    void suspend_notActiveUser_throwsBadRequest() {
        AppUser target = userWithStatus(uuid(7), AccountStatus.SUSPENDED);
        when(userRepo.findById(uuid(7))).thenReturn(Optional.of(target));

        assertThrows(BadRequestException.class, () -> accountStatusService.suspend(uuid(7), null, uuid(1)));
    }

    // ---------------------------------------------------------------
    // reactivateExpiredSuspensions (RG11)
    // ---------------------------------------------------------------

    @Test
    void reactivateExpiredSuspensions_expiredAccounts_areReactivatedAndCounted() {
        AppUser first = userWithStatus(uuid(1), AccountStatus.SUSPENDED);
        first.setSuspensionEndDate(LocalDateTime.now().minusHours(2));
        AppUser second = userWithStatus(uuid(2), AccountStatus.SUSPENDED);
        second.setSuspensionEndDate(LocalDateTime.now().minusMinutes(1));
        when(userRepo.findByStatusAndSuspensionEndDateLessThanEqual(eq(AccountStatus.SUSPENDED), any(LocalDateTime.class)))
                .thenReturn(List.of(first, second));

        int count = accountStatusService.reactivateExpiredSuspensions();

        assertThat(count).isEqualTo(2);
        assertThat(first.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(first.getSuspensionEndDate()).isNull();
        assertThat(second.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(second.getSuspensionEndDate()).isNull();
        verify(userRepo).saveAll(List.of(first, second));
    }

    @Test
    void reactivateExpiredSuspensions_nothingExpired_returnsZero() {
        when(userRepo.findByStatusAndSuspensionEndDateLessThanEqual(eq(AccountStatus.SUSPENDED), any(LocalDateTime.class)))
                .thenReturn(List.of());

        assertThat(accountStatusService.reactivateExpiredSuspensions()).isZero();
    }

    // ---------------------------------------------------------------
    // explainLoginRefusal
    // ---------------------------------------------------------------

    @Test
    void explainLoginRefusal_suspendedWithEndDate_correctPassword_mentionsTheDate() {
        AppUser user = userWithStatus(AccountStatus.SUSPENDED);
        user.setSuspensionEndDate(LocalDateTime.of(2026, 10, 10, 12, 0));
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bonMotDePasse", "hash")).thenReturn(true);

        Optional<String> result = accountStatusService.explainLoginRefusal("alice.martin@example.com", "bonMotDePasse");

        assertThat(result).contains("Votre compte est suspendu jusqu'au 10/10/2026.");
    }

    @Test
    void explainLoginRefusal_suspendedWithoutEndDate_correctPassword_saysSuspended() {
        AppUser user = userWithStatus(AccountStatus.SUSPENDED);
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bonMotDePasse", "hash")).thenReturn(true);

        Optional<String> result = accountStatusService.explainLoginRefusal("alice.martin@example.com", "bonMotDePasse");

        assertThat(result).contains("Votre compte est suspendu.");
    }

    @Test
    void explainLoginRefusal_inactive_correctPassword_asksToActivate() {
        AppUser user = userWithStatus(AccountStatus.INACTIVE);
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bonMotDePasse", "hash")).thenReturn(true);

        Optional<String> result = accountStatusService.explainLoginRefusal("alice.martin@example.com", "bonMotDePasse");

        assertThat(result).isPresent();
        assertThat(result.get()).contains("pas encore activé");
    }

    @Test
    void explainLoginRefusal_suspended_wrongPassword_revealsNothing() {
        AppUser user = userWithStatus(AccountStatus.SUSPENDED);
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mauvaisMotDePasse", "hash")).thenReturn(false);

        Optional<String> result = accountStatusService.explainLoginRefusal("alice.martin@example.com", "mauvaisMotDePasse");

        assertThat(result).isEmpty();
    }

    @Test
    void explainLoginRefusal_unknownEmail_revealsNothing() {
        when(userRepo.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        Optional<String> result = accountStatusService.explainLoginRefusal("inconnu@example.com", "nImporteQuoi");

        assertThat(result).isEmpty();
    }

    @Test
    void explainLoginRefusal_anonymized_correctPassword_revealsNothing() {
        AppUser user = userWithStatus(AccountStatus.ANONYMIZE);
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bonMotDePasse", "hash")).thenReturn(true);

        Optional<String> result = accountStatusService.explainLoginRefusal("alice.martin@example.com", "bonMotDePasse");

        assertThat(result).isEmpty();
    }

    private AppUser userWithStatus(AccountStatus status) {
        AppUser user = new AppUser();
        user.setEmail("alice.martin@example.com");
        user.setHashedPassword("hash");
        user.setStatus(status);
        return user;
    }
}
