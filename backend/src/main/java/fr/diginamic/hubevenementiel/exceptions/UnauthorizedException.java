package fr.diginamic.hubevenementiel.exceptions;

import org.springframework.http.HttpStatus;

public class UnauthorizedException extends HttpException {

    public UnauthorizedException(String message) {
        super(message, HttpStatus.UNAUTHORIZED);
    }

}
