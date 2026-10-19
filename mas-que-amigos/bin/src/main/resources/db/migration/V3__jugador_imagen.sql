-- FRONTEND_VISION.md Fase2, hallazgo bonus: foto de jugador como fondo
-- suave de su card, mismo mecanismo que el escudo de equipo (Fase1-01).
ALTER TABLE jugador ADD COLUMN imagen_url VARCHAR(255) NULL;
