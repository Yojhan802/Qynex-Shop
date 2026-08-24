package com.freestyleperu.aplicacion.reporte.service;

import com.freestyleperu.aplicacion.ia.OpenRouterClient;
import com.freestyleperu.aplicacion.reporte.dto.request.AsistenteReporteRequest;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * "Pregúntale a tus datos" — el frontend manda la pregunta más el JSON del reporte que YA tiene
 * en pantalla (el mismo que ve el usuario, ya calculado por los endpoints reales de reportes);
 * el asistente solo lo lee y responde en lenguaje natural, nunca recalcula ni inventa números
 * que no estén en ese JSON.
 */
@Service
public class ReporteAsistenteService {

    private final OpenRouterClient openRouterClient;

    public ReporteAsistenteService(OpenRouterClient openRouterClient) {
        this.openRouterClient = openRouterClient;
    }

    public String responder(AsistenteReporteRequest request) {
        String systemPrompt = "Eres un analista de datos para un negocio de ropa en Perú (tienda física y online). "
                + "Respondes en español, breve y directo, con los números exactos cuando los pidan. "
                + "SOLO puedes usar los datos del reporte de abajo — no los recalcules, no inventes cifras que no estén ahí. "
                + "Si la pregunta no se puede responder con estos datos, dilo claramente en vez de adivinar.\n\n"
                + "Datos del reporte (JSON tal como está en pantalla ahora mismo):\n" + request.datos();

        return openRouterClient.completar(List.of(
                new OpenRouterClient.OpenRouterMessage("system", systemPrompt),
                new OpenRouterClient.OpenRouterMessage("user", request.pregunta())));
    }
}
