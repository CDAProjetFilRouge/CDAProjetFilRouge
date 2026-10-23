package fr.diginamic.hubevenementiel.config;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Adresses des fronts autorisés : celle de {@code FRONTEND_URL} (par défaut) et celles de {@code ALLOWED_FRONT_URLS}
 * (liste séparée par des virgules). Elles servent aux règles CORS et aux liens des emails.
 *
 * Le lien d'un email reprend l'adresse du front qui a envoyé la requête (en-tête {@code Origin}) seulement si elle
 * figure dans la liste ; sinon, ou hors requête, c'est l'adresse par défaut. Lire l'en-tête sans cette liste
 * permettrait à un attaquant d'envoyer à une victime un lien de réinitialisation qui pointe vers son propre site.
 */
@Component
public class FrontendUrls {

    private final String defaultUrl;
    private final Set<String> allowed;

    public FrontendUrls(@Value("${app.frontend-url}") String defaultUrl,
            @Value("${app.frontend-allowed-urls:}") String allowedUrls) {
        this.defaultUrl = normalize(defaultUrl);
        Set<String> urls = new LinkedHashSet<>();
        urls.add(this.defaultUrl);
        for (String url : allowedUrls.split(",")) {
            if (!url.isBlank()) {
                urls.add(normalize(url));
            }
        }
        this.allowed = Collections.unmodifiableSet(urls);
    }

    public List<String> allowedOrigins() {
        return List.copyOf(allowed);
    }

    public String defaultUrl() {
        return defaultUrl;
    }

    /** Adresse du front à utiliser dans un lien, pour la requête en cours. */
    public String current() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            String origin = attributes.getRequest().getHeader("Origin");
            if (origin != null && allowed.contains(normalize(origin))) {
                return normalize(origin);
            }
        }
        return defaultUrl;
    }

    private static String normalize(String url) {
        String trimmed = url.trim().toLowerCase(Locale.ROOT);
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
