package fr.diginamic.hubevenementiel.openapi;

import org.springframework.http.ResponseEntity;

import fr.diginamic.hubevenementiel.dtos.auth.LoginRequestDto;
import fr.diginamic.hubevenementiel.dtos.auth.LoginResponseDto;
import fr.diginamic.hubevenementiel.dtos.auth.RefreshRequestDto;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Authentification", description = "Connexion à la plateforme et émission du token JWT.")
public interface LoginApi {

        @Operation(summary = "Se connecter", description = "Authentifie un utilisateur avec son email et son mot de passe et renvoie un token JWT (access token) et un refresh token à utiliser dans l'en-tête Authorization (Bearer). "
                        + "Le compte doit être au statut ACTIF ; une suspension dont la date de fin est passée est levée à la connexion. "
                        + "Accessible sans authentification.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Authentification réussie, token JWT renvoyé"),
                        @ApiResponse(responseCode = "403", description = "Identifiants invalides, ou compte non ACTIF. Si le mot de passe est correct, le corps de la réponse explique pourquoi (compte suspendu, compte non activé)")
        })
        ResponseEntity<LoginResponseDto> login(LoginRequestDto request) throws HttpException;

        @Operation(summary = "Renouveler la session", description = "Échange un refresh token contre un nouvel access token et un nouveau refresh token (rotation : l'ancien ne sert qu'une fois). "
                        + "Si un refresh token déjà utilisé est présenté au-delà de 10 secondes, toute la session est révoquée. "
                        + "Accessible sans authentification.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Nouveaux tokens renvoyés"),
                        @ApiResponse(responseCode = "401", description = "Refresh token inconnu, expiré, révoqué ou réutilisé, ou compte non actif")
        })
        ResponseEntity<LoginResponseDto> refresh(RefreshRequestDto request) throws HttpException;

        @Operation(summary = "Se déconnecter", description = "Révoque toute la session associée au refresh token fourni. Répond toujours 204, même si le token est inconnu. "
                        + "Accessible sans authentification.")
        @ApiResponses({
                        @ApiResponse(responseCode = "204", description = "Session révoquée")
        })
        ResponseEntity<Void> logout(RefreshRequestDto request);

}
