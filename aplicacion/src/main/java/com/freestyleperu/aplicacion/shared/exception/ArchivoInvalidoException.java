package com.freestyleperu.aplicacion.shared.exception;

import org.springframework.http.HttpStatus;

public class ArchivoInvalidoException extends BusinessException {

    public ArchivoInvalidoException(String message) {
        super(HttpStatus.BAD_REQUEST, "INVALID_FILE", message);
    }
}
