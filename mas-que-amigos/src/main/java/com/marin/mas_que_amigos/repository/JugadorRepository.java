package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.Jugador;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface JugadorRepository extends JpaRepository<Jugador, Long> {
    
    Optional<Jugador> findByDorsal(int dorsal);
    
    boolean existsByDorsalAndEquipoId(int dorsal, Long equipoId);
}
