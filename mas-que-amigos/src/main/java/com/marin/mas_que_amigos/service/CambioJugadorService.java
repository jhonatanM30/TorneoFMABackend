package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.CambioJugadorDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.mapper.CambioJugadorMapper;
import com.marin.mas_que_amigos.model.Alineacion;
import com.marin.mas_que_amigos.model.CambioJugador;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.repository.AlineacionRepository;
import com.marin.mas_que_amigos.repository.CambioJugadorRepository;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * FRONTEND_VISION.md Fase3-09: registrar y consultar los cambios de
 * jugador (sustituciones) de un partido en curso, con el minuto en que
 * ocurrieron. Junto con Estadistica.minuto (Fase3-09) forma el historial
 * del partido que pide Fase3-10.
 *
 * @author JhonatanAlexanderCue
 */
@Service
@RequiredArgsConstructor
public class CambioJugadorService {

    private final CambioJugadorRepository cambioJugadorRepository;
    private final PartidoRepository partidoRepository;
    private final AlineacionRepository alineacionRepository;
    private final CambioJugadorMapper mapper;

    public List<CambioJugadorDTO> listarPorPartido(Long idPartido) {
        return cambioJugadorRepository.findByPartidoIdOrderByMinutoAsc(idPartido)
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public CambioJugadorDTO registrar(Long idPartido, CambioJugadorDTO dto) {

        Partido partido = partidoRepository.findById(idPartido)
                .orElseThrow(() -> new BusinessException("Fuera de juego! El partido indicado no existe."));

        if (partido.getEstado() != Partido.Estado.EN_CURSO) {
            throw new BusinessException("Solo se pueden registrar cambios de jugador en un partido en curso "
                    + "(inicia el partido primero).");
        }

        if (dto.getIdJugadorSale().equals(dto.getIdJugadorEntra())) {
            throw new BusinessException("El jugador que sale y el que entra no pueden ser el mismo.");
        }

        Alineacion alineacionSale = alineacionRepository.findByIdPartidoAndIdJugador(idPartido, dto.getIdJugadorSale())
                .orElseThrow(() -> new BusinessException("El jugador que sale no está alineado en este partido."));

        if (!Boolean.TRUE.equals(alineacionSale.getTitular())) {
            throw new BusinessException("El jugador que sale debe ser titular actualmente en este partido.");
        }

        Alineacion alineacionEntra = alineacionRepository.findByIdPartidoAndIdJugador(idPartido, dto.getIdJugadorEntra())
                .orElseThrow(() -> new BusinessException("El jugador que entra no está alineado en este partido (debe estar en la banca como suplente)."));

        if (Boolean.TRUE.equals(alineacionEntra.getTitular())) {
            throw new BusinessException("El jugador que entra ya es titular en este partido.");
        }

        if (!alineacionSale.getJugador().getEquipo().getId().equals(alineacionEntra.getJugador().getEquipo().getId())) {
            throw new BusinessException("El cambio debe ser entre jugadores del mismo equipo.");
        }

        CambioJugador cambio = new CambioJugador();
        cambio.setPartido(partido);
        cambio.setJugadorSale(alineacionSale.getJugador());
        cambio.setJugadorEntra(alineacionEntra.getJugador());
        cambio.setMinuto(dto.getMinuto());
        cambio.setFechaRegistro(LocalDateTime.now());
        CambioJugador guardado = cambioJugadorRepository.save(cambio);

        // El jugador que entra pasa a ser titular (esta en cancha) y el que
        // sale deja de serlo, para que Alineacion siga reflejando quien esta
        // jugando ahora mismo - así el modal de Estadísticas (que ya lista
        // "jugadores alineados" para ese partido) automáticamente puede
        // registrarle goles/tarjetas al que entró, sin cambios adicionales.
        alineacionSale.setTitular(false);
        alineacionEntra.setTitular(true);
        alineacionRepository.save(alineacionSale);
        alineacionRepository.save(alineacionEntra);

        return mapper.toDTO(guardado);
    }
}
