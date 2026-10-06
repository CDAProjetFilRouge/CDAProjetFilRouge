package fr.diginamic.hubevenementiel.controllers;

import java.util.Optional;

import fr.diginamic.hubevenementiel.dtos.auth.LoginRequestDto;
import fr.diginamic.hubevenementiel.dtos.auth.LoginResponseDto;
import fr.diginamic.hubevenementiel.exceptions.ForbiddenException;
import fr.diginamic.hubevenementiel.exceptions.HttpException;
import fr.diginamic.hubevenementiel.openapi.LoginApi;
import fr.diginamic.hubevenementiel.security.AppUserDetails;
import fr.diginamic.hubevenementiel.services.AppUserService;
import fr.diginamic.hubevenementiel.services.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController implements LoginApi {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserService appUserService;

    public LoginController(AuthenticationManager authenticationManager, JwtService jwtService,
            AppUserService appUserService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appUserService = appUserService;
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

        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(principal.getAppUser());

        return ResponseEntity.ok(new LoginResponseDto(token));
    }
}
