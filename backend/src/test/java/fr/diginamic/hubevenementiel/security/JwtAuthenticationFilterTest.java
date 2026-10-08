package fr.diginamic.hubevenementiel.security;

import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.repositories.UserRepo;
import fr.diginamic.hubevenementiel.services.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
    @Mock
    private UserRepo userRepo;
    @Mock
    private DpopProofVerifier dpopProofVerifier;
    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private JwtAuthenticationFilter filter;

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_noAuthorizationHeader_continuesUnauthenticated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn(null);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_invalidToken_continuesUnauthenticated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-invalide");
        when(jwtService.isTokenValid("token-invalide")).thenReturn(false);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validToken_populatesSecurityContextWithPrincipal() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valide");
        when(jwtService.isTokenValid("token-valide")).thenReturn(true);
        when(jwtService.extractUserId("token-valide")).thenReturn(6L);
        when(userRepo.findById(6L)).thenReturn(Optional.of(userWithStatus(AccountStatus.ACTIVE)));
        when(jwtService.extractEmail("token-valide")).thenReturn("alice.martin@example.com");
        when(jwtService.extractRole("token-valide")).thenReturn("ORGANIZER");

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
        assertThat(principal.id()).isEqualTo(6L);
        assertThat(principal.email()).isEqualTo("alice.martin@example.com");
        assertThat(principal.role()).isEqualTo("ORGANIZER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validTokenOfSuspendedUser_continuesUnauthenticated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valide");
        when(jwtService.isTokenValid("token-valide")).thenReturn(true);
        when(jwtService.extractUserId("token-valide")).thenReturn(6L);
        when(userRepo.findById(6L)).thenReturn(Optional.of(userWithStatus(AccountStatus.SUSPENDED)));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validTokenOfUnknownUser_continuesUnauthenticated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valide");
        when(jwtService.isTokenValid("token-valide")).thenReturn(true);
        when(jwtService.extractUserId("token-valide")).thenReturn(6L);
        when(userRepo.findById(6L)).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private AppUser userWithStatus(AccountStatus status) {
        AppUser user = new AppUser();
        user.setStatus(status);
        return user;
    }
}
