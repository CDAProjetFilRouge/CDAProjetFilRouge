package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Token;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.TokenType;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountActivationServiceTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private TokenRepo tokenRepo;
    @Mock
    private PasswordEncoder passwordEncoder;

    private AccountActivationService accountActivationService;

    @BeforeEach
    void setUp() {
        accountActivationService = new AccountActivationService(userRepo, new TokenService(tokenRepo), passwordEncoder);
    }

    @Test
    void confirmAccountVerification_happyPath_setsStatusActive() throws HttpException {
        AppUser user = new AppUser();
        user.setStatus(AccountStatus.INACTIVE);
        Token token = new Token();
        token.setTokenType(TokenType.ENABLE_ACCOUNT);
        token.setExpirationDateTime(LocalDateTime.now().plusHours(1));
        token.setUser(user);
        when(tokenRepo.findByValue(TokenHasher.sha256Hex("abc"))).thenReturn(Optional.of(token));

        accountActivationService.confirmAccountVerification("abc");

        assertThat(user.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verify(userRepo).save(user);
    }
}
