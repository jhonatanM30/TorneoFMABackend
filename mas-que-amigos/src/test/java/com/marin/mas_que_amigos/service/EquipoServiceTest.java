package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.NotFoundException;
import com.marin.mas_que_amigos.mapper.EquipoMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EquipoServiceTest {

    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private EquipoMapper mapper;

    private EquipoService equipoService;

    @BeforeEach
    void setUp() {
        equipoService = new EquipoService(equipoRepository, mapper);
    }

    @Test
    void guardarEquipo_devuelveRepresentacionCompletaDelEquipoGuardado() {
        EquipoDTO dto = new EquipoDTO();
        dto.setNombre("Macalister");
        dto.setDirectorTecnico("Johan");
        dto.setTipoClasificacion(Equipo.TipoClasificacion.ELIMINATORIA);

        Equipo entidadASalvar = new Equipo();
        Equipo guardado = new Equipo();
        guardado.setId(1L);
        guardado.setNombre("Macalister");

        EquipoDTO dtoMapeado = new EquipoDTO();
        dtoMapeado.setId(1L);
        dtoMapeado.setNombre("Macalister");

        when(equipoRepository.existsByNombre("Macalister")).thenReturn(false);
        when(mapper.toEntity(dto)).thenReturn(entidadASalvar);
        when(equipoRepository.save(entidadASalvar)).thenReturn(guardado);
        when(mapper.toDTO(guardado)).thenReturn(dtoMapeado);

        EquipoDTO resultado = equipoService.guardarEquipo(dto);

        // Antes de esta corrección la respuesta solo traía indicadorRespuesta/
        // mensaje y el resto de campos llegaba en null; ahora se devuelve la
        // representación completa de la entidad recién guardada.
        assertThat(resultado.getId()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Macalister");
        assertThat(resultado.getMensaje()).contains("Macalister");
    }

    @Test
    void guardarEquipo_nombreDuplicado_lanzaBusinessException() {
        EquipoDTO dto = new EquipoDTO();
        dto.setNombre("Macalister");

        when(equipoRepository.existsByNombre("Macalister")).thenReturn(true);

        assertThatThrownBy(() -> equipoService.guardarEquipo(dto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void actualizarEquipo_mutaEntidadExistenteYNoTocaJugadores() {
        EquipoDTO dto = new EquipoDTO();
        dto.setId(5L);
        dto.setNombre("Nuevo Nombre");
        dto.setDirectorTecnico("Nuevo DT");
        dto.setImagenUrl("url");
        dto.setTitulos(3);
        dto.setTipoClasificacion(Equipo.TipoClasificacion.ELIMINATORIA);

        Equipo existente = new Equipo();
        existente.setId(5L);
        existente.setNombre("Viejo Nombre");
        existente.setJugadores(Arrays.asList(new Jugador(), new Jugador()));

        when(equipoRepository.findById(5L)).thenReturn(Optional.of(existente));
        when(equipoRepository.save(existente)).thenReturn(existente);
        when(mapper.toDTO(existente)).thenReturn(new EquipoDTO());

        equipoService.actualizarEquipo(dto);

        ArgumentCaptor<Equipo> captor = ArgumentCaptor.forClass(Equipo.class);
        verify(equipoRepository).save(captor.capture());
        Equipo guardado = captor.getValue();

        // Se debe reutilizar la MISMA instancia administrada por JPA (no una
        // nueva), ya que guardar una nueva sin id provocaba un INSERT
        // duplicado en lugar de un UPDATE.
        assertThat(guardado).isSameAs(existente);
        assertThat(guardado.getNombre()).isEqualTo("Nuevo Nombre");
        // La colección de jugadores nunca se reemplaza ni se vacía: si se
        // construyera un Equipo nuevo con jugadores=[] y orphanRemoval=true,
        // esto habría borrado a todos los jugadores del equipo al editarlo.
        assertThat(guardado.getJugadores()).hasSize(2);
    }

    @Test
    void actualizarEquipo_noExiste_lanzaNotFoundException() {
        EquipoDTO dto = new EquipoDTO();
        dto.setId(99L);
        dto.setNombre("Fantasma");

        when(equipoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> equipoService.actualizarEquipo(dto))
                .isInstanceOf(NotFoundException.class);
    }
}
