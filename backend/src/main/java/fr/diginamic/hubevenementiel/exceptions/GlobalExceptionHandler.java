package fr.diginamic.hubevenementiel.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    public static final String ERROR_CODE_HEADER = "X-Error-Code";

    @ExceptionHandler(HttpException.class)
    public ResponseEntity<String> handleHttpException(HttpException exception) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.getStatus());
        if (exception.getCode() != null) {
            response.header(ERROR_CODE_HEADER, exception.getCode());
        }
        return response.body(exception.getMessage());
    }
}
