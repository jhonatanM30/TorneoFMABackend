CREATE TABLE registro_informativo (
    id_registro_informativo BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo              VARCHAR(150) NOT NULL,
    contenido           TEXT NOT NULL,
    fecha_publicacion   DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
