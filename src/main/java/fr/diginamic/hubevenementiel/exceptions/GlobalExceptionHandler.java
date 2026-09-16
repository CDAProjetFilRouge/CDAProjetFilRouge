package fr.diginamic.hubevenementiel.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(HttpException.class)
    public ResponseEntity<String> handleHttpException(HttpException exception) {
        return ResponseEntity.status(exception.getStatus()).body(exception.getMessage());
    }
}
