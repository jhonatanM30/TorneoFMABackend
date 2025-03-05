/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.repository;

import com.marin.mas_que_amigos.model.Estadistica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 *
 * @author JhonatanAlexanderCue
 */

@Repository
public interface EstadisticaRepository extends JpaRepository<Estadistica, Long>{
    
}
