package fr.diginamic.hubevenementiel.openapi;

import java.util.List;

import fr.diginamic.hubevenementiel.dtos.appUser.*;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;

@Tag(name = "Utilisateurs", description = "Gestion des comptes utilisateur (membres, organisateurs, administrateurs).")
@SecurityRequirement(name = "bearerAuth")
public interface AppUserApi {

    @Operation(summary = "Lister les comptes utilisateur", description = "Réservé aux administrateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Liste paginée des comptes"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ADMINISTRATOR requis")
    })
    List<AppUserSummaryResponseDto> getUsers(
            @Parameter(description = "Numéro de page (0-indexé)") int page,
            @Parameter(description = "Taille de page") int size);

    @Operation(summary = "Consulter un compte utilisateur")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte trouvé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    AppUserResponseDto getById(@Parameter(description = "Identifiant du compte", required = true) Long id) throws HttpException;

    @Operation(summary = "Créer son compte (auto-inscription)",
            description = "Création d'un compte non affilié par un utilisateur non connecté. Le statut initial est INACTIF jusqu'à validation par email. Accessible sans authentification.")
    @SecurityRequirements
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé, email de vérification envoyé"),
            @ApiResponse(responseCode = "400", description = "Données invalides (mot de passe trop faible, champs manquants...)"),
            @ApiResponse(responseCode = "409", description = "Adresse email déjà utilisée")
    })
    ResponseEntity<AppUserResponseDto> register(AppUserRequestDto requestDto) throws HttpException;

    @Operation(summary = "Créer un compte membre affilié, organisateur ou administrateur",
            description = "Réservé aux administrateurs. Un mot de passe temporaire est envoyé par email à la personne concernée.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Compte créé"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ADMINISTRATOR requis"),
            @ApiResponse(responseCode = "409", description = "Adresse email déjà utilisée")
    })
    ResponseEntity<AppUserResponseDto> createByAdmin(AppUserAdminCreateRequestDto requestDto) throws HttpException;

    @Operation(summary = "Modifier son propre compte",
            description = "Permet à l'utilisateur connecté de modifier ses informations personnelles, à l'exception de ses affiliations à des clubs.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    AppUserResponseDto updateOwnAccount(@Parameter(description = "Identifiant du compte", required = true) Long id,
            AppUserUpdateRequestDto requestDto) throws HttpException;

    @Operation(summary = "Modifier un compte en tant qu'administrateur",
            description = "Réservé aux administrateurs. Permet notamment de modifier le rôle, le statut et les clubs affiliés du compte.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Compte mis à jour"),
            @ApiResponse(responseCode = "400", description = "Données invalides"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ADMINISTRATOR requis"),
            @ApiResponse(responseCode = "404", description = "Utilisateur ou club introuvable")
    })
    AppUserResponseDto updateByAdmin(@Parameter(description = "Identifiant du compte", required = true) Long id,
            AppUserAdminUpdateRequestDto requestDto) throws HttpException;

    @Operation(summary = "Supprimer un compte", description = "Réservé aux administrateurs.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Compte supprimé"),
            @ApiResponse(responseCode = "401", description = "Non authentifié"),
            @ApiResponse(responseCode = "403", description = "Rôle ADMINISTRATOR requis"),
            @ApiResponse(responseCode = "404", description = "Utilisateur introuvable")
    })
    ResponseEntity<Void> delete(@Parameter(description = "Identifiant du compte", required = true) Long id) throws HttpException;
}
