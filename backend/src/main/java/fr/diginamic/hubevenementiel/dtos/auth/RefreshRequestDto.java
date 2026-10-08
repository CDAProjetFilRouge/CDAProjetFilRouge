package fr.diginamic.hubevenementiel.dtos.auth;

public class RefreshRequestDto {

    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}
