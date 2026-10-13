package fr.diginamic.hubevenementiel.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Token;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.TokenType;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import jakarta.transaction.Transactional;

/**
 * Activation of an account from the link received by email: verification of the
 * address for a self-registered account, definitive password for an account created
 * by an administrator.
 */
@Service
public class AccountActivationService {

    private final UserRepo userRepo;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public AccountActivationService(UserRepo userRepo, TokenService tokenService, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     *
     * @param tokenValue value of the ENABLE_ACCOUNT token to confirm
     * @throws HttpException if the token is invalid, of the wrong type, already
     *                       used or expired
     */
    @Transactional
    public void confirmAccountVerification(String tokenValue) throws HttpException {
        Token token = tokenService.getValidToken(tokenValue, TokenType.ENABLE_ACCOUNT);

        AppUser user = token.getUser();
        user.setStatus(AccountStatus.ACTIVE);
        userRepo.save(user);

        tokenService.markUsed(token);
    }

    /**
     *
     * @param tokenValue        value of the ACCOUNT_ACTIVATION token to confirm
     * @param temporaryPassword temporary password sent to the AppUser, verified
     *                          before activation
     * @param newPassword       new password to set once the account is activated
     * @throws HttpException if the token is invalid, of the wrong type, already
     *                       used, expired, or temporaryPassword doesn't match
     */
    @Transactional
    public void activateAccount(String tokenValue, String temporaryPassword, String newPassword) throws HttpException {
        Token token = tokenService.getValidToken(tokenValue, TokenType.ACCOUNT_ACTIVATION);

        AppUser user = token.getUser();

        if (!passwordEncoder.matches(temporaryPassword, user.getHashedPassword())) {
            throw new BadRequestException("Mot de passe temporaire incorrect.");
        }

        user.setHashedPassword(passwordEncoder.encode(newPassword));
        user.setStatus(AccountStatus.ACTIVE);
        userRepo.save(user);

        tokenService.markUsed(token);
    }
}
