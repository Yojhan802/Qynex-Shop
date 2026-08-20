package com.freestyleperu.aplicacion.pago.repository;

import com.freestyleperu.aplicacion.pago.domain.PaymentMethod;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Long> {

    List<PaymentMethod> findAllByOrderBySortOrderAsc();
}
