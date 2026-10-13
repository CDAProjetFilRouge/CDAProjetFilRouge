package fr.diginamic.hubevenementiel.config;

import fr.diginamic.hubevenementiel.exceptions.BadRequestException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Borne la pagination en un point commun, pour toutes les routes : {@code page} doit valoir au moins 0 et
 * {@code size} au moins 1. Sans cela, {@code size=0} ou {@code page=-1} atteignent la couche d'accès aux données
 * et ressortent en erreur serveur (500) au lieu d'une erreur 400. Les plafonds de taille restent propres à
 * chaque route.
 */
@Component
public class PaginationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws BadRequestException {
        check(request, "page", 0);
        check(request, "size", 1);
        return true;
    }

    // Une valeur non numérique est laissée à la conversion de type de Spring, qui répond déjà 400.
    private void check(HttpServletRequest request, String name, long minimum) throws BadRequestException {
        String[] values = request.getParameterValues(name);
        if (values == null) {
            return;
        }
        for (String value : values) {
            try {
                if (Long.parseLong(value.trim()) < minimum) {
                    throw new BadRequestException("Pagination invalide : " + name + " doit être supérieur ou égal à " + minimum + ".");
                }
            } catch (NumberFormatException ignored) {
                // Voir le commentaire de la méthode.
            }
        }
    }
}
