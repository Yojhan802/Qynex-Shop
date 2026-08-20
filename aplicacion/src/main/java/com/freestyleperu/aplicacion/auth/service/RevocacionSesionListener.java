package com.freestyleperu.aplicacion.auth.service;

import com.freestyleperu.aplicacion.auth.repository.RefreshTokenRepository;
import com.freestyleperu.aplicacion.usuario.domain.UsuarioEstado;
import com.freestyleperu.aplicacion.usuario.event.UsuarioEstadoCambiadoEvent;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class RevocacionSesionListener {

    private final RefreshTokenRepository refreshTokenRepository;

    public RevocacionSesionListener(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUsuarioEstadoCambiado(UsuarioEstadoCambiadoEvent event) {
        if (event.nuevoEstado() != UsuarioEstado.ACTIVE) {
            refreshTokenRepository.revocarTodosDelUsuario(event.usuarioId(), LocalDateTime.now());
        }
    }
}
