package fr.diginamic.hubevenementiel.dtos.appUser;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AccountActivationRequestDto {

    @NotBlank
    private String token;

    @NotBlank
    private String temporaryPassword;

    @NotBlank
    @Size(min = 12, message = "Le mot de passe doit contenir au moins 12 caractères.")
    private String newPassword;

    @NotBlank
    private String confirmNewPassword;

    @AssertTrue(message = "Les mots de passe ne correspondent pas.")
    public boolean isPasswordConfirmed() {
        if (newPassword == null || confirmNewPassword == null) {
            return true;
        }
        return newPassword.equals(confirmNewPassword);
    }

    public AccountActivationRequestDto() {
    }

    public AccountActivationRequestDto(String token, String temporaryPassword, String newPassword, String confirmNewPassword) {
        this.token = token;
        this.temporaryPassword = temporaryPassword;
        this.newPassword = newPassword;
        this.confirmNewPassword = confirmNewPassword;
    }


    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTemporaryPassword() {
        return temporaryPassword;
    }

    public void setTemporaryPassword(String temporaryPassword) {
        this.temporaryPassword = temporaryPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }
}