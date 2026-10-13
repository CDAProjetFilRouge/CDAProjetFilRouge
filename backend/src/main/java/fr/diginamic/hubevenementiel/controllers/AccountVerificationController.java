package fr.diginamic.hubevenementiel.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import fr.diginamic.hubevenementiel.dtos.appUser.AccountActivationRequestDto;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserPasswordChange;
import fr.diginamic.hubevenementiel.dtos.appUser.AppUserPasswordReset;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.openapi.AccountVerificationApi;
import fr.diginamic.hubevenementiel.security.AppUserPrincipal;
import fr.diginamic.hubevenementiel.services.AccountActivationService;
import fr.diginamic.hubevenementiel.services.PasswordService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/account")
public class AccountVerificationController implements AccountVerificationApi {

    private final AccountActivationService accountActivationService;
    private final PasswordService passwordService;

    public AccountVerificationController(AccountActivationService accountActivationService,
            PasswordService passwordService) {
        this.accountActivationService = accountActivationService;
        this.passwordService = passwordService;
    }

    @Override
    @GetMapping("/verify")
    public ResponseEntity<Void> verifyAccount(@RequestParam String token) throws HttpException {
        accountActivationService.confirmAccountVerification(token);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> requesPasswordReset(@RequestParam String email) {
        passwordService.requestPasswordReset(email);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/password/reset")
    public ResponseEntity<Void> submitNewPassword(@RequestBody AppUserPasswordReset dto) throws HttpException {
        passwordService.submitNewPasswordAfterReset(dto.getToken(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/password/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@RequestParam String token) throws HttpException {
        passwordService.confirmPasswordReset(token);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/password/change")
    public ResponseEntity<Void> requestPasswordChange(
            @RequestBody AppUserPasswordChange dto) throws HttpException {
        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        passwordService.requestPasswordChange(principal.id(), dto.getCurrentPassword(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/activate")
    public ResponseEntity<Void> activateAccount(@Valid @RequestBody AccountActivationRequestDto dto)
            throws HttpException {
        accountActivationService.activateAccount(dto.getToken(), dto.getTemporaryPassword(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
