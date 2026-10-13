package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Token;
import fr.diginamic.hubevenementiel.enums.TokenType;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordServiceTest {

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

    private PasswordService passwordService;

    @BeforeEach
    void setUp() {
        AppUserService appUserService = new AppUserService(userRepo, tokenRepo, clubRepo, emailService, passwordEncoder);
        passwordService = new PasswordService(appUserService, userRepo, new TokenService(tokenRepo), emailService,
                passwordEncoder);
    }

    @Test
    void requestPasswordChange_wrongCurrentPassword_throwsBadRequestAndNoTokenCreated() {
        AppUser existing = new AppUser();
        existing.setHashedPassword("hashActuel");
        when(userRepo.findById(1L)).thenReturn(Optional.of(existing));
        when(passwordEncoder.matches("mauvaisMdp", "hashActuel")).thenReturn(false);

        assertThrows(BadRequestException.class,
                () -> passwordService.requestPasswordChange(1L, "mauvaisMdp", "nouveauMdp12"));

        verify(tokenRepo, never()).save(any());
    }

    @Test
    void requestPasswordReset_unknownEmail_returnsSilentlyWithoutException() {
        when(userRepo.findByEmail("inconnu@example.com")).thenReturn(Optional.empty());

        passwordService.requestPasswordReset("inconnu@example.com");

        verify(tokenRepo, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void confirmPasswordReset_wrongTokenType_throwsBadRequest() {
        Token token = new Token();
        token.setTokenType(TokenType.ENABLE_ACCOUNT);
        when(tokenRepo.findByValue("abc")).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class, () -> passwordService.confirmPasswordReset("abc"));
    }

    @Test
    void confirmPasswordReset_alreadyUsedToken_throwsBadRequest() {
        Token token = new Token();
        token.setTokenType(TokenType.CHANGE_PWD);
        token.setUseDate(LocalDateTime.now().minusMinutes(5));
        when(tokenRepo.findByValue("abc")).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class, () -> passwordService.confirmPasswordReset("abc"));
    }

    @Test
    void confirmPasswordReset_expiredToken_throwsBadRequest() {
        Token token = new Token();
        token.setTokenType(TokenType.CHANGE_PWD);
        token.setExpirationDateTime(LocalDateTime.now().minusMinutes(1));
        when(tokenRepo.findByValue("abc")).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class, () -> passwordService.confirmPasswordReset("abc"));
    }
}
