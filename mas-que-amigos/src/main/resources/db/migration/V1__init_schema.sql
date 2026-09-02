-- V1__init_schema.sql
-- Esquema inicial MySQL para MAS-QUE-AMIGOS, equivalente a las entidades JPA
-- actuales (Equipo, Jugador, Partido, Alineacion, Estadistica).

CREATE TABLE equipos (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre              VARCHAR(100) NOT NULL,
    director_tecnico    VARCHAR(100) NOT NULL,
    imagen_url          VARCHAR(255),
    titulos             INT,
    tipo_clasificacion  VARCHAR(255),
    CONSTRAINT uk_equipos_nombre UNIQUE (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE jugador (
    id_jugador  BIGINT AUTO_INCREMENT PRIMARY KEY,
    nombre      VARCHAR(100) NOT NULL,
    posicion    VARCHAR(255) NOT NULL,
    edad        INT NOT NULL,
    dorsal      INT NOT NULL,
    id_equipo   BIGINT NOT NULL,
    CONSTRAINT fk_jugador_equipo FOREIGN KEY (id_equipo) REFERENCES equipos (id),
    CONSTRAINT uk_jugador_equipo_dorsal UNIQUE (id_equipo, dorsal)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE partido (
    id_partido        BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipo_local      BIGINT NOT NULL,
    equipo_visitante  BIGINT NOT NULL,
    fecha             DATE NOT NULL,
    hora              TIME NOT NULL,
    goles_local       INT NOT NULL,
    goles_visitante   INT NOT NULL,
    fase              VARCHAR(255),
    CONSTRAINT fk_partido_equipo_local FOREIGN KEY (equipo_local) REFERENCES equipos (id),
    CONSTRAINT fk_partido_equipo_visitante FOREIGN KEY (equipo_visitante) REFERENCES equipos (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE alineacion (
    id_partido  BIGINT NOT NULL,
    id_jugador  BIGINT NOT NULL,
    titular     BOOLEAN NOT NULL,
    PRIMARY KEY (id_partido, id_jugador),
    CONSTRAINT fk_alineacion_partido FOREIGN KEY (id_partido) REFERENCES partido (id_partido),
    CONSTRAINT fk_alineacion_jugador FOREIGN KEY (id_jugador) REFERENCES jugador (id_jugador)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE estadistica (
    id_estadistica      BIGINT AUTO_INCREMENT PRIMARY KEY,
    id_jugador          BIGINT NOT NULL,
    id_partido          BIGINT NOT NULL,
    goles               INT NOT NULL DEFAULT 0,
    tarjetas_amarillas  INT NOT NULL DEFAULT 0,
    tarjetas_rojas      INT NOT NULL DEFAULT 0,
    asistencias         INT NOT NULL DEFAULT 0,
    CONSTRAINT fk_estadistica_jugador FOREIGN KEY (id_jugador) REFERENCES jugador (id_jugador),
    CONSTRAINT fk_estadistica_partido FOREIGN KEY (id_partido) REFERENCES partido (id_partido)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
