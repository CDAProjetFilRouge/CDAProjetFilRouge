package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.Event;
import fr.diginamic.hubevenementiel.entities.Inscription;
import fr.diginamic.hubevenementiel.enums.InscriptionStatus;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.EventRepo;
import fr.diginamic.hubevenementiel.repositories.InscriptionRepo;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.transaction.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InscriptionService {

    private final InscriptionRepo inscriptionRepo;
    private final EventRepo eventRepo;

    public InscriptionService(InscriptionRepo inscriptionRepo, EventRepo eventRepo) {
        this.inscriptionRepo = inscriptionRepo;
        this.eventRepo = eventRepo;
    }

    @Transactional
    public boolean isEventFull(Long eventId) throws HttpException {
        Event event = eventRepo.findByIdForUpdate(eventId)
                .orElseThrow(() -> new NotFoundException("Aucun évènement trouvé avec cet identifiant."));

        long confirmedCount = inscriptionRepo.countByEventIdAndStatus(eventId, InscriptionStatus.CONFIRMED);

        return confirmedCount >= event.getMaxCapacity();
    }

    public List<Inscription> getAllInscription() {
        return inscriptionRepo.findAll();
    }

    public Inscription getInscriptionById(Long id) throws HttpException {
        Optional<Inscription> inscription = inscriptionRepo.findById(id);

        if (inscription.isEmpty()) {
            throw new NotFoundException("No inscription found with id: " + id);
        }

        return inscription.get();
    }

    public List<Inscription> findByUser(Long id, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<Inscription> inscriptions = inscriptionRepo.findByUserId(id, pageable).getContent();

        if (inscriptions.isEmpty()) {
            throw new NotFoundException("No inscriptions found with user id: " + id);
        }

        return inscriptions;
    }

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

    @Transactional
    public Inscription cancelByMember(Long id) throws HttpException {
        Inscription inscription = getInscriptionById(id);

        if (inscription.getStatus() == InscriptionStatus.CANCELED) {
            throw new ConflictException("Cette inscription est déjà annulée.");
        }

        inscription.cancelPerMember();

        return inscriptionRepo.save(inscription);
    }

    @Transactional
    public Inscription cancelByOrganizer(Long id, String motif) throws HttpException {
        Inscription inscription = getInscriptionById(id);

        if (inscription.getStatus() == InscriptionStatus.CANCELED) {
            throw new ConflictException("Cette inscription est déjà annulée.");
        }

        inscription.cancelPerOrganizer(motif);

        return inscriptionRepo.save(inscription);
    }

    @Transactional
    public void addInscription(Inscription inscription) {
        inscriptionRepo.save(inscription);
    }

    @Transactional
    public void updateInscription(Inscription inscription) throws HttpException {
        Optional<Inscription> inscriptionDB = inscriptionRepo.findById(inscription.getId());
        if (inscriptionDB.isEmpty()) {
            throw new NotFoundException("No inscription found with id: " + inscription.getId());
        }

        inscriptionDB.get().setUser(inscription.getUser());
        inscriptionDB.get().setEvent(inscription.getEvent());
        inscriptionDB.get().setInscriptionDate(inscription.getInscriptionDate());
        inscriptionDB.get().setStatus(inscription.getStatus());
        inscriptionDB.get().setPrice(inscription.getPrice());
        inscriptionDB.get().setCancellationDate(inscription.getCancellationDate());
        inscriptionDB.get().setCancelObject(inscription.getCancelObject());
        inscriptionDB.get().setCanceledById(inscription.getCanceledById());
    }

    @Transactional
    public void deleteInscription(Long id) throws HttpException {
        Optional<Inscription> inscription = inscriptionRepo.findById(id);
        if (inscription.isEmpty()) {
            throw new NotFoundException("No inscription found with id: " + id);
        }

        inscriptionRepo.delete(inscription.get());
    }
}
