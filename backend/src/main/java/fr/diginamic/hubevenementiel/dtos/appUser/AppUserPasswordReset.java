package fr.diginamic.hubevenementiel.dtos.appUser;

public class AppUserPasswordReset {
    private String token;
    private String newPassword;

    public AppUserPasswordReset() {
    }

    public AppUserPasswordReset(String token, String newPassword) {
        this.token = token;
        this.newPassword = newPassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }
}
