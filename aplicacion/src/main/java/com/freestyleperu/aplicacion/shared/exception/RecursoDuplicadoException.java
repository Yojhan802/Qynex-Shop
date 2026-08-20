package com.freestyleperu.aplicacion.shared.exception;

import org.springframework.http.HttpStatus;

public class RecursoDuplicadoException extends BusinessException {

    public RecursoDuplicadoException(String message) {
        super(HttpStatus.CONFLICT, "DUPLICATE_RESOURCE", message);
    }
}
