package fr.diginamic.hubevenementiel.services;

import java.util.UUID;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.EventStatus;
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
    public void promoteNext(UUID eventId) throws NotFoundException {
        Event event = lockEvent(eventId);
        if (hasStarted(event)) {
            return;
        }
        promote(eventId, 1);
    }

    /**
     * Removes every inscription of a user. A removed confirmed inscription on an
     * upcoming event frees a spot, which goes to the next person in the waiting list.
     * Used when an account is deleted.
     *
     * @param userId id of the user whose inscriptions are removed
     * @throws NotFoundException if an event of the inscriptions does not exist
     */
    @Transactional
    public void removeInscriptionsOf(UUID userId) throws NotFoundException {
        List<Inscription> inscriptions = inscriptionRepo.findAllByUserId(userId);

        for (Inscription inscription : inscriptions) {
            boolean freesASpot = inscription.getStatus() == InscriptionStatus.CONFIRMED
                    && inscription.getEvent().getStartDateTime().isAfter(LocalDateTime.now());
            UUID eventId = inscription.getEvent().getId();

            inscriptionRepo.delete(inscription);
            inscriptionRepo.flush();

            if (freesASpot) {
                promoteNext(eventId);
            }
        }
    }

    /**
     * Fills every free spot of the event from the waiting list, oldest first.
     * Used when the capacity of an event is raised.
     *
     * @param eventId id of the event whose waiting list is promoted
     * @throws NotFoundException if the event does not exist
     */
    @Transactional
    public void fillFreeSpots(UUID eventId) throws NotFoundException {
        Event event = lockEvent(eventId);
        if (hasStarted(event) || event.getStatus() == EventStatus.CANCELLED || event.getMaxCapacity() == null) {
            return;
        }

        long confirmed = inscriptionRepo.countByEventIdAndStatus(eventId, InscriptionStatus.CONFIRMED);
        long freeSpots = event.getMaxCapacity() - confirmed;
        if (freeSpots > 0) {
            promote(eventId, freeSpots);
        }
    }

    private Event lockEvent(UUID eventId) throws NotFoundException {
        return eventRepo.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun évènement trouvé avec cet identifiant."));
    }

    private boolean hasStarted(Event event) {
        return event.getStartDateTime() != null && event.getStartDateTime().isBefore(LocalDateTime.now());
    }

    private void promote(UUID eventId, long count) {
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
