package fr.diginamic.hubevenementiel.services;

import java.util.UUID;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.repositories.AnonymizationDemandRepo;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import fr.diginamic.hubevenementiel.repositories.CommentRepo;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.LegalDocumentRepo;

/**
 * Decides whether an account may be deleted. Accounts that organize events, own a
 * club, authored a legal document or handled anonymization demands are refused:
 * anonymization is the way to go for them. Posted comments also refuse the
 * deletion, unless the caller accepts that they are deleted with the account.
 */
@Service
public class AccountDeletionGuard {

    public static final String ACCOUNT_HAS_COMMENTS = "ACCOUNT_HAS_COMMENTS";

    private final EventRepo eventRepo;
    private final ClubRepo clubRepo;
    private final LegalDocumentRepo legalDocumentRepo;
    private final AnonymizationDemandRepo anonymizationDemandRepo;
    private final CommentRepo commentRepo;

    public AccountDeletionGuard(EventRepo eventRepo, ClubRepo clubRepo, LegalDocumentRepo legalDocumentRepo,
            AnonymizationDemandRepo anonymizationDemandRepo, CommentRepo commentRepo) {
        this.eventRepo = eventRepo;
        this.clubRepo = clubRepo;
        this.legalDocumentRepo = legalDocumentRepo;
        this.anonymizationDemandRepo = anonymizationDemandRepo;
        this.commentRepo = commentRepo;
    }

    /**
     * Checks that the account can be deleted.
     *
     * @param id             id of the account
     * @param deleteComments true if the comments of the account may be deleted along with it
     * @return true if the account has comments (they will have to be deleted with it)
     * @throws ConflictException if the account has activity that prevents its deletion
     *                           (code ACCOUNT_HAS_COMMENTS when the comments are the only obstacle)
     */
    public boolean check(UUID id, boolean deleteComments) throws ConflictException {
        List<String> blockers = new ArrayList<>();
        if (eventRepo.existsByOrganizerId(id)) {
            blockers.add("organise des évènements");
        }
        if (clubRepo.existsByOwnerId(id)) {
            blockers.add("est propriétaire d'un club");
        }
        if (legalDocumentRepo.existsByUserId(id)) {
            blockers.add("a publié des documents légaux");
        }
        if (anonymizationDemandRepo.existsByAdminId(id)) {
            blockers.add("a traité des demandes d'anonymisation");
        }

        boolean hasComments = commentRepo.existsByAuthorId(id);

        if (!blockers.isEmpty()) {
            if (hasComments) {
                blockers.add("a posté des commentaires");
            }
            throw new ConflictException("Ce compte ne peut pas être supprimé car il " + String.join(", ", blockers)
                    + ". Utilisez plutôt l'anonymisation.");
        }
        if (hasComments && !deleteComments) {
            throw new ConflictException(
                    "Ce compte a posté des commentaires. Utilisez plutôt l'anonymisation, ou confirmez la suppression du compte avec ses commentaires.",
                    ACCOUNT_HAS_COMMENTS);
        }

        return hasComments;
    }
}
