package fr.diginamic.hubevenementiel.services;

import java.util.UUID;
import org.springframework.stereotype.Service;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.AnonymizationDemandRepo;
import fr.diginamic.hubevenementiel.repositories.CommentRepo;
import fr.diginamic.hubevenementiel.repositories.RefreshTokenRepo;
import fr.diginamic.hubevenementiel.repositories.TokenRepo;

/**
 * Removes everything that hangs off an account before the account itself is
 * deleted: comments (when requested), inscriptions (spots are given back to the
 * waiting list), tokens, refresh tokens and its own anonymization demands.
 */
@Service
public class AccountDataCleaner {

    private final CommentRepo commentRepo;
    private final TokenRepo tokenRepo;
    private final RefreshTokenRepo refreshTokenRepo;
    private final AnonymizationDemandRepo anonymizationDemandRepo;
    private final WaitingListService waitingListService;

    public AccountDataCleaner(CommentRepo commentRepo, TokenRepo tokenRepo, RefreshTokenRepo refreshTokenRepo,
            AnonymizationDemandRepo anonymizationDemandRepo, WaitingListService waitingListService) {
        this.commentRepo = commentRepo;
        this.tokenRepo = tokenRepo;
        this.refreshTokenRepo = refreshTokenRepo;
        this.anonymizationDemandRepo = anonymizationDemandRepo;
        this.waitingListService = waitingListService;
    }

    /**
     * @param user           the account about to be deleted
     * @param deleteComments true to delete the comments of the account as well
     * @throws NotFoundException if an event of one of its inscriptions does not exist
     */
    public void clean(AppUser user, boolean deleteComments) throws NotFoundException {
        UUID id = user.getId();

        if (deleteComments) {
            commentRepo.deleteByAuthorId(id);
        }
        waitingListService.removeInscriptionsOf(id);
        tokenRepo.deleteByUser(user);
        refreshTokenRepo.deleteByUser(user);
        anonymizationDemandRepo.deleteByRequesterId(id);
    }
}
