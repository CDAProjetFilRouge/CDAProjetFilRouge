package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WaitingListService {

    private final EventRepo eventRepo;
    private final InscriptionRepo inscriptionRepo;

    public WaitingListService(EventRepo eventRepo, InscriptionRepo inscriptionRepo) {
        this.eventRepo = eventRepo;
        this.inscriptionRepo = inscriptionRepo;
    }

    /**
     * Gives a freed spot to the oldest inscription of the waiting list (RG24).
     * The event row is locked so two simultaneous cancellations never promote
     * the same person.
     *
     * @param eventId id of the event whose waiting list is promoted
     * @throws NotFoundException if the event does not exist
     */
    @Transactional
    public void promoteNext(Long eventId) throws NotFoundException {
        eventRepo.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun évènement trouvé avec cet identifiant."));

        inscriptionRepo.findFirstByEventIdAndStatusOrderByInscriptionDateAsc(eventId, InscriptionStatus.WAITING_LIST)
                .ifPresent(next -> {
                    next.setStatus(InscriptionStatus.CONFIRMED);
                    inscriptionRepo.save(next);
                });
    }
}
