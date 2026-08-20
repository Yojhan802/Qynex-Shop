package com.freestyleperu.aplicacion.usuario.service;

import com.freestyleperu.aplicacion.shared.audit.AuditResult;
import com.freestyleperu.aplicacion.shared.audit.AuditService;
import com.freestyleperu.aplicacion.shared.exception.OperacionNoPermitidaException;
import com.freestyleperu.aplicacion.shared.exception.RecursoDuplicadoException;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.usuario.domain.Permiso;
import com.freestyleperu.aplicacion.usuario.domain.Rol;
import com.freestyleperu.aplicacion.usuario.dto.request.ActualizarRolRequest;
import com.freestyleperu.aplicacion.usuario.dto.request.AsignarPermisosRequest;
import com.freestyleperu.aplicacion.usuario.dto.request.CrearRolRequest;
import com.freestyleperu.aplicacion.usuario.dto.response.RolResponse;
import com.freestyleperu.aplicacion.usuario.mapper.RolMapper;
import com.freestyleperu.aplicacion.usuario.repository.PermisoRepository;
import com.freestyleperu.aplicacion.usuario.repository.RolRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RolService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final RolMapper rolMapper;
    private final AuditService auditService;

    public RolService(RolRepository rolRepository, PermisoRepository permisoRepository,
            RolMapper rolMapper, AuditService auditService) {
        this.rolRepository = rolRepository;
        this.permisoRepository = permisoRepository;
        this.rolMapper = rolMapper;
        this.auditService = auditService;
    }

    public List<RolResponse> listar() {
        return rolRepository.findAllByOrderByNameAsc().stream().map(rolMapper::toResponse).toList();
    }

    public RolResponse obtener(Long id) {
        return rolMapper.toResponse(buscarOFallar(id));
    }

    @Transactional
    public RolResponse crear(CrearRolRequest request) {
        if (rolRepository.existsByCode(request.code())) {
            throw new RecursoDuplicadoException("Ya existe un rol con el código " + request.code());
        }
        Rol rol = new Rol();
        rol.setCode(request.code());
        rol.setName(request.name());
        rol.setDescription(request.description());
        rol.setSystem(false);
        Rol guardado = rolRepository.save(rol);
        auditService.log("ROL_CREADO", "ROL", guardado.getId(), null, request, AuditResult.SUCCESS);
        return rolMapper.toResponse(guardado);
    }

    @Transactional
    public RolResponse actualizar(Long id, ActualizarRolRequest request) {
        Rol rol = buscarOFallar(id);
        rol.setName(request.name());
        rol.setDescription(request.description());
        auditService.log("ROL_ACTUALIZADO", "ROL", rol.getId(), null, request, AuditResult.SUCCESS);
        return rolMapper.toResponse(rol);
    }

    @Transactional
    public RolResponse actualizarPermisos(Long id, AsignarPermisosRequest request) {
        Rol rol = buscarOFallar(id);
        if (rol.isSystem() && "ADMINISTRADOR".equals(rol.getCode())) {
            throw new OperacionNoPermitidaException("No se pueden modificar los permisos del rol Administrador");
        }
        List<Permiso> permisos = permisoRepository.findAllById(request.permissionIds());
        if (permisos.size() != request.permissionIds().size()) {
            throw new RecursoNoEncontradoException("Uno o más permisos no existen");
        }
        rol.setPermisos(new HashSet<>(permisos));
        auditService.log("ROL_PERMISOS_ACTUALIZADOS", "ROL", rol.getId(), null,
                Set.copyOf(request.permissionIds()), AuditResult.SUCCESS);
        return rolMapper.toResponse(rol);
    }

    private Rol buscarOFallar(Long id) {
        return rolRepository.findWithPermisosById(id)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Rol", id));
    }
}
