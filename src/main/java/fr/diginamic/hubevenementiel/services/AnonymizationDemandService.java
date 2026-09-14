package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AnonymizationDemand;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.RequestStatus;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.AnonymizationDemandRepo;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AnonymizationDemandService {

    private final AnonymizationDemandRepo anonymizationDemandRepository;

    private final AppUserService appUserService;

    public AnonymizationDemandService(AnonymizationDemandRepo anonymizationDemandRepository,
            AppUserService appUserService) {
        this.anonymizationDemandRepository = anonymizationDemandRepository;
        this.appUserService = appUserService;
    }

    public List<AnonymizationDemand> findAllDemands(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return anonymizationDemandRepository.findAll(pageable).getContent();
    }

    public AnonymizationDemand findById(Long demandId) throws HttpException {
        Optional<AnonymizationDemand> optionalDemand = anonymizationDemandRepository.findById(demandId);

        if (optionalDemand.isEmpty()) {
            throw new NotFoundException("Aucune demande trouvée avec cet identifiant.");
        }

        return optionalDemand.get();
    }

    public List<AnonymizationDemand> findByStatus(int page, int size, RequestStatus status) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (status == null) {
            throw new BadRequestException("Veuillez renseigner un statut pour votre demande.");
        }

        return anonymizationDemandRepository.findByRequestStatus(pageable, status).getContent();
    }

    public List<AnonymizationDemand> findByDemandBetweenDates(int page, int size, LocalDateTime startDate,
            LocalDateTime endDate) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("La date de début ne peut pas être postérieure à la date de fin.");
        }

        return anonymizationDemandRepository.findByDemandDateBetween(pageable, startDate, endDate).getContent();
    }

    public List<AnonymizationDemand> findApprovedDemandBetweenDates(int page, int size, LocalDateTime startDate,
            LocalDateTime endDate) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("La date de début ne peut pas être postérieure à la date de fin.");
        }

        return anonymizationDemandRepository.findByApprovedDateBetween(pageable, startDate, endDate).getContent();
    }

    public List<AnonymizationDemand> findByRequesterId(int page, int size, Long id) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (id == null) {
            throw new BadRequestException("Veuillez rentrer un identifiant valide.");
        }

        return anonymizationDemandRepository.findByRequesterId(pageable, id).getContent();
    }

    public List<AnonymizationDemand> findByRequesterLastName(int page, int size, String lastName) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (lastName == null) {
            throw new BadRequestException("Veuillez rentrer un nom valide.");
        }

        return anonymizationDemandRepository.findByRequesterLastName(pageable, lastName).getContent();
    }

    public List<AnonymizationDemand> findByRequesterFirstName(int page, int size, String firstName)
            throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (firstName == null) {
            throw new BadRequestException("Veuillez rentrer un prénom valide.");
        }

        return anonymizationDemandRepository.findByRequesterFirstName(pageable, firstName).getContent();
    }

    public List<AnonymizationDemand> findByRequesterEmail(int page, int size, String email) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (email == null) {
            throw new BadRequestException("Veuillez rentrer un email valide.");
        }

        return anonymizationDemandRepository.findByRequesterEmail(pageable, email).getContent();
    }

    // ajouter liste déroulante sur le front pour gérer les admins sur la recherche
    // de quel admin a gérée quelles demandes
    public List<AnonymizationDemand> findByAdminId(int page, int size, Long id) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (id == null) {
            throw new BadRequestException("Veuillez rentrer un identifiant valide.");
        }

        return anonymizationDemandRepository.findByAdminId(pageable, id).getContent();
    }

    public List<AnonymizationDemand> findByAdminLastName(int page, int size, String lastName) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (lastName == null) {
            throw new BadRequestException("Veuillez rentrer un nom valide.");
        }

        return anonymizationDemandRepository.findByAdminLastName(pageable, lastName).getContent();
    }

    public List<AnonymizationDemand> findByAdminFirstName(int page, int size, String firstName) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (firstName == null) {
            throw new BadRequestException("Veuillez rentrer un prénom valide.");
        }

        return anonymizationDemandRepository.findByAdminFirstName(pageable, firstName).getContent();
    }

    public List<AnonymizationDemand> findByAdminEmail(int page, int size, String email) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (email == null) {
            throw new BadRequestException("Veuillez rentrer un email valide.");
        }

        return anonymizationDemandRepository.findByAdminEmail(pageable, email).getContent();
    }

    public boolean anonymizationDemandChecker(AnonymizationDemand anonymizationDemand) throws HttpException {

        if (anonymizationDemand == null) {
            throw new BadRequestException("La demande d'anonymisation ne peut pas être nul.");
        }

        AppUser requester = anonymizationDemand.getRequester();

        if (requester == null) {
            throw new BadRequestException("L'utilisateur doit être fourni.");
        }

        appUserService.findById(requester.getId());

        return true;
    }

    @Transactional
    public AnonymizationDemand createDemand(AnonymizationDemand anonymizationDemand) throws HttpException {

        anonymizationDemand.setRequestStatus(RequestStatus.PENDING);
        anonymizationDemand.setDemandDate(LocalDateTime.now());
        anonymizationDemand.setAdmin(null);
        anonymizationDemand.setApprovedDate(null);

        anonymizationDemandChecker(anonymizationDemand);

        return anonymizationDemandRepository.save(anonymizationDemand);
    }

    @Transactional
    public AnonymizationDemand updateDemand(Long demandId, AnonymizationDemand modifiedDemand) throws HttpException {

        AnonymizationDemand existingDemand = findById(demandId);

        anonymizationDemandChecker(modifiedDemand);

        existingDemand.setRequester(modifiedDemand.getRequester());
        existingDemand.setAdmin(modifiedDemand.getAdmin());
        existingDemand.setRequestStatus(modifiedDemand.getRequestStatus());
        existingDemand.setApprovedDate(modifiedDemand.getApprovedDate());

        return anonymizationDemandRepository.save(existingDemand);
    }

    @Transactional
    public void deleteDemand(Long demandId) throws HttpException {

        AnonymizationDemand demand = findById(demandId);

        anonymizationDemandRepository.delete(demand);
    }

}
