package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.Jugador;
import java.util.List;
import java.util.Optional;
import javax.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JugadorRepository extends JpaRepository<Jugador, Long> {

    List<Jugador> findByNombreContainingIgnoreCase(String nombre);

    boolean existsByIdAndEquipoId(Long idJugador, Long idEquipo);

    boolean existsByDorsalAndEquipoIdAndIdNot(int dorsal, Long idEquipo, Long id);

    // Row lock used by PlayerTransferService#confirmTransfer to serialize
    // confirmations of different requests of the same player.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select j from Jugador j where j.id = :id")
    Optional<Jugador> findByIdForUpdate(@Param("id") Long id);

}
