package com.marin.mas_que_amigos.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Hallazgo post-despliegue (fase9): "Inicio" mostraba una galeria de 5
 * imagenes fijas (assets/*.jpg codificadas directamente en index.html, sin
 * relacion con la base de datos). El usuario pidio poder subir fotos desde
 * Configuracion y que aparezcan alli, en un carrusel. Cada fila de esta
 * tabla es una foto subida para ese carrusel; se administran desde
 * Configuracion (subir/listar/eliminar) igual que el escudo de un Equipo
 * (ver ImagenGaleriaService, calcado de EquipoService#actualizarImagenEquipo).
 *
 * @author JhonatanAlexanderCue
 */
@Entity
@Getter
@Setter
@Table(name = "imagen_galeria")
public class ImagenGaleria implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_imagen_galeria")
    private Long id;

    @Column(name = "imagen_url", nullable = false)
    private String imagenUrl;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;
}
