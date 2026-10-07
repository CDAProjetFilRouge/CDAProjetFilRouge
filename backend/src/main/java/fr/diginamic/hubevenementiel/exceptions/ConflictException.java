package fr.diginamic.hubevenementiel.exceptions;

import org.springframework.http.HttpStatus;

public class ConflictException extends HttpException {

    public ConflictException(String message) {
        super(message, HttpStatus.CONFLICT); // Définit le statut HTTP à 409
    }

    public ConflictException(String message, String code) {
        super(message, HttpStatus.CONFLICT, code);
    }
}
