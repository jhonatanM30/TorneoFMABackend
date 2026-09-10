-- Hallazgo post-despliegue (fase9): galeria de fotos administrable desde
-- Configuracion, para reemplazar las 5 imagenes fijas (assets/*.jpg) que
-- estaban codificadas directamente en index.html en la seccion "Galeria
-- del torneo". Cada fila es una foto subida para el carrusel de Inicio.
CREATE TABLE imagen_galeria (
    id_imagen_galeria  BIGINT AUTO_INCREMENT PRIMARY KEY,
    imagen_url         VARCHAR(255) NOT NULL,
    fecha_subida       DATETIME NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
