-- Datos de demostración del catálogo (docs/01-requisitos.md §74).

INSERT INTO categories (name, slug, status, created_at, updated_at) VALUES
    ('Polos',       'polos',       'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Pantalones',  'pantalones',  'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Camisas',     'camisas',     'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Casacas',     'casacas',     'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Poleras',     'poleras',     'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Shorts',      'shorts',      'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Accesorios',  'accesorios',  'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO colors (name, hex_code, status, created_at, updated_at) VALUES
    ('Negro',  '#000000', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Blanco', '#FFFFFF', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Azul',   '#1669F3', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Rojo',   '#DC2626', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Gris',   '#667085', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('Beige',  '#D8CAB8', 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));

INSERT INTO sizes (name, sort_order, status, created_at, updated_at) VALUES
    ('XS',  1, 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('S',   2, 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('M',   3, 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('L',   4, 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('XL',  5, 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6)),
    ('XXL', 6, 'ACTIVE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6));
