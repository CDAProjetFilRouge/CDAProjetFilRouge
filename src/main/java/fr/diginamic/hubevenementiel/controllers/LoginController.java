package fr.diginamic.hubevenementiel.controllers;

import fr.diginamic.hubevenementiel.dtos.auth.LoginRequestDto;
import fr.diginamic.hubevenementiel.dtos.auth.LoginResponseDto;
import fr.diginamic.hubevenementiel.security.AppUserDetails;
import fr.diginamic.hubevenementiel.services.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LoginController {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public LoginController(AuthenticationManager authenticationManager, JwtService jwtService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));

        AppUserDetails principal = (AppUserDetails) authentication.getPrincipal();
        String token = jwtService.generateToken(principal.getAppUser());

        return ResponseEntity.ok(new LoginResponseDto(token));
    }
}
