package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.enums.EventStatus;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.ForbiddenException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InscriptionService {

    private final InscriptionRepo inscriptionRepo;
    private final EventRepo eventRepo;
    private final AppUserService appUserService;
    private final EventService eventService;
    private final EmailService emailService;

    public InscriptionService(InscriptionRepo inscriptionRepo, EventRepo eventRepo,
            AppUserService appUserService, EventService eventService, EmailService emailService) {
        this.inscriptionRepo = inscriptionRepo;
        this.eventRepo = eventRepo;
        this.appUserService = appUserService;
        this.eventService = eventService;
        this.emailService = emailService;
    }

    /**
     *
     * @param eventId id of the event to perform the check on
     * @return true if the check passed else false
     * @throws HttpException
     */
    @Transactional
    public boolean isEventFull(Long eventId) throws HttpException {
        Event event = eventRepo.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun évènement trouvé avec cet identifiant."));

        long confirmedCount = inscriptionRepo.countByEventIdAndStatus(eventId, InscriptionStatus.CONFIRMED);

        return confirmedCount >= event.getMaxCapacity();
    }

    /**
     *
     * @param userId  id of the user to associate the inscription with
     * @param eventId id of the event to associate the inscription with
     * @return inscription saved in the DB
     * @throws HttpException
     */
    @org.springframework.transaction.annotation.Transactional(isolation = Isolation.READ_COMMITTED)
    public Inscription register(Long userId, Long eventId) throws HttpException {
        AppUser user = appUserService.findById(userId);
        Event event = eventService.findById(eventId);

        if (event.getStatus() != EventStatus.PUBLISHED || event.getStartDateTime().isBefore(LocalDateTime.now())) {
            throw new ConflictException("Les inscriptions à cet évènement sont closes.");
        }

        if (inscriptionRepo.existsByUserIdAndEventIdAndStatusNot(userId, eventId, InscriptionStatus.CANCELED)) {
            throw new ConflictException("Vous êtes déjà inscrit à cet évènement.");
        }

        Inscription inscription = new Inscription();
        inscription.setUser(user);
        inscription.setEvent(event);
        inscription.setInscriptionDate(LocalDateTime.now());
        boolean isAffiliated = user.getClubs().stream().anyMatch(
                club -> club.getEndValidityDate() == null || club.getEndValidityDate().isAfter(LocalDate.now()));
        inscription.setPrice(isAffiliated ? event.getAffiliatePrice() : event.getNonAffiliatePrice());
        inscription.setStatus(isEventFull(eventId) ? InscriptionStatus.WAITING_LIST : InscriptionStatus.CONFIRMED);

        return inscriptionRepo.save(inscription);
    }

    public List<Inscription> getAllInscription() {
        return inscriptionRepo.findAll();
    }

    /**
     *
     * @param id id of the inscription to find
     * @return object of type inscription
     * @throws HttpException
     */
    public Inscription getInscriptionById(Long id) throws HttpException {
        Optional<Inscription> inscription = inscriptionRepo.findById(id);

        if (inscription.isEmpty()) {
            throw new NotFoundException("No inscription found with id: " + id);
        }

        return inscription.get();
    }

    /**
     *
     * @param id   id of the user to find inscription associated with
     * @param page starting page
     * @param size number of entries per page
     * @return a list of inscription
     * @throws HttpException
     */
    public List<Inscription> findByUser(Long id, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Inscription> inscriptions = inscriptionRepo.findByUserId(id, pageable).getContent();

        if (inscriptions.isEmpty()) {
            throw new NotFoundException("No inscriptions found with user id: " + id);
        }

        return inscriptions;
    }

    /**
     *
     * @param id   id of the event to find inscription associated with
     * @param page starting page
     * @param size number of entries
     * @return a list of inscription
     * @throws HttpException
     */
    public List<Inscription> findByEvent(Long id, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Inscription> inscriptions = inscriptionRepo.findByEventId(id, pageable).getContent();

        if (inscriptions.isEmpty()) {
            throw new NotFoundException("No inscriptions found with event id: " + id);
        }

        return inscriptions;
    }

    public List<Inscription> getInscriptionDate(LocalDateTime dateMin, LocalDateTime dateMax, int page, int size)
            throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Inscription> inscriptions = inscriptionRepo.findByInscriptionDateBetween(dateMin, dateMax, pageable)
                .getContent();

        if (inscriptions.isEmpty()) {
            throw new NotFoundException(
                    "No inscription where inscriptionDate is between " + dateMin + " and " + dateMax);
        }

        return inscriptions;
    }

    public List<Inscription> getInscriptionStatus(InscriptionStatus status, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Inscription> inscriptions = inscriptionRepo.findByStatus(status, pageable).getContent();

        if (inscriptions.isEmpty()) {
            throw new NotFoundException("No inscription found where status is: " + status);
        }

        return inscriptions;
    }

    public List<Inscription> getInscriptionCancellationDate(LocalDateTime dateMin, LocalDateTime dateMax, int page,
            int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Inscription> inscriptions = inscriptionRepo.findByCancellationDate(dateMin, dateMax, pageable)
                .getContent();

        if (inscriptions.isEmpty()) {
            throw new NotFoundException(
                    "Not inscriptions found where cancellation date between " + dateMin + " and " + dateMax);
        }

        return inscriptions;
    }

    public List<Inscription> getInscriptionCanceledId(Long id, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Inscription> inscriptions = inscriptionRepo.findByCanceledById(id, pageable).getContent();

        if (inscriptions.isEmpty()) {
            throw new NotFoundException("No inscription found for cancelled id: " + id);
        }

        return inscriptions;
    }

    /**
     *
     * @param id id of the inscription to cancel
     * @return inscription with the updated status
     * @throws HttpException
     */
    @Transactional
    public Inscription cancelByMember(Long id, AppUserPrincipal principal) throws HttpException {
        Inscription inscription = getInscriptionById(id);

        if (!inscription.getUser().getId().equals(principal.id())) {
            throw new ForbiddenException("Vous ne pouvez annuler que vos propres inscriptions.");
        }

        if (inscription.getStatus() == InscriptionStatus.CANCELED) {
            throw new ConflictException("Cette inscription est déjà annulée.");
        }

        boolean freesASpot = inscription.getStatus() == InscriptionStatus.CONFIRMED;

        inscription.cancelPerMember();
        Inscription saved = inscriptionRepo.save(inscription);

        if (freesASpot) {
            promoteNextInWaitingList(inscription.getEvent().getId());
        }

        return saved;
    }

    /**
     *
     * @param id    id of the inscription to cancel
     * @param motif reason of the cancel
     * @return inscription with the updated status
     * @throws HttpException
     */
    @Transactional
    public Inscription cancelByOrganizer(Long id, String motif) throws HttpException {
        if (motif == null || motif.isBlank()) {
            throw new BadRequestException(
                    "Un motif est obligatoire pour annuler une inscription en tant qu'organisateur.");
        }

        Inscription inscription = getInscriptionById(id);

        if (inscription.getStatus() == InscriptionStatus.CANCELED) {
            throw new ConflictException("Cette inscription est déjà annulée.");
        }

        boolean freesASpot = inscription.getStatus() == InscriptionStatus.CONFIRMED;

        inscription.cancelPerOrganizer(motif);
        Inscription saved = inscriptionRepo.save(inscription);

        emailService.sendInscriptionCancellationEmail(
                inscription.getUser().getEmail(), inscription.getEvent().getTitle(), motif);

        if (freesASpot) {
            promoteNextInWaitingList(inscription.getEvent().getId());
        }

        return saved;
    }

    /**
     *
     * @param eventId id of the event to find inscription associated with
     * @throws HttpException
     */
    @Transactional
    public void promoteNextInWaitingList(Long eventId) throws HttpException {
        eventRepo.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun évènement trouvé avec cet identifiant."));

        inscriptionRepo.findFirstByEventIdAndStatusOrderByInscriptionDateAsc(eventId, InscriptionStatus.WAITING_LIST)
                .ifPresent(next -> {
                    next.setStatus(InscriptionStatus.CONFIRMED);
                    inscriptionRepo.save(next);
                });
    }

    // Non utilisée par le controller, remplacée par register(). Désactivée pour
    // éviter un doublon de logique.
    // @Transactional
    // public void addInscription(Inscription inscription) {
    // inscriptionRepo.save(inscription);
    // }

    // Désactivée : aucun champ de cette entité n'a de raison d'être modifié
    // librement.
    // user/event référent l'inscription elle-même, inscriptionDate est posée à la
    // création,
    // et status/price/cancellationDate/cancelObject/canceledById sont pilotés par
    // register()
    // et cancelByMember()/cancelByOrganizer(). À réactiver seulement si un vrai
    // besoin de
    // correction générique (admin) apparaît, avec une liste de champs explicitement
    // restreinte.
    // @Transactional
    // public void updateInscription(Inscription inscription) throws HttpException {
    // Optional<Inscription> inscriptionDB =
    // inscriptionRepo.findById(inscription.getId());
    // if (inscriptionDB.isEmpty()) {
    // throw new NotFoundException("No inscription found with id: " +
    // inscription.getId());
    // }
    //
    // inscriptionDB.get().setUser(inscription.getUser());
    // inscriptionDB.get().setEvent(inscription.getEvent());
    // inscriptionDB.get().setInscriptionDate(inscription.getInscriptionDate());
    // inscriptionDB.get().setStatus(inscription.getStatus());
    // inscriptionDB.get().setPrice(inscription.getPrice());
    // inscriptionDB.get().setCancellationDate(inscription.getCancellationDate());
    // inscriptionDB.get().setCancelObject(inscription.getCancelObject());
    // inscriptionDB.get().setCanceledById(inscription.getCanceledById());
    // }

    // Non utilisée par le controller, remplacée par
    // cancelByMember()/cancelByOrganizer() (annulation, pas suppression).
    // @Transactional
    // public void deleteInscription(Long id) throws HttpException {
    // Optional<Inscription> inscription = inscriptionRepo.findById(id);
    // if (inscription.isEmpty()) {
    // throw new NotFoundException("No inscription found with id: " + id);
    // }
    //
    // inscriptionRepo.delete(inscription.get());
    // }
}
