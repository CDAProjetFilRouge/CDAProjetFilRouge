package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Token;
import fr.diginamic.hubevenementiel.enums.TokenType;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.TokenRepo;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private TokenRepo tokenRepo;

    @InjectMocks
    private TokenService tokenService;

    // Non-regression pour le bug corrige par la PR #265 : l'ordre du test etait
    // isEmpty() avant le null-check, ce qui provoquait une NullPointerException
    // au lieu de la NotFoundException attendue quand value == null.
    @Test
    void createToken_nullValue_throwsNotFoundInsteadOfNPE() {
        Token token = new Token();
        token.setValue(null);
        token.setTokenType(TokenType.ENABLE_ACCOUNT);
        token.setUser(new AppUser());

        assertThrows(NotFoundException.class, () -> tokenService.createToken(token));
        verify(tokenRepo, never()).save(token);
    }

    @Test
    void createToken_emptyValue_throwsNotFound() {
        Token token = new Token();
        token.setValue("");
        token.setTokenType(TokenType.ENABLE_ACCOUNT);
        token.setUser(new AppUser());

        assertThrows(NotFoundException.class, () -> tokenService.createToken(token));
    }

    @Test
    void createToken_nullType_throwsNotFound() {
        Token token = new Token();
        token.setValue("abc");
        token.setTokenType(null);
        token.setUser(new AppUser());

        assertThrows(NotFoundException.class, () -> tokenService.createToken(token));
    }

    @Test
    void createToken_nullUser_throwsNotFound() {
        Token token = new Token();
        token.setValue("abc");
        token.setTokenType(TokenType.ENABLE_ACCOUNT);
        token.setUser(null);

        assertThrows(NotFoundException.class, () -> tokenService.createToken(token));
    }

    @Test
    void createToken_validToken_savesWithoutException() throws HttpException {
        Token token = new Token();
        token.setValue("abc");
        token.setTokenType(TokenType.ENABLE_ACCOUNT);
        token.setUser(new AppUser());

        assertDoesNotThrow(() -> tokenService.createToken(token));
        verify(tokenRepo).save(token);
    }

    @Test
    void issue_storesOnlyTheHashAndKeepsTheRawValueForTheEmail() {
        ArgumentCaptor<Token> captor = ArgumentCaptor.forClass(Token.class);

        Token issued = tokenService.issue(new AppUser(), TokenType.CHANGE_PWD, Duration.ofHours(1), "");

        verify(tokenRepo).save(captor.capture());
        Token stored = captor.getValue();
        assertThat(issued.getRawValue()).isNotBlank();
        assertThat(stored.getValue()).isEqualTo(TokenHasher.sha256Hex(issued.getRawValue()));
        assertThat(stored.getValue()).isNotEqualTo(issued.getRawValue()).hasSize(64);
    }

    @Test
    void getValidToken_looksUpTheHashOfTheValueReceived() throws HttpException {
        Token token = new Token();
        token.setTokenType(TokenType.ENABLE_ACCOUNT);
        token.setExpirationDateTime(LocalDateTime.now().plusHours(1));
        when(tokenRepo.findByValue(TokenHasher.sha256Hex("secret"))).thenReturn(Optional.of(token));

        assertThat(tokenService.getValidToken("secret", TokenType.ENABLE_ACCOUNT)).isSameAs(token);
    }

    @Test
    void getValidToken_rejectsTheStoredHashPresentedAsIfItWereTheSecret() {
        String hash = TokenHasher.sha256Hex("secret");

        assertThrows(BadRequestException.class, () -> tokenService.getValidToken(hash, TokenType.ENABLE_ACCOUNT));
    }

    @Test
    void getValidToken_rejectsMissingValue() {
        assertThrows(BadRequestException.class, () -> tokenService.getValidToken(null, TokenType.ENABLE_ACCOUNT));
        assertThrows(BadRequestException.class, () -> tokenService.getValidToken(" ", TokenType.ENABLE_ACCOUNT));
    }
}
