/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.exception;

import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.dto.ErrorResponseDTO;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 *
 * @author JhonatanAlexanderCue
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<EquipoDTO> handleEquipoNotFound(NotFoundException ex) {        
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new EquipoDTO("Sin Registros", ex.getMessage()));
    }
    
    @ExceptionHandler(JugadorNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleJugadorNotFound(NotFoundException ex) {
        Map<String, String> response = new HashMap<>();
        response.put("error", ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }
    
    
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponseDTO> handleBusinessException(BusinessException ex) {
      
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponseDTO("Error", ex.getMessage()));
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
         return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponseDTO("Error", ex.getMessage()));
    }
    
     @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errores.put(error.getField(), error.getDefaultMessage()));

        return new ResponseEntity<>(errores, HttpStatus.BAD_REQUEST);
    }
}
