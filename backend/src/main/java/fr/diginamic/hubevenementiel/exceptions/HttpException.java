package fr.diginamic.hubevenementiel.exceptions;

import org.springframework.http.HttpStatus;

public class HttpException extends Exception {

    private final HttpStatus status;
    private final String code;

    public HttpException(String message, HttpStatus status) {
        this(message, status, null);
    }

    public HttpException(String message, HttpStatus status, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /**
     * @return machine-readable error code sent in the X-Error-Code header, or null
     */
    public String getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

}
