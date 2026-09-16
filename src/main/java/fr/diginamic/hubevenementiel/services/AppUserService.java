package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import jakarta.transaction.Transactional;
import org.aspectj.weaver.ast.Not;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class AppUserService {

    private UserRepo userRepo;

    public AppUserService(UserRepo userRepo) {
        this.userRepo = userRepo;
    }

    /**
     *
     * @param page starting page
     * @param size number of entries per page
     * @return a list of AppUser
     */
    public List<AppUser> findAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepo.findAll(pageable).getContent();
    }

    /**
     *
     * @param id id of the AppUser you want to find
     * @return a object of type AppUser
     * @throws NotFoundException
     */
    public AppUser findById(Long id) throws NotFoundException {
        AppUser user = userRepo.findById(id).orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        return user;
    }

    /**
     *
     * @param lastName last name to search an AppUser on
     * @param page starting page
     * @param size number of entries per page
     * @return a list of AppUser
     * @throws HttpException
     */
    public List<AppUser> findByLastName(String lastName, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByLastName(lastName, pageable).getContent();
        if (users.isEmpty()) {
            throw new NotFoundException("No users found with last name: " + lastName);
        }

        return users;
    }

    /**
     *
     * @param firstName first name to search an AppUser on
     * @param page starting page
     * @param size number of entries per page
     * @return a list of AppUser
     * @throws HttpException
     */
    public List<AppUser> findByFirstName(String firstName, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByFirstName(firstName, pageable).getContent();
        if (users.isEmpty()) {
            throw new NotFoundException("No users found with last name: " + firstName);
        }

        return users;
    }

    /**
     *
     * @param email email to search an AppUser on
     * @return an object of AppUser
     * @throws HttpException
     */
    public AppUser findByEmail(String email) throws HttpException {
        AppUser user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with this email: " + email));

        return user;
    }

    /**
     *
     * @param role role to search an AppUser on
     * @param page starting page
     * @param size number of entries per page
     * @return a list of AppUser
     * @throws HttpException
     */
    public List<AppUser> findByRole(Role role, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByRole(role, pageable).getContent();
        if (users.isEmpty()) {
            throw new NotFoundException("No users found with role: " + role);
        }

        return users;
    }

    /**
     *
     * @param status status to search an AppUser on
     * @param page starting page
     * @param size number of entries per page
     * @return a list of AppUser
     * @throws HttpException
     */
    public List<AppUser> findByStatus(AccountStatus status, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByStatus(status, pageable).getContent();
        if (users.isEmpty()) {
            throw new NotFoundException("No users found with status: " + status);
        }

        return users;
    }

    /**
     *
     * @param dateMin starting date to do the search on
     * @param dateMax maximum date to do the search on
     * @param page starting page
     * @param size number of entries per page
     * @return a list of AppUser
     * @throws HttpException
     */
    public List<AppUser> findBySuspensionEndDate(LocalDateTime dateMin, LocalDateTime dateMax, int page, int size)
            throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findBySuspensionEndDateBetween(dateMin, dateMax, pageable).getContent();
        if (users.isEmpty()) {
            throw new NotFoundException("No users found with end date between " + dateMin + " and " + dateMax);
        }

        return users;
    }

    /**
     *
     * @param dateMin starting date to do the search on
     * @param dateMax maximum date to do the search on
     * @param page starting page
     * @param size number of entries
     * @return  a list of AppUser
     * @throws HttpException
     */
    public List<AppUser> findByCreationDateBetween(LocalDate dateMin, LocalDate dateMax, int page, int size)
            throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByCreationDateBetween(dateMin, dateMax, pageable).getContent();
        if (users.isEmpty()) {
            throw new NotFoundException("No users found with creation date between" + dateMin + " and " + dateMax);
        }

        return users;
    }

    /**
     *
     * @param appUser AppUser to save in the DB
     * @return AppUser saved in the DB
     * @throws HttpException
     */
    @Transactional
    public AppUser createAccount(AppUser appUser) throws HttpException {
        appUserChecker(appUser, false);

        if (userRepo.existsByEmail(appUser.getEmail())) {
            throw new ConflictException("Un compte existe déjà avec cette adresse email.");
        }

        appUser.setStatus(AccountStatus.INACTIVE);
        appUser.setRole(Role.MEMBER);

        return userRepo.save(appUser);
    }

    /**
     *
     * @param appUser AppUser to save in the DB as admin
     * @param role role to assign the AppUser to
     * @return AppUser saved in the DB
     * @throws HttpException
     */
    // TODO securite : aucune verification que l'appelant est bien administrateur (pas d'auth branchee sur le projet pour l'instant, cf. #97)
    @Transactional
    public AppUser createAccountByAdmin(AppUser appUser, Role role) throws HttpException {
        appUserChecker(appUser, true);

        if (userRepo.existsByEmail(appUser.getEmail())) {
            throw new ConflictException("Un compte existe déjà avec cette adresse email.");
        }

        appUser.setStatus(AccountStatus.INACTIVE);
        appUser.setRole(role);

        return userRepo.save(appUser);
    }

    /**
     *
     * @param appUser AppUser to perform the checks on
     * @param phoneRequired set the state of the requirement
     * @return return true if the AppUser passed all checks else return false
     * @throws HttpException
     */
    public boolean appUserChecker(AppUser appUser, boolean phoneRequired) throws HttpException {

        if (appUser == null) {
            throw new BadRequestException("Le compte ne peut pas être nul.");
        }

        String lastName = appUser.getLastName();
        if (lastName == null || lastName.isBlank()) {
            throw new BadRequestException("Le nom doit contenir au moins un caractère.");
        }

        String firstName = appUser.getFirstName();
        if (firstName == null || firstName.isBlank()) {
            throw new BadRequestException("Le prénom doit contenir au moins un caractère.");
        }

        String email = appUser.getEmail();
        if (email == null || email.isBlank()) {
            throw new BadRequestException("L'adresse email est obligatoire.");
        }
        if (!email.matches("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$")) {
            throw new BadRequestException("L'adresse email n'est pas valide.");
        }

        String password = appUser.getHashedPassword();
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Le mot de passe est obligatoire.");
        }
        if (password.length() < 12) {
            throw new BadRequestException("Le mot de passe doit contenir au moins 12 caractères.");
        }

        String phone = appUser.getPhone();
        if (phoneRequired && (phone == null || phone.isBlank())) {
            throw new BadRequestException("Le téléphone est obligatoire pour un compte créé par un administrateur.");
        }

        return true;
    }

    /**
     *
     * @param id id of the AppUser to update
     * @param modifiedUser updated AppUser information
     * @return modified AppUser
     * @throws HttpException
     */
    @Transactional
    public AppUser updateOwnAccount(Long id, AppUser modifiedUser) throws HttpException {
        AppUser existing = findById(id);

        appUserChecker(modifiedUser, false);

        if (!existing.getEmail().equalsIgnoreCase(modifiedUser.getEmail())
                && userRepo.existsByEmail(modifiedUser.getEmail())) {
            throw new ConflictException("Un compte existe déjà avec cette adresse email.");
        }

        existing.setLastName(modifiedUser.getLastName());
        existing.setFirstName(modifiedUser.getFirstName());
        existing.setEmail(modifiedUser.getEmail());
        existing.setHashedPassword(modifiedUser.getHashedPassword());
        existing.setPhone(modifiedUser.getPhone());
        existing.setAddress(modifiedUser.getAddress());

        return userRepo.save(existing);
    }

    /**
     *
     * @param id if of the AppUser to update
     * @param modifiedUser updated AppUser information
     * @return modified AppUser
     * @throws HttpException
     */
    @Transactional
    public AppUser updateAccountByAdmin(Long id, AppUser modifiedUser) throws HttpException {
        AppUser existing = findById(id);

        appUserChecker(modifiedUser, true);

        if (!existing.getEmail().equalsIgnoreCase(modifiedUser.getEmail())
                && userRepo.existsByEmail(modifiedUser.getEmail())) {
            throw new ConflictException("Un compte existe déjà avec cette adresse email.");
        }

        existing.setLastName(modifiedUser.getLastName());
        existing.setFirstName(modifiedUser.getFirstName());
        existing.setEmail(modifiedUser.getEmail());
        existing.setHashedPassword(modifiedUser.getHashedPassword());
        existing.setPhone(modifiedUser.getPhone());
        existing.setAddress(modifiedUser.getAddress());
        existing.setRole(modifiedUser.getRole());
        existing.setClubs(modifiedUser.getClubs());

        return userRepo.save(existing);
    }

    /**
     *
     * @param id id of the AppUser to deleted
     * @throws HttpException
     */
    @Transactional
    public void deleteAccount(Long id) throws HttpException {
        AppUser user = findById(id);

        userRepo.delete(user);
    }
}
