package com.marin.mas_que_amigos.repository;


import com.marin.mas_que_amigos.model.Equipo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipoRepository extends JpaRepository<Equipo, Long> {
    
    Equipo findByNombreIgnoreCase(String nombre);
    
    boolean existsByNombre(String nombre);

    // FRONTEND_VISION.md Fase1: el frontend necesitaba busqueda por
    // coincidencia parcial (ej. escribir "Rea" y encontrar "Real Madrid"),
    // cosa que findByNombreIgnoreCase (match exacto) no soporta. Se agrega
    // este metodo nuevo en vez de modificar el existente, para no romper a
    // otros consumidores de GET /api/equipos/{nombre}.
    List<Equipo> findByNombreContainingIgnoreCase(String nombre);

}
