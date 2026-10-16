package fr.diginamic.hubevenementiel.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import fr.diginamic.hubevenementiel.services.RequestRateLimiter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    public static final String ERROR_CODE = "TOO_MANY_REQUESTS";
    private static final long WINDOW_MILLIS = 60_000;

    private enum Policy {
        SENSITIVE, PDF, SEARCH, DEFAULT
    }

    private final RequestRateLimiter limiter;
    private final boolean enabled;
    private final int sensitivePerMinute;
    private final int pdfPerMinute;
    private final int searchPerMinute;
    private final int defaultPerMinute;

    public RateLimitFilter(RequestRateLimiter limiter,
            @Value("${app.security.rate-limit.enabled:true}") boolean enabled,
            @Value("${app.security.rate-limit.sensitive-per-minute:10}") int sensitivePerMinute,
            @Value("${app.security.rate-limit.pdf-per-minute:20}") int pdfPerMinute,
            @Value("${app.security.rate-limit.search-per-minute:60}") int searchPerMinute,
            @Value("${app.security.rate-limit.default-per-minute:300}") int defaultPerMinute) {
        this.limiter = limiter;
        this.enabled = enabled;
        this.sensitivePerMinute = sensitivePerMinute;
        this.pdfPerMinute = pdfPerMinute;
        this.searchPerMinute = searchPerMinute;
        this.defaultPerMinute = defaultPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled || HttpMethod.OPTIONS.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Policy policy = policyOf(request);
        String key = policy + ":" + ClientIp.of(request);
        long retryAfterMillis = limiter.tryConsume(key, capacityOf(policy), WINDOW_MILLIS);

        if (retryAfterMillis > 0) {
            long seconds = Math.max(1, (retryAfterMillis + 999) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(seconds));
            response.setHeader("X-Error-Code", ERROR_CODE);
            response.setContentType("text/plain;charset=UTF-8");
            response.getOutputStream().write(
                    ("Trop de requêtes. Réessayez dans " + seconds + " secondes.").getBytes(StandardCharsets.UTF_8));
            return;
        }

        chain.doFilter(request, response);
    }

    private int capacityOf(Policy policy) {
        return switch (policy) {
            case SENSITIVE -> sensitivePerMinute;
            case PDF -> pdfPerMinute;
            case SEARCH -> searchPerMinute;
            case DEFAULT -> defaultPerMinute;
        };
    }

    private Policy policyOf(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean get = HttpMethod.GET.matches(request.getMethod());
        boolean post = HttpMethod.POST.matches(request.getMethod());

        if (post && (path.equals("/auth/refresh") || path.equals("/account/password/forgot")
                || path.equals("/account/password/reset") || path.equals("/account/activate")
                || path.equals("/users"))) {
            return Policy.SENSITIVE;
        }
        if (get && (path.equals("/account/verify") || path.equals("/account/password/confirm"))) {
            return Policy.SENSITIVE;
        }
        if (get && path.startsWith("/PDF/")) {
            return Policy.PDF;
        }
        if (get && (path.equals("/events") || path.equals("/events/filter") || path.equals("/events/summary")
                || path.equals("/clubs") || path.equals("/users"))) {
            return Policy.SEARCH;
        }
        return Policy.DEFAULT;
    }
}
