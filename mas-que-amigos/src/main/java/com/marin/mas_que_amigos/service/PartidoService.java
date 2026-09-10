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
import com.marin.mas_que_amigos.repository.CambioJugadorRepository;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.EstadisticaRepository;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Service
@RequiredArgsConstructor
public class PartidoService {

    // Hallazgo post-despliegue (fase9): "no se deberia permitir iniciar un
    // partido sin que antes cada equipo haya al menos alineado 7 titulares".
    // ValidationCommonService.validarMaximoTitulares ya limitaba el MAXIMO
    // (11) al alinear; este es un MINIMO nuevo, exigido recien al iniciar.
    private static final int MINIMO_TITULARES_PARA_INICIAR = 7;

    private final PartidoRepository partidoRepository;
    private final EquipoRepository equipoRepository;
    private final AlineacionRepository alineacionRepository;
    private final EstadisticaRepository estadisticaRepository;
    private final CambioJugadorRepository cambioJugadorRepository;

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

        // Hallazgo post-despliegue (fase9): un partido nace siempre en 0-0;
        // los goles solo se mueven al registrar estadisticas (ver
        // recalcularGoles). Se fija explicitamente en vez de confiar en el
        // valor por defecto del DTO/entidad para que quede claro que es
        // intencional, no un olvido.
        partido.setGolesLocal(0);
        partido.setGolesVisitante(0);

        Partido guardado = partidoRepository.save(partido);

        PartidoDTO respuesta = mapper.toDTO(guardado);
        respuesta.setMensaje("Equipos, el partido ya fue programado");
        return respuesta;
    }

    // FRONTEND_VISION.md Fase3: "un partido se deberia permitir Editar".
    // Permite corregir fecha/hora/fase o los equipos de un partido que
    // todavia no ha finalizado.
    public PartidoDTO actualizarPartido(PartidoDTO partidoDTO) {

        Partido partidoExistente = partidoRepository.findById(partidoDTO.getId())
                .orElseThrow(() -> new NotFoundException("Fuera de juego! El partido que deseas editar no existe."));

        // Hallazgo post-despliegue (fase9): "una vez finalizado el partido,
        // no se puede editar ni eliminar".
        if (partidoExistente.getEstado() == Partido.Estado.FINALIZADO) {
            throw new BusinessException("Este partido ya finalizó: no se puede editar.");
        }

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
        // Fecha/hora/fase si se pueden seguir editando en ese caso.
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
        partidoExistente.setFase(datosNuevos.getFase());
        // Hallazgo post-despliegue (fase9): golesLocal/golesVisitante ya NO
        // se tocan aqui - son de solo lectura, calculados desde Estadistica.

        Partido actualizado = partidoRepository.save(partidoExistente);

        PartidoDTO respuesta = mapper.toDTO(actualizado);
        respuesta.setMensaje("Partido actualizado correctamente.");
        return respuesta;
    }

    // FRONTEND_VISION.md Fase3-09: marca el partido como en curso, lo que
    // habilita registrar cambios de jugador (CambioJugadorService) y anotar
    // el minuto al registrar una estadistica durante el partido.
    //
    // Hallazgo post-despliegue (fase9): "no se deberia permitir iniciar un
    // partido sin que antes cada equipo haya al menos alineado 7 titulares" -
    // se agrega esta validacion antes de permitir el cambio de estado.
    public PartidoDTO iniciarPartido(Long id) {
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fuera de juego! El partido que deseas iniciar no existe."));

        if (partido.getEstado() != Partido.Estado.PROGRAMADO) {
            throw new BusinessException("Solo se puede iniciar un partido que esté programado (estado actual: "
                    + partido.getEstado() + ").");
        }

        long titularesLocal = alineacionRepository.contarTitularesPorEquipo(id, partido.getEquipoLocal().getId());
        long titularesVisitante = alineacionRepository.contarTitularesPorEquipo(id, partido.getEquipoVisitante().getId());

        if (titularesLocal < MINIMO_TITULARES_PARA_INICIAR || titularesVisitante < MINIMO_TITULARES_PARA_INICIAR) {
            throw new BusinessException("Para iniciar el partido cada equipo debe tener alineados al menos "
                    + MINIMO_TITULARES_PARA_INICIAR + " titulares (" + partido.getEquipoLocal().getNombre() + ": "
                    + titularesLocal + "/" + MINIMO_TITULARES_PARA_INICIAR + ", " + partido.getEquipoVisitante().getNombre()
                    + ": " + titularesVisitante + "/" + MINIMO_TITULARES_PARA_INICIAR + "). Completa la alineación primero.");
        }

        partido.setEstado(Partido.Estado.EN_CURSO);
        Partido actualizado = partidoRepository.save(partido);

        PartidoDTO respuesta = mapper.toDTO(actualizado);
        respuesta.setMensaje("¡Arrancó el partido!");
        return respuesta;
    }

    // Cierra el partido; a partir de aquí ya no se pueden registrar más
    // cambios de jugador para él (ver CambioJugadorService), y tampoco se
    // podra editar ni eliminar (ver actualizarPartido/eliminar).
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

    // Hallazgo post-despliegue (fase9): "una vez finalizado el partido, no
    // se puede editar ni eliminar" + "si se elimina un partido se debe
    // borrar sus estadisticas relacionadas". No existe ON DELETE CASCADE en
    // la base de datos (ver V1/V4__*.sql), asi que se borran a mano, en
    // orden, las filas que dependen del partido antes de borrar el partido
    // en si: primero los cambios de jugador y las estadisticas (ambos
    // referencian solo al partido), despues la alineacion. Se anota
    // @Transactional para que, si algun paso falla, no queden borrados
    // parciales.
    @Transactional
    public void eliminar(Long id) {
        Partido partido = partidoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fuera de juego! El partido que deseas eliminar no existe."));

        if (partido.getEstado() == Partido.Estado.FINALIZADO) {
            throw new BusinessException("Este partido ya finalizó: no se puede eliminar.");
        }

        cambioJugadorRepository.deleteByPartidoId(id);
        estadisticaRepository.deleteByPartidoId(id);
        alineacionRepository.eliminarPorPartido(id);
        partidoRepository.deleteById(id);
    }

    // Hallazgo post-despliegue (fase9): "los goles que se reflejan en
    // partido deben ser ingresados por el registro de estadisticas". Se
    // llama desde EstadisticaService cada vez que se crea, edita o elimina
    // una estadistica, para que Partido.golesLocal/golesVisitante siempre
    // reflejen la suma real de goles registrados por jugador. Se ejecuta
    // sin importar el estado actual del partido (incluso FINALIZADO): la
    // congelacion de goles solo aplica como politica de migracion unica a
    // los partidos que ya estaban finalizados antes de este cambio (no se
    // recalculan retroactivamente al desplegar), no como una prohibicion
    // permanente de recalcular si despues alguien toca sus estadisticas.
    public void recalcularGoles(Long idPartido) {
        Partido partido = partidoRepository.findById(idPartido).orElse(null);
        if (partido == null) {
            return;
        }

        int golesLocal = estadisticaRepository.sumarGolesPorEquipo(idPartido, partido.getEquipoLocal().getId());
        int golesVisitante = estadisticaRepository.sumarGolesPorEquipo(idPartido, partido.getEquipoVisitante().getId());

        partido.setGolesLocal(golesLocal);
        partido.setGolesVisitante(golesVisitante);
        partidoRepository.save(partido);
    }
}
