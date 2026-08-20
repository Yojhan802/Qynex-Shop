package com.freestyleperu.aplicacion.producto.service;

/** Resultado interno de un ajuste de stock, usado solo entre servicios (nunca expuesto por un controller). */
public record AjusteStockResultado(Long variantId, String variantSku, String productName, int stockBefore, int stockAfter) {
}
