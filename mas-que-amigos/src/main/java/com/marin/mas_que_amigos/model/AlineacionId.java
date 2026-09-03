package com.marin.mas_que_amigos.model;

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AlineacionId implements Serializable {

    @Column(name = "id_partido")
    private Long idPartido;

    @Column(name = "id_jugador")
    private Long idJugador;
}
