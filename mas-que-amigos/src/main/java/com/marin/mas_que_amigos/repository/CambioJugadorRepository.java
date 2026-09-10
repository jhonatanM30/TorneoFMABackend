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

    // Hallazgo post-despliegue (fase9): "si se elimina un partido se deben
    // borrar sus estadisticas relacionadas" - se extiende tambien a los
    // cambios de jugador (sustituciones), que igual que Estadistica y
    // Alineacion no tienen ON DELETE CASCADE en la base de datos. Ver
    // PartidoService#eliminar.
    void deleteByPartidoId(Long idPartido);
}
