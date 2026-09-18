package fr.diginamic.hubevenementiel.openapi;

import fr.diginamic.hubevenementiel.dtos.auth.LoginRequestDto;
import fr.diginamic.hubevenementiel.dtos.auth.LoginResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Authentification", description = "Connexion à la plateforme et émission du token JWT.")
public interface LoginApi {

    @Operation(summary = "Se connecter",
            description = "Authentifie un utilisateur avec son email et son mot de passe et renvoie un token JWT à utiliser dans l'en-tête Authorization (Bearer). "
                    + "Le compte doit être au statut ACTIF. Accessible sans authentification.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification réussie, token JWT renvoyé"),
            @ApiResponse(responseCode = "401", description = "Identifiants invalides ou compte non ACTIF")
    })
    ResponseEntity<LoginResponseDto> login(LoginRequestDto request);
}
