-- El panel ya muestra el plazo, pero solo lo ve quien entra a esa pantalla: si nadie la
-- abre en dos semanas, el plazo del D.S. 011-2011-PCM se pasa igual. El aviso por correo
-- va a buscar a la empresa en vez de esperar a que mire.
--
-- Guarda cuándo se avisó por última vez, no un simple "ya se avisó": eso permite repetir
-- el recordatorio mientras la hoja siga sin responder, sin mandar uno cada día.
-- NULL = nunca se avisó de esta hoja.
ALTER TABLE complaint_book_entries
    ADD COLUMN deadline_reminder_at DATETIME(6) NULL AFTER response_emailed_at;
