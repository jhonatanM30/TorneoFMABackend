-- FRONTEND_VISION.md Fase3, hallazgos 9 y 10: estado de partido (para
-- saber si esta en curso) y registro con minuto de los cambios de
-- jugador (sustituciones), que ademas sirve como historial consultable
-- del partido (Fase3-10).
ALTER TABLE partido ADD COLUMN estado VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADO';

-- Permite anotar en que minuto ocurrio un gol/tarjeta/asistencia
-- registrado durante el partido (columna opcional: los registros ya
-- existentes, creados antes de este hallazgo, quedan con minuto NULL).
ALTER TABLE estadistica ADD COLUMN minuto INT NULL;

CREATE TABLE cambio_jugador (
    id_cambio_jugador  BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_partido         BIGINT NOT NULL,
    id_jugador_sale    BIGINT NOT NULL,
    id_jugador_entra   BIGINT NOT NULL,
    minuto             INT NOT NULL,
    fecha_registro     DATETIME NOT NULL,
    CONSTRAINT fk_cambio_jugador_partido FOREIGN KEY (id_partido) REFERENCES partido (id_partido),
    CONSTRAINT fk_cambio_jugador_sale FOREIGN KEY (id_jugador_sale) REFERENCES jugador (id_jugador),
    CONSTRAINT fk_cambio_jugador_entra FOREIGN KEY (id_jugador_entra) REFERENCES jugador (id_jugador)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
