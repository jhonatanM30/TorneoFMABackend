/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.exception;

/**
 *
 * @author JhonatanAlexanderCue
 */
public class EquipoNotFoundException extends RuntimeException{
    
    public EquipoNotFoundException(String mensaje) {
        super(mensaje);
    }
    
}
