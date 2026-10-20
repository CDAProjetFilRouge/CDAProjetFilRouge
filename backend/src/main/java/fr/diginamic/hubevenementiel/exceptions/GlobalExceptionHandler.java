package fr.diginamic.hubevenementiel.exceptions;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    public static final String ERROR_CODE_HEADER = "X-Error-Code";

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<String> handleTooManyRequests(TooManyRequestsException exception) {
        return ResponseEntity.status(exception.getStatus())
                .header(ERROR_CODE_HEADER, exception.getCode())
                .header(HttpHeaders.RETRY_AFTER, String.valueOf(exception.getRetryAfterSeconds()))
                .body(exception.getMessage());
    }

    @ExceptionHandler(HttpException.class)
    public ResponseEntity<String> handleHttpException(HttpException exception) {
        ResponseEntity.BodyBuilder response = ResponseEntity.status(exception.getStatus());
        if (exception.getCode() != null) {
            response.header(ERROR_CODE_HEADER, exception.getCode());
        }
        return response.body(exception.getMessage());
    }
}
