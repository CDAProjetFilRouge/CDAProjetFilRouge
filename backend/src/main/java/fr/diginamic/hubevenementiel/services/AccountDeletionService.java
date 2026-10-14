package fr.diginamic.hubevenementiel.services;

import java.util.UUID;
import org.springframework.stereotype.Service;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import jakarta.transaction.Transactional;

@Service
public class AccountDeletionService {

    private final AppUserService appUserService;
    private final UserRepo userRepo;
    private final AccountDeletionGuard guard;
    private final AccountDataCleaner cleaner;

    public AccountDeletionService(AppUserService appUserService, UserRepo userRepo, AccountDeletionGuard guard,
            AccountDataCleaner cleaner) {
        this.appUserService = appUserService;
        this.userRepo = userRepo;
        this.guard = guard;
        this.cleaner = cleaner;
    }

    /**
     * Deletes an account that has no meaningful activity. Its tokens, inscriptions
     * (spots are given back to the waiting list) and own anonymization demands are
     * removed with it. Accounts that organize events, own a club, authored a legal
     * document or handled anonymization demands are refused: anonymization is the
     * way to go for them. Posted comments also refuse the deletion, unless
     * deleteComments is true, in which case they are deleted with the account.
     *
     * @param id             id of the AppUser to delete
     * @param deleteComments true to delete the comments of the account along with
     *                       it
     * @throws HttpException NotFoundException if the user does not exist,
     *                       ConflictException if the account has activity that
     *                       prevents its deletion (code
     *                       {@link AccountDeletionGuard#ACCOUNT_HAS_COMMENTS} when
     *                       the comments are the only obstacle)
     */
    @Transactional
    public void deleteAccount(UUID id, boolean deleteComments) throws HttpException {
        AppUser user = appUserService.findById(id);

        boolean hasComments = guard.check(id, deleteComments);

        cleaner.clean(user, hasComments);
        userRepo.delete(user);
    }
}
