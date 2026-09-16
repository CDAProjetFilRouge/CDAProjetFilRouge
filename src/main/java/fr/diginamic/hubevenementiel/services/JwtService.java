package fr.diginamic.hubevenementiel.services;

import org.springframework.stereotype.Service;

// Squelette pour demain (issue #97). Rien n'est implemente : juste les methodes
// qu'on sait deja qu'il faudra, pour ne pas avoir a re-reflechir a la forme
// avant de coder le contenu.
//
// A trancher demain avant de remplir :
// - duree d'expiration du token (et refresh token ou pas)
// - quelles infos dans les claims (id, email, role ? juste l'email ?)
// - ou stocker la cle secrete (application.yml / variable d'environnement)
@Service
public class JwtService {

    public String generateToken(String email) {
        throw new UnsupportedOperationException("A implementer");
    }

    public String extractEmail(String token) {
        throw new UnsupportedOperationException("A implementer");
    }

    public boolean isTokenValid(String token) {
        throw new UnsupportedOperationException("A implementer");
    }
}
