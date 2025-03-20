/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Service
public class ValidationCommonService {
    
    @Autowired
    private EquipoRepository equipoRepository;
    

   public void validarEquipo(Long idEquipo) {
        if (idEquipo == null || !equipoRepository.existsById(idEquipo)) {           
            throw new BusinessException("Fuera de juego! No se ha seleccionado un o unos equipos existentes.");
        }
    }

    
}
