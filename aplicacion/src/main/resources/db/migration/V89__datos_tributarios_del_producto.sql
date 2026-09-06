-- Datos que SUNAT exige por línea del comprobante y que el catálogo no guardaba.
--
-- Hasta ahora el snapshot de facturación mandaba descripción, cantidad y precio, y el
-- proveedor rellenaba el resto. Eso obliga a suponer que todo está gravado al 18 % y que
-- todo se vende por unidades, que es justo el error más común al migrar un maestro: los
-- libros, algunos alimentos y varios servicios son exonerados, y ponerlos como gravados
-- cobra un IGV que no corresponde y declara mal la venta.
--
-- igv_affectation_type — catálogo 07 de SUNAT: 10 gravado, 20 exonerado, 30 inafecto,
--   40 exportación. Es del producto, no de la venta. El default 10 conserva exactamente
--   lo que se venía declarando, así que la migración no cambia el comportamiento de nadie.
--
-- unit_code — catálogo 03 (UN/ECE Rec. 20): NIU unidades, ZZ servicios, KGM kilos.
--   No se valida en local: una unidad inventada la rechaza SUNAT con el error 2936, y ese
--   rechazo quema el correlativo. Por eso el default es NIU y no vacío.
--
-- sunat_product_code — catálogo 25 (UNSPSC), 8 dígitos. Nace NULL a propósito: hoy
--   omitirlo es a lo sumo una observación, pero el 01.01.2027 el error 3496 pasa de
--   observación a rechazo. Se rellena producto a producto desde la ficha; inventar un
--   valor por defecto sería declarar una clasificación falsa para todo el catálogo.
ALTER TABLE products
    ADD COLUMN igv_affectation_type CHAR(2) NOT NULL DEFAULT '10' AFTER promo_price,
    ADD COLUMN unit_code VARCHAR(3) NOT NULL DEFAULT 'NIU' AFTER igv_affectation_type,
    ADD COLUMN sunat_product_code CHAR(8) NULL AFTER unit_code;
