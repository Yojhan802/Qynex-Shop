package com.freestyleperu.aplicacion.notificacion.service;

import com.freestyleperu.aplicacion.pedido.dto.response.PedidoResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Empuja eventos de pedidos a los navegadores conectados vía SSE — sin cola/broker externo,
 * solo listas en memoria (aceptable porque un reinicio del proceso igual tumba las conexiones
 * SSE abiertas, así que no hay estado que sobreviva un restart de todas formas). Pensado para
 * que agregar WhatsApp/email más adelante sea sumar otro "suscriptor" a estos mismos eventos,
 * no modificar quien los dispara ({@code PedidoService}).
 */
@Service
public class NotificacionService {

    private static final long EMITTER_TIMEOUT_MS = 30 * 60 * 1000L;

    private final List<SseEmitter> staffEmitters = new CopyOnWriteArrayList<>();
    private final Map<Long, List<SseEmitter>> emittersPorCliente = new ConcurrentHashMap<>();

    public SseEmitter suscribirStaff() {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        emitter.onCompletion(() -> staffEmitters.remove(emitter));
        emitter.onTimeout(() -> staffEmitters.remove(emitter));
        emitter.onError(e -> staffEmitters.remove(emitter));
        staffEmitters.add(emitter);
        return emitter;
    }

    public SseEmitter suscribirCliente(Long customerId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        List<SseEmitter> lista = emittersPorCliente.computeIfAbsent(customerId, k -> new CopyOnWriteArrayList<>());
        emitter.onCompletion(() -> lista.remove(emitter));
        emitter.onTimeout(() -> lista.remove(emitter));
        emitter.onError(e -> lista.remove(emitter));
        lista.add(emitter);
        return emitter;
    }

    public void notificarPedidoNuevo(PedidoResponse pedido) {
        enviarATodos(staffEmitters, "pedido-nuevo", pedido);
    }

    public void notificarPedidoActualizado(Long customerId, PedidoResponse pedido) {
        List<SseEmitter> lista = emittersPorCliente.get(customerId);
        if (lista != null) {
            enviarATodos(lista, "pedido-actualizado", pedido);
        }
    }

    private void enviarATodos(List<SseEmitter> emitters, String evento, Object datos) {
        for (SseEmitter emitter : List.copyOf(emitters)) {
            try {
                emitter.send(SseEmitter.event().name(evento).data(datos));
            } catch (IOException | IllegalStateException e) {
                // Cliente desconectado o emitter ya cerrado — nunca debe tumbar la transacción que originó el evento.
                emitters.remove(emitter);
            }
        }
    }
}
