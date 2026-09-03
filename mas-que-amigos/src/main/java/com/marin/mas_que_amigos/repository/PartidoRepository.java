/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.Partido;
import java.time.LocalDate;
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
public interface PartidoRepository extends JpaRepository<Partido, Long> {

    @Query("SELECT p FROM Partido p "
            + "JOIN Equipo eLocal ON p.equipoLocal.id = eLocal.id "
            + "JOIN Equipo eVisitante ON p.equipoVisitante.id = eVisitante.id "
            + "WHERE eLocal.nombre = :nombre OR eVisitante.nombre = :nombre "
            + "ORDER BY p.fecha ASC, p.hora ASC")
    List<Partido> findPartidosByEquipo(@Param("nombre") String nombre);

    // FRONTEND_VISION.md Fase3 ("todas las funcionalidades de los filtros
    // deben funcionar correctamente"): findPartidosByEquipo exige nombre
    // EXACTO (ni siquiera ignora mayusculas/minusculas), igual que el bug ya
    // corregido en Equipos - el buscador de Partidos por equipo tenia el
    // mismo problema. Se agrega, sin tocar la anterior, una version por
    // coincidencia parcial insensible a mayusculas.
    @Query("SELECT p FROM Partido p "
            + "JOIN Equipo eLocal ON p.equipoLocal.id = eLocal.id "
            + "JOIN Equipo eVisitante ON p.equipoVisitante.id = eVisitante.id "
            + "WHERE LOWER(eLocal.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) "
            + "OR LOWER(eVisitante.nombre) LIKE LOWER(CONCAT('%', :nombre, '%')) "
            + "ORDER BY p.fecha ASC, p.hora ASC")
    List<Partido> findPartidosByEquipoNombreParcial(@Param("nombre") String nombre);

    @Query("SELECT COUNT(p) > 0 FROM Partido p WHERE p.fecha = :fecha AND (p.equipoLocal.id = :equipoLocal OR p.equipoVisitante.id = :equipoLocal OR p.equipoLocal.id = :equipoVisitante OR p.equipoVisitante.id = :equipoVisitante)")
    boolean existePartidoEnFechaParaEquipos(@Param("fecha") LocalDate fecha, @Param("equipoLocal") Long equipoLocal, @Param("equipoVisitante") Long equipoVisitante);

}
