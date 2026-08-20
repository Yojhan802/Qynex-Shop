package com.freestyleperu.aplicacion.pago.web;

import com.freestyleperu.aplicacion.pago.dto.request.ActualizarPaymentMethodRequest;
import com.freestyleperu.aplicacion.pago.dto.response.PaymentMethodResponse;
import com.freestyleperu.aplicacion.pago.service.PagoService;
import com.freestyleperu.aplicacion.shared.dto.CambiarEstadoRequest;
import com.freestyleperu.aplicacion.shared.security.Permisos;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class PagoController {

    private final PagoService pagoService;

    public PagoController(PagoService pagoService) {
        this.pagoService = pagoService;
    }

    @GetMapping("/api/payment-methods")
    public List<PaymentMethodResponse> listar() {
        return pagoService.listar();
    }

    @GetMapping("/api/payment-methods/{id}")
    public PaymentMethodResponse obtener(@PathVariable Long id) {
        return pagoService.obtener(id);
    }

    @PutMapping("/api/payment-methods/{id}")
    @PreAuthorize("hasAuthority('" + Permisos.CONFIGURACION_PAGOS + "')")
    public PaymentMethodResponse actualizar(@PathVariable Long id, @Valid @RequestBody ActualizarPaymentMethodRequest request) {
        return pagoService.actualizar(id, request);
    }

    @PostMapping("/api/payment-methods/{id}/qr")
    @PreAuthorize("hasAuthority('" + Permisos.CONFIGURACION_PAGOS + "')")
    public PaymentMethodResponse actualizarQr(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return pagoService.actualizarQr(id, file);
    }

    @PatchMapping("/api/payment-methods/{id}/status")
    @PreAuthorize("hasAuthority('" + Permisos.CONFIGURACION_PAGOS + "')")
    public PaymentMethodResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambiarEstadoRequest request) {
        return pagoService.cambiarEstado(id, request.status());
    }
}
