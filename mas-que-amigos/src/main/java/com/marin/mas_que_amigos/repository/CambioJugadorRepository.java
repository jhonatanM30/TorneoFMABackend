package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.CambioJugador;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Repository
public interface CambioJugadorRepository extends JpaRepository<CambioJugador, Long> {

    List<CambioJugador> findByPartidoIdOrderByMinutoAsc(Long idPartido);
}
