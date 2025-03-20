/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

/**
 *
 * @author JhonatanAlexanderCue
 */

@Getter
@Setter
@AllArgsConstructor // Constructor con todos los campos
public class ErrorResponseDTO {
     
     private String indicadorRespuesta;
     private String mensaje;
}
