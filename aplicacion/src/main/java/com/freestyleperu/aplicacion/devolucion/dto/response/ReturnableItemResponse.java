package com.freestyleperu.aplicacion.devolucion.dto.response;

public record ReturnableItemResponse(
        Long saleDetailId,
        Long variantId,
        String productName,
        String variantSku,
        int quantitySold,
        int quantityReturned,
        int quantityReturnable) {
}
