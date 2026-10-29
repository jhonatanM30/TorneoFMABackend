-- Cuentas de usuario para el login (roles ADMIN y DT). Tabla nueva, por eso va
-- en ingles (docs/base-standards.md 2.5).
--   username   siempre en minusculas (la app lo normaliza); unico.
--   team_id    equipo del DT, opcional. UNIQUE: un equipo tiene como maximo una
--              cuenta (y, al ser una sola columna, una cuenta tiene como maximo
--              un equipo). MySQL admite varios NULL en un indice unico, asi que
--              muchos DT pueden estar sin equipo.
CREATE TABLE app_user (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    username       VARCHAR(50)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    role           VARCHAR(20)  NOT NULL,
    team_id        BIGINT NULL,
    created_at     DATETIME NOT NULL,
    CONSTRAINT uk_app_user_username UNIQUE (username),
    CONSTRAINT uk_app_user_team UNIQUE (team_id),
    CONSTRAINT fk_app_user_team FOREIGN KEY (team_id) REFERENCES equipos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
