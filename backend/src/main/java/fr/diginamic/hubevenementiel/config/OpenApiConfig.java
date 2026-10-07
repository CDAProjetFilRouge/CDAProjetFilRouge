package fr.diginamic.hubevenementiel.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    public static final String BEARER_SECURITY_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI hubEvenementielOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Hub Événementiel API")
                        .description("API de gestion d'un hub évènementiel : clubs, évènements, inscriptions, "
                                + "commentaires, documents légaux et demandes d'anonymisation RGPD.

"
                                + "**Authentification** : POST /login renvoie un access token (JWT, 15 minutes), à envoyer dans "
                                + "l'en-tête Authorization (Bearer), et un refresh token. Une réponse 401 signifie que l'utilisateur "
                                + "n'est pas authentifié (token absent, expiré ou invalide) : appeler POST /auth/refresh avec le "
                                + "refresh token pour obtenir de nouveaux tokens, puis rejouer la requête. Chaque refresh token "
                                + "ne sert qu'une fois. Une réponse 403 signifie que l'utilisateur est connecté mais n'a pas les "
                                + "droits nécessaires. POST /auth/logout révoque la session.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
