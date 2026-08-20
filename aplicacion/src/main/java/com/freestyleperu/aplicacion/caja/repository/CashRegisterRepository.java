package com.freestyleperu.aplicacion.caja.repository;

import com.freestyleperu.aplicacion.caja.domain.CashRegister;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CashRegisterRepository extends JpaRepository<CashRegister, Long> {

    List<CashRegister> findAllByOrderByNameAsc();
}
