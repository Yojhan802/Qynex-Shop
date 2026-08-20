-- Sucursal y almacén únicos de la Fase 1 (docs/01-requisitos.md S-04).

INSERT INTO branches (code, name, address, phone, status, created_at, updated_at) VALUES
    ('SUC-01', 'Tienda Principal', NULL, NULL, 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO warehouses (branch_id, code, name, status, created_at, updated_at)
SELECT id, 'ALM-01', 'Almacén Principal', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM branches WHERE code = 'SUC-01';
