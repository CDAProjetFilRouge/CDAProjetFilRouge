package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

@Service
public class WaitingListService {

    private static final Set<AccountStatus> NOT_PROMOTABLE = EnumSet.of(AccountStatus.SUSPENDED,
            AccountStatus.ANONYMIZE);

    private final EventRepo eventRepo;
    private final InscriptionRepo inscriptionRepo;

    public WaitingListService(EventRepo eventRepo, InscriptionRepo inscriptionRepo) {
        this.eventRepo = eventRepo;
        this.inscriptionRepo = inscriptionRepo;
    }

    /**
     * Gives a freed spot to the oldest inscription of the waiting list (RG24).
     * Suspended or anonymized accounts are skipped, and nothing happens once the
     * event has started. The event row is locked so two simultaneous
     * cancellations never promote the same person.
     *
     * @param eventId id of the event whose waiting list is promoted
     * @throws NotFoundException if the event does not exist
     */
    @Transactional
    public void promoteNext(Long eventId) throws NotFoundException {
        Event event = eventRepo.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun évènement trouvé avec cet identifiant."));

        if (event.getStartDateTime() != null && event.getStartDateTime().isBefore(LocalDateTime.now())) {
            return;
        }

        inscriptionRepo.findByEventIdAndStatusOrderByInscriptionDateAsc(eventId, InscriptionStatus.WAITING_LIST)
                .stream()
                .filter(inscription -> inscription.getUser() == null
                        || !NOT_PROMOTABLE.contains(inscription.getUser().getStatus()))
                .findFirst()
                .ifPresent(next -> {
                    next.setStatus(InscriptionStatus.CONFIRMED);
                    inscriptionRepo.save(next);
                });
    }
}
