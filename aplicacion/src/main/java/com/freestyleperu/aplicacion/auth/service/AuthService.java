package com.freestyleperu.aplicacion.auth.service;

import com.freestyleperu.aplicacion.auth.domain.RefreshToken;
import com.freestyleperu.aplicacion.auth.dto.LoginResponse;
import com.freestyleperu.aplicacion.auth.dto.TokenResponse;
import com.freestyleperu.aplicacion.auth.dto.UsuarioActualResponse;
import com.freestyleperu.aplicacion.auth.repository.RefreshTokenRepository;
import com.freestyleperu.aplicacion.shared.audit.AuditResult;
import com.freestyleperu.aplicacion.shared.audit.AuditService;
import com.freestyleperu.aplicacion.shared.exception.AutenticacionException;
import com.freestyleperu.aplicacion.shared.exception.RecursoNoEncontradoException;
import com.freestyleperu.aplicacion.shared.exception.ReglaDeNegocioException;
import com.freestyleperu.aplicacion.shared.security.AccountLockProperties;
import com.freestyleperu.aplicacion.shared.security.JwtService;
import com.freestyleperu.aplicacion.shared.security.TokenHasher;
import com.freestyleperu.aplicacion.usuario.domain.Rol;
import com.freestyleperu.aplicacion.usuario.domain.Usuario;
import com.freestyleperu.aplicacion.usuario.domain.UsuarioEstado;
import com.freestyleperu.aplicacion.usuario.repository.UsuarioRepository;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private static final String MENSAJE_CREDENCIALES_INVALIDAS = "Usuario o contraseña incorrectos";
    private static final Pattern PASSWORD_POLICY = Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d).{8,}$");
    private static final String TOKEN_TYPE = "Bearer";

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;
    private final AccountLockProperties lockProperties;

    public AuthService(UsuarioRepository usuarioRepository, RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService, PasswordEncoder passwordEncoder, AuditService auditService,
            AccountLockProperties lockProperties) {
        this.usuarioRepository = usuarioRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
        this.auditService = auditService;
        this.lockProperties = lockProperties;
    }

    public LoginResponse login(String username, String rawPassword) {
        Usuario usuario = usuarioRepository.findByUsername(username).orElse(null);

        if (usuario == null || !passwordEncoder.matches(rawPassword, usuario.getPasswordHash())) {
            if (usuario != null) {
                registrarIntentoFallido(usuario);
            }
            auditService.logAs(usuario == null ? null : usuario.getId(), username,
                    "LOGIN", "USUARIO", usuario == null ? null : usuario.getId(), null, null, AuditResult.FAILURE);
            throw new AutenticacionException(MENSAJE_CREDENCIALES_INVALIDAS);
        }

        if (usuario.getStatus() != UsuarioEstado.ACTIVE) {
            auditService.logAs(usuario.getId(), username, "LOGIN", "USUARIO", usuario.getId(), null, null, AuditResult.DENIED);
            throw new AutenticacionException("Esta cuenta no está activa. Contacta a un administrador.");
        }

        if (usuario.isBloqueadoTemporalmente()) {
            auditService.logAs(usuario.getId(), username, "LOGIN", "USUARIO", usuario.getId(), null, null, AuditResult.DENIED);
            throw new AutenticacionException("Cuenta bloqueada temporalmente por intentos fallidos. Intenta más tarde.");
        }

        usuario.setFailedAttempts((short) 0);
        usuario.setLockedUntil(null);
        usuario.setLastLoginAt(LocalDateTime.now());

        Set<String> authorities = usuario.permisosEfectivos();
        String accessToken = jwtService.generateAccessToken(usuario.getId(), usuario.getUsername(), authorities);
        String rawRefreshToken = crearRefreshToken(usuario);

        auditService.logAs(usuario.getId(), username, "LOGIN", "USUARIO", usuario.getId(), null, null, AuditResult.SUCCESS);

        return new LoginResponse(accessToken, rawRefreshToken, TOKEN_TYPE, jwtService.getAccessTokenSeconds(),
                aRespuestaActual(usuario, authorities));
    }

    public TokenResponse refresh(String rawRefreshToken) {
        String hash = TokenHasher.sha256Hex(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new AutenticacionException("Token de actualización inválido"));

        if (!token.isValido()) {
            throw new AutenticacionException("Token de actualización inválido o expirado");
        }

        Usuario usuario = token.getUsuario();
        if (usuario.getStatus() != UsuarioEstado.ACTIVE) {
            throw new AutenticacionException("Esta cuenta no está activa");
        }

        token.setRevokedAt(LocalDateTime.now());
        String nuevoRawRefreshToken = crearRefreshToken(usuario);
        String accessToken = jwtService.generateAccessToken(usuario.getId(), usuario.getUsername(), usuario.permisosEfectivos());

        return new TokenResponse(accessToken, nuevoRawRefreshToken, TOKEN_TYPE, jwtService.getAccessTokenSeconds());
    }

    public void logout(String rawRefreshToken) {
        String hash = TokenHasher.sha256Hex(rawRefreshToken);
        refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(LocalDateTime.now());
                auditService.log("LOGOUT", "USUARIO", token.getUsuario().getId(), null, null, AuditResult.SUCCESS);
            }
        });
    }

    public UsuarioActualResponse me(Long usuarioId) {
        Usuario usuario = usuarioRepository.findWithRolesById(usuarioId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", usuarioId));
        return aRespuestaActual(usuario, usuario.permisosEfectivos());
    }

    public void changePassword(Long usuarioId, String currentPassword, String newPassword) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> RecursoNoEncontradoException.de("Usuario", usuarioId));

        if (!passwordEncoder.matches(currentPassword, usuario.getPasswordHash())) {
            throw new AutenticacionException("La contraseña actual es incorrecta");
        }
        if (!PASSWORD_POLICY.matcher(newPassword).matches()) {
            throw new ReglaDeNegocioException("La nueva contraseña debe tener al menos 8 caracteres, con letras y números");
        }

        usuario.setPasswordHash(passwordEncoder.encode(newPassword));
        usuario.setMustChangePassword(false);
        refreshTokenRepository.revocarTodosDelUsuario(usuario.getId(), LocalDateTime.now());

        auditService.log("PASSWORD_CAMBIADO", "USUARIO", usuario.getId(), null, null, AuditResult.SUCCESS);
    }

    private void registrarIntentoFallido(Usuario usuario) {
        short intentos = (short) (usuario.getFailedAttempts() + 1);
        usuario.setFailedAttempts(intentos);
        if (intentos >= lockProperties.getMaxFailedAttempts()) {
            usuario.setLockedUntil(LocalDateTime.now().plusMinutes(lockProperties.getLockDurationMinutes()));
        }
    }

    private String crearRefreshToken(Usuario usuario) {
        String raw = jwtService.generateRawRefreshToken();
        RefreshToken token = new RefreshToken();
        token.setUsuario(usuario);
        token.setTokenHash(TokenHasher.sha256Hex(raw));
        token.setExpiresAt(LocalDateTime.now().plusDays(jwtService.getRefreshTokenDays()));
        token.setCreatedAt(LocalDateTime.now());
        refreshTokenRepository.save(token);
        return raw;
    }

    private UsuarioActualResponse aRespuestaActual(Usuario usuario, Set<String> authorities) {
        List<String> roles = usuario.getRoles().stream().map(Rol::getCode).sorted().toList();
        List<String> permissions = authorities.stream().sorted(Comparator.naturalOrder()).toList();
        return new UsuarioActualResponse(usuario.getId(), usuario.getUsername(), usuario.getFullName(),
                usuario.isMustChangePassword(), roles, permissions);
    }
}
