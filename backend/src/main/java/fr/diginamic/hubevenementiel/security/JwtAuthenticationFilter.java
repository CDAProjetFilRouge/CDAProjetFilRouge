package fr.diginamic.hubevenementiel.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import fr.diginamic.hubevenementiel.logging.RequestLoggingFilter;
import java.util.UUID;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import fr.diginamic.hubevenementiel.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final String DPOP_HEADER = "DPoP";

    private final JwtService jwtService;
    private final UserRepo userRepo;
    private final DpopProofVerifier dpopProofVerifier;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepo userRepo, DpopProofVerifier dpopProofVerifier) {
        this.jwtService = jwtService;
        this.userRepo = userRepo;
        this.dpopProofVerifier = dpopProofVerifier;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String header = request.getHeader(AUTHORIZATION_HEADER);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());

            if (jwtService.isTokenValid(token)) {
                UUID id = jwtService.extractUserId(token);

                // Status and role come from the database, not from the token, so that a suspension
                // or a role change takes effect immediately instead of when the token expires.
                AppUser account = userRepo.findById(id)
                        .filter(user -> user.getStatus() == AccountStatus.ACTIVE)
                        .orElse(null);

                boolean keyRespected = account != null && keyBindingIsRespected(token, request);
                if (account != null && !keyRespected) {
                    log.warn("Jeton refusé : preuve DPoP absente ou invalide userId={}", id);
                }
                if (account == null) {
                    log.warn("Jeton refusé : compte introuvable ou non actif userId={}", id);
                }
                if (keyRespected) {
                    String email = account.getEmail();
                    String role = account.getRole().name();

                    AppUserPrincipal principal = new AppUserPrincipal(id, email, role);

                    Authentication auth = new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new RoleAuthority("ROLE_" + role)));

                    SecurityContextHolder.getContext().setAuthentication(auth);
                    MDC.put(RequestLoggingFilter.USER_ID, id.toString());
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean keyBindingIsRespected(String accessToken, HttpServletRequest request) {
        String boundKey = jwtService.extractKeyThumbprint(accessToken);
        if (boundKey == null) {
            return !dpopProofVerifier.isRequired();
        }

        String proof = request.getHeader(DPOP_HEADER);
        if (proof == null) {
            return false;
        }

        try {
            DpopProofVerifier.Proof verified = dpopProofVerifier.verify(proof, request.getMethod(),
                    request.getRequestURL().toString());
            return boundKey.equals(verified.thumbprint())
                    && DpopProofVerifier.hashOfAccessToken(accessToken).equals(verified.accessTokenHash());
        } catch (HttpException exception) {
            return false;
        }
    }
}
