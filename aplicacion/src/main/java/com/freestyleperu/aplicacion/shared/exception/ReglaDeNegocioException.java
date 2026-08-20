package com.freestyleperu.aplicacion.shared.exception;

import org.springframework.http.HttpStatus;

public class ReglaDeNegocioException extends BusinessException {

    public ReglaDeNegocioException(String message) {
        super(HttpStatus.CONFLICT, "BUSINESS_RULE_VIOLATION", message);
    }
}
