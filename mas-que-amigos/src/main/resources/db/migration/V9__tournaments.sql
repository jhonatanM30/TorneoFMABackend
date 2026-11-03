-- Torneos: formato definido antes de iniciar, equipos inscritos y partidos
-- que pertenecen a un torneo. Las tablas nuevas van en ingles
-- (docs/base-standards.md 2.5); la columna nueva en partido sigue el idioma
-- de esa tabla (espanol), por eso es id_torneo.
CREATE TABLE tournament (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    format      VARCHAR(20) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_at  DATETIME NOT NULL,
    started_at  DATETIME NULL,
    CONSTRAINT uk_tournament_name UNIQUE (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE tournament_team (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_id  BIGINT NOT NULL,
    team_id        BIGINT NOT NULL,
    enrolled_at    DATETIME NOT NULL,
    CONSTRAINT uk_tournament_team UNIQUE (tournament_id, team_id),
    CONSTRAINT fk_tournament_team_tournament FOREIGN KEY (tournament_id) REFERENCES tournament (id),
    CONSTRAINT fk_tournament_team_team FOREIGN KEY (team_id) REFERENCES equipos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Torneo protegido que agrupa los partidos creados antes de existir los
-- torneos y los que se crean sin indicar torneo (formato interno LEGACY).
INSERT INTO tournament (name, format, status, created_at)
VALUES ('Histórico', 'LEGACY', 'IN_PROGRESS', NOW());

-- La columna nace NULL para poder rellenar los partidos existentes; recien
-- despues se vuelve obligatoria.
ALTER TABLE partido ADD COLUMN id_torneo BIGINT NULL;

UPDATE partido
   SET id_torneo = (SELECT id FROM tournament WHERE format = 'LEGACY');

ALTER TABLE partido MODIFY COLUMN id_torneo BIGINT NOT NULL;
ALTER TABLE partido ADD CONSTRAINT fk_partido_torneo FOREIGN KEY (id_torneo) REFERENCES tournament (id);
CREATE INDEX idx_partido_torneo ON partido (id_torneo);
