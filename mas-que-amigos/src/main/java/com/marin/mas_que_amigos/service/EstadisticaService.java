/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.EstadisticaDTO;
import com.marin.mas_que_amigos.dto.GoleadorDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.NotFoundException;
import com.marin.mas_que_amigos.mapper.EstadisticaMapper;
import com.marin.mas_que_amigos.model.Estadistica;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.repository.EstadisticaRepository;
import com.marin.mas_que_amigos.repository.EstadisticaRepository.GoleadorProyeccion;
import com.marin.mas_que_amigos.repository.JugadorRepository;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * @author JhonatanAlexanderCue
 */
@Service
@RequiredArgsConstructor
public class EstadisticaService {

    private final EstadisticaRepository estadisticaRepository;
    private final EstadisticaMapper estadisticaMapper;
    private final PartidoRepository partidoRepository;
    private final JugadorRepository jugadorRepository;
    private final PartidoService partidoService;

    public List<EstadisticaDTO> listarTodas() {
        return estadisticaRepository.findAll()
                .stream()
                .map(estadisticaMapper::toDTO)
                .collect(Collectors.toList());
    }

    public EstadisticaDTO buscarPorId(Long id) {
        return estadisticaRepository.findById(id)
                .map(estadisticaMapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Estadística no encontrada"));
    }

    // Hallazgo post-despliegue (fase9): "una vez creado un registro de
    // estadisticas para un jugador en un partido, las siguientes para el
    // mismo jugador y el mismo partido se puede gestionar desde el mismo
    // registro". El frontend deberia llamar primero a este endpoint antes
    // de abrir el modal de creacion, para decidir si crea uno nuevo o edita
    // el existente; se expone tambien como ultima linea de defensa aqui
    // mismo en guardar().
    public Optional<EstadisticaDTO> buscarExistente(Long idJugador, Long idPartido) {
        return estadisticaRepository.findByJugadorIdAndPartidoId(idJugador, idPartido)
                .map(estadisticaMapper::toDTO);
    }

    // Goleador del torneo: no existe entidad Torneo, asi que se agrega sobre
    // todos los partidos existentes (ver proposal.md/design.md del change
    // add-top-scorer-endpoint). El repositorio ya devuelve las filas
    // ordenadas por total de goles descendente, asi que la primera fila es
    // el maximo; si mas de un jugador comparte ese maximo, se devuelven
    // todos los empatados en vez de elegir uno arbitrariamente.
    public List<GoleadorDTO> obtenerGoleadores() {
        List<GoleadorProyeccion> filas = estadisticaRepository.obtenerTotalGolesPorJugador();

        if (filas.isEmpty()) {
            return Collections.emptyList();
        }

        long maximoGoles = filas.get(0).getTotalGoles();

        return filas.stream()
                .filter(fila -> fila.getTotalGoles() == maximoGoles)
                .map(estadisticaMapper::toGoleadorDTO)
                .collect(Collectors.toList());
    }

    // Hallazgo post-despliegue (fase9): "al registrar una estadistica se
    // deberia ver reflejada en el partido" y "no debe quedar 2 filas con
    // estadisticas del mismo jugador para el mismo partido". Ademas, por
    // decision de diseño (respuesta a pregunta de aclaracion), solo se
    // permite registrar estadisticas cuando el partido esta EN_CURSO o
    // FINALIZADO (no PROGRAMADO): no tiene sentido anotarle goles a un
    // jugador de un partido que ni siquiera ha arrancado.
    public EstadisticaDTO guardar(EstadisticaDTO estadisticaDTO) {

        Partido partido = partidoRepository.findById(estadisticaDTO.getIdPartido())
                .orElseThrow(() -> new BusinessException("Fuera de juego! El partido indicado no existe."));

        if (partido.getEstado() == Partido.Estado.PROGRAMADO) {
            throw new BusinessException("No se pueden registrar estadísticas de un partido que aún no ha iniciado.");
        }

        Jugador jugador = jugadorRepository.findById(estadisticaDTO.getIdJugador())
                .orElseThrow(() -> new BusinessException("El jugador con ID " + estadisticaDTO.getIdJugador() + " no existe."));

        if (estadisticaRepository.findByJugadorIdAndPartidoId(jugador.getId(), partido.getId()).isPresent()) {
            throw new BusinessException("Ya existe un registro de estadística para este jugador en este partido: edítalo en vez de crear uno nuevo.");
        }

        Estadistica estadistica = estadisticaMapper.toEntity(estadisticaDTO);
        estadistica.setJugador(jugador);
        estadistica.setPartido(partido);

        Estadistica guardada = estadisticaRepository.save(estadistica);

        partidoService.recalcularGoles(partido.getId());

        return estadisticaMapper.toDTO(guardada);
    }

    // Hallazgo post-despliegue (fase9): "se puede editar... un registro de
    // estadistica y verse reflejado en el partido". Solo se editan los
    // contadores (goles/tarjetas/asistencias/minuto); el jugador y el
    // partido del registro no se pueden reasignar desde aqui (si se
    // necesita mover el registro a otro jugador o partido, se elimina y se
    // crea uno nuevo) para no romper accidentalmente la relacion 1 registro
    // = 1 jugador + 1 partido.
    public EstadisticaDTO actualizar(Long id, EstadisticaDTO estadisticaDTO) {

        Estadistica existente = estadisticaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Estadística no encontrada"));

        if (!existente.getJugador().getId().equals(estadisticaDTO.getIdJugador())
                || !existente.getPartido().getId().equals(estadisticaDTO.getIdPartido())) {
            throw new BusinessException("No se puede reasignar un registro de estadística a otro jugador o partido: elimina este registro y crea uno nuevo.");
        }

        existente.setGoles(estadisticaDTO.getGoles());
        existente.setTarjetasAmarillas(estadisticaDTO.getTarjetasAmarillas());
        existente.setTarjetasRojas(estadisticaDTO.getTarjetasRojas());
        existente.setAsistencias(estadisticaDTO.getAsistencias());
        existente.setMinuto(estadisticaDTO.getMinuto());

        Estadistica actualizada = estadisticaRepository.save(existente);

        partidoService.recalcularGoles(actualizada.getPartido().getId());

        return estadisticaMapper.toDTO(actualizada);
    }

    // Hallazgo post-despliegue (fase9): al eliminar una estadistica los
    // goles del partido tambien deben actualizarse (recalcularGoles), por
    // eso se necesita el idPartido de la estadistica ANTES de borrarla.
    public void eliminar(Long id) {
        Estadistica existente = estadisticaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Estadística no encontrada"));

        Long idPartido = existente.getPartido().getId();

        estadisticaRepository.deleteById(id);

        partidoService.recalcularGoles(idPartido);
    }
}
