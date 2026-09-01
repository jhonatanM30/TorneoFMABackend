/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.Alineacion;
import com.marin.mas_que_amigos.model.AlineacionId;
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
public interface AlineacionRepository extends JpaRepository<Alineacion, AlineacionId> {

    @Query("SELECT a FROM Alineacion a WHERE a.id.idPartido = :idPartido")
    List<Alineacion> findByIdPartido(@Param("idPartido") Long idPartido);

    @Query("SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END FROM Alineacion a WHERE a.id.idPartido = :idPartido AND a.id.idJugador = :idJugador")
    boolean existsByIdPartidoAndIdJugador(@Param("idPartido") Long idPartido, @Param("idJugador") Long idJugador);

    @Query("SELECT COUNT(a) FROM Alineacion a WHERE a.id.idPartido = :idPartido AND a.jugador.equipo.id = :idEquipo AND a.titular = true")
    long contarTitularesPorEquipo(@Param("idPartido") Long idPartido, @Param("idEquipo") Long idEquipo);

}
