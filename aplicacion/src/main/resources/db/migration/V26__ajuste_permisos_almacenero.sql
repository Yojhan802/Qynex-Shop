-- El rol ALMACENERO había acumulado PRODUCTOS_CREAR/EDITAR/ELIMINAR en algún
-- momento posterior a la semilla original (V2, que nunca se los dio) — un
-- almacenero gestiona stock, variantes y códigos de barras, no el catálogo
-- de productos en sí. PRODUCTOS_ELIMINAR en particular es más sensible que
-- lo que incluso SUPERVISOR tiene: eliminar un producto es una decisión de
-- catálogo, no de almacén.

DELETE rp FROM role_permissions rp
JOIN roles r ON r.id = rp.role_id
JOIN permissions p ON p.id = rp.permission_id
WHERE r.code = 'ALMACENERO' AND p.code IN ('PRODUCTOS_CREAR', 'PRODUCTOS_EDITAR', 'PRODUCTOS_ELIMINAR');
