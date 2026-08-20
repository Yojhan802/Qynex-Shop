package com.freestyleperu.aplicacion.shared.exception;

import org.springframework.http.HttpStatus;

public class OperacionNoPermitidaException extends BusinessException {

    public OperacionNoPermitidaException(String message) {
        super(HttpStatus.FORBIDDEN, "OPERATION_NOT_ALLOWED", message);
    }
}
