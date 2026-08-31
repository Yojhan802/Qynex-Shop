package com.freestyleperu.aplicacion.plataforma.repository;

import com.freestyleperu.aplicacion.configuracion.domain.BusinessVertical;
import com.freestyleperu.aplicacion.configuracion.domain.Plan;
import com.freestyleperu.aplicacion.configuracion.domain.SubscriptionStatus;
import com.freestyleperu.aplicacion.plataforma.dto.response.TenantResponse;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

/** Acceso deliberadamente global: solo lo usa el módulo protegido del operador de plataforma. */
@Repository
public class PlatformTenantRepository {

    private final JdbcTemplate jdbc;

    public PlatformTenantRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<TenantResponse> findAll(String search, SubscriptionStatus status) {
        StringBuilder sql = new StringBuilder("""
                SELECT cs.id, cs.slug, cs.name, cs.ruc, cs.address, cs.phone, cs.email, cs.business_vertical, cs.plan,
                       cs.subscription_status, cs.next_payment_due, cs.updated_at,
                       (SELECT u.username FROM users u
                          JOIN user_roles ur ON ur.user_id = u.id
                          JOIN roles r ON r.id = ur.role_id
                         WHERE u.tenant_id = cs.id AND r.code = 'ADMINISTRADOR'
                         ORDER BY u.id LIMIT 1) AS owner_username,
                       (SELECT COUNT(*) FROM users au
                         WHERE au.tenant_id = cs.id AND au.status = 'ACTIVE') AS active_users
                  FROM company_settings cs
                 WHERE 1 = 1
                """);
        List<Object> args = new ArrayList<>();
        if (search != null && !search.isBlank()) {
            sql.append(" AND (LOWER(cs.name) LIKE ? OR LOWER(cs.slug) LIKE ? OR COALESCE(cs.ruc, '') LIKE ?)");
            String value = "%" + search.trim().toLowerCase() + "%";
            args.add(value);
            args.add(value);
            args.add(value);
        }
        if (status != null) {
            sql.append(" AND cs.subscription_status = ?");
            args.add(status.name());
        }
        sql.append(" ORDER BY cs.id DESC");
        return jdbc.query(sql.toString(), (rs, rowNum) -> map(rs), args.toArray());
    }

    public boolean existsBySlug(String slug) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM company_settings WHERE slug = ?", Integer.class, slug) > 0;
    }

    public boolean existsUserInTenant(String username, String email, Long tenantId) {
        return jdbc.queryForObject("""
                SELECT COUNT(*) FROM users
                 WHERE tenant_id = ? AND (username = ? OR (? IS NOT NULL AND email = ?))
                """, Integer.class, tenantId, username, email, email) > 0;
    }

    public boolean existsTenant(Long tenantId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM company_settings WHERE id = ?", Integer.class, tenantId) > 0;
    }

    public Long insertTenant(String name, String slug, String ruc, String address, String phone, String email,
            BusinessVertical businessVertical, Plan plan, java.time.LocalDate nextPaymentDue, Long actorId, LocalDateTime now) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO company_settings (
                        slug, name, business_vertical, business_description, ruc, address, phone, email, logo_url,
                        currency_code, currency_symbol, igv_rate, ticket_footer, shipping_flat_rate,
                        reservation_deposit_amount, reservation_expiration_days, plan, subscription_status,
                        next_payment_due, online_payments_enabled, electronic_invoicing_enabled, updated_at, updated_by)
                    VALUES (?, ?, ?, NULL, ?, ?, ?, ?, NULL, 'PEN', 'S/', 0.1800,
                            'Gracias por su compra', 15.00, 20.00, 3, ?, 'ACTIVA', ?, FALSE, FALSE, ?, ?)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            int i = 1;
            statement.setString(i++, slug);
            statement.setString(i++, name);
            statement.setString(i++, businessVertical.name());
            statement.setString(i++, ruc);
            statement.setString(i++, address);
            statement.setString(i++, phone);
            statement.setString(i++, email);
            statement.setString(i++, plan.name());
            statement.setObject(i++, nextPaymentDue);
            statement.setObject(i++, now);
            statement.setObject(i, actorId);
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) throw new IllegalStateException("No se pudo obtener el id del nuevo tenant");
        return key.longValue();
    }

    public void updateTenant(Long tenantId, String name, String ruc, String address, String phone, String email,
            BusinessVertical businessVertical, Plan plan, SubscriptionStatus status, java.time.LocalDate nextPaymentDue,
            Long actorId, LocalDateTime now) {
        jdbc.update("""
                UPDATE company_settings
                   SET name = ?, ruc = ?, address = ?, phone = ?, email = ?, business_vertical = ?, plan = ?,
                       subscription_status = ?, next_payment_due = ?, updated_at = ?, updated_by = ?
                 WHERE id = ?
                """, name, ruc, address, phone, email, businessVertical.name(), plan.name(), status.name(), nextPaymentDue,
                now, actorId, tenantId);
    }

    public TenantResponse findById(Long tenantId) {
        return jdbc.query("""
                SELECT cs.id, cs.slug, cs.name, cs.ruc, cs.address, cs.phone, cs.email, cs.business_vertical, cs.plan,
                       cs.subscription_status, cs.next_payment_due, cs.updated_at,
                       (SELECT u.username FROM users u JOIN user_roles ur ON ur.user_id = u.id
                          JOIN roles r ON r.id = ur.role_id
                         WHERE u.tenant_id = cs.id AND r.code = 'ADMINISTRADOR'
                         ORDER BY u.id LIMIT 1) AS owner_username,
                       (SELECT COUNT(*) FROM users au
                         WHERE au.tenant_id = cs.id AND au.status = 'ACTIVE') AS active_users
                  FROM company_settings cs WHERE cs.id = ?
                """, (rs, rowNum) -> map(rs), tenantId).stream().findFirst().orElse(null);
    }

    public void insertAudit(Long tenantId, Long actorId, String action, Long entityId, LocalDateTime now) {
        jdbc.update("""
                INSERT INTO audit_logs (tenant_id, user_id, username, action, entity, entity_id,
                                        old_value, new_value, result, ip_address, user_agent, created_at)
                VALUES (?, ?, 'operador-plataforma', ?, 'COMPANY_SETTINGS', ?, NULL,
                        '{"source":"platform-panel"}', 'SUCCESS', NULL, NULL, ?)
                """, tenantId, actorId, action, entityId, now);
    }

    public void seedTenant(Long tenantId, String ownerUsername, String ownerEmail, String ownerFullName,
            String passwordHash, LocalDateTime now) {
        jdbc.update("""
                INSERT INTO roles (tenant_id, code, name, description, is_system, hierarchy_level)
                VALUES (?, 'ADMINISTRADOR', 'Administrador', 'Acceso completo al sistema', TRUE, 100),
                       (?, 'SUPERVISOR', 'Supervisor', 'Ventas, caja, inventario, reportes y clientes', TRUE, 50),
                       (?, 'VENDEDOR', 'Vendedor', 'Ventas, consulta de productos y clientes', TRUE, 10),
                       (?, 'ALMACENERO', 'Almacenero', 'Consulta de productos y gestión de inventario', TRUE, 10)
                """, tenantId, tenantId, tenantId, tenantId);

        jdbc.update("""
                INSERT INTO role_permissions (role_id, permission_id)
                SELECT target.id, rp.permission_id
                  FROM roles source
                  JOIN role_permissions rp ON rp.role_id = source.id
                  JOIN roles target ON target.tenant_id = ? AND target.code = source.code
                 WHERE source.tenant_id = 1
                """, tenantId);

        jdbc.update("""
                INSERT INTO users (tenant_id, username, email, password_hash, full_name, status,
                                   failed_attempts, locked_until, must_change_password, last_login_at,
                                   platform_operator, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', 0, NULL, TRUE, NULL, FALSE, ?, ?)
                """, tenantId, ownerUsername, ownerEmail, passwordHash, ownerFullName, now, now);
        Long ownerId = jdbc.queryForObject("SELECT id FROM users WHERE tenant_id = ? AND username = ?", Long.class,
                tenantId, ownerUsername);
        jdbc.update("""
                INSERT INTO user_roles (user_id, role_id)
                SELECT ?, id FROM roles WHERE tenant_id = ? AND code = 'ADMINISTRADOR'
                """, ownerId, tenantId);

        jdbc.update("""
                INSERT INTO payment_methods (tenant_id, code, name, type, affects_cash, requires_reference,
                                             account_holder, account_number, qr_image_url, status, sort_order, created_at, updated_at)
                VALUES (?, 'EFECTIVO', 'Efectivo', 'CASH', TRUE, FALSE, NULL, NULL, NULL, 'ACTIVE', 1, ?, ?),
                       (?, 'YAPE', 'Yape', 'DIGITAL_WALLET', FALSE, TRUE, NULL, NULL, NULL, 'ACTIVE', 2, ?, ?),
                       (?, 'PLIN', 'Plin', 'DIGITAL_WALLET', FALSE, TRUE, NULL, NULL, NULL, 'ACTIVE', 3, ?, ?),
                       (?, 'TARJETA', 'Tarjeta', 'CARD', FALSE, TRUE, NULL, NULL, NULL, 'ACTIVE', 4, ?, ?),
                       (?, 'TRANSFERENCIA', 'Transferencia', 'TRANSFER', FALSE, TRUE, NULL, NULL, NULL, 'ACTIVE', 5, ?, ?),
                       (?, 'CONTRAENTREGA', 'Contraentrega (solo Huacho)', 'CASH', FALSE, FALSE, NULL, NULL, NULL, 'ACTIVE', 6, ?, ?)
                """, tenantId, now, now, tenantId, now, now, tenantId, now, now, tenantId, now, now,
                tenantId, now, now, tenantId, now, now);

        jdbc.update("""
                INSERT INTO sequences (tenant_id, name, prefix, current_value, padding)
                VALUES (?, 'VENTA', 'V001', 0, 8), (?, 'PEDIDO', 'PED', 0, 8),
                       (?, 'DEVOLUCION', 'D001', 0, 8), (?, 'RESERVA', 'RES', 0, 8),
                       (?, 'BARCODE', '775', 0, 9)
                """, tenantId, tenantId, tenantId, tenantId, tenantId);

        KeyHolder branchKey = new GeneratedKeyHolder();
        jdbc.update(connection -> {
            var statement = connection.prepareStatement("""
                    INSERT INTO branches (tenant_id, code, name, address, phone, status, created_at, updated_at)
                    VALUES (?, 'SUC-01', 'Tienda Principal', NULL, NULL, 'ACTIVE', ?, ?)
                    """, java.sql.Statement.RETURN_GENERATED_KEYS);
            statement.setObject(1, tenantId);
            statement.setObject(2, now);
            statement.setObject(3, now);
            return statement;
        }, branchKey);
        Long branchId = branchKey.getKey().longValue();
        jdbc.update("""
                INSERT INTO warehouses (tenant_id, branch_id, code, name, status, created_at, updated_at)
                VALUES (?, ?, 'ALM-01', 'Almacén Principal', 'ACTIVE', ?, ?)
                """, tenantId, branchId, now, now);
        jdbc.update("""
                INSERT INTO cash_registers (tenant_id, branch_id, code, name, status, created_at, updated_at)
                VALUES (?, ?, 'CAJA-01', 'Caja #01', 'ACTIVE', ?, ?)
                """, tenantId, branchId, now, now);
    }

    private TenantResponse map(ResultSet rs) throws SQLException {
        return new TenantResponse(
                rs.getLong("id"), rs.getString("slug"), rs.getString("name"), rs.getString("ruc"),
                rs.getString("address"), rs.getString("phone"), rs.getString("email"),
                BusinessVertical.valueOf(rs.getString("business_vertical")), Plan.valueOf(rs.getString("plan")),
                SubscriptionStatus.valueOf(rs.getString("subscription_status")),
                rs.getObject("next_payment_due", java.time.LocalDate.class), rs.getString("owner_username"),
                rs.getInt("active_users"), rs.getObject("updated_at", LocalDateTime.class));
    }
}
