package fr.diginamic.hubevenementiel.security;

import java.util.UUID;

public record AppUserPrincipal(UUID id, String email, String role) {
}
