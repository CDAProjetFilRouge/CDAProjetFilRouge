package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AnonymizationDemand;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.RequestStatus;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.AnonymizationDemandRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
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

    /**
     *
     * @param principal the authenticated caller, requesting their own anonymization
     * @return a single object of anonymizationDemand
     * @throws HttpException
     */
    public AnonymizationDemand request(AppUserPrincipal principal) throws HttpException {

        AppUser user = appUserService.findById(principal.id());

        AnonymizationDemand anonymizationDemand = new AnonymizationDemand();

        anonymizationDemand.setRequester(user);

        return createDemand(anonymizationDemand);
    }

    /**
     *
     * @param id id of the demand to validate
     * @param principal the authenticated admin validating the demand
     * @return object of type demande
     * @throws HttpException
     */
    public AnonymizationDemand validate(Long id, AppUserPrincipal principal) throws HttpException {

        AppUser admin = appUserService.findById(principal.id());

        AnonymizationDemand anonymizationDemand = findById(id);

        appUserService.anonymizeAccount(anonymizationDemand.getRequester());

        anonymizationDemand.setAdmin(admin);
        anonymizationDemand.setApprovedDate(LocalDateTime.now());
        anonymizationDemand.setRequestStatus(RequestStatus.VALIDATE);

        return updateDemand(id, anonymizationDemand);
    }

    /**
     *
     * @param page starting page
     * @param size number of entries per pages
     * @return a list of demands
     */
    public List<AnonymizationDemand> findAllDemands(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        return anonymizationDemandRepository.findAll(pageable).getContent();
    }

    /**
     *
     * @param demandId id of the demand to find
     * @return object of type anonymizationDemand
     * @throws HttpException
     */
    public AnonymizationDemand findById(Long demandId) throws HttpException {
        Optional<AnonymizationDemand> optionalDemand = anonymizationDemandRepository.findById(demandId);

        if (optionalDemand.isEmpty()) {
            throw new NotFoundException("Aucune demande trouvée avec cet identifiant.");
        }

        return optionalDemand.get();
    }

    /**
     *
     * @param page starting page
     * @param size number of entries per page
     * @param status status to search demand associated with it
     * @return a list of demands
     * @throws HttpException
     */
    public List<AnonymizationDemand> search(int page, int size, RequestStatus status) throws HttpException {
        if (status != null) {
            return findByStatus(page, size, status);
        }

        return findAllDemands(page, size);
    }

    /**
     *
     * @param page starting page
     * @param size number of entries per pages
     * @param status to find demands associated with
     * @return a list of status with pagination info
     * @throws HttpException
     */
    public List<AnonymizationDemand> findByStatus(int page, int size, RequestStatus status) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (status == null) {
            throw new BadRequestException("Veuillez renseigner un statut pour votre demande.");
        }

        return anonymizationDemandRepository.findByRequestStatus(pageable, status).getContent();
    }

    /**
     *
     * @param page starting page
     * @param size  number of entries per page
     * @param startDate starting date at which we search demands
     * @param endDate maximum date at which we search demands
     * @return a list of demands
     * @throws HttpException
     */
    public List<AnonymizationDemand> findByDemandBetweenDates(int page, int size, LocalDateTime startDate,
            LocalDateTime endDate) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("La date de début ne peut pas être postérieure à la date de fin.");
        }

        return anonymizationDemandRepository.findByDemandDateBetween(pageable, startDate, endDate).getContent();
    }

    /**
     *
     * @param page starting page
     * @param size number of entries per page
     * @param startDate starting date at which we search demands
     * @param endDate maximum date at which we search demands
     * @return a list of demands
     * @throws HttpException
     */
    public List<AnonymizationDemand> findApprovedDemandBetweenDates(int page, int size, LocalDateTime startDate,
            LocalDateTime endDate) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException("La date de début ne peut pas être postérieure à la date de fin.");
        }

        return anonymizationDemandRepository.findByApprovedDateBetween(pageable, startDate, endDate).getContent();
    }

    /**
     *
     * @param page starting page
     * @param size number of entries per page
     * @param id id of the user that made the demand
     * @return a list of demands
     * @throws HttpException
     */
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

    /**
     *
     * @param page starting page
     * @param size number of entries per page
     * @param firstName search demands by the first name of the requester
     * @return a list of demands
     * @throws HttpException
     */
    public List<AnonymizationDemand> findByRequesterFirstName(int page, int size, String firstName)
            throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (firstName == null) {
            throw new BadRequestException("Veuillez rentrer un prénom valide.");
        }

        return anonymizationDemandRepository.findByRequesterFirstName(pageable, firstName).getContent();
    }

    /**
     *
     * @param page starting page
     * @param size number of entries per page
     * @param email email of the requester we want to find demand associated with
     * @return a list of demands with pagination info
     * @throws HttpException
     */
    public List<AnonymizationDemand> findByRequesterEmail(int page, int size, String email) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (email == null) {
            throw new BadRequestException("Veuillez rentrer un email valide.");
        }

        return anonymizationDemandRepository.findByRequesterEmail(pageable, email).getContent();
    }

    // ajouter liste déroulante sur le front pour gérer les admins sur la recherche
    // de quel admin a gérée quelles demandes

    /**
     *
     * @param page starting page
     * @param size number of entries per page
     * @param id id of the admin we want to find the demands associated with
     * @return a list of demands
     * @throws HttpException
     */
    public List<AnonymizationDemand> findByAdminId(int page, int size, Long id) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (id == null) {
            throw new BadRequestException("Veuillez rentrer un identifiant valide.");
        }

        return anonymizationDemandRepository.findByAdminId(pageable, id).getContent();
    }

    /**
     *
     * @param page starting page
     * @param size number of entries per page
     * @param lastName last name of the admin we want to find the demands associated with
     * @return a list of demands
     * @throws HttpException
     */
    public List<AnonymizationDemand> findByAdminLastName(int page, int size, String lastName) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (lastName == null) {
            throw new BadRequestException("Veuillez rentrer un nom valide.");
        }

        return anonymizationDemandRepository.findByAdminLastName(pageable, lastName).getContent();
    }
    /**
     *
     * @param page starting page
     * @param size number of entries per page
     * @param firstName first name of the admin we want to find the demands associated with
     * @return a list of demands
     * @throws HttpException
     */
    public List<AnonymizationDemand> findByAdminFirstName(int page, int size, String firstName) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (firstName == null) {
            throw new BadRequestException("Veuillez rentrer un prénom valide.");
        }

        return anonymizationDemandRepository.findByAdminFirstName(pageable, firstName).getContent();
    }

    /**
     *
     * @param page starting page
     * @param size number of entries per page
     * @param email email of the admin we want to find the demands associated with
     * @return a list of demands
     * @throws HttpException
     */
    public List<AnonymizationDemand> findByAdminEmail(int page, int size, String email) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);

        if (email == null) {
            throw new BadRequestException("Veuillez rentrer un email valide.");
        }

        return anonymizationDemandRepository.findByAdminEmail(pageable, email).getContent();
    }

    /**
     *
     * @param anonymizationDemand demand we want to do checks on
     * @return true if all check passed or false if not
     * @throws HttpException
     */
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

    /**
     *
     * @param anonymizationDemand demand to save in the DB
     * @return demand saved
     * @throws HttpException
     */
    @Transactional
    public AnonymizationDemand createDemand(AnonymizationDemand anonymizationDemand) throws HttpException {

        anonymizationDemand.setRequestStatus(RequestStatus.PENDING);
        anonymizationDemand.setDemandDate(LocalDateTime.now());
        anonymizationDemand.setAdmin(null);
        anonymizationDemand.setApprovedDate(null);

        anonymizationDemandChecker(anonymizationDemand);

        return anonymizationDemandRepository.save(anonymizationDemand);
    }

    /**
     *
     * @param demandId if of the demand to update
     * @param modifiedDemand details of the demand to update
     * @return the updated demand
     * @throws HttpException
     */
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

    /**
     *
     * @param demandId id of the demand to delete
     * @throws HttpException
     */
    @Transactional
    public void deleteDemand(Long demandId) throws HttpException {

        AnonymizationDemand demand = findById(demandId);

        anonymizationDemandRepository.delete(demand);
    }

}
