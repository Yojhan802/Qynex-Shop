package com.freestyleperu.aplicacion.producto.mapper;

import com.freestyleperu.aplicacion.producto.domain.Product;
import com.freestyleperu.aplicacion.producto.domain.ProductVariant;
import com.freestyleperu.aplicacion.producto.dto.response.ProductoDetalleResponse;
import com.freestyleperu.aplicacion.producto.dto.response.ProductoResumenResponse;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProductoMapper {

    private final VarianteMapper varianteMapper;

    public ProductoMapper(VarianteMapper varianteMapper) {
        this.varianteMapper = varianteMapper;
    }

    public ProductoResumenResponse toResumen(Product product, List<ProductVariant> variants) {
        int totalStock = variants.stream().mapToInt(ProductVariant::getStock).sum();
        return new ProductoResumenResponse(
                product.getId(),
                product.getInternalCode(),
                product.getSku(),
                product.getName(),
                product.getCategory().getName(),
                product.getBrand() != null ? product.getBrand().getName() : null,
                product.getPrice(),
                product.getPromoPrice(),
                product.getStatus(),
                product.getImageUrl(),
                variants.size(),
                totalStock,
                product.getCreatedAt());
    }

    public ProductoDetalleResponse toDetalle(Product product, List<ProductVariant> variants) {
        return new ProductoDetalleResponse(
                product.getId(),
                product.getInternalCode(),
                product.getSku(),
                product.getName(),
                product.getCategory().getId(),
                product.getCategory().getName(),
                product.getSubcategory() != null ? product.getSubcategory().getId() : null,
                product.getSubcategory() != null ? product.getSubcategory().getName() : null,
                product.getBrand() != null ? product.getBrand().getId() : null,
                product.getBrand() != null ? product.getBrand().getName() : null,
                product.getDescription(),
                product.getMaterial(),
                product.getFit(),
                product.getPrice(),
                product.getPromoPrice(),
                product.getStatus(),
                product.getImageUrl(),
                product.getSizeGuideImageUrl(),
                variants.stream().map(varianteMapper::toResponse).toList(),
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
