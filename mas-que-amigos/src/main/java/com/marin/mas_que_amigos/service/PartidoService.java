/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.mapper.PartidoMapper;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import java.time.LocalDate;
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
public class PartidoService {

    private final PartidoRepository partidoRepository;
    private final PartidoMapper partidoMapper;

    public List<PartidoDTO> listarTodos() {
        return partidoRepository.findAll()
                .stream()
                .map(partidoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<PartidoDTO> buscarPorFecha(LocalDate fecha) {
        List<PartidoDTO> partidos = partidoRepository.findByFecha(fecha)
                .stream()
                .map(partidoMapper::toDTO)
                .collect(Collectors.toList());

        if (partidos != null) {
            return partidos;
        } else {
            throw new BusinessException("No hay partidos para esta fecha");
        }

    }

    public PartidoDTO guardar(PartidoDTO partidoDTO) {

        Partido partido = partidoRepository.save(partidoMapper.toEntity(partidoDTO));
        return partidoMapper.toDTO(partido);
    }

    public void eliminar(Long id) {
        partidoRepository.deleteById(id);
    }
}
