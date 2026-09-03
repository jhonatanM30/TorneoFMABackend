package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.RegistroInformativo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Repository
public interface RegistroInformativoRepository extends JpaRepository<RegistroInformativo, Long> {

    List<RegistroInformativo> findAllByOrderByFechaPublicacionDesc();
}
