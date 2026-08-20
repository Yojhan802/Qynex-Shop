package com.freestyleperu.aplicacion.shared.exception;

import org.springframework.security.core.AuthenticationException;

public class AutenticacionException extends AuthenticationException {

    public AutenticacionException(String message) {
        super(message);
    }
}
