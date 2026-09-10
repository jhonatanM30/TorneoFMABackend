package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.ImagenGaleria;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * @author JhonatanAlexanderCue
 */
@Repository
public interface ImagenGaleriaRepository extends JpaRepository<ImagenGaleria, Long> {

    List<ImagenGaleria> findAllByOrderByFechaSubidaDesc();
}
