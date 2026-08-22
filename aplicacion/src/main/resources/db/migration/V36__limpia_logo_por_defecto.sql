-- Complementa V35: al corregir el nombre semilla a "Qynex", una instalación
-- que ya tenía un logo subido (de cuando la semilla era "Freestyle Perú")
-- se quedaba con el nombre nuevo pero el logo viejo. logo_url NULL hace que
-- login/tienda pública caigan de vuelta a los assets estáticos por defecto
-- (front/assets/brand/, ya actualizados a Qynex) hasta que el cliente real
-- suba el suyo desde Configuración (RN-26).
--
-- El WHERE evita tocar una instalación donde ya se subió un logo real de
-- cliente después de que el nombre pasó a ser "Qynex" — solo limpia el
-- residuo específico de la migración de marca.
UPDATE company_settings
SET logo_url = NULL
WHERE id = 1 AND name = 'Qynex';
