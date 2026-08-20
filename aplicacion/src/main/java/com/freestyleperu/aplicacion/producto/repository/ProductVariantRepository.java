package com.freestyleperu.aplicacion.producto.repository;

import com.freestyleperu.aplicacion.producto.domain.ProductVariant;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    @EntityGraph(attributePaths = { "product", "color", "size" })
    List<ProductVariant> findAllByProductIdOrderBySizeSortOrderAscColorNameAsc(Long productId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT v FROM ProductVariant v WHERE v.id = :id")
    Optional<ProductVariant> lockById(@Param("id") Long id);

    @EntityGraph(attributePaths = { "product", "color", "size" })
    @Query("""
            SELECT v FROM ProductVariant v
            WHERE (:search IS NULL
                   OR LOWER(v.product.name) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(v.sku) LIKE LOWER(CONCAT('%', :search, '%'))
                   OR LOWER(v.barcode) LIKE LOWER(CONCAT('%', :search, '%')))
            """)
    Page<ProductVariant> buscarInventario(@Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = { "product", "color", "size" })
    @Query("SELECT v FROM ProductVariant v WHERE v.stock <= v.minStock AND v.status = 'ACTIVE' ORDER BY v.stock ASC")
    List<ProductVariant> findLowStock();

    @EntityGraph(attributePaths = { "product", "color", "size" })
    @Query("SELECT v FROM ProductVariant v WHERE v.stock = 0 AND v.status = 'ACTIVE' ORDER BY v.updatedAt DESC")
    List<ProductVariant> findOutOfStock();

    boolean existsByProductIdAndColorIdAndSizeId(Long productId, Long colorId, Long sizeId);

    boolean existsByBarcode(String barcode);

    boolean existsBySku(String sku);

    @EntityGraph(attributePaths = { "product", "color", "size" })
    Optional<ProductVariant> findByBarcode(String barcode);

    @EntityGraph(attributePaths = { "product", "color", "size" })
    @Query("""
            SELECT v FROM ProductVariant v
            WHERE LOWER(v.sku) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(v.barcode) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(v.product.name) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    List<ProductVariant> buscar(@Param("query") String query);
}
