package fr.diginamic.hubevenementiel.security;

import static fr.diginamic.hubevenementiel.TestIds.uuid;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.enums.AccountStatus;
import fr.diginamic.hubevenementiel.enums.Role;
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
        when(jwtService.extractUserId("token-valide")).thenReturn(uuid(6));
        AppUser user = userWithStatus(AccountStatus.ACTIVE);
        user.setEmail("alice.martin@example.com");
        when(userRepo.findById(uuid(6))).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        AppUserPrincipal principal = (AppUserPrincipal) auth.getPrincipal();
        assertThat(principal.id()).isEqualTo(uuid(6));
        assertThat(principal.email()).isEqualTo("alice.martin@example.com");
        assertThat(principal.role()).isEqualTo("ORGANIZER");
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_roleAndEmailComeFromTheDatabaseNotFromTheToken() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valide");
        when(jwtService.isTokenValid("token-valide")).thenReturn(true);
        when(jwtService.extractUserId("token-valide")).thenReturn(uuid(6));
        AppUser demoted = userWithStatus(AccountStatus.ACTIVE);
        demoted.setEmail("nouvelle-adresse@example.com");
        demoted.setRole(Role.MEMBER);
        when(userRepo.findById(uuid(6))).thenReturn(Optional.of(demoted));

        filter.doFilterInternal(request, response, filterChain);

        AppUserPrincipal principal = (AppUserPrincipal) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        assertThat(principal.role()).isEqualTo("MEMBER");
        assertThat(principal.email()).isEqualTo("nouvelle-adresse@example.com");
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(org.springframework.security.core.GrantedAuthority::getAuthority).containsExactly("ROLE_MEMBER");
    }

    @Test
    void doFilterInternal_tokenWithoutBoundKey_whenDpopRequired_continuesUnauthenticated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valide");
        when(jwtService.isTokenValid("token-valide")).thenReturn(true);
        when(jwtService.extractUserId("token-valide")).thenReturn(uuid(6));
        when(userRepo.findById(uuid(6))).thenReturn(Optional.of(userWithStatus(AccountStatus.ACTIVE)));
        when(jwtService.extractKeyThumbprint("token-valide")).thenReturn(null);
        when(dpopProofVerifier.isRequired()).thenReturn(true);

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validTokenOfSuspendedUser_continuesUnauthenticated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valide");
        when(jwtService.isTokenValid("token-valide")).thenReturn(true);
        when(jwtService.extractUserId("token-valide")).thenReturn(uuid(6));
        when(userRepo.findById(uuid(6))).thenReturn(Optional.of(userWithStatus(AccountStatus.SUSPENDED)));

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_validTokenOfUnknownUser_continuesUnauthenticated() throws Exception {
        when(request.getHeader("Authorization")).thenReturn("Bearer token-valide");
        when(jwtService.isTokenValid("token-valide")).thenReturn(true);
        when(jwtService.extractUserId("token-valide")).thenReturn(uuid(6));
        when(userRepo.findById(uuid(6))).thenReturn(Optional.empty());

        filter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    private AppUser userWithStatus(AccountStatus status) {
        AppUser user = new AppUser();
        user.setStatus(status);
        user.setRole(fr.diginamic.hubevenementiel.enums.Role.ORGANIZER);
        return user;
    }
}
