package fr.diginamic.hubevenementiel.controllers;

import java.util.Optional;

import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
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
import fr.diginamic.hubevenementiel.openapi.LoginApi;
import fr.diginamic.hubevenementiel.security.AppUserDetails;
import fr.diginamic.hubevenementiel.security.DpopProofVerifier;
import fr.diginamic.hubevenementiel.services.AppUserService;
import fr.diginamic.hubevenementiel.services.JwtService;
import fr.diginamic.hubevenementiel.services.RefreshTokenService;
import jakarta.servlet.http.HttpServletRequest;

@RestController
public class LoginController implements LoginApi {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserService appUserService;
    private final RefreshTokenService refreshTokenService;
    private final DpopProofVerifier dpopProofVerifier;
    private final HttpServletRequest httpRequest;

    public LoginController(AuthenticationManager authenticationManager, JwtService jwtService,
            AppUserService appUserService, RefreshTokenService refreshTokenService,
            DpopProofVerifier dpopProofVerifier, HttpServletRequest httpRequest) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appUserService = appUserService;
        this.refreshTokenService = refreshTokenService;
        this.dpopProofVerifier = dpopProofVerifier;
        this.httpRequest = httpRequest;
    }

    @Override
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request) throws HttpException {
        Authentication authentication;

        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (DisabledException exception) {
            Optional<String> explanation = appUserService.explainLoginRefusal(request.getEmail(),
                    request.getPassword());

            if (explanation.isPresent()) {
                throw new ForbiddenException(explanation.get());
            }

            throw exception;
        }

        AppUser user = ((AppUserDetails) authentication.getPrincipal()).getAppUser();
        String token = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.issueForLogin(user, currentThumbprint());

        return ResponseEntity.ok(new LoginResponseDto(token, refreshToken));
    }

    @PostMapping("/auth/refresh")
    public ResponseEntity<LoginResponseDto> refresh(@RequestBody RefreshRequestDto request) throws HttpException {
        RefreshTokenService.Rotation rotation = refreshTokenService.rotate(request.getRefreshToken(),
                currentThumbprint());

        return ResponseEntity.ok(new LoginResponseDto(jwtService.generateToken(rotation.user()),
                rotation.refreshToken()));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshRequestDto request) {
        refreshTokenService.logout(request.getRefreshToken());

        return ResponseEntity.noContent().build();
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
