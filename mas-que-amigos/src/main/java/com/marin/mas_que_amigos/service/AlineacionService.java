/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.AlineacionDTO;
import com.marin.mas_que_amigos.mapper.AlineacionMapper;
import com.marin.mas_que_amigos.model.Alineacion;
import com.marin.mas_que_amigos.repository.AlineacionRepository;
import java.util.List;
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
    private final AlineacionMapper mapper;

    @Autowired
    private ValidationCommonService validacionService;

    public void guardarAlineacion(AlineacionDTO alineacion) {

        if (alineacion.isTitular()) {
            validacionService.validarMaximoTitulares(alineacion.getIdPartido(), alineacion.getJugador().getEquipo().getId());
        }

        validacionService.validarPartidoExiste(alineacion.getIdPartido());
        validacionService.validarJugadorExiste(alineacion.getIdJugador());
        validacionService.validarJugadorNoDuplicado(alineacion.getIdPartido(), alineacion.getIdJugador());

        Alineacion rspAlineacion = mapper.toEntity(alineacion);

        alineacionRepository.save(rspAlineacion);
    }

    public List<Alineacion> obtenerAlineacionPorPartido(Long idPartido) {
        return alineacionRepository.findByIdPartido(idPartido);
    }

}
