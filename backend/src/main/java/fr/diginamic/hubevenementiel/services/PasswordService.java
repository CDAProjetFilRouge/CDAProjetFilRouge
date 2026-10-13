package fr.diginamic.hubevenementiel.services;

import java.time.Duration;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Token;
import fr.diginamic.hubevenementiel.enums.TokenType;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import jakarta.transaction.Transactional;

/**
 * Password change and password reset, both confirmed through a CHANGE_PWD token.
 */
@Service
public class PasswordService {

    private static final Duration TOKEN_VALIDITY = Duration.ofHours(1);

    private final AppUserService appUserService;
    private final UserRepo userRepo;
    private final TokenService tokenService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public PasswordService(AppUserService appUserService, UserRepo userRepo, TokenService tokenService,
            EmailService emailService, PasswordEncoder passwordEncoder) {
        this.appUserService = appUserService;
        this.userRepo = userRepo;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     *
     * @param userId          id of the AppUser requesting the password change
     * @param currentPassword current password, verified before issuing the change
     *                        token
     * @param newPassword     new password to apply once the change is confirmed
     * @throws HttpException if the AppUser cannot be found or currentPassword
     *                       doesn't match
     */
    @Transactional
    public void requestPasswordChange(Long userId, String currentPassword, String newPassword) throws HttpException {
        if (newPassword == null || newPassword.length() < 12) {
            throw new BadRequestException("Le mot de passe doit contenir au moins 12 caractères.");
        }

        AppUser user = appUserService.findById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getHashedPassword())) {
            throw new BadRequestException("Mot de passe actuel incorrect.");
        }

        createPasswordChangeToken(user, newPassword);
    }

    /**
     *
     * @param email email of the AppUser requesting a password reset; silently
     *              ignored if unknown
     */
    @Transactional
    public void requestPasswordReset(String email) {
        Optional<AppUser> userOptional = userRepo.findByEmail(email);

        if (userOptional.isEmpty()) {
            return;
        }

        createPasswordChangeToken(userOptional.get(), null);
    }

    private void createPasswordChangeToken(AppUser user, String newPassword) {
        Token token = tokenService.issue(user, TokenType.CHANGE_PWD, TOKEN_VALIDITY,
                newPassword != null ? passwordEncoder.encode(newPassword) : "");

        if (newPassword != null) {
            emailService.sendPasswordChangeConfirmationEmail(user.getEmail(), token.getValue());
        } else {
            emailService.sendPasswordResetEmail(user.getEmail(), token.getValue());
        }
    }

    /**
     *
     * @param tokenValue  value of the pending CHANGE_PWD token
     * @param newPassword new password to store as the token's pending data
     * @throws HttpException if the token is invalid, of the wrong type, already
     *                       used or expired
     */
    @Transactional
    public void submitNewPasswordAfterReset(String tokenValue, String newPassword) throws HttpException {
        if (newPassword == null || newPassword.length() < 12) {
            throw new BadRequestException("Le mot de passe doit contenir au moins 12 caractères.");
        }

        Token token = tokenService.getValidToken(tokenValue, TokenType.CHANGE_PWD);

        AppUser user = token.getUser();
        user.setHashedPassword(passwordEncoder.encode(newPassword));
        userRepo.save(user);

        tokenService.markUsed(token);
    }

    /**
     *
     * @param tokenValue value of the CHANGE_PWD token to confirm
     * @throws HttpException if the token is invalid, of the wrong type, already
     *                       used, expired, or has no pending password
     */
    @Transactional
    public void confirmPasswordReset(String tokenValue) throws HttpException {
        Token token = tokenService.getValidToken(tokenValue, TokenType.CHANGE_PWD);

        if (token.getPendingData() == null || token.getPendingData().isBlank()) {
            throw new BadRequestException("Aucun mot de passe en attente pour ce token.");
        }

        AppUser user = token.getUser();
        user.setHashedPassword(token.getPendingData());
        userRepo.save(user);

        tokenService.markUsed(token);
    }
}
