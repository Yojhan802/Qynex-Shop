-- Información de producto para la tienda online: material, calce y una
-- imagen de guía de tallas — se llenan al crear/editar el producto en el
-- admin y se muestran en la ficha pública (docs/03-modelo-datos.md §12).

ALTER TABLE products
    ADD COLUMN material VARCHAR(150) NULL,
    ADD COLUMN fit VARCHAR(100) NULL,
    ADD COLUMN size_guide_image_url VARCHAR(255) NULL;
