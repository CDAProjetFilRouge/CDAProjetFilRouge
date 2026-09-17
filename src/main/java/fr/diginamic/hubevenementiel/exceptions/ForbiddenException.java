package fr.diginamic.hubevenementiel.exceptions;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends HttpException {

    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
}
