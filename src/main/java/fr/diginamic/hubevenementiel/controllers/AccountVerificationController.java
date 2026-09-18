package fr.diginamic.hubevenementiel.controllers;

import fr.diginamic.hubevenementiel.dtos.appUser.AccountActivationRequestDto;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.openapi.AccountVerificationApi;
import fr.diginamic.hubevenementiel.services.AppUserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/account")
public class AccountVerificationController implements AccountVerificationApi {

    private final AppUserService appUserService;


    public AccountVerificationController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @Override
    @GetMapping("/verify")
    public ResponseEntity<Void> verifyAccount(@RequestParam String token) throws HttpException {
        appUserService.confirmAccountVerification(token);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/password/forgot")
    public ResponseEntity<Void> requesPasswordReset(@RequestParam String email) {
        appUserService.requestPasswordReset(email);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/password/reset")
    public ResponseEntity<Void> submitNewPassword(@RequestParam String token, @RequestParam String newPassword) throws HttpException {
        appUserService.submitNewPasswordAfterReset(token, newPassword);
        return ResponseEntity.ok().build();
    }

    @Override
    @GetMapping("/password/confirm")
    public ResponseEntity<Void> confirmPasswordReset(@RequestParam String token) throws HttpException {
        appUserService.confirmPasswordReset(token);
        return ResponseEntity.ok().build();
    }

    //TODO @RequestParam Long userId est un param temporaire. Il faudra le remplacer par un @AuthenticationPrincipal une fois la sécurité terminée.
    //TODO a terme remplacer les des DTOs
    @Override
    @PostMapping("/password/change")
    public ResponseEntity<Void> requestPasswordChange(
            @RequestParam Long userId,
            @RequestParam String currentPassword,
            @RequestParam String newPassword) throws HttpException {
        appUserService.requestPasswordChange(userId, currentPassword, newPassword);
        return ResponseEntity.ok().build();
    }

    @Override
    @PostMapping("/activate")
    public ResponseEntity<Void> activateAccount(@Valid @RequestBody AccountActivationRequestDto dto) throws HttpException {
        appUserService.activateAccount(dto.getToken(), dto.getTemporaryPassword(), dto.getNewPassword());
        return ResponseEntity.ok().build();
    }
}

