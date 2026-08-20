package com.freestyleperu.aplicacion.reporte.dto.response;

import java.util.List;

public record DashboardResponse(
        ResumenPeriodoResponse salesToday,
        ResumenPeriodoResponse salesMonth,
        long productsSoldToday,
        long lowStockCount,
        long outOfStockCount,
        List<SerieEtiquetaResponse> paymentBreakdown,
        List<SerieDiaResponse> salesByDay,
        List<ProductoTopResponse> topProducts) {
}
