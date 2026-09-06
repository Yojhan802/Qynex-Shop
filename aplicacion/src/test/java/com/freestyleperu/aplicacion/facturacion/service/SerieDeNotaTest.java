package com.freestyleperu.aplicacion.facturacion.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.freestyleperu.aplicacion.facturacion.domain.BillingConfiguration;
import com.freestyleperu.aplicacion.facturacion.domain.BillingProvider;
import com.freestyleperu.aplicacion.facturacion.domain.ElectronicDocument;
import com.freestyleperu.aplicacion.facturacion.domain.ElectronicDocumentType;
import org.junit.jupiter.api.Test;

/**
 * La serie de una nota la decide el comprobante que modifica, no el tipo de nota.
 *
 * <p>SUNAT exige serie F para una nota sobre factura y B para una sobre boleta. Cruzarlas es
 * un rechazo por formato (error 1001) que, a diferencia de una validacion local, ocurre
 * <b>despues</b> de reservar el correlativo: ese numero ya no vuelve. Por eso se fija con un
 * test, en vez de confiar en que quien configure ponga la serie correcta.
 */
class SerieDeNotaTest {

    @Test
    void unaNotaSobreFacturaUsaLaSerieDeFacturaYUnaSobreBoletaLaDeBoleta() {
        BillingConfiguration config = config("BC01", "BD01", "FC01", "FD01");

        assertThat(serie(config, ElectronicDocumentType.NOTA_CREDITO, ElectronicDocumentType.FACTURA))
                .isEqualTo("FC01");
        assertThat(serie(config, ElectronicDocumentType.NOTA_DEBITO, ElectronicDocumentType.FACTURA))
                .isEqualTo("FD01");
        assertThat(serie(config, ElectronicDocumentType.NOTA_CREDITO, ElectronicDocumentType.BOLETA))
                .isEqualTo("BC01");
        assertThat(serie(config, ElectronicDocumentType.NOTA_DEBITO, ElectronicDocumentType.BOLETA))
                .isEqualTo("BD01");
    }

    /**
     * Las empresas configuradas antes de separar las series solo tienen la general. Caer a
     * ella es preferible a no emitir: si fuera la equivocada lo dira SUNAT, pero dejar de
     * emitir por una columna nueva vacia romperia a quien ya estaba funcionando.
     */
    @Test
    void sinSerieEspecificaDeFacturaSeUsaLaGeneral() {
        BillingConfiguration config = config("B001", "B001", null, "   ");

        assertThat(serie(config, ElectronicDocumentType.NOTA_CREDITO, ElectronicDocumentType.FACTURA))
                .isEqualTo("B001");
        assertThat(serie(config, ElectronicDocumentType.NOTA_DEBITO, ElectronicDocumentType.FACTURA))
                .isEqualTo("B001");
    }

    /** Factura y boleta toman su propia serie, sin pasar por la logica de notas. */
    @Test
    void laFacturaYLaBoletaNoCambian() {
        BillingConfiguration config = config("BC01", "BD01", "FC01", "FD01");
        config.setInvoiceSeries("F001");
        config.setReceiptSeries("B001");

        assertThat(serie(config, ElectronicDocumentType.FACTURA, null)).isEqualTo("F001");
        assertThat(serie(config, ElectronicDocumentType.BOLETA, null)).isEqualTo("B001");
    }

    /**
     * NubeFact numera las notas en la serie del comprobante de origen, asi que ignora lo
     * configurado. Separar las series no puede cambiarle el comportamiento.
     */
    @Test
    void nubeFactSigueReutilizandoLaSerieDelComprobanteDeOrigen() {
        BillingConfiguration config = config("BC01", "BD01", "FC01", "FD01");
        config.setProvider(BillingProvider.NUBEFACT);

        ElectronicDocument origen = new ElectronicDocument();
        origen.setDocumentType(ElectronicDocumentType.FACTURA);
        origen.setSeries("F007");

        assertThat(ElectronicDocumentService.seriesFor(
                config, ElectronicDocumentType.NOTA_CREDITO, origen)).isEqualTo("F007");
    }

    private BillingConfiguration config(String credito, String debito, String creditoFactura, String debitoFactura) {
        BillingConfiguration config = new BillingConfiguration();
        config.setProvider(BillingProvider.QYNEX_CPE);
        config.setCreditNoteSeries(credito);
        config.setDebitNoteSeries(debito);
        config.setCreditNoteInvoiceSeries(creditoFactura);
        config.setDebitNoteInvoiceSeries(debitoFactura);
        return config;
    }

    private String serie(BillingConfiguration config, ElectronicDocumentType tipo,
            ElectronicDocumentType tipoOrigen) {
        ElectronicDocument origen = null;
        if (tipoOrigen != null) {
            origen = new ElectronicDocument();
            origen.setDocumentType(tipoOrigen);
        }
        return ElectronicDocumentService.seriesFor(config, tipo, origen);
    }
}
