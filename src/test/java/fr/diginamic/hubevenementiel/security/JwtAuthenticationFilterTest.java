package fr.diginamic.hubevenementiel.security;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;
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
}
