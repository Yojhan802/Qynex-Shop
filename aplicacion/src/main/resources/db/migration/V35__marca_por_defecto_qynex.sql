-- El sistema es un producto propio (Qynex) que se vende a distintas tiendas
-- de ropa como despliegue independiente — "Freestyle Perú" (V16) era solo el
-- nombre del primer cliente piloto, sembrado por accidente como si fuera la
-- marca del producto. Corrige el nombre por defecto a "Qynex" para que toda
-- instalación nueva arranque con la marca del producto, no con la de un
-- cliente específico. El logo por defecto vive en front/assets/brand/ (no en
-- esta tabla) — logo_url se queda NULL hasta que el cliente real suba el
-- suyo desde Configuración (RN-26).
--
-- El WHERE evita tocar una instalación donde el cliente ya personalizó su
-- razón social — esta migración solo corrige el valor semilla sin modificar.
UPDATE company_settings
SET name = 'Qynex'
WHERE id = 1 AND name = 'Freestyle Perú';
