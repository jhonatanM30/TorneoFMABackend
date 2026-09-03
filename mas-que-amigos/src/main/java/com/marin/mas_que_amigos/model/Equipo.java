package com.marin.mas_que_amigos.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "equipos")  // Nombre de la tabla en la BD
public class Equipo implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "director_tecnico", nullable = false, length = 100)
    private String directorTecnico;

    @Column(name = "imagen_url")
    private String imagenUrl;

    @Column(name = "titulos")
    private Integer titulos;
    
    @Column(name = "tipo_clasificacion")
    @Enumerated(EnumType.STRING)  // Igual que Jugador.Posicion: se guarda como String legible en BD
    private TipoClasificacion tipoClasificacion;

    
    // Relación con jugadores (un equipo tiene muchos jugadores)
    @OneToMany(mappedBy = "equipo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Jugador> jugadores = new ArrayList<>();

    // FRONTEND_VISION.md Fase1: "Tipo de clasificacion deberia ser unos
    // valores que lleguen por parametria, inicialmente esten Eliminatoria,
    // Repechaje" -> se resuelve con un enum estricto (antes era String
    // libre). Nota de despliegue: cualquier equipo de prueba con un valor
    // de tipoClasificacion distinto a estos dos debe actualizarse/eliminarse
    // en la base de datos antes de levantar la app con este cambio, porque
    // @Enumerated(EnumType.STRING) fallará al leer un valor que no matchee.
    public enum TipoClasificacion {
        ELIMINATORIA,
        REPECHAJE
    }
}
