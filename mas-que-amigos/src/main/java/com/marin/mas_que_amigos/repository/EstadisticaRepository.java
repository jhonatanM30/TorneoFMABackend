/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.Estadistica;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 *
 * @author JhonatanAlexanderCue
 */

@Repository
public interface EstadisticaRepository extends JpaRepository<Estadistica, Long>{

    // Hallazgo post-despliegue (fase9): antes se podian crear N registros de
    // estadistica para el mismo jugador en el mismo partido (2 filas
    // separadas contando goles distintos). Ahora EstadisticaService usa este
    // metodo para, si ya existe un registro para ese jugador+partido,
    // rechazar la creacion de uno nuevo y en su lugar gestionar todo desde
    // el mismo registro (editarlo). Tambien lo usa el nuevo endpoint
    // GET /api/estadisticas/buscar para que el frontend detecte de antemano
    // si ya existe un registro y abra el modal en modo edicion.
    @Query("SELECT e FROM Estadistica e WHERE e.jugador.id = :idJugador AND e.partido.id = :idPartido")
    Optional<Estadistica> findByJugadorIdAndPartidoId(@Param("idJugador") Long idJugador, @Param("idPartido") Long idPartido);

    // Hallazgo post-despliegue (fase9): "si se elimina un partido se deben
    // borrar sus estadisticas relacionadas" - no existe ON DELETE CASCADE en
    // la base de datos (ver V1__init_schema.sql), asi que PartidoService
    // debe borrar explicitamente las estadisticas del partido antes de
    // borrar el partido. Al no llevar @Modifying, Spring Data la resuelve
    // cargando y eliminando cada entidad (requiere que el llamador este en
    // una transaccion, ver PartidoService#eliminar).
    void deleteByPartidoId(Long idPartido);

    // Hallazgo post-despliegue (fase9): "los goles que se reflejan en
    // partido deben ser ingresados por el registro de estadisticas" - los
    // goles de un equipo en un partido pasan a calcularse sumando esta
    // columna para todos los jugadores de ese equipo en ese partido, en vez
    // de guardarse como un campo editable a mano en Partido. Ver
    // PartidoService#recalcularGoles.
    @Query("SELECT COALESCE(SUM(e.goles), 0) FROM Estadistica e WHERE e.partido.id = :idPartido AND e.jugador.equipo.id = :idEquipo")
    int sumarGolesPorEquipo(@Param("idPartido") Long idPartido, @Param("idEquipo") Long idEquipo);
}
