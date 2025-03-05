package com.marin.mas_que_amigos.model;

import java.io.Serializable;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Getter
@Setter
@Entity
@Table(name = "jugador")
public class Jugador implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_jugador")
    private Long id;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Column(name = "posicion", nullable = false)
    @Enumerated(EnumType.STRING)  // Para que guarde la posición como un String
    private Posicion posicion;

    @Column(name = "edad", nullable = false)
    private int edad;

    @Column(name = "dorsal", nullable = false)
    private int dorsal;
      

    @ManyToOne
    @JoinColumn(name = "id_equipo", nullable = false) // 🔹 Esto asegura que el ID del equipo se guarde en la BD
    private Equipo equipo;  

    // Enumeración de las posibles posiciones de los jugadores
    public enum Posicion {
        PORTERO,
        DEFENSA,
        MEDIOCAMPISTA,
        DELANTERO
    }
}
