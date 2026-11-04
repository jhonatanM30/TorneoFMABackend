package com.marin.mas_que_amigos.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.dto.PenaltiesRequestDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.NotFoundException;
import com.marin.mas_que_amigos.mapper.PartidoMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.model.Tournament;
import com.marin.mas_que_amigos.model.TournamentFormat;
import com.marin.mas_que_amigos.model.TournamentStatus;
import com.marin.mas_que_amigos.repository.AlineacionRepository;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import com.marin.mas_que_amigos.repository.EstadisticaRepository;
import com.marin.mas_que_amigos.repository.CambioJugadorRepository;

@ExtendWith(MockitoExtension.class)
class PartidoServiceTest {

    @Mock
    private PartidoRepository partidoRepository;
    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private AlineacionRepository alineacionRepository;
    @Mock
    private EstadisticaRepository estadisticasRepository;
    @Mock
    private CambioJugadorRepository cambioJugadorRepository;
    @Mock
    private PartidoMapper mapper;
    @Mock
    private ValidationCommonService validacionService;
    @Mock
    private TournamentService tournamentService;
    @Mock
    private BracketService bracketService;

    private PartidoService partidoService;

    @BeforeEach
    void setUp() {
        partidoService = new PartidoService(partidoRepository, equipoRepository, alineacionRepository, estadisticasRepository, cambioJugadorRepository, mapper, tournamentService, bracketService);
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

    // ------------------------------------------------------------------ torneos (add-tournaments)

    private Equipo equipo(Long id, String nombre) {
        Equipo equipo = new Equipo();
        equipo.setId(id);
        equipo.setNombre(nombre);
        return equipo;
    }

    private Tournament torneo(Long id, TournamentFormat formato) {
        Tournament torneo = new Tournament();
        torneo.setId(id);
        torneo.setName("Torneo " + id);
        torneo.setFormat(formato);
        torneo.setStatus(TournamentStatus.IN_PROGRESS);
        return torneo;
    }

    private PartidoDTO dtoDePartido(Long idLocal, Long idVisitante, Long idTorneo) {
        PartidoDTO dto = new PartidoDTO();
        dto.setIdEquipoLocal(idLocal);
        dto.setIdEquipoVisitante(idVisitante);
        dto.setFecha(LocalDate.of(2026, 12, 26));
        dto.setHora(LocalTime.of(15, 0));
        dto.setIdTorneo(idTorneo);
        return dto;
    }

    @Test
    void guardar_sinIdTorneo_asignaElTorneoQueResuelveElServicioDeTorneos() {
        PartidoDTO dto = dtoDePartido(1L, 2L, null);
        Equipo local = equipo(1L, "Macalister");
        Equipo visitante = equipo(2L, "Comark");
        Tournament historico = torneo(1L, TournamentFormat.LEGACY);
        Partido entidad = new Partido();

        when(equipoRepository.findById(1L)).thenReturn(Optional.of(local));
        when(equipoRepository.findById(2L)).thenReturn(Optional.of(visitante));
        when(tournamentService.resolveForNewMatch(null, local, visitante)).thenReturn(historico);
        when(mapper.toEntity(dto)).thenReturn(entidad);
        when(partidoRepository.save(entidad)).thenReturn(entidad);
        when(mapper.toDTO(entidad)).thenReturn(new PartidoDTO());

        partidoService.guardar(dto);

        assertThat(entidad.getTorneo()).isSameAs(historico);
    }

    @Test
    void guardar_conIdTorneo_pasaElIdYLosEquiposRealesAlServicioDeTorneos() {
        PartidoDTO dto = dtoDePartido(1L, 3L, 7L);
        Equipo local = equipo(1L, "Macalister");
        Equipo reemplazo = equipo(3L, "Real Madrid");
        Tournament copa = torneo(7L, TournamentFormat.KNOCKOUT);
        Partido entidad = new Partido();

        when(equipoRepository.findById(1L)).thenReturn(Optional.of(local));
        when(equipoRepository.findById(3L)).thenReturn(Optional.of(reemplazo));
        when(tournamentService.resolveForNewMatch(7L, local, reemplazo)).thenReturn(copa);
        when(mapper.toEntity(dto)).thenReturn(entidad);
        when(partidoRepository.save(entidad)).thenReturn(entidad);
        when(mapper.toDTO(entidad)).thenReturn(new PartidoDTO());

        partidoService.guardar(dto);

        assertThat(entidad.getTorneo()).isSameAs(copa);
        assertThat(entidad.getEquipoVisitante()).isSameAs(reemplazo);
    }

    @Test
    void guardar_torneoNoValidoParaElPartido_noGuardaNada() {
        PartidoDTO dto = dtoDePartido(1L, 2L, 7L);
        Equipo local = equipo(1L, "Macalister");
        Equipo visitante = equipo(2L, "Comark");

        when(equipoRepository.findById(1L)).thenReturn(Optional.of(local));
        when(equipoRepository.findById(2L)).thenReturn(Optional.of(visitante));
        when(tournamentService.resolveForNewMatch(7L, local, visitante))
                .thenThrow(new BusinessException("El equipo Comark no está inscrito en el torneo."));

        assertThatThrownBy(() -> partidoService.guardar(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no está inscrito");

        verify(partidoRepository, never()).save(any());
    }

    private Partido partidoExistente(Tournament torneo, Equipo local, Equipo visitante) {
        Partido existente = new Partido();
        existente.setId(50L);
        existente.setEquipoLocal(local);
        existente.setEquipoVisitante(visitante);
        existente.setTorneo(torneo);
        existente.setFase(Partido.Fase.FASE_DE_GRUPOS);
        existente.setEstado(Partido.Estado.PROGRAMADO);
        return existente;
    }

    private void preparaEdicion(Partido existente, PartidoDTO dto, Equipo local, Equipo visitante) {
        Partido datosNuevos = new Partido();
        datosNuevos.setFecha(dto.getFecha());
        datosNuevos.setHora(dto.getHora());
        datosNuevos.setFase(Partido.Fase.FASE_DE_GRUPOS);
        when(partidoRepository.findById(50L)).thenReturn(Optional.of(existente));
        // lenient: algunos casos (equipo no inscrito, partido de las llaves) fallan antes de leer los equipos o de mapear.
        lenient().when(equipoRepository.findById(local.getId())).thenReturn(Optional.of(local));
        lenient().when(equipoRepository.findById(visitante.getId())).thenReturn(Optional.of(visitante));
        lenient().when(mapper.toEntity(dto)).thenReturn(datosNuevos);
    }

    @Test
    void actualizarPartido_ignoraElIdTorneoDelRequest() {
        Tournament original = torneo(7L, TournamentFormat.LEAGUE);
        Equipo local = equipo(1L, "Macalister");
        Equipo visitante = equipo(2L, "Comark");
        Partido existente = partidoExistente(original, local, visitante);
        PartidoDTO dto = dtoDePartido(1L, 2L, 99L);
        dto.setId(50L);
        preparaEdicion(existente, dto, local, visitante);
        when(partidoRepository.save(existente)).thenReturn(existente);
        when(mapper.toDTO(existente)).thenReturn(new PartidoDTO());

        partidoService.actualizarPartido(dto);

        assertThat(existente.getTorneo()).isSameAs(original);
        verify(tournamentService, never()).validateTeamsEnrolled(any(), any(), any());
    }

    @Test
    void actualizarPartido_reemplazaUnEquipoPorOtroInscrito_validaLaInscripcion() {
        Tournament copa = torneo(7L, TournamentFormat.KNOCKOUT);
        Equipo local = equipo(1L, "Macalister");
        Equipo visitante = equipo(2L, "Comark");
        Equipo reemplazo = equipo(3L, "Real Madrid");
        Partido existente = partidoExistente(copa, local, visitante);
        PartidoDTO dto = dtoDePartido(1L, 3L, null);
        dto.setId(50L);
        preparaEdicion(existente, dto, local, reemplazo);
        when(partidoRepository.save(existente)).thenReturn(existente);
        when(mapper.toDTO(existente)).thenReturn(new PartidoDTO());

        partidoService.actualizarPartido(dto);

        verify(tournamentService).validateTeamsEnrolled(copa, local, reemplazo);
        assertThat(existente.getEquipoVisitante()).isSameAs(reemplazo);
    }

    @Test
    void actualizarPartido_equipoNoInscrito_lanzaBusinessExceptionYNoGuarda() {
        Tournament copa = torneo(7L, TournamentFormat.KNOCKOUT);
        Equipo local = equipo(1L, "Macalister");
        Equipo visitante = equipo(2L, "Comark");
        Equipo intruso = equipo(3L, "Real Madrid");
        Partido existente = partidoExistente(copa, local, visitante);
        PartidoDTO dto = dtoDePartido(1L, 3L, null);
        dto.setId(50L);
        preparaEdicion(existente, dto, local, intruso);
        doThrow(new BusinessException("El equipo Real Madrid no está inscrito en el torneo."))
                .when(tournamentService).validateTeamsEnrolled(copa, local, intruso);

        assertThatThrownBy(() -> partidoService.actualizarPartido(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no está inscrito");

        verify(partidoRepository, never()).save(any());
    }

    @Test
    void actualizarPartido_cambioDeFechaDeUnPartidoGenerado_conservaTorneoYEquipos() {
        // Un partido del calendario generado es un partido normal: se edita igual.
        Tournament liga = torneo(7L, TournamentFormat.LEAGUE);
        Equipo local = equipo(1L, "Macalister");
        Equipo visitante = equipo(2L, "Comark");
        Partido generado = partidoExistente(liga, local, visitante);
        generado.setFecha(LocalDate.of(2027, 3, 6));
        PartidoDTO dto = dtoDePartido(1L, 2L, null);
        dto.setId(50L);
        dto.setFecha(LocalDate.of(2027, 3, 20));
        preparaEdicion(generado, dto, local, visitante);
        when(partidoRepository.save(generado)).thenReturn(generado);
        when(mapper.toDTO(generado)).thenReturn(new PartidoDTO());

        partidoService.actualizarPartido(dto);

        assertThat(generado.getFecha()).isEqualTo(LocalDate.of(2027, 3, 20));
        assertThat(generado.getTorneo()).isSameAs(liga);
        assertThat(generado.getEquipoLocal()).isSameAs(local);
        assertThat(generado.getEquipoVisitante()).isSameAs(visitante);
        verify(tournamentService, never()).validateTeamsEnrolled(any(), any(), any());
    }

    @Test
    void eliminar_partidoGeneradoProgramado_seEliminaComoCualquierOtro() {
        Tournament liga = torneo(7L, TournamentFormat.LEAGUE);
        Partido generado = partidoExistente(liga, equipo(1L, "Macalister"), equipo(2L, "Comark"));
        when(partidoRepository.findById(50L)).thenReturn(Optional.of(generado));

        partidoService.eliminar(50L);

        verify(estadisticasRepository).deleteByPartidoId(50L);
        verify(partidoRepository).deleteById(50L);
    }

    // ------------------------------------------------------------------ penales (add-knockout-and-groups)

    private PenaltiesRequestDTO penales(Integer local, Integer visitante) {
        PenaltiesRequestDTO request = new PenaltiesRequestDTO();
        request.setHomePenalties(local);
        request.setAwayPenalties(visitante);
        return request;
    }

    private Partido partidoEnCurso() {
        Partido partido = partidoExistente(torneo(7L, TournamentFormat.KNOCKOUT), equipo(1L, "Macalister"), equipo(2L, "Comark"));
        partido.setEstado(Partido.Estado.EN_CURSO);
        partido.setGolesLocal(1);
        partido.setGolesVisitante(1);
        return partido;
    }

    @Test
    void registrarPenales_validosEnUnPartidoQueExigeGanador_losGuardaYNoTocaElMarcador() {
        Partido partido = partidoEnCurso();
        when(partidoRepository.findById(50L)).thenReturn(Optional.of(partido));
        when(bracketService.requiresWinner(partido)).thenReturn(true);
        when(partidoRepository.save(partido)).thenReturn(partido);
        when(mapper.toDTO(partido)).thenReturn(new PartidoDTO());

        partidoService.registrarPenales(50L, penales(5, 4));

        assertThat(partido.getPenalesLocal()).isEqualTo(5);
        assertThat(partido.getPenalesVisitante()).isEqualTo(4);
        assertThat(partido.getGolesLocal()).isEqualTo(1);
        assertThat(partido.getGolesVisitante()).isEqualTo(1);
    }

    @Test
    void registrarPenales_iguales_lanzaBusinessExceptionYNoGuarda() {
        Partido partido = partidoEnCurso();
        when(partidoRepository.findById(50L)).thenReturn(Optional.of(partido));
        when(bracketService.requiresWinner(partido)).thenReturn(true);

        assertThatThrownBy(() -> partidoService.registrarPenales(50L, penales(4, 4)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empatados");

        verify(partidoRepository, never()).save(any());
    }

    @Test
    void registrarPenales_enUnPartidoQueNoExigeGanador_lanzaBusinessException() {
        Partido partido = partidoEnCurso();
        when(partidoRepository.findById(50L)).thenReturn(Optional.of(partido));
        when(bracketService.requiresWinner(partido)).thenReturn(false);

        assertThatThrownBy(() -> partidoService.registrarPenales(50L, penales(5, 4)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no usa penales");

        verify(partidoRepository, never()).save(any());
    }

    @Test
    void registrarPenales_enUnPartidoProgramadoOFinalizado_lanzaBusinessException() {
        for (Partido.Estado estado : new Partido.Estado[]{Partido.Estado.PROGRAMADO, Partido.Estado.FINALIZADO}) {
            Partido partido = partidoEnCurso();
            partido.setEstado(estado);
            when(partidoRepository.findById(50L)).thenReturn(Optional.of(partido));

            assertThatThrownBy(() -> partidoService.registrarPenales(50L, penales(5, 4)))
                    .isInstanceOf(BusinessException.class)
                    .hasMessageContaining("en curso");
        }
        verify(partidoRepository, never()).save(any());
    }

    @Test
    void registrarPenales_partidoInexistente_lanzaNotFound() {
        when(partidoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> partidoService.registrarPenales(99L, penales(5, 4)))
                .isInstanceOf(NotFoundException.class);
    }

    // ------------------------------------------------------------------ llaves (add-knockout-and-groups)

    @Test
    void finalizarPartido_quePideGanadorYEstaEmpatadoSinPenales_noLoFinalizaNiAvanza() {
        Partido partido = partidoEnCurso();
        when(partidoRepository.findById(50L)).thenReturn(Optional.of(partido));
        doThrow(new BusinessException("Este partido no puede terminar empatado: registra los penales antes de finalizarlo."))
                .when(bracketService).requireDecided(partido);

        assertThatThrownBy(() -> partidoService.finalizarPartido(50L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("empatado");

        assertThat(partido.getEstado()).isEqualTo(Partido.Estado.EN_CURSO);
        verify(partidoRepository, never()).save(any());
        verify(bracketService, never()).onMatchFinished(any());
    }

    @Test
    void finalizarPartido_conGanador_loFinalizaYDespuesHaceAvanzarLasLlaves() {
        Partido partido = partidoEnCurso();
        partido.setGolesLocal(2);
        when(partidoRepository.findById(50L)).thenReturn(Optional.of(partido));
        when(partidoRepository.save(partido)).thenReturn(partido);
        when(mapper.toDTO(partido)).thenReturn(new PartidoDTO());

        partidoService.finalizarPartido(50L);

        assertThat(partido.getEstado()).isEqualTo(Partido.Estado.FINALIZADO);
        org.mockito.InOrder order = org.mockito.Mockito.inOrder(bracketService, partidoRepository);
        order.verify(bracketService).requireDecided(partido);
        order.verify(partidoRepository).save(partido);
        order.verify(bracketService).onMatchFinished(partido);
    }

    @Test
    void finalizarPartido_unPartidoCualquieraSigueFinalizandoseComoSiempre() {
        Partido partido = partidoEnCurso();
        partido.setFase(Partido.Fase.FASE_DE_GRUPOS);
        when(partidoRepository.findById(50L)).thenReturn(Optional.of(partido));
        when(partidoRepository.save(partido)).thenReturn(partido);
        when(mapper.toDTO(partido)).thenReturn(new PartidoDTO());

        partidoService.finalizarPartido(50L);

        assertThat(partido.getEstado()).isEqualTo(Partido.Estado.FINALIZADO);
    }

    @Test
    void eliminar_unPartidoDeLasLlaves_loRechazaYNoBorraNada() {
        Partido partido = partidoExistente(torneo(7L, TournamentFormat.KNOCKOUT), equipo(1L, "Macalister"), equipo(2L, "Comark"));
        when(partidoRepository.findById(50L)).thenReturn(Optional.of(partido));
        when(bracketService.isBracketMatch(50L)).thenReturn(true);

        assertThatThrownBy(() -> partidoService.eliminar(50L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("llaves");

        verify(partidoRepository, never()).deleteById(any());
        verify(estadisticasRepository, never()).deleteByPartidoId(any());
    }

    @Test
    void actualizarPartido_cambiarLosEquiposDeUnPartidoDeLasLlaves_loRechaza() {
        Tournament copa = torneo(7L, TournamentFormat.KNOCKOUT);
        Equipo local = equipo(1L, "Macalister");
        Equipo visitante = equipo(2L, "Comark");
        Equipo otro = equipo(3L, "Real Madrid");
        Partido deLasLlaves = partidoExistente(copa, local, visitante);
        PartidoDTO dto = dtoDePartido(1L, 3L, null);
        dto.setId(50L);
        preparaEdicion(deLasLlaves, dto, local, otro);
        when(bracketService.isBracketMatch(50L)).thenReturn(true);

        assertThatThrownBy(() -> partidoService.actualizarPartido(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("llaves");

        verify(partidoRepository, never()).save(any());
    }

    @Test
    void actualizarPartido_cambiarLaFechaDeUnPartidoDeLasLlaves_loAcepta() {
        Tournament copa = torneo(7L, TournamentFormat.KNOCKOUT);
        Equipo local = equipo(1L, "Macalister");
        Equipo visitante = equipo(2L, "Comark");
        Partido deLasLlaves = partidoExistente(copa, local, visitante);
        PartidoDTO dto = dtoDePartido(1L, 2L, null);
        dto.setId(50L);
        dto.setFecha(LocalDate.of(2027, 5, 9));
        preparaEdicion(deLasLlaves, dto, local, visitante);
        lenient().when(bracketService.isBracketMatch(50L)).thenReturn(true);
        when(partidoRepository.save(deLasLlaves)).thenReturn(deLasLlaves);
        when(mapper.toDTO(deLasLlaves)).thenReturn(new PartidoDTO());

        partidoService.actualizarPartido(dto);

        assertThat(deLasLlaves.getFecha()).isEqualTo(LocalDate.of(2027, 5, 9));
        assertThat(deLasLlaves.getEquipoLocal()).isSameAs(local);
        assertThat(deLasLlaves.getEquipoVisitante()).isSameAs(visitante);
    }

    @Test
    void partidoDeTorneo_seIniciaFinalizaYCalculaGolesComoCualquierOtro() {
        // No existe una bandera de "partido manual": un partido de torneo, creado
        // a mano, recorre el mismo ciclo y sus goles salen de las estadisticas.
        Tournament copa = torneo(7L, TournamentFormat.KNOCKOUT);
        Equipo local = equipo(1L, "Macalister");
        Equipo visitante = equipo(3L, "Real Madrid");
        Partido partido = partidoExistente(copa, local, visitante);
        when(partidoRepository.findById(50L)).thenReturn(Optional.of(partido));
        when(alineacionRepository.contarTitularesPorEquipo(50L, 1L)).thenReturn(7L);
        when(alineacionRepository.contarTitularesPorEquipo(50L, 3L)).thenReturn(7L);
        when(partidoRepository.save(partido)).thenReturn(partido);
        when(mapper.toDTO(partido)).thenReturn(new PartidoDTO());
        when(estadisticasRepository.sumarGolesPorEquipo(50L, 1L)).thenReturn(2);
        when(estadisticasRepository.sumarGolesPorEquipo(50L, 3L)).thenReturn(1);

        partidoService.iniciarPartido(50L);
        assertThat(partido.getEstado()).isEqualTo(Partido.Estado.EN_CURSO);

        partidoService.recalcularGoles(50L);
        assertThat(partido.getGolesLocal()).isEqualTo(2);
        assertThat(partido.getGolesVisitante()).isEqualTo(1);

        partidoService.finalizarPartido(50L);
        assertThat(partido.getEstado()).isEqualTo(Partido.Estado.FINALIZADO);
    }
}
