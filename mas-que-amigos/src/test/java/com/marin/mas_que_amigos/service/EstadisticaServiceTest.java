package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.marin.mas_que_amigos.dto.GoleadorDTO;
import com.marin.mas_que_amigos.mapper.EstadisticaMapper;
import com.marin.mas_que_amigos.repository.EstadisticaRepository;
import com.marin.mas_que_amigos.repository.EstadisticaRepository.GoleadorProyeccion;
import com.marin.mas_que_amigos.repository.JugadorRepository;
import com.marin.mas_que_amigos.repository.PartidoRepository;

@ExtendWith(MockitoExtension.class)
class EstadisticaServiceTest {

    @Mock
    private EstadisticaRepository estadisticaRepository;
    @Mock
    private EstadisticaMapper estadisticaMapper;
    @Mock
    private PartidoRepository partidoRepository;
    @Mock
    private JugadorRepository jugadorRepository;
    @Mock
    private PartidoService partidoService;

    private EstadisticaService estadisticaService;

    @BeforeEach
    void setUp() {
        estadisticaService = new EstadisticaService(estadisticaRepository, estadisticaMapper, partidoRepository, jugadorRepository, partidoService);
    }

    @Test
    void obtenerGoleadores_unJugadorConMasGoles_devuelveSoloEseJugador() {
        GoleadorProyeccion filaGoleador = proyeccion(1L, "Messi", 10L, "Macalister", 5L);
        GoleadorProyeccion filaOtro = proyeccion(2L, "Cavani", 11L, "Comark", 3L);
        when(estadisticaRepository.obtenerTotalGolesPorJugador()).thenReturn(Arrays.asList(filaGoleador, filaOtro));

        GoleadorDTO dtoEsperado = new GoleadorDTO(1L, "Messi", 10L, "Macalister", 5);
        when(estadisticaMapper.toGoleadorDTO(filaGoleador)).thenReturn(dtoEsperado);

        List<GoleadorDTO> resultado = estadisticaService.obtenerGoleadores();

        assertThat(resultado).containsExactly(dtoEsperado);
    }

    @Test
    void obtenerGoleadores_empateEnElMaximo_devuelveATodosLosEmpatados() {
        GoleadorProyeccion filaA = proyeccion(1L, "Messi", 10L, "Macalister", 5L);
        GoleadorProyeccion filaB = proyeccion(2L, "Cavani", 11L, "Comark", 5L);
        GoleadorProyeccion filaC = proyeccion(3L, "Suarez", 12L, "River", 4L);
        when(estadisticaRepository.obtenerTotalGolesPorJugador()).thenReturn(Arrays.asList(filaA, filaB, filaC));

        GoleadorDTO dtoA = new GoleadorDTO(1L, "Messi", 10L, "Macalister", 5);
        GoleadorDTO dtoB = new GoleadorDTO(2L, "Cavani", 11L, "Comark", 5);
        when(estadisticaMapper.toGoleadorDTO(filaA)).thenReturn(dtoA);
        when(estadisticaMapper.toGoleadorDTO(filaB)).thenReturn(dtoB);

        List<GoleadorDTO> resultado = estadisticaService.obtenerGoleadores();

        assertThat(resultado).containsExactlyInAnyOrder(dtoA, dtoB);
    }

    @Test
    void obtenerGoleadores_sinGolesRegistrados_devuelveListaVacia() {
        when(estadisticaRepository.obtenerTotalGolesPorJugador()).thenReturn(Collections.emptyList());

        List<GoleadorDTO> resultado = estadisticaService.obtenerGoleadores();

        assertThat(resultado).isEmpty();
    }

    private GoleadorProyeccion proyeccion(Long idJugador, String nombreJugador, Long idEquipo, String nombreEquipo, Long totalGoles) {
        return new GoleadorProyeccion() {
            @Override
            public Long getIdJugador() {
                return idJugador;
            }

            @Override
            public String getNombreJugador() {
                return nombreJugador;
            }

            @Override
            public Long getIdEquipo() {
                return idEquipo;
            }

            @Override
            public String getNombreEquipo() {
                return nombreEquipo;
            }

            @Override
            public Long getTotalGoles() {
                return totalGoles;
            }
        };
    }
}
