/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.NotFoundException;
import com.marin.mas_que_amigos.mapper.PartidoMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.repository.AlineacionRepository;
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
    private final AlineacionRepository alineacionRepository;

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

    // FRONTEND_VISION.md Fase3: busqueda de partidos por coincidencia
    // parcial de nombre de equipo (buscarPartidoPorEquipo exige match
    // exacto). Ver PartidoRepository.findPartidosByEquipoNombreParcial.
    public List<PartidoDTO> buscarPartidosPorEquipoParcial(String nombre) {
        if (nombre == null || nombre.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return partidoRepository.findPartidosByEquipoNombreParcial(nombre.trim())
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
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

    // FRONTEND_VISION.md Fase3: "un partido se deberia permitir Editar".
    // Permite corregir fecha/hora/fase/goles o los equipos de un partido ya
    // programado (por ejemplo, para registrar el marcador final, ya que
    // hoy solo se puede fijar goles en la creacion).
    public PartidoDTO actualizarPartido(PartidoDTO partidoDTO) {

        Partido partidoExistente = partidoRepository.findById(partidoDTO.getId())
                .orElseThrow(() -> new NotFoundException("Fuera de juego! El partido que deseas editar no existe."));

        validacionService.validarEquipo(partidoDTO.getIdEquipoLocal());
        validacionService.validarEquipo(partidoDTO.getIdEquipoVisitante());

        if (partidoDTO.getIdEquipoLocal().equals(partidoDTO.getIdEquipoVisitante())) {
            throw new BusinessException("Los equipos seleccionados son los mismos. Un equipo no puede jugar contra sí mismo.");
        }

        boolean cambianLosEquipos = !partidoDTO.getIdEquipoLocal().equals(partidoExistente.getEquipoLocal().getId())
                || !partidoDTO.getIdEquipoVisitante().equals(partidoExistente.getEquipoVisitante().getId());

        // No se permite cambiar los equipos de un partido que ya tiene
        // alineaciones registradas: los jugadores alineados pertenecen a los
        // equipos originales, y cambiar los equipos los dejaria "huerfanos"
        // (alineados en un partido de equipos a los que ya no pertenecen).
        // Fecha/hora/goles/fase si se pueden seguir editando en ese caso.
        if (cambianLosEquipos && !alineacionRepository.findByIdPartido(partidoExistente.getId()).isEmpty()) {
            throw new BusinessException("Este partido ya tiene una alineación registrada: no se pueden cambiar los equipos (elimina la alineación primero si necesitas hacerlo).");
        }

        if (partidoRepository.existePartidoEnFechaParaEquiposExcluyendo(partidoDTO.getFecha(), partidoDTO.getIdEquipoLocal(),
                partidoDTO.getIdEquipoVisitante(), partidoExistente.getId())) {
            throw new BusinessException("Ya existe otro partido programado el dia " + partidoDTO.getFecha() + " para uno de estos equipos");
        }

        Equipo equipoLocal = equipoRepository.findById(partidoDTO.getIdEquipoLocal())
                .orElseThrow(() -> new BusinessException("Fuera de juego! No se ha seleccionado un o unos equipos existentes."));
        Equipo equipoVisitante = equipoRepository.findById(partidoDTO.getIdEquipoVisitante())
                .orElseThrow(() -> new BusinessException("Fuera de juego! No se ha seleccionado un o unos equipos existentes."));

        // Se reutiliza mapper.toEntity() (en vez de asignar los campos a
        // mano) para no duplicar la conversion String -> Partido.Fase que
        // ya hace el mapper (incluida su validacion de fase invalida).
        Partido datosNuevos = mapper.toEntity(partidoDTO);

        partidoExistente.setEquipoLocal(equipoLocal);
        partidoExistente.setEquipoVisitante(equipoVisitante);
        partidoExistente.setFecha(datosNuevos.getFecha());
        partidoExistente.setHora(datosNuevos.getHora());
        partidoExistente.setGolesLocal(datosNuevos.getGolesLocal());
        partidoExistente.setGolesVisitante(datosNuevos.getGolesVisitante());
        partidoExistente.setFase(datosNuevos.getFase());

        Partido actualizado = partidoRepository.save(partidoExistente);

        PartidoDTO respuesta = mapper.toDTO(actualizado);
        respuesta.setMensaje("Partido actualizado correctamente.");
        return respuesta;
    }

    // FRONTEND_VISION.md Fase3-09: marca el partido como en curso, lo que
    // habilita registrar cambios de jugador (CambioJugadorService) y anotar
    // el minuto al registrar una estadistica durante el partido.
    public PartidoDTO iniciarPartido(Long id) {
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fuera de juego! El partido que deseas iniciar no existe."));

        if (partido.getEstado() != Partido.Estado.PROGRAMADO) {
            throw new BusinessException("Solo se puede iniciar un partido que esté programado (estado actual: "
                    + partido.getEstado() + ").");
        }

        partido.setEstado(Partido.Estado.EN_CURSO);
        Partido actualizado = partidoRepository.save(partido);

        PartidoDTO respuesta = mapper.toDTO(actualizado);
        respuesta.setMensaje("¡Arrancó el partido!");
        return respuesta;
    }

    // Cierra el partido; a partir de aquí ya no se pueden registrar más
    // cambios de jugador para él (ver CambioJugadorService).
    public PartidoDTO finalizarPartido(Long id) {
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fuera de juego! El partido que deseas finalizar no existe."));

        if (partido.getEstado() != Partido.Estado.EN_CURSO) {
            throw new BusinessException("Solo se puede finalizar un partido que esté en curso (estado actual: "
                    + partido.getEstado() + ").");
        }

        partido.setEstado(Partido.Estado.FINALIZADO);
        Partido actualizado = partidoRepository.save(partido);

        PartidoDTO respuesta = mapper.toDTO(actualizado);
        respuesta.setMensaje("Partido finalizado.");
        return respuesta;
    }

    public void eliminar(Long id) {
        partidoRepository.deleteById(id);
    }
}
