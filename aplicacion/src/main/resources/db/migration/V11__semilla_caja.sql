-- Caja física única de la Fase 1.

INSERT INTO cash_registers (branch_id, code, name, status, created_at, updated_at)
SELECT id, 'CAJA-01', 'Caja #01', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)
FROM branches WHERE code = 'SUC-01';
