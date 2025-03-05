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
    private int titulos;

    
    // Relación con jugadores (un equipo tiene muchos jugadores)
    @OneToMany(mappedBy = "equipo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Jugador> jugadores = new ArrayList<>();


}
