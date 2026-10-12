package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
public class WaitingListService {

    private static final Set<AccountStatus> NOT_PROMOTABLE = EnumSet.of(AccountStatus.SUSPENDED,
            AccountStatus.ANONYMIZE);

    private final EventRepo eventRepo;
    private final InscriptionRepo inscriptionRepo;
    private final ApplicationEventPublisher eventPublisher;

    public WaitingListService(EventRepo eventRepo, InscriptionRepo inscriptionRepo,
            ApplicationEventPublisher eventPublisher) {
        this.eventRepo = eventRepo;
        this.inscriptionRepo = inscriptionRepo;
        this.eventPublisher = eventPublisher;
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
        Event event = lockEvent(eventId);
        if (hasStarted(event)) {
            return;
        }
        promote(eventId, 1);
    }

    /**
     * Fills every free spot of the event from the waiting list, oldest first.
     * Used when the capacity of an event is raised.
     *
     * @param eventId id of the event whose waiting list is promoted
     * @throws NotFoundException if the event does not exist
     */
    @Transactional
    public void fillFreeSpots(Long eventId) throws NotFoundException {
        Event event = lockEvent(eventId);
        if (hasStarted(event) || event.getMaxCapacity() == null) {
            return;
        }

        long confirmed = inscriptionRepo.countByEventIdAndStatus(eventId, InscriptionStatus.CONFIRMED);
        long freeSpots = event.getMaxCapacity() - confirmed;
        if (freeSpots > 0) {
            promote(eventId, freeSpots);
        }
    }

    private Event lockEvent(Long eventId) throws NotFoundException {
        return eventRepo.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun évènement trouvé avec cet identifiant."));
    }

    private boolean hasStarted(Event event) {
        return event.getStartDateTime() != null && event.getStartDateTime().isBefore(LocalDateTime.now());
    }

    private void promote(Long eventId, long count) {
        List<Inscription> waiting = inscriptionRepo
                .findByEventIdAndStatusOrderByInscriptionDateAsc(eventId, InscriptionStatus.WAITING_LIST);

        waiting.stream()
                .filter(inscription -> inscription.getUser() == null
                        || !NOT_PROMOTABLE.contains(inscription.getUser().getStatus()))
                .limit(count)
                .forEach(next -> {
                    next.setStatus(InscriptionStatus.CONFIRMED);
                    inscriptionRepo.save(next);
                    notifyPromotion(next);
                });
    }

    private void notifyPromotion(Inscription promoted) {
        if (promoted.getUser() == null || promoted.getUser().getEmail() == null || promoted.getEvent() == null) {
            return;
        }
        eventPublisher.publishEvent(
                new InscriptionPromotedEvent(promoted.getUser().getEmail(), promoted.getEvent().getTitle()));
    }
}
