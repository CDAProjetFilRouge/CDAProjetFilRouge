package fr.diginamic.hubevenementiel.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.UUID;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import jakarta.transaction.Transactional;

/**
 * Suspension and reactivation of accounts, and the explanation given to someone
 * whose login is refused because of the state of their account.
 */
@Service
public class AccountStatusService {

    private static final Logger log = LoggerFactory.getLogger(AccountStatusService.class);

    private final AppUserService appUserService;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;

    public AccountStatusService(AppUserService appUserService, UserRepo userRepo, PasswordEncoder passwordEncoder) {
        this.appUserService = appUserService;
        this.userRepo = userRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void suspend(UUID id, LocalDateTime endDate, UUID actingAdminId) throws HttpException {
        if (id.equals(actingAdminId)) {
            throw new BadRequestException("Vous ne pouvez pas suspendre votre propre compte.");
        }

        AppUser user = appUserService.findById(id);

        if (endDate != null && endDate.isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Impossible de définir une date de suspension dans le passé");
        }

        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw new BadRequestException("Impossible de suspendre un compte qui n'a pas le statut Actif");
        }

        user.setStatus(AccountStatus.SUSPENDED);
        user.setSuspensionEndDate(endDate);

        userRepo.save(user);
        log.info("Compte {} suspendu (fin : {})", id, endDate == null ? "définitive" : endDate);
    }

    @Transactional
    public void reactivate(UUID id) throws HttpException {
        AppUser user = appUserService.findById(id);

        if (user.getStatus() != AccountStatus.SUSPENDED) {
            throw new BadRequestException("Le statut du compte que vous avez sélectionné n'est pas suspendu.");
        }

        user.setStatus(AccountStatus.ACTIVE);
        user.setSuspensionEndDate(null);

        userRepo.save(user);
        log.info("Compte {} réactivé", id);
    }

    /**
     * Reactivates every suspended account whose suspension end date is reached (RG11).
     *
     * @return the number of accounts reactivated
     */
    @Transactional
    public int reactivateExpiredSuspensions() {
        List<AppUser> expired = userRepo.findByStatusAndSuspensionEndDateLessThanEqual(
                AccountStatus.SUSPENDED, LocalDateTime.now());

        for (AppUser user : expired) {
            user.setStatus(AccountStatus.ACTIVE);
            user.setSuspensionEndDate(null);
        }

        userRepo.saveAll(expired);
        return expired.size();
    }

    /**
     * Explains why a login was refused, but only to someone who knows the password,
     * so that the state of an account is never revealed to a stranger.
     *
     * @param email       email used to log in
     * @param rawPassword password typed by the user
     * @return a message when the password is correct and the account cannot log in
     *         (suspended, not activated), otherwise empty
     */
    public Optional<String> explainLoginRefusal(String email, String rawPassword) {
        return userRepo.findByEmail(email)
                .filter(user -> passwordEncoder.matches(rawPassword, user.getHashedPassword()))
                .flatMap(user -> switch (user.getStatus()) {
                    case SUSPENDED -> Optional.of(suspensionMessage(user));
                    case INACTIVE, PENDING_ACTIVATION -> Optional.of(
                            "Votre compte n'est pas encore activé. Cliquez sur le lien d'activation reçu par email.");
                    default -> Optional.empty();
                });
    }

    private String suspensionMessage(AppUser user) {
        LocalDateTime endDate = user.getSuspensionEndDate();

        if (endDate == null) {
            return "Votre compte est suspendu.";
        }

        return "Votre compte est suspendu jusqu'au " + endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + ".";
    }
}
