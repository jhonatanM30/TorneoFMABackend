package com.marin.mas_que_amigos.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import lombok.*;

/**
 * Registro informativo (tipo blog) que se muestra en la pagina de Inicio.
 * Se administra desde la pagina de Configuracion (Fase 6).
 *
 * @author JhonatanAlexanderCue
 */
@Entity
@Getter
@Setter
@Table(name = "registro_informativo")
public class RegistroInformativo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro_informativo")
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Lob
    @Column(nullable = false)
    private String contenido;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion;
}
