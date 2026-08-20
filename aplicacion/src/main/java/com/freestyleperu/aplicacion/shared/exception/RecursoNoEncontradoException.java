package com.freestyleperu.aplicacion.shared.exception;

import org.springframework.http.HttpStatus;

public class RecursoNoEncontradoException extends BusinessException {

    public RecursoNoEncontradoException(String message) {
        super(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    public static RecursoNoEncontradoException de(String entidad, Object id) {
        return new RecursoNoEncontradoException(entidad + " no encontrado: " + id);
    }
}
