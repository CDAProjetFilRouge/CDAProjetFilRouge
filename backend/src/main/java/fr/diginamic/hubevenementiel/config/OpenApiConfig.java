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
                                + "commentaires, documents légaux et demandes d'anonymisation RGPD.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SECURITY_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
