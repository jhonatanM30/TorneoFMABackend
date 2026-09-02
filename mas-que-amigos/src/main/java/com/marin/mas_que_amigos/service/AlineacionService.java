/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.AlineacionDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.mapper.AlineacionMapper;
import com.marin.mas_que_amigos.model.Alineacion;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.repository.AlineacionRepository;
import com.marin.mas_que_amigos.repository.JugadorRepository;
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

}
