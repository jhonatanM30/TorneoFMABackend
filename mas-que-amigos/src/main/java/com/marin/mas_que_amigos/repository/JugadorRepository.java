package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.Jugador;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JugadorRepository extends JpaRepository<Jugador, Long> {

    List<Jugador> findByNombreContainingIgnoreCase(String nombre);

    boolean existsByIdAndEquipoId(Long idJugador, Long idEquipo);

    boolean existsByDorsalAndEquipoIdAndIdNot(int dorsal, Long idEquipo, Long id);

}
