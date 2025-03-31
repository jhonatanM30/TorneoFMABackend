/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.repository.AlineacionRepository;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.JugadorRepository;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 *
 * @author JhonatanAlexanderCue
 */
@Service
public class ValidationCommonService {

    @Autowired
    private EquipoRepository equipoRepository;

    @Autowired
    private PartidoRepository partidoRepository;

    @Autowired
    private JugadorRepository jugadorRepository;

    @Autowired
    private AlineacionRepository alineacionRepository;

    public void validarEquipo(Long idEquipo) {
        if (idEquipo == null || !equipoRepository.existsById(idEquipo)) {
            throw new BusinessException("Fuera de juego! No se ha seleccionado un o unos equipos existentes.");
        }
    }

    public void validarPartidoExiste(Long idPartido) {
        if (!partidoRepository.existsById(idPartido)) {
            throw new BusinessException("Fuera de juego! El partido no esta programado.");
        }
    }

    public void validarJugadorExiste(Long idJugador) {
        if (!jugadorRepository.existsById(idJugador)) {
            throw new BusinessException("El jugador con ID " + idJugador + " no existe.");
        }
    }

    public void validarJugadorNoDuplicado(Long idPartido, Long idJugador) {
        boolean existe = alineacionRepository.existsByIdPartidoAndIdJugador(idPartido, idJugador);
        if (existe) {
            throw new BusinessException("El jugador ya está alineado en este partido.");
        }
    }

    public void validarMaximoTitulares(Long idPartido, Long idEquipo) {
        long titularesEquipo = alineacionRepository.contarTitularesPorEquipo(idPartido, idEquipo);

        if (titularesEquipo == 11) {
            throw new BusinessException("El equipo con ID " + idEquipo + " ya tiene 11 jugadores titulares en este partido.");
        }
    }

}
