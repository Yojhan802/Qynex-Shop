package com.freestyleperu.aplicacion.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String CLAIM_USERNAME = "username";
    private static final String CLAIM_AUTHORITIES = "authorities";
    private static final String CLAIM_TENANT_ID = "tenantId";
    /** Marca los tokens que NO son de sesión. Ver generateComplaintReceiptToken y parse. */
    private static final String CLAIM_PROPOSITO = "purpose";
    private static final String PROPOSITO_CONSTANCIA = "complaint-receipt";
    private static final int MINUTOS_CONSTANCIA = 15;

    private final JwtProperties properties;
    private final SecureRandom secureRandom = new SecureRandom();
    private SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    void validarSecreto() {
        if (properties.getSecret() == null || properties.getSecret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException(
                    "JWT_SECRET no está definido o tiene menos de 32 caracteres. "
                            + "Defínelo como variable de entorno antes de iniciar la aplicación.");
        }
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(Long userId, String username, Set<String> authorities, Long tenantId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getAccessTokenMinutes(), ChronoUnit.MINUTES);
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim(CLAIM_USERNAME, username)
                .claim(CLAIM_AUTHORITIES, authorities)
                .claim(CLAIM_TENANT_ID, String.valueOf(tenantId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public int getAccessTokenSeconds() {
        return properties.getAccessTokenMinutes() * 60;
    }

    public int getRefreshTokenDays() {
        return properties.getRefreshTokenDays();
    }

    public String generateRawRefreshToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * Permiso de un solo propósito para descargar la constancia de una hoja de reclamación
     * recién registrada. Se entrega junto a la constancia y vive 15 minutos.
     *
     * <p>Existe porque los números de hoja son correlativos y predecibles: un endpoint que
     * aceptara el número sin más dejaría recorrerlos y bajar los datos personales de todos
     * los consumidores. Con esto, solo quien acaba de registrar la hoja puede descargarla.
     */
    public String generateComplaintReceiptToken(Long complaintId, Long tenantId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(String.valueOf(complaintId))
                .claim(CLAIM_PROPOSITO, PROPOSITO_CONSTANCIA)
                .claim(CLAIM_TENANT_ID, String.valueOf(tenantId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(MINUTOS_CONSTANCIA, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /**
     * Id de la hoja que autoriza el token, o {@code null} si no es válido. Exige que el
     * tenant coincida: un token emitido en una tienda no sirve en otra.
     */
    public Long parseComplaintReceiptToken(String token, Long tenantId) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            if (!PROPOSITO_CONSTANCIA.equals(claims.get(CLAIM_PROPOSITO, String.class))) return null;
            if (!String.valueOf(tenantId).equals(claims.get(CLAIM_TENANT_ID, String.class))) return null;
            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }

    public AuthenticatedUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            // Un token con propósito propio no es de sesión. Sin este corte, el de la
            // constancia autenticaría con subject = id de la hoja, es decir como el usuario
            // que tuviera ese id. Se rechaza explícitamente en vez de confiar en el formato.
            if (claims.get(CLAIM_PROPOSITO) != null) return null;
            Long userId = Long.valueOf(claims.getSubject());
            String username = claims.get(CLAIM_USERNAME, String.class);
            @SuppressWarnings("unchecked")
            List<String> authorities = claims.get(CLAIM_AUTHORITIES, List.class);
            Set<String> authoritySet = authorities == null
                    ? Set.of()
                    : authorities.stream().collect(Collectors.toUnmodifiableSet());
            Long tenantId = Long.valueOf(claims.get(CLAIM_TENANT_ID, String.class));
            return new AuthenticatedUser(userId, username, authoritySet, tenantId);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }
}
