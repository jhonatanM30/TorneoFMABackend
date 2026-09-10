/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.AlineacionDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.NotFoundException;
import com.marin.mas_que_amigos.mapper.AlineacionMapper;
import com.marin.mas_que_amigos.model.Alineacion;
import com.marin.mas_que_amigos.model.AlineacionId;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.repository.AlineacionRepository;
import com.marin.mas_que_amigos.repository.JugadorRepository;
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
public class AlineacionService {

    private final AlineacionRepository alineacionRepository;
    private final JugadorRepository jugadorRepository;
    private final PartidoRepository partidoRepository;
    private final AlineacionMapper mapper;

    @Autowired
    private ValidationCommonService validacionService;

    public AlineacionDTO guardarAlineacion(AlineacionDTO alineacion) {

        validacionService.validarPartidoExiste(alineacion.getIdPartido());
        validacionService.validarJugadorExiste(alineacion.getIdJugador());
        validacionService.validarJugadorNoDuplicado(alineacion.getIdPartido(), alineacion.getIdJugador());

        if (alineacion.isTitular()) {
            // 🔹 Antes se leía el equipo desde alineacion.getJugador().getEquipo(),
            // un campo que normalmente viaja vacío en la petición y provocaba un
            // NullPointerException. Ahora se resuelve el equipo real del jugador
            // consultando el repositorio.
            Jugador jugador = jugadorRepository.findById(alineacion.getIdJugador())
                    .orElseThrow(() -> new BusinessException("El jugador con ID " + alineacion.getIdJugador() + " no existe."));

            validacionService.validarMaximoTitulares(alineacion.getIdPartido(), jugador.getEquipo().getId());
        }

        Alineacion guardada = alineacionRepository.save(mapper.toEntity(alineacion));

        // Las asociaciones partido/jugador están mapeadas insertable=false,
        // updatable=false: la entidad recién guardada por save() todavía no
        // las trae cargadas en memoria. Se relee por su id compuesto para
        // obtener una instancia completamente hidratada y así devolver el
        // detalle completo en la respuesta, en vez de campos en null.
        Alineacion rehidratada = alineacionRepository.findById(guardada.getId())
                .orElseThrow(() -> new BusinessException("No se pudo recuperar la alineación recién guardada."));

        AlineacionDTO respuesta = mapper.toDTO(rehidratada);
        respuesta.setMensaje("Alineación registrada correctamente.");
        return respuesta;
    }

    public List<AlineacionDTO> obtenerAlineacionPorPartido(Long idPartido) {
        return alineacionRepository.findByIdPartido(idPartido)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    // Hallazgo post-despliegue (fase9): "se deberia permitir editar la
    // alineacion, un suplente pase a titular y viceversa". Se restringe a
    // partidos PROGRAMADO (por extension propia del mismo criterio que pide
    // el usuario para eliminar: "si el partido no ha iniciado") porque un
    // cambio de rol titular/suplente mientras el partido esta EN_CURSO ya
    // tiene su propio mecanismo, auditado con minuto y motivo:
    // CambioJugadorService (sustituciones). Permitir tambien este atajo
    // durante un partido en curso duplicaria esa logica sin dejar rastro en
    // el historial del partido.
    public AlineacionDTO actualizarAlineacion(Long idPartido, Long idJugador, AlineacionDTO dto) {

        Alineacion alineacion = alineacionRepository.findById(new AlineacionId(idPartido, idJugador))
                .orElseThrow(() -> new NotFoundException("Ese jugador no está alineado en este partido."));

        Partido partido = partidoRepository.findById(idPartido)
                .orElseThrow(() -> new NotFoundException("Fuera de juego! El partido indicado no existe."));

        if (partido.getEstado() != Partido.Estado.PROGRAMADO) {
            throw new BusinessException("Solo se puede editar la alineación mientras el partido no ha iniciado "
                    + "(usa el registro de cambios de jugador para sustituciones durante el partido).");
        }

        boolean pasaASerTitular = dto.isTitular() && !Boolean.TRUE.equals(alineacion.getTitular());
        if (pasaASerTitular) {
            validacionService.validarMaximoTitulares(idPartido, alineacion.getJugador().getEquipo().getId());
        }

        alineacion.setTitular(dto.isTitular());
        Alineacion actualizada = alineacionRepository.save(alineacion);

        Alineacion rehidratada = alineacionRepository.findById(actualizada.getId())
                .orElseThrow(() -> new BusinessException("No se pudo recuperar la alineación recién actualizada."));

        AlineacionDTO respuesta = mapper.toDTO(rehidratada);
        respuesta.setMensaje("Alineación actualizada correctamente.");
        return respuesta;
    }

    // Hallazgo post-despliegue (fase9): "eliminar un jugador de la
    // alineación si el partido no ha iniciado".
    public void eliminarAlineacion(Long idPartido, Long idJugador) {

        AlineacionId id = new AlineacionId(idPartido, idJugador);

        if (!alineacionRepository.existsById(id)) {
            throw new NotFoundException("Ese jugador no está alineado en este partido.");
        }

        Partido partido = partidoRepository.findById(idPartido)
                .orElseThrow(() -> new NotFoundException("Fuera de juego! El partido indicado no existe."));

        if (partido.getEstado() != Partido.Estado.PROGRAMADO) {
            throw new BusinessException("Solo se puede eliminar un jugador de la alineación mientras el partido no ha iniciado.");
        }

        alineacionRepository.deleteById(id);
    }

}
