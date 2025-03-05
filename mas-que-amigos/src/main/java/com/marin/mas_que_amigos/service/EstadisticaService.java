/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.EstadisticaDTO;
import com.marin.mas_que_amigos.mapper.EstadisticaMapper;
import com.marin.mas_que_amigos.model.Estadistica;
import com.marin.mas_que_amigos.repository.EstadisticaRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Service
@RequiredArgsConstructor
public class EstadisticaService {

    private final EstadisticaRepository estadisticaRepository;
    private final EstadisticaMapper estadisticaMapper;

    public List<EstadisticaDTO> listarTodas() {
        return estadisticaRepository.findAll()
                .stream()
                .map(estadisticaMapper::toDTO)
                .collect(Collectors.toList());
    }

    public EstadisticaDTO buscarPorId(Long id) {
        return estadisticaRepository.findById(id)
                .map(estadisticaMapper::toDTO)
                .orElseThrow(() -> new RuntimeException("Estadística no encontrada"));
    }

    public EstadisticaDTO guardar(EstadisticaDTO estadisticaDTO) {
        Estadistica estadistica = estadisticaMapper.toEntity(estadisticaDTO);
        estadistica = estadisticaRepository.save(estadistica);
        return estadisticaMapper.toDTO(estadistica);
    }

    public void eliminar(Long id) {
        estadisticaRepository.deleteById(id);
    }
}
