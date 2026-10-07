package fr.diginamic.hubevenementiel.security;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private UserRepo userRepo;

    @InjectMocks
    private AppUserDetailsService appUserDetailsService;

    @Test
    void loadUserByUsername_unknownEmail_throwsUsernameNotFound() {
        when(userRepo.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class,
                () -> appUserDetailsService.loadUserByUsername("inconnu@example.com"));
    }

    @Test
    void loadUserByUsername_knownEmail_buildsAppUserDetails() {
        AppUser user = new AppUser();
        user.setEmail("alice.martin@example.com");
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));

        UserDetails result = appUserDetailsService.loadUserByUsername("alice.martin@example.com");

        assertThat(result).isInstanceOf(AppUserDetails.class);
        assertThat(result.getUsername()).isEqualTo("alice.martin@example.com");
    }

    @Test
    void loadUserByUsername_suspensionEndDatePassed_reactivatesAccount() {
        AppUser user = suspendedUntil(LocalDateTime.now().minusMinutes(1));
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));

        UserDetails result = appUserDetailsService.loadUserByUsername("alice.martin@example.com");

        assertThat(result.isEnabled()).isTrue();
        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        assertThat(user.getSuspensionEndDate()).isNull();
        verify(userRepo).save(user);
    }

    @Test
    void loadUserByUsername_suspensionStillRunning_keepsAccountSuspended() {
        AppUser user = suspendedUntil(LocalDateTime.now().plusDays(1));
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));

        UserDetails result = appUserDetailsService.loadUserByUsername("alice.martin@example.com");

        assertThat(result.isEnabled()).isFalse();
        assertThat(user.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        verify(userRepo, never()).save(any());
    }

    @Test
    void loadUserByUsername_suspensionWithoutEndDate_keepsAccountSuspended() {
        AppUser user = suspendedUntil(null);
        when(userRepo.findByEmail("alice.martin@example.com")).thenReturn(Optional.of(user));

        UserDetails result = appUserDetailsService.loadUserByUsername("alice.martin@example.com");

        assertThat(result.isEnabled()).isFalse();
        assertThat(user.getStatus()).isEqualTo(AccountStatus.SUSPENDED);
        verify(userRepo, never()).save(any());
    }

    private AppUser suspendedUntil(LocalDateTime endDate) {
        AppUser user = new AppUser();
        user.setEmail("alice.martin@example.com");
        user.setStatus(AccountStatus.SUSPENDED);
        user.setSuspensionEndDate(endDate);
        return user;
    }
}
