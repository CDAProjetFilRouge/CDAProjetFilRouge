package fr.diginamic.hubevenementiel.openapi;

import fr.diginamic.hubevenementiel.exceptions.HttpException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Compte - Vérification et mot de passe", description = "Validation de compte par email et cycle de vie du mot de passe (oubli, réinitialisation, changement).")
public interface AccountVerificationApi {

    @Operation(summary = "Valider la création d'un compte",
            description = "Consomme le lien envoyé par email lors de la création du compte et passe son statut à ACTIF. Accessible sans authentification.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte activé"),
            @ApiResponse(responseCode = "400", description = "Token invalide, expiré ou de mauvais type"),
            @ApiResponse(responseCode = "404", description = "Token introuvable")
    })
    ResponseEntity<Void> verifyAccount(
            @Parameter(description = "Token de vérification reçu par email", required = true) String token) throws HttpException;

    @Operation(summary = "Démarrer une réinitialisation de mot de passe",
            description = "Envoie un email contenant un lien de réinitialisation si l'adresse correspond à un compte existant. Accessible sans authentification.")
    @ApiResponse(responseCode = "200", description = "Demande prise en compte (aucune information sur l'existence du compte n'est renvoyée)")
    ResponseEntity<Void> requesPasswordReset(
            @Parameter(description = "Adresse email du compte concerné", required = true) String email);

    @Operation(summary = "Soumettre un nouveau mot de passe après oubli",
            description = "Enregistre le nouveau mot de passe associé au token de réinitialisation reçu par email. Accessible sans authentification.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Nouveau mot de passe enregistré"),
            @ApiResponse(responseCode = "400", description = "Token invalide/expiré ou mot de passe ne respectant pas la politique de sécurité"),
            @ApiResponse(responseCode = "404", description = "Token introuvable")
    })
    ResponseEntity<Void> submitNewPassword(
            @Parameter(description = "Token de réinitialisation reçu par email", required = true) String token,
            @Parameter(description = "Nouveau mot de passe (12 caractères minimum)", required = true) String newPassword) throws HttpException;

    @Operation(summary = "Confirmer la réinitialisation du mot de passe",
            description = "Lien de confirmation envoyé par email après soumission d'un nouveau mot de passe ; sans ce clic, l'ancien mot de passe reste actif. Accessible sans authentification.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Réinitialisation confirmée"),
            @ApiResponse(responseCode = "400", description = "Token invalide ou expiré"),
            @ApiResponse(responseCode = "404", description = "Token introuvable")
    })
    ResponseEntity<Void> confirmPasswordReset(
            @Parameter(description = "Token de confirmation reçu par email", required = true) String token) throws HttpException;

    @Operation(summary = "Demander le changement de mot de passe d'un compte connecté",
            description = "Vérifie le mot de passe courant puis envoie un email de confirmation ; le mot de passe n'est effectif qu'après confirmation.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Email de confirmation envoyé"),
            @ApiResponse(responseCode = "400", description = "Mot de passe courant incorrect ou nouveau mot de passe invalide"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    ResponseEntity<Void> requestPasswordChange(
            @Parameter(description = "Identifiant de l'utilisateur", required = true) Long userId,
            @Parameter(description = "Mot de passe actuel", required = true) String currentPassword,
            @Parameter(description = "Nouveau mot de passe (12 caractères minimum)", required = true) String newPassword) throws HttpException;
}
