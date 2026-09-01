package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marin.mas_que_amigos.dto.AlineacionDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.mapper.AlineacionMapper;
import com.marin.mas_que_amigos.model.Alineacion;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.repository.AlineacionRepository;
import com.marin.mas_que_amigos.repository.JugadorRepository;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AlineacionServiceTest {

    @Mock
    private AlineacionRepository alineacionRepository;
    @Mock
    private JugadorRepository jugadorRepository;
    @Mock
    private AlineacionMapper mapper;
    @Mock
    private ValidationCommonService validacionService;

    private AlineacionService alineacionService;

    @BeforeEach
    void setUp() {
        alineacionService = new AlineacionService(alineacionRepository, jugadorRepository, mapper);
        ReflectionTestUtils.setField(alineacionService, "validacionService", validacionService);
    }

    @Test
    void guardarAlineacion_titularTrue_resuelveEquipoDesdeRepositorioSinNPE() {
        AlineacionDTO dto = new AlineacionDTO();
        dto.setIdPartido(10L);
        dto.setIdJugador(20L);
        dto.setTitular(true);
        // dto.getJugador() se deja intencionalmente en null: así llega normalmente
        // desde el cliente, que solo envía idPartido/idJugador/titular.

        Equipo equipo = new Equipo();
        equipo.setId(99L);
        Jugador jugador = new Jugador();
        jugador.setId(20L);
        jugador.setEquipo(equipo);

        when(jugadorRepository.findById(20L)).thenReturn(Optional.of(jugador));
        when(mapper.toEntity(dto)).thenReturn(new Alineacion());
        when(mapper.toRSPDTO("Success", "Alineación registrada correctamente."))
                .thenReturn(new AlineacionDTO("Success", "Alineación registrada correctamente."));

        AlineacionDTO resultado = alineacionService.guardarAlineacion(dto);

        assertThat(resultado.getIndicadorRespuesta()).isEqualTo("Success");
        verify(validacionService).validarMaximoTitulares(10L, 99L);
        verify(alineacionRepository).save(any(Alineacion.class));
    }

    @Test
    void guardarAlineacion_jugadorNoExiste_lanzaBusinessException() {
        AlineacionDTO dto = new AlineacionDTO();
        dto.setIdPartido(10L);
        dto.setIdJugador(20L);
        dto.setTitular(true);

        when(jugadorRepository.findById(20L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> alineacionService.guardarAlineacion(dto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void obtenerAlineacionPorPartido_mapeaEntidadesADTO() {
        Alineacion entidad = new Alineacion();
        AlineacionDTO dtoEsperado = new AlineacionDTO();

        when(alineacionRepository.findByIdPartido(5L)).thenReturn(Arrays.asList(entidad));
        when(mapper.toDTO(entidad)).thenReturn(dtoEsperado);

        List<AlineacionDTO> resultado = alineacionService.obtenerAlineacionPorPartido(5L);

        assertThat(resultado).containsExactly(dtoEsperado);
    }
}
