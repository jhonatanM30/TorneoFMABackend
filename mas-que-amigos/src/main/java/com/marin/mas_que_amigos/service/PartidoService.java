/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.mapper.PartidoMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Service
@RequiredArgsConstructor
public class PartidoService {

    private final PartidoRepository partidoRepository;
    private final EquipoRepository equipoRepository;

    @Autowired
    private ValidationCommonService validacionService;

    private final PartidoMapper mapper;

    public List<PartidoDTO> listarPartidos() {
        return partidoRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<PartidoDTO> buscarPartidoPorEquipo(String nombre) {
        List<PartidoDTO> partidos = partidoRepository.findPartidosByEquipo(nombre)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());

        if (partidos != null) {
            return partidos;
        } else {
            throw new BusinessException("No hay partidos para esta fecha");
        }

    }

    public PartidoDTO guardar(PartidoDTO partidoDTO) {

        validacionService.validarEquipo(partidoDTO.getIdEquipoLocal());
        validacionService.validarEquipo(partidoDTO.getIdEquipoVisitante());

        if (partidoDTO.getIdEquipoLocal().equals(partidoDTO.getIdEquipoVisitante())) {
            throw new BusinessException("Los equipos seleccionados son los mismos. Un equipo no puede jugar contra sí mismo.");
        }

        if (partidoRepository.existePartidoEnFechaParaEquipos(partidoDTO.getFecha(), partidoDTO.getIdEquipoLocal(), partidoDTO.getIdEquipoVisitante())) {
            throw new BusinessException("Ya existe un partido programado el dia " + partidoDTO.getFecha() + " para uno de estos equipos");
        }

        // Se resuelven las entidades Equipo reales (ya persistidas) en vez de
        // dejar que el mapper construya Equipos "shell" con solo el id: así el
        // partido guardado queda con ambas asociaciones completamente
        // hidratadas y la respuesta incluye los datos reales de cada equipo.
        Equipo equipoLocal = equipoRepository.findById(partidoDTO.getIdEquipoLocal())
                .orElseThrow(() -> new BusinessException("Fuera de juego! No se ha seleccionado un o unos equipos existentes."));
        Equipo equipoVisitante = equipoRepository.findById(partidoDTO.getIdEquipoVisitante())
                .orElseThrow(() -> new BusinessException("Fuera de juego! No se ha seleccionado un o unos equipos existentes."));

        Partido partido = mapper.toEntity(partidoDTO);
        partido.setEquipoLocal(equipoLocal);
        partido.setEquipoVisitante(equipoVisitante);

        Partido guardado = partidoRepository.save(partido);

        PartidoDTO respuesta = mapper.toDTO(guardado);
        respuesta.setMensaje("Equipos, el partido ya fue programado");
        return respuesta;
    }

    public void eliminar(Long id) {
        partidoRepository.deleteById(id);
    }
}
