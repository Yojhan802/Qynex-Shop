-- Una promoción (ej. Black Friday) puede además reflejarse sola en la tienda
-- online, sin que un cajero tenga que elegirla — pero por defecto sigue
-- siendo solo para POS, igual que hasta ahora (RN-28, docs/03 §18).
ALTER TABLE promotions ADD COLUMN visible_online TINYINT(1) NOT NULL DEFAULT 0 AFTER status;
