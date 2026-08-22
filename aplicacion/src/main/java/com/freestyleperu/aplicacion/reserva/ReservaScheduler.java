package com.freestyleperu.aplicacion.reserva;

import com.freestyleperu.aplicacion.reserva.service.ReservaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Revisión horaria de separaciones vencidas — más frecuente que
 * {@code SuscripcionScheduler} (diario) porque una separación típica dura
 * pocos días y el stock debe liberarse pronto para no perder ventas.
 */
@Component
public class ReservaScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReservaScheduler.class);

    private final ReservaService reservaService;

    public ReservaScheduler(ReservaService reservaService) {
        this.reservaService = reservaService;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void vencerSeparaciones() {
        int vencidas = reservaService.vencerSeparacionesPendientes();
        if (vencidas > 0) {
            log.info("{} separación(es) vencida(s) automáticamente, stock liberado", vencidas);
        }
    }
}
