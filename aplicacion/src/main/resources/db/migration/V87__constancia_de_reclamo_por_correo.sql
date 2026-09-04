-- El D.S. 011-2011-PCM (Art. 5) obliga a entregar al consumidor la constancia de su
-- hoja de reclamación, y a responderle dentro del plazo. Hasta ahora la constancia
-- solo se mostraba en pantalla: si el consumidor cerraba la página, no le quedaba nada.
--
-- Se guarda cuándo salió cada correo, no si "se pidió enviarlo": es la diferencia entre
-- poder acreditar la entrega ante INDECOPI y suponerla. NULL significa que no se envió
-- (sin SMTP configurado, correo rechazado, o la hoja es anterior a este cambio).
ALTER TABLE complaint_book_entries
    ADD COLUMN receipt_emailed_at DATETIME(6) NULL AFTER consumer_address,
    ADD COLUMN response_emailed_at DATETIME(6) NULL AFTER receipt_emailed_at;
