package fr.diginamic.hubevenementiel.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import fr.diginamic.hubevenementiel.dtos.auth.LoginRequestDto;
import fr.diginamic.hubevenementiel.dtos.auth.LoginResponseDto;
import fr.diginamic.hubevenementiel.dtos.auth.RefreshRequestDto;
import fr.diginamic.hubevenementiel.entities.AppUser;
import fr.diginamic.hubevenementiel.exceptions.ForbiddenException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.exceptions.UnauthorizedException;
import fr.diginamic.hubevenementiel.openapi.LoginApi;
import fr.diginamic.hubevenementiel.security.AppUserDetails;
import fr.diginamic.hubevenementiel.security.DpopProofVerifier;
import fr.diginamic.hubevenementiel.services.AccountStatusService;
import fr.diginamic.hubevenementiel.services.JwtService;
import fr.diginamic.hubevenementiel.services.LoginAttemptService;
import fr.diginamic.hubevenementiel.services.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class LoginController implements LoginApi {

    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AccountStatusService accountStatusService;
    private final RefreshTokenService refreshTokenService;
    private final DpopProofVerifier dpopProofVerifier;
    private final HttpServletRequest httpRequest;
    private final LoginAttemptService loginAttemptService;

    public LoginController(AuthenticationManager authenticationManager, JwtService jwtService,
            AccountStatusService accountStatusService, RefreshTokenService refreshTokenService,
            DpopProofVerifier dpopProofVerifier, HttpServletRequest httpRequest,
            LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.accountStatusService = accountStatusService;
        this.refreshTokenService = refreshTokenService;
        this.dpopProofVerifier = dpopProofVerifier;
        this.httpRequest = httpRequest;
        this.loginAttemptService = loginAttemptService;
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request) throws HttpException {
        Authentication authentication;
        String clientIp = clientIp();

        loginAttemptService.checkAllowed(request.getEmail(), clientIp);

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (DisabledException exception) {
            Optional<String> explanation = accountStatusService.explainLoginRefusal(request.getEmail(),
                    request.getPassword());

            if (explanation.isPresent()) {
                log.warn("Connexion refusée : compte non actif");
                throw new ForbiddenException(explanation.get());
            }

            log.warn("Échec de connexion");
            loginAttemptService.recordFailure(request.getEmail(), clientIp);
            throw exception;
        } catch (BadCredentialsException exception) {
            log.warn("Échec de connexion");
            loginAttemptService.recordFailure(request.getEmail(), clientIp);
            throw exception;
        }

        loginAttemptService.recordSuccess(request.getEmail());

        AppUser user = ((AppUserDetails) authentication.getPrincipal()).getAppUser();
        log.info("Connexion réussie userId={}", user.getId());
        String keyThumbprint = currentThumbprint();
        if (keyThumbprint == null && dpopProofVerifier.isRequired()) {
            throw new UnauthorizedException("Preuve DPoP requise.");
        }
        String token = jwtService.generateToken(user, keyThumbprint);
        String refreshToken = refreshTokenService.issueForLogin(user, keyThumbprint);

        return ResponseEntity.ok(new LoginResponseDto(token, refreshToken));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<LoginResponseDto> refresh(@RequestBody RefreshRequestDto request) throws HttpException {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(request.getRefreshToken(),
                currentThumbprint());

        return ResponseEntity.ok(new LoginResponseDto(jwtService.generateToken(rotation.user(), rotation.keyThumbprint()),
                rotation.refreshToken()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequestDto request) {
        refreshTokenService.logout(request.getRefreshToken());

        return ResponseEntity.noContent().build();
    }

    private String clientIp() {
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        return httpRequest.getRemoteAddr();
    }

    private String currentThumbprint() throws HttpException {
        String proof = httpRequest.getHeader("DPoP");
        if (proof == null) {
            return null;
        }
        return dpopProofVerifier.verify(proof, httpRequest.getMethod(), httpRequest.getRequestURL().toString())
                .thumbprint();
    }

}
