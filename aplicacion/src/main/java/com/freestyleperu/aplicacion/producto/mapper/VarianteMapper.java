package com.freestyleperu.aplicacion.producto.mapper;

import com.freestyleperu.aplicacion.producto.domain.Product;
import com.freestyleperu.aplicacion.producto.domain.ProductVariant;
import com.freestyleperu.aplicacion.producto.dto.response.VarianteBusquedaResponse;
import com.freestyleperu.aplicacion.producto.dto.response.VarianteResponse;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class VarianteMapper {

    public VarianteResponse toResponse(ProductVariant variant) {
        return new VarianteResponse(
                variant.getId(),
                variant.getProduct().getId(),
                variant.getProduct().getName(),
                variant.getColor().getId(),
                variant.getColor().getName(),
                variant.getColor().getHexCode(),
                variant.getSize().getId(),
                variant.getSize().getName(),
                variant.getSku(),
                variant.getBarcode(),
                variant.getStock(),
                variant.getMinStock(),
                variant.getStatus());
    }

    public VarianteBusquedaResponse toBusquedaResponse(ProductVariant variant) {
        Product product = variant.getProduct();
        BigDecimal effectivePrice = product.getPromoPrice() != null ? product.getPromoPrice() : product.getPrice();
        return new VarianteBusquedaResponse(
                variant.getId(),
                product.getName(),
                variant.getColor().getName(),
                variant.getSize().getName(),
                variant.getSku(),
                variant.getBarcode(),
                product.getPrice(),
                product.getPromoPrice(),
                effectivePrice,
                variant.getStock(),
                variant.getStatus());
    }
}
