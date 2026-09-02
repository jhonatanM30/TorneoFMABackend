package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marin.mas_que_amigos.dto.JugadorDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.JugadorNotFoundException;
import com.marin.mas_que_amigos.mapper.JugadorMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.JugadorRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JugadorServiceTest {

    @Mock
    private JugadorRepository jugadorRepository;
    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private JugadorMapper mapper;
    @Mock
    private ValidationCommonService validacionService;

    private JugadorService jugadorService;

    @BeforeEach
    void setUp() {
        jugadorService = new JugadorService(jugadorRepository, equipoRepository, mapper);
        ReflectionTestUtils.setField(jugadorService, "validacionService", validacionService);
    }

    @Test
    void guardarJugador_resuelveEquipoRealYDevuelveRepresentacionCompleta() {
        JugadorDTO dto = new JugadorDTO();
        dto.setNombre("Jhon");
        dto.setIdEquipo(1L);
        dto.setDorsal(7);

        Equipo equipoReal = new Equipo();
        equipoReal.setId(1L);
        equipoReal.setNombre("Macalister");

        Jugador entidadASalvar = new Jugador();
        Jugador guardado = new Jugador();
        guardado.setId(10L);
        guardado.setNombre("Jhon");
        guardado.setEquipo(equipoReal);

        JugadorDTO dtoMapeado = new JugadorDTO();
        dtoMapeado.setId(10L);
        dtoMapeado.setNombre("Jhon");

        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipoReal));
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(7, 1L, -1L)).thenReturn(false);
        when(mapper.toEntity(dto)).thenReturn(entidadASalvar);
        when(jugadorRepository.save(entidadASalvar)).thenReturn(guardado);
        when(mapper.toDTO(guardado)).thenReturn(dtoMapeado);

        JugadorDTO resultado = jugadorService.guardarJugador(dto);

        // Antes el mapper construía un Equipo "shell" (solo id) que se
        // quedaba en null en el resto de sus campos; ahora se asigna la
        // entidad Equipo real, ya cargada desde la base de datos.
        assertThat(entidadASalvar.getEquipo()).isSameAs(equipoReal);
        assertThat(resultado.getId()).isEqualTo(10L);
        assertThat(resultado.getNombre()).isEqualTo("Jhon");
        assertThat(resultado.getMensaje()).contains("Jhon");
    }

    @Test
    void guardarJugador_equipoNoExiste_lanzaBusinessException() {
        JugadorDTO dto = new JugadorDTO();
        dto.setIdEquipo(99L);
        dto.setDorsal(7);

        when(equipoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jugadorService.guardarJugador(dto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void actualizarJugador_mutaEntidadExistenteEnLugarDeCrearUnaNueva() {
        JugadorDTO dto = new JugadorDTO();
        dto.setId(10L);
        dto.setNombre("Jhon Actualizado");
        dto.setIdEquipo(1L);
        dto.setDorsal(9);

        Jugador existente = new Jugador();
        existente.setId(10L);
        existente.setNombre("Jhon");

        Equipo equipoReal = new Equipo();
        equipoReal.setId(1L);

        when(jugadorRepository.existsByIdAndEquipoId(10L, 1L)).thenReturn(true);
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(9, 1L, 10L)).thenReturn(false);
        when(jugadorRepository.findById(10L)).thenReturn(Optional.of(existente));
        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipoReal));
        when(jugadorRepository.save(existente)).thenReturn(existente);
        when(mapper.toDTO(existente)).thenReturn(new JugadorDTO());

        jugadorService.actualizarJugador(dto);

        ArgumentCaptor<Jugador> captor = ArgumentCaptor.forClass(Jugador.class);
        verify(jugadorRepository).save(captor.capture());

        // Se reutiliza la MISMA instancia administrada por JPA (no una nueva
        // sin id), que era lo que provocaba un INSERT duplicado en cada
        // "actualización" en vez de un UPDATE real.
        assertThat(captor.getValue()).isSameAs(existente);
        assertThat(captor.getValue().getNombre()).isEqualTo("Jhon Actualizado");
        assertThat(captor.getValue().getEquipo()).isSameAs(equipoReal);
    }

    @Test
    void actualizarJugador_noExiste_lanzaJugadorNotFoundException() {
        JugadorDTO dto = new JugadorDTO();
        dto.setId(999L);
        dto.setIdEquipo(1L);
        dto.setDorsal(9);

        when(jugadorRepository.existsByIdAndEquipoId(999L, 1L)).thenReturn(true);
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(9, 1L, 999L)).thenReturn(false);
        when(jugadorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> jugadorService.actualizarJugador(dto))
                .isInstanceOf(JugadorNotFoundException.class);
    }
}
