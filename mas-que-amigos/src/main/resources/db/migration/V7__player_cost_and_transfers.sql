-- Costo del jugador (valor monetario, sin divisa) y traspasos entre equipos.
-- La columna nueva en jugador sigue el idioma de esa tabla (espanol); la
-- tabla nueva player_transfer va en ingles (docs/base-standards.md 2.5).
-- Los jugadores existentes quedan con costo 0 (DEFAULT), sin backfill.
ALTER TABLE jugador ADD COLUMN costo DECIMAL(12,2) NOT NULL DEFAULT 0;

CREATE TABLE player_transfer (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    player_id             BIGINT NOT NULL,
    origin_team_id        BIGINT NOT NULL,
    destination_team_id   BIGINT NOT NULL,
    cost                  DECIMAL(12,2) NOT NULL,
    status                VARCHAR(20) NOT NULL,
    destination_dorsal    INT NULL,
    requested_at          DATETIME NOT NULL,
    resolved_at           DATETIME NULL,
    CONSTRAINT fk_player_transfer_player FOREIGN KEY (player_id) REFERENCES jugador (id_jugador),
    CONSTRAINT fk_player_transfer_origin FOREIGN KEY (origin_team_id) REFERENCES equipos (id),
    CONSTRAINT fk_player_transfer_destination FOREIGN KEY (destination_team_id) REFERENCES equipos (id),
    INDEX idx_player_transfer_player_status (player_id, status),
    INDEX idx_player_transfer_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
