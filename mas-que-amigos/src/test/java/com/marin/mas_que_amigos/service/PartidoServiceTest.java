package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.mapper.PartidoMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PartidoServiceTest {

    @Mock
    private PartidoRepository partidoRepository;
    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private PartidoMapper mapper;
    @Mock
    private ValidationCommonService validacionService;

    private PartidoService partidoService;

    @BeforeEach
    void setUp() {
        partidoService = new PartidoService(partidoRepository, equipoRepository, mapper);
        ReflectionTestUtils.setField(partidoService, "validacionService", validacionService);
    }

    @Test
    void guardar_resuelveEquiposRealesYDevuelveRepresentacionCompleta() {
        PartidoDTO dto = new PartidoDTO();
        dto.setIdEquipoLocal(1L);
        dto.setIdEquipoVisitante(2L);
        dto.setFecha(LocalDate.of(2026, 12, 26));
        dto.setHora(LocalTime.of(15, 0));
        dto.setFase("FASE_DE_GRUPOS");

        Equipo local = new Equipo();
        local.setId(1L);
        local.setNombre("Macalister");
        Equipo visitante = new Equipo();
        visitante.setId(2L);
        visitante.setNombre("Comark");

        Partido entidadASalvar = new Partido();
        Partido guardado = new Partido();
        guardado.setId(50L);

        PartidoDTO dtoMapeado = new PartidoDTO();
        dtoMapeado.setId(50L);

        when(equipoRepository.findById(1L)).thenReturn(Optional.of(local));
        when(equipoRepository.findById(2L)).thenReturn(Optional.of(visitante));
        when(partidoRepository.existePartidoEnFechaParaEquipos(dto.getFecha(), 1L, 2L)).thenReturn(false);
        when(mapper.toEntity(dto)).thenReturn(entidadASalvar);
        when(partidoRepository.save(entidadASalvar)).thenReturn(guardado);
        when(mapper.toDTO(guardado)).thenReturn(dtoMapeado);

        PartidoDTO resultado = partidoService.guardar(dto);

        // Antes se construían Equipos "shell" (solo id) para las
        // asociaciones; ahora se asignan las entidades reales ya cargadas.
        assertThat(entidadASalvar.getEquipoLocal()).isSameAs(local);
        assertThat(entidadASalvar.getEquipoVisitante()).isSameAs(visitante);
        assertThat(resultado.getId()).isEqualTo(50L);
        assertThat(resultado.getMensaje()).isEqualTo("Equipos, el partido ya fue programado");
    }

    @Test
    void guardar_equiposIguales_lanzaBusinessException() {
        PartidoDTO dto = new PartidoDTO();
        dto.setIdEquipoLocal(1L);
        dto.setIdEquipoVisitante(1L);

        assertThatThrownBy(() -> partidoService.guardar(dto))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void guardar_fechaYaOcupadaParaEquipos_lanzaBusinessException() {
        PartidoDTO dto = new PartidoDTO();
        dto.setIdEquipoLocal(1L);
        dto.setIdEquipoVisitante(2L);
        dto.setFecha(LocalDate.of(2026, 12, 26));

        when(partidoRepository.existePartidoEnFechaParaEquipos(dto.getFecha(), 1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> partidoService.guardar(dto))
                .isInstanceOf(BusinessException.class);
    }
}
