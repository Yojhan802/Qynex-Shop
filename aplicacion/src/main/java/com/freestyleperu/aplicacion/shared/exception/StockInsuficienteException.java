package com.freestyleperu.aplicacion.shared.exception;

import org.springframework.http.HttpStatus;

public class StockInsuficienteException extends BusinessException {

    public StockInsuficienteException(String message) {
        super(HttpStatus.CONFLICT, "INSUFFICIENT_STOCK", message);
    }
}
