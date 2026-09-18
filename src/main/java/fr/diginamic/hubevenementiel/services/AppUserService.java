package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Token;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.enums.TokenType;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.ForbiddenException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.ClubRepo;
import fr.diginamic.hubevenementiel.repositories.TokenRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import jakarta.transaction.Transactional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AppUserService {

    private final UserRepo userRepo;
    private final TokenRepo tokenRepo;
    private final ClubRepo clubRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(UserRepo userRepo, TokenRepo tokenRepo, ClubRepo clubRepo, EmailService emailService,
            PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.clubRepo = clubRepo;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
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
        appUserChecker(appUser, false, true);

        if (userRepo.existsByEmail(appUser.getEmail())) {
            throw new ConflictException("Un compte existe déjà avec cette adresse email.");
        }

        appUser.setStatus(AccountStatus.INACTIVE);
        appUser.setRole(Role.MEMBER);
        appUser.setHashedPassword(passwordEncoder.encode(appUser.getHashedPassword()));
        appUser.setCreationDate(LocalDate.now());

        AppUser savedUser = userRepo.save(appUser);

        Token token = new Token();
        token.setValue(UUID.randomUUID().toString());
        token.setTokenType(TokenType.ENABLE_ACCOUNT);
        token.setUser(savedUser);
        token.setCreationDateTime(LocalDateTime.now());
        token.setExpirationDateTime(LocalDateTime.now().plusHours(24));
        token.setPendingData("");
        tokenRepo.save(token);

        emailService.sendVerificationEmail(savedUser.getEmail(), token.getValue());

        return savedUser;
    }

    /**
     *
     * @param appUser AppUser to save in the DB as admin
     * @param role role to assign the AppUser to
     * @return AppUser saved in the DB
     * @throws HttpException
     */
    // TODO securite : aucune verification que l'appelant est bien administrateur
    // (pas d'auth branchee sur le projet pour l'instant, cf. #97)
    @Transactional
    public AppUser createAccountByAdmin(AppUser appUser, Role role, List<Long> clubIds) throws HttpException {

        String temporaryPassword = generateTemporaryPassword();
        appUser.setHashedPassword(temporaryPassword);

        appUserChecker(appUser, true, true);

        if (userRepo.existsByEmail(appUser.getEmail())) {
            throw new ConflictException("Un compte existe déjà avec cette adresse email.");
        }

        appUser.setStatus(AccountStatus.PENDING_ACTIVATION);
        appUser.setRole(role);
        appUser.setHashedPassword(passwordEncoder.encode(temporaryPassword));
        appUser.setCreationDate(LocalDate.now());
        appUser.setClubs(clubIds != null ? clubRepo.findAllById(clubIds) : List.of());

        AppUser savedUser = userRepo.save(appUser);

        createAccountActivationToken(savedUser, temporaryPassword);

        return savedUser;
    }

    @Transactional
    public void anonymizeAccount(AppUser user) {
        String randomSuffix = UUID.randomUUID().toString();

        user.setLastName("ANONYME-" + randomSuffix);
        user.setFirstName("ANONYME-" + randomSuffix);
        user.setEmail("anonyme-" + randomSuffix + "@anonymise.local");
        user.setPhone(randomSuffix);
        user.setHashedPassword(UUID.randomUUID().toString());
        user.setStatus(AccountStatus.ANONYMIZE);
        user.setAddress(null);

        userRepo.save(user);
    }
        /**
     *
     * @param appUser AppUser to perform the checks on
     * @param phoneRequired set the state of the requirement
     * @param passwordRequired set the state of the requirement
     * @return return true if the AppUser passed all checks else return false
     * @throws HttpException
     */
    public boolean appUserChecker(AppUser appUser, boolean phoneRequired, boolean passwordRequired) throws HttpException {

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

        if (passwordRequired) {
            String password = appUser.getHashedPassword();
            if (password == null || password.isBlank()) {
                throw new BadRequestException("Le mot de passe est obligatoire.");
            }
            if (password.length() < 12) {
                throw new BadRequestException("Le mot de passe doit contenir au moins 12 caractères.");
            }
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
    public AppUser updateOwnAccount(Long id, AppUser modifiedUser, AppUserPrincipal principal) throws HttpException {
        if (!id.equals(principal.id())) {
            throw new ForbiddenException("Vous ne pouvez modifier que votre propre compte.");
        }

        AppUser existing = findById(id);

        appUserChecker(modifiedUser, false, false);

        if (!existing.getEmail().equalsIgnoreCase(modifiedUser.getEmail())
                && userRepo.existsByEmail(modifiedUser.getEmail())) {
            throw new ConflictException("Un compte existe déjà avec cette adresse email.");
        }

        existing.setLastName(modifiedUser.getLastName());
        existing.setFirstName(modifiedUser.getFirstName());
        existing.setEmail(modifiedUser.getEmail());
        existing.setPhone(modifiedUser.getPhone());
        existing.setAddress(modifiedUser.getAddress());

        AppUser savedUser = userRepo.save(existing);

        emailService.sendAccountInfoUpdatedEmail(savedUser.getEmail());

        return savedUser;
    }

    /**
     *
     * @param id if of the AppUser to update
     * @param modifiedUser updated AppUser information
     * @return modified AppUser
     * @throws HttpException
     */
    @Transactional
    public AppUser updateAccountByAdmin(Long id, AppUser modifiedUser, List<Long> clubIds) throws HttpException {
        AppUser existing = findById(id);

        appUserChecker(modifiedUser, true, false);

        if (!existing.getEmail().equalsIgnoreCase(modifiedUser.getEmail())
                && userRepo.existsByEmail(modifiedUser.getEmail())) {
            throw new ConflictException("Un compte existe déjà avec cette adresse email.");
        }

        existing.setLastName(modifiedUser.getLastName());
        existing.setFirstName(modifiedUser.getFirstName());
        existing.setEmail(modifiedUser.getEmail());
        existing.setPhone(modifiedUser.getPhone());
        existing.setAddress(modifiedUser.getAddress());
        existing.setRole(modifiedUser.getRole());
        existing.setClubs(clubIds != null ? clubRepo.findAllById(clubIds) : new ArrayList<>());

        AppUser savedUser = userRepo.save(existing);

        emailService.sendAccountInfoUpdatedEmail(savedUser.getEmail());

        return savedUser;
    }

    /**
     *
     * @param id id of the AppUser to deleted
     * @throws HttpException
     */
    @Transactional
    public void requestPasswordChange(Long userId, String currentPassword, String newPassword) throws HttpException {
        AppUser user = findById(userId);

        if (!passwordEncoder.matches(currentPassword, user.getHashedPassword())) {
            throw new BadRequestException("Mot de passe actuel incorrect.");
        }

        createPasswordChangeToken(user, newPassword);
    }

    @Transactional
    public void requestPasswordReset(String email) {
        Optional<AppUser> userOptional = userRepo.findByEmail(email);

        if (userOptional.isEmpty()) {
            return;
        }

        createPasswordChangeToken(userOptional.get(), null);
    }

    private void createPasswordChangeToken(AppUser user, String newPassword) {
        Token token = new Token();
        token.setValue(UUID.randomUUID().toString());
        token.setTokenType(TokenType.CHANGE_PWD);
        token.setUser(user);
        token.setCreationDateTime(LocalDateTime.now());
        token.setExpirationDateTime(LocalDateTime.now().plusHours(1));
        token.setPendingData(newPassword != null ? passwordEncoder.encode(newPassword) : "");
        tokenRepo.save(token);

        emailService.sendPasswordResetEmail(user.getEmail(), token.getValue());
    }

    private void createAccountActivationToken(AppUser user, String temporaryPassword) {
        Token token = new Token();
        token.setValue(UUID.randomUUID().toString());
        token.setTokenType(TokenType.ACCOUNT_ACTIVATION);
        token.setUser(user);
        token.setCreationDateTime(LocalDateTime.now());
        token.setExpirationDateTime(LocalDateTime.now().plusHours(1));
        token.setPendingData("");
        tokenRepo.save(token);

        emailService.sendAccountActivationEmail(user.getEmail(), token.getValue(), temporaryPassword);
    }

    private String generateTemporaryPassword() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }

    @Transactional
    public void submitNewPasswordAfterReset(String tokenValue, String newPassword) throws HttpException {
        Token token = getValidToken(tokenValue, TokenType.CHANGE_PWD);

        token.setPendingData(passwordEncoder.encode(newPassword));
        tokenRepo.save(token);

        emailService.sendPasswordChangeConfirmationEmail(token.getUser().getEmail(), token.getValue());
    }

    @Transactional
    public void confirmPasswordReset(String tokenValue) throws HttpException {
        Token token = getValidToken(tokenValue, TokenType.CHANGE_PWD);

        if (token.getPendingData() == null || token.getPendingData().isBlank()) {
            throw new BadRequestException("Aucun mot de passe en attente pour ce token.");
        }

        AppUser user = token.getUser();
        user.setHashedPassword(token.getPendingData());
        userRepo.save(user);

        token.setUseDate(LocalDateTime.now());
        tokenRepo.save(token);
    }

    private Token getValidToken(String tokenVaue, TokenType expectedType) throws HttpException {
        Token token = tokenRepo.findByValue(tokenVaue)
                .orElseThrow(() -> new BadRequestException("Token invalide."));

        if (token.getTokenType() != expectedType) {
            throw new BadRequestException("Type de token incorrect.");
        }

        if (token.getUseDate() != null) {
            throw new BadRequestException("Ce token a déjà été utilisé.");
        }

        if (token.getExpirationDateTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Ce token a expiré.");
        }

        return token;
    }

    @Transactional
    public void confirmAccountVerification(String tokenValue) throws HttpException {
        Token token = getValidToken(tokenValue, TokenType.ENABLE_ACCOUNT);

        AppUser user = token.getUser();
        user.setStatus(AccountStatus.ACTIVE);
        userRepo.save(user);

        token.setUseDate(LocalDateTime.now());
        tokenRepo.save(token);
    }

    @Transactional
    public void activateAccount(String tokenValue, String temporaryPassword, String newPassword) throws HttpException {
        Token token = getValidToken(tokenValue, TokenType.ACCOUNT_ACTIVATION);

        AppUser user = token.getUser();

        if (!passwordEncoder.matches(temporaryPassword, user.getHashedPassword())) {
            throw new BadRequestException("Mot de passe temporaire incorrect.");
        }

        user.setHashedPassword(passwordEncoder.encode(newPassword));
        user.setStatus(AccountStatus.ACTIVE);
        userRepo.save(user);

        token.setUseDate(LocalDateTime.now());
        tokenRepo.save(token);
    }

    @Transactional
    public void deleteAccount(Long id) throws HttpException {
        AppUser user = findById(id);

        userRepo.delete(user);
    }
}
