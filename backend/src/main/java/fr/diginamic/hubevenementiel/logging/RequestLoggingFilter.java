package fr.diginamic.hubevenementiel.logging;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import fr.diginamic.hubevenementiel.security.ClientIp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Donne un identifiant à chaque requête (repris de l'en-tête X-Request-Id s'il est sain, sinon généré), le place dans
 * le contexte de log (MDC) avec l'adresse IP, le renvoie dans la réponse, puis écrit une ligne par requête : méthode,
 * chemin, statut, durée. L'utilisateur (userId) s'ajoute au contexte une fois authentifié. La chaîne de requête
 * n'est jamais journalisée : elle peut contenir du texte saisi par l'utilisateur.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RequestLoggingFilter extends OncePerRequestFilter {

    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    public static final String REQUEST_ID = "requestId";
    public static final String CLIENT_IP = "clientIp";
    public static final String USER_ID = "userId";

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    private static final Pattern VALID_REQUEST_ID = Pattern.compile("[A-Za-z0-9-]{8,64}");

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod()) || request.getRequestURI().startsWith("/uploads/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String incoming = request.getHeader(REQUEST_ID_HEADER);
        String requestId = incoming != null && VALID_REQUEST_ID.matcher(incoming).matches() ? incoming
                : UUID.randomUUID().toString();
        MDC.put(REQUEST_ID, requestId);
        MDC.put(CLIENT_IP, ClientIp.of(request));
        response.setHeader(REQUEST_ID_HEADER, requestId);

        long start = System.nanoTime();
        try {
            chain.doFilter(request, response);
            logCompletion(request, response, (System.nanoTime() - start) / 1_000_000);
        } catch (ServletException | IOException | RuntimeException exception) {
            log.error("{} {} : erreur inattendue", request.getMethod(), request.getRequestURI(), exception);
            throw exception;
        } finally {
            MDC.remove(USER_ID);
            MDC.remove(CLIENT_IP);
            MDC.remove(REQUEST_ID);
        }
    }

    private void logCompletion(HttpServletRequest request, HttpServletResponse response, long millis) {
        int status = response.getStatus();
        String method = request.getMethod();
        String path = request.getRequestURI();
        if (status >= 500) {
            log.error("{} {} -> {} ({} ms)", method, path, status, millis);
        } else if (status == 401 || status == 403 || status == 429) {
            log.warn("{} {} -> {} ({} ms)", method, path, status, millis);
        } else {
            log.info("{} {} -> {} ({} ms)", method, path, status, millis);
        }
    }
}
