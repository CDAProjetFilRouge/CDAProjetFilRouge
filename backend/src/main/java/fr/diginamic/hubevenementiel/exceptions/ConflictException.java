package fr.diginamic.hubevenementiel.exceptions;

import org.springframework.http.HttpStatus;

public class ConflictException extends HttpException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT); // Définit le statut HTTP à 409
    }
}
