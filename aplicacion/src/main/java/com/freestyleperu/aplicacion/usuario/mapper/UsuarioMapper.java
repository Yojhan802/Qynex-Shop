package com.freestyleperu.aplicacion.usuario.mapper;

import com.freestyleperu.aplicacion.usuario.domain.Rol;
import com.freestyleperu.aplicacion.usuario.domain.Usuario;
import com.freestyleperu.aplicacion.usuario.dto.response.UsuarioResponse;
import java.util.Comparator;
import org.springframework.stereotype.Component;

@Component
public class UsuarioMapper {

    private final RolMapper rolMapper;

    public UsuarioMapper(RolMapper rolMapper) {
        this.rolMapper = rolMapper;
    }

    public UsuarioResponse toResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getUsername(),
                usuario.getEmail(),
                usuario.getFullName(),
                usuario.getDni(),
                usuario.getPhone(),
                usuario.getStatus(),
                usuario.isMustChangePassword(),
                usuario.getLastLoginAt(),
                usuario.getRoles().stream()
                        .sorted(Comparator.comparing(Rol::getName))
                        .map(rolMapper::toResumen)
                        .toList(),
                usuario.getCreatedAt());
    }
}
