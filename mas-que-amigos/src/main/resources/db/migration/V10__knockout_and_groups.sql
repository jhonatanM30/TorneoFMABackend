-- Eliminatoria y fase de grupos: penales, grupos, clasificados por grupo y
-- cruces de las llaves. Las tablas nuevas van en ingles (docs/base-standards.md
-- 2.5); las columnas nuevas en partido siguen el idioma de esa tabla (espanol)
-- y las de tournament_team y tournament siguen el ingles de las suyas.
-- Ningun dato existente cambia: todo lo agregado nace nulo o vacio.

-- Penales de un partido que exige ganador (llaves y repechaje).
ALTER TABLE partido
    ADD COLUMN penales_local INT NULL,
    ADD COLUMN penales_visitante INT NULL;

-- Equipos que clasifican por grupo (1 o 2), definido al formar los grupos.
ALTER TABLE tournament ADD COLUMN qualifiers_per_group INT NULL;

CREATE TABLE tournament_group (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_id  BIGINT NOT NULL,
    name           VARCHAR(10) NOT NULL,
    CONSTRAINT uk_tournament_group UNIQUE (tournament_id, name),
    CONSTRAINT fk_tournament_group_tournament FOREIGN KEY (tournament_id) REFERENCES tournament (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE tournament_team
    ADD COLUMN group_id BIGINT NULL,
    ADD CONSTRAINT fk_tournament_team_group FOREIGN KEY (group_id) REFERENCES tournament_group (id);

-- Cruces de las llaves. El partido se crea cuando se conocen los dos equipos,
-- por eso home/away/match son nulos al principio. La fecha y la hora de cada
-- cruce se fijan al generar las llaves, segun su ronda.
CREATE TABLE bracket_tie (
    id                   BIGINT AUTO_INCREMENT PRIMARY KEY,
    tournament_id        BIGINT NOT NULL,
    kind                 VARCHAR(20) NOT NULL,
    round_number         INT NOT NULL,
    position_in_round    INT NOT NULL,
    home_team_id         BIGINT NULL,
    away_team_id         BIGINT NULL,
    winner_team_id       BIGINT NULL,
    match_id             BIGINT NULL,
    winner_next_tie_id   BIGINT NULL,
    winner_next_is_home  BOOLEAN NULL,
    loser_next_tie_id    BIGINT NULL,
    loser_next_is_home   BOOLEAN NULL,
    scheduled_date       DATE NOT NULL,
    scheduled_time       TIME NOT NULL,
    CONSTRAINT uk_bracket_tie UNIQUE (tournament_id, kind, round_number, position_in_round),
    CONSTRAINT fk_bracket_tie_tournament FOREIGN KEY (tournament_id) REFERENCES tournament (id),
    CONSTRAINT fk_bracket_tie_home FOREIGN KEY (home_team_id) REFERENCES equipos (id),
    CONSTRAINT fk_bracket_tie_away FOREIGN KEY (away_team_id) REFERENCES equipos (id),
    CONSTRAINT fk_bracket_tie_winner FOREIGN KEY (winner_team_id) REFERENCES equipos (id),
    CONSTRAINT fk_bracket_tie_match FOREIGN KEY (match_id) REFERENCES partido (id_partido),
    CONSTRAINT fk_bracket_tie_winner_next FOREIGN KEY (winner_next_tie_id) REFERENCES bracket_tie (id),
    CONSTRAINT fk_bracket_tie_loser_next FOREIGN KEY (loser_next_tie_id) REFERENCES bracket_tie (id),
    INDEX idx_bracket_tie_match (match_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
