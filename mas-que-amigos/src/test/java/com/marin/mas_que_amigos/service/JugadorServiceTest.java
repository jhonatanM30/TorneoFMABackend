package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marin.mas_que_amigos.dto.JugadorBatchResponseDTO;
import com.marin.mas_que_amigos.dto.JugadorDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.JugadorNotFoundException;
import com.marin.mas_que_amigos.mapper.JugadorMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.model.Jugador.Posicion;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.JugadorRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.validation.Validation;
import javax.validation.Validator;
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

    // Validador real (no mock): las pruebas de lote necesitan que las
    // anotaciones de bean validation (@NotBlank, @NotNull, @Min) de
    // JugadorDTO se evalúen de verdad, igual que en EstadisticaDTOValidationTest.
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    private JugadorService jugadorService;

    @BeforeEach
    void setUp() {
        jugadorService = new JugadorService(jugadorRepository, equipoRepository, mapper, validator);
        ReflectionTestUtils.setField(jugadorService, "validacionService", validacionService);
    }

    private JugadorDTO jugadorValido(String nombre, Long idEquipo, int dorsal) {
        JugadorDTO dto = new JugadorDTO();
        dto.setNombre(nombre);
        dto.setPosicion(Posicion.DELANTERO);
        dto.setEdad(20);
        dto.setDorsal(dorsal);
        dto.setIdEquipo(idEquipo);
        return dto;
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

    @Test
    void guardarJugadoresEnLote_todosValidos_creaTodos() {
        JugadorDTO dto1 = jugadorValido("Jhon", 1L, 7);
        JugadorDTO dto2 = jugadorValido("Alex", 1L, 8);

        Equipo equipoReal = new Equipo();
        equipoReal.setId(1L);

        Jugador entidad1 = new Jugador();
        Jugador entidad2 = new Jugador();
        Jugador guardado1 = new Jugador();
        guardado1.setId(10L);
        Jugador guardado2 = new Jugador();
        guardado2.setId(11L);

        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipoReal));
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(7, 1L, -1L)).thenReturn(false);
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(8, 1L, -1L)).thenReturn(false);
        when(mapper.toEntity(dto1)).thenReturn(entidad1);
        when(mapper.toEntity(dto2)).thenReturn(entidad2);
        when(jugadorRepository.save(entidad1)).thenReturn(guardado1);
        when(jugadorRepository.save(entidad2)).thenReturn(guardado2);
        when(mapper.toDTO(guardado1)).thenReturn(new JugadorDTO());
        when(mapper.toDTO(guardado2)).thenReturn(new JugadorDTO());

        JugadorBatchResponseDTO resultado = jugadorService.guardarJugadoresEnLote(Arrays.asList(dto1, dto2));

        assertThat(resultado.getTotal()).isEqualTo(2);
        assertThat(resultado.getExitosos()).isEqualTo(2);
        assertThat(resultado.getFallidos()).isZero();
        assertThat(resultado.getResultados()).hasSize(2);
        assertThat(resultado.getResultados().get(0).isExito()).isTrue();
        assertThat(resultado.getResultados().get(0).getIndice()).isZero();
        assertThat(resultado.getResultados().get(1).isExito()).isTrue();
        assertThat(resultado.getResultados().get(1).getIndice()).isEqualTo(1);
    }

    @Test
    void guardarJugadoresEnLote_unoInvalido_creaLosDemasYReportaElFallo() {
        // Simula el caso "19 de 20": de 3 jugadores, el del medio trae datos
        // inválidos (dorsal 0, viola @Min(1)) y los otros dos sí deben
        // crearse igual.
        JugadorDTO valido1 = jugadorValido("Jhon", 1L, 7);
        JugadorDTO invalido = jugadorValido("SinDorsal", 1L, 0);
        JugadorDTO valido2 = jugadorValido("Alex", 1L, 8);

        Equipo equipoReal = new Equipo();
        equipoReal.setId(1L);

        Jugador entidad1 = new Jugador();
        Jugador entidad3 = new Jugador();
        Jugador guardado1 = new Jugador();
        guardado1.setId(10L);
        Jugador guardado3 = new Jugador();
        guardado3.setId(12L);

        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipoReal));
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(7, 1L, -1L)).thenReturn(false);
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(8, 1L, -1L)).thenReturn(false);
        when(mapper.toEntity(valido1)).thenReturn(entidad1);
        when(mapper.toEntity(valido2)).thenReturn(entidad3);
        when(jugadorRepository.save(entidad1)).thenReturn(guardado1);
        when(jugadorRepository.save(entidad3)).thenReturn(guardado3);
        when(mapper.toDTO(guardado1)).thenReturn(new JugadorDTO());
        when(mapper.toDTO(guardado3)).thenReturn(new JugadorDTO());

        JugadorBatchResponseDTO resultado = jugadorService.guardarJugadoresEnLote(Arrays.asList(valido1, invalido, valido2));

        assertThat(resultado.getTotal()).isEqualTo(3);
        assertThat(resultado.getExitosos()).isEqualTo(2);
        assertThat(resultado.getFallidos()).isEqualTo(1);

        assertThat(resultado.getResultados().get(0).isExito()).isTrue();
        assertThat(resultado.getResultados().get(1).isExito()).isFalse();
        assertThat(resultado.getResultados().get(1).getError()).isNotBlank();
        assertThat(resultado.getResultados().get(1).getJugador()).isNull();
        assertThat(resultado.getResultados().get(2).isExito()).isTrue();

        // El jugador inválido nunca debió intentar mapearse ni guardarse:
        // la validación de datos lo detiene antes de tocar el repositorio.
        verify(mapper, org.mockito.Mockito.never()).toEntity(invalido);
    }

    @Test
    void guardarJugadoresEnLote_dorsalDuplicadoDentroDelLote_elSegundoFalla() {
        // Dos jugadores del mismo lote compiten por el mismo dorsal en el
        // mismo equipo. El primero se guarda y queda persistido; el segundo
        // debe fallar porque la validación se reconsulta contra la base de
        // datos (que ya "tiene" al primero) en vez de una sola vez al inicio.
        JugadorDTO dto1 = jugadorValido("Jhon", 1L, 9);
        JugadorDTO dto2 = jugadorValido("Otro", 1L, 9);

        Equipo equipoReal = new Equipo();
        equipoReal.setId(1L);
        Jugador entidad1 = new Jugador();
        Jugador guardado1 = new Jugador();
        guardado1.setId(10L);

        when(equipoRepository.findById(1L)).thenReturn(Optional.of(equipoReal));
        // Primera consulta (jugador 1): dorsal libre. Segunda consulta
        // (jugador 2, mismo dorsal/equipo): ya ocupado.
        when(jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(9, 1L, -1L)).thenReturn(false, true);
        when(mapper.toEntity(dto1)).thenReturn(entidad1);
        when(jugadorRepository.save(entidad1)).thenReturn(guardado1);
        when(mapper.toDTO(guardado1)).thenReturn(new JugadorDTO());

        JugadorBatchResponseDTO resultado = jugadorService.guardarJugadoresEnLote(Arrays.asList(dto1, dto2));

        assertThat(resultado.getExitosos()).isEqualTo(1);
        assertThat(resultado.getFallidos()).isEqualTo(1);
        assertThat(resultado.getResultados().get(0).isExito()).isTrue();
        assertThat(resultado.getResultados().get(1).isExito()).isFalse();
        assertThat(resultado.getResultados().get(1).getError()).contains("dorsal");
    }

    @Test
    void guardarJugadoresEnLote_listaVacia_lanzaBusinessException() {
        assertThatThrownBy(() -> jugadorService.guardarJugadoresEnLote(new ArrayList<>()))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void guardarJugadoresEnLote_excedeTamanoMaximo_lanzaBusinessException() {
        List<JugadorDTO> loteGrande = new ArrayList<>();
        for (int i = 0; i < 51; i++) {
            loteGrande.add(jugadorValido("Jugador" + i, 1L, i + 1));
        }

        assertThatThrownBy(() -> jugadorService.guardarJugadoresEnLote(loteGrande))
                .isInstanceOf(BusinessException.class);
    }
}
