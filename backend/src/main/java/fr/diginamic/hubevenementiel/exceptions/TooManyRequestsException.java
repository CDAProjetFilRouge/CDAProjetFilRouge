package fr.diginamic.hubevenementiel.exceptions;

import org.springframework.http.HttpStatus;

public class TooManyRequestsException extends HttpException {

    public static final String CODE = "TOO_MANY_ATTEMPTS";

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, CODE);
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
