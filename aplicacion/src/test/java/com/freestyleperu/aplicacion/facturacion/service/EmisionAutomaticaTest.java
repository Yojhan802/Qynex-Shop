package com.freestyleperu.aplicacion.facturacion.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.freestyleperu.aplicacion.pedido.domain.PedidoBillingDocumentType;
import com.freestyleperu.aplicacion.venta.domain.Sale;
import com.freestyleperu.aplicacion.venta.repository.SaleRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * El comprobante se emite solo al cerrar la venta, venga de donde venga: caja, tienda web
 * pagada a mano, pago online o separacion. Lo que se fija aqui es lo que no puede fallar en
 * ninguno de esos cuatro caminos.
 *
 * <p>La regla de fondo: <b>emitir es un efecto posterior a la venta, y nunca al reves</b>. La
 * venta ya esta cobrada cuando esto corre; si el proveedor no responde, el cobro sigue siendo
 * valido y el comprobante se reintenta. Al contrario —dejar que un fallo del proveedor tumbe
 * la venta— se pierde dinero real por un problema de red.
 */
class EmisionAutomaticaTest {

    @Test
    void unaVentaConTicketNoEmiteComprobanteElectronico() {
        Escenario esc = escenario(PedidoBillingDocumentType.TICKET);

        esc.service().emitirAutomaticamenteParaVenta(7L);

        // TICKET es un comprobante interno, no un documento ante SUNAT: emitir uno seria
        // declarar una venta que el usuario decidio no documentar.
        verify(esc.repository(), never()).save(any());
    }

    @Test
    void unaVentaSinTipoDeComprobanteSeTrataComoTicket() {
        Escenario esc = escenario(null);

        esc.service().emitirAutomaticamenteParaVenta(7L);

        // Nulo no puede significar "emite lo que sea": ante la duda no se declara nada.
        verify(esc.repository(), never()).save(any());
    }

    /**
     * El caso que justifica todo el diseno: si el proveedor esta caido, la caja no se puede
     * quedar bloqueada ni la venta revertida.
     */
    @Test
    void unFalloDelProveedorNoRompeLaVenta() {
        Escenario esc = escenario(PedidoBillingDocumentType.BOLETA);
        when(esc.repository().findById(7L)).thenThrow(new IllegalStateException("proveedor caido"));

        assertThatCode(() -> esc.service().emitirTrasCommit(7L)).doesNotThrowAnyException();
    }

    @Test
    void sinVentaNoSeIntentaEmitirNada() {
        Escenario esc = escenario(PedidoBillingDocumentType.BOLETA);
        when(esc.repository().findById(anyLong())).thenReturn(Optional.empty());

        // Una venta que ya no esta —borrada, o de otra empresa— no puede tumbar el flujo.
        assertThatCode(() -> esc.service().emitirTrasCommit(7L)).doesNotThrowAnyException();
    }

    private record Escenario(ElectronicDocumentService service, SaleRepository repository) {
    }

    /**
     * Solo se le da el repositorio de ventas: es lo unico que interviene en decidir si hay
     * algo que emitir. El resto de dependencias van nulas a proposito — si alguna hiciera
     * falta, el test reventaria y estaria diciendo que la decision dejo de ser local.
     */
    private Escenario escenario(PedidoBillingDocumentType tipo) {
        Sale sale = new Sale();
        sale.setBillingDocumentType(tipo);

        SaleRepository repository = mock(SaleRepository.class);
        when(repository.findById(7L)).thenReturn(Optional.of(sale));

        ElectronicDocumentService service = new ElectronicDocumentService(
                null, null, null, null, repository, null, null, null, null, null, java.util.List.of());
        return new Escenario(service, repository);
    }

}
