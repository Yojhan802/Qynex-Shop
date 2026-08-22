-- Separa la identidad de la empresa (razón social, RUC, dirección, contacto,
-- logo) de los datos operativos (moneda, IGV, envío, pie de ticket) dentro
-- de Configuración → Empresa. La identidad queda reservada al operador de
-- la plataforma — el sistema sirve a varias empresas, y ni el propio
-- Administrador del lado del cliente debería poder cambiar de quién es el
-- sistema. Ver docs/04-reglas-negocio.md RN-26.
--
-- Se otorga solo a ADMINISTRADOR en la semilla; la convención operativa es
-- que ese rol se reserva para la cuenta que usa el propio operador de la
-- plataforma, nunca se asigna al personal del cliente (ver RN-25 y su
-- techo de asignación).

INSERT INTO permissions (code, module, description) VALUES
    ('CONFIGURACION_IDENTIDAD_EDITAR', 'CONFIGURACION', 'Editar razón social, RUC, dirección, contacto y logo');

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p
WHERE r.code = 'ADMINISTRADOR' AND p.code = 'CONFIGURACION_IDENTIDAD_EDITAR';
