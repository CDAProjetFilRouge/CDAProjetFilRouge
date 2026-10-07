package fr.diginamic.hubevenementiel.openapi;

import fr.diginamic.hubevenementiel.dtos.auth.LoginRequestDto;
import fr.diginamic.hubevenementiel.dtos.auth.LoginResponseDto;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Authentification", description = "Connexion à la plateforme et émission du token JWT.")
public interface LoginApi {

    @Operation(summary = "Se connecter",
            description = "Authentifie un utilisateur avec son email et son mot de passe et renvoie un token JWT à utiliser dans l'en-tête Authorization (Bearer). "
                    + "Le compte doit être au statut ACTIF ; une suspension dont la date de fin est passée est levée à la connexion. "
                    + "Accessible sans authentification.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authentification réussie, token JWT renvoyé"),
            @ApiResponse(responseCode = "403", description = "Identifiants invalides, ou compte non ACTIF. Si le mot de passe est correct, le corps de la réponse explique pourquoi (compte suspendu, compte non activé)")
    })
    ResponseEntity<LoginResponseDto> login(LoginRequestDto request) throws HttpException;
}
