/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.Alineacion;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Repository
public interface AlineacionRepository extends JpaRepository<Alineacion, Integer> {

    List<Alineacion> findByIdPartido(Long idPartido);

    public boolean existsByIdPartidoAndIdJugador(Long idPartido, Long idJugador);

    @Query("SELECT COUNT(a) FROM Alineacion a WHERE a.partido.id = :idPartido AND a.jugador.equipo.id = :idEquipo AND a.titular = true")
    long contarTitularesPorEquipo(@Param("idPartido") Long idPartido, @Param("idEquipo") Long idEquipo);

}
