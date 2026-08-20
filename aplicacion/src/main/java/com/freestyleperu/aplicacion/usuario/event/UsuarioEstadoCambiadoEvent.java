package com.freestyleperu.aplicacion.usuario.event;

import com.freestyleperu.aplicacion.usuario.domain.UsuarioEstado;

/**
 * Publicado por {@code usuario} cuando el estado de una cuenta cambia, para
 * que otros módulos (p. ej. {@code auth}, revocando refresh tokens) puedan
 * reaccionar sin que {@code usuario} dependa de ellos.
 */
public record UsuarioEstadoCambiadoEvent(Long usuarioId, UsuarioEstado nuevoEstado) {
}
