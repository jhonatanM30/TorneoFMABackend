package com.marin.mas_que_amigos.repository;


import com.marin.mas_que_amigos.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {
    
    Equipo findByNombre(String nombre);

    
}
