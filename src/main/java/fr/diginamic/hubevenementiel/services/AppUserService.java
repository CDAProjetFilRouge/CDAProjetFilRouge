package fr.diginamic.hubevenementiel.services;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.entities.Token;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
import fr.diginamic.hubevenementiel.enums.TokenType;
import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import fr.diginamic.hubevenementiel.exceptions.ConflictException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.NotFoundException;
import fr.diginamic.hubevenementiel.repositories.TokenRepo;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import jakarta.transaction.Transactional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AppUserService {

    private final UserRepo userRepo;
    private final TokenRepo tokenRepo;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AppUserService(UserRepo userRepo, TokenRepo tokenRepo, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.tokenRepo = tokenRepo;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    public List<AppUser> findAllUsers(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepo.findAll(pageable).getContent();
    }

    public AppUser findById(Long id) throws NotFoundException {
        AppUser user = userRepo.findById(id).orElseThrow(() -> new NotFoundException("User not found with id: " + id));

        return user;
    }

    public List<AppUser> findByLastName(String lastName, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByLastName(lastName, pageable).getContent();
        if (users.isEmpty()) {
            throw new NotFoundException("No users found with last name: " + lastName);
        }

        return users;
    }

    public List<AppUser> findByFirstName(String firstName, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByFirstName(firstName, pageable).getContent();
        if (users.isEmpty()) {
            throw new NotFoundException("No users found with last name: " + firstName);
        }

        return users;
    }

    public AppUser findByEmail(String email) throws HttpException {
        AppUser user = userRepo.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found with this email: " + email));

        return user;
    }

    public List<AppUser> findByRole(Role role, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByRole(role, pageable).getContent();
        if (users.isEmpty()) {
            throw new NotFoundException("No users found with role: " + role);
        }

        return users;
    }

    public List<AppUser> findByStatus(AccountStatus status, int page, int size) throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByStatus(status, pageable).getContent();
        if (users.isEmpty()) {
            throw new NotFoundException("No users found with status: " + status);
        }

        return users;
    }

    public List<AppUser> findBySuspensionEndDate(LocalDateTime dateMin, LocalDateTime dateMax, int page, int size)
            throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findBySuspensionEndDateBetween(dateMin, dateMax, pageable).getContent();
        if (users.isEmpty()) {
            throw new NotFoundException("No users found with end date between " + dateMin + " and " + dateMax);
        }

        return users;
    }

    public List<AppUser> findByCreationDateBetween(LocalDate dateMin, LocalDate dateMax, int page, int size)
            throws HttpException {
        Pageable pageable = PageRequest.of(page, size);
        List<AppUser> users = userRepo.findByCreationDateBetween(dateMin, dateMax, pageable).getContent();
        if (users.isEmpty()) {
            throw new NotFoundException("No users found with creation date between" + dateMin + " and " + dateMax);
        }

        return users;
    }

    @Transactional
    public AppUser createAccount(AppUser appUser) throws HttpException {
        appUserChecker(appUser, false);

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

    // TODO securite : aucune verification que l'appelant est bien administrateur
    // (pas d'auth branchee sur le projet pour l'instant, cf. #97)
    @Transactional
    public AppUser createAccountByAdmin(AppUser appUser, Role role) throws HttpException {
        appUserChecker(appUser, true);

        if (userRepo.existsByEmail(appUser.getEmail())) {
            throw new ConflictException("Un compte existe déjà avec cette adresse email.");
        }

        appUser.setStatus(AccountStatus.INACTIVE);
        appUser.setRole(role);
        appUser.setHashedPassword(passwordEncoder.encode(appUser.getHashedPassword()));
        appUser.setCreationDate(LocalDate.now());

        return userRepo.save(appUser);
    }

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
        existing.setPhone(modifiedUser.getPhone());
        existing.setAddress(modifiedUser.getAddress());

        return userRepo.save(existing);
    }

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
        existing.setPhone(modifiedUser.getPhone());
        existing.setAddress(modifiedUser.getAddress());
        existing.setRole(modifiedUser.getRole());
        existing.setClubs(modifiedUser.getClubs());

        return userRepo.save(existing);
    }

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
    public void deleteAccount(Long id) throws HttpException {
        AppUser user = findById(id);

        userRepo.delete(user);
    }
}
