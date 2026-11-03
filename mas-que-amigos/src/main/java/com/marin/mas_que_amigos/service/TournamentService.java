package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.dto.TournamentDTO;
import com.marin.mas_que_amigos.dto.TournamentRequestDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.NotFoundException;
import com.marin.mas_que_amigos.mapper.EquipoMapper;
import com.marin.mas_que_amigos.mapper.PartidoMapper;
import com.marin.mas_que_amigos.mapper.TournamentMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Partido;
import com.marin.mas_que_amigos.model.Tournament;
import com.marin.mas_que_amigos.model.TournamentFormat;
import com.marin.mas_que_amigos.model.TournamentStatus;
import com.marin.mas_que_amigos.model.TournamentTeam;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.PartidoRepository;
import com.marin.mas_que_amigos.repository.TournamentRepository;
import com.marin.mas_que_amigos.repository.TournamentTeamRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TournamentService {

    private static final int MINIMUM_TEAMS_TO_START = 2;

    private final TournamentRepository tournamentRepository;
    private final TournamentTeamRepository tournamentTeamRepository;
    private final EquipoRepository equipoRepository;
    private final PartidoRepository partidoRepository;
    private final TournamentMapper mapper;
    private final EquipoMapper equipoMapper;
    private final PartidoMapper partidoMapper;

    public TournamentService(TournamentRepository tournamentRepository,
            TournamentTeamRepository tournamentTeamRepository, EquipoRepository equipoRepository,
            PartidoRepository partidoRepository, TournamentMapper mapper, EquipoMapper equipoMapper,
            PartidoMapper partidoMapper) {
        this.tournamentRepository = tournamentRepository;
        this.tournamentTeamRepository = tournamentTeamRepository;
        this.equipoRepository = equipoRepository;
        this.partidoRepository = partidoRepository;
        this.mapper = mapper;
        this.equipoMapper = equipoMapper;
        this.partidoMapper = partidoMapper;
    }

    /**
     * Creates a tournament in DRAFT. The format is mandatory and must be one of
     * the three real formats; LEGACY is internal and cannot be chosen.
     */
    @Transactional
    public TournamentDTO create(TournamentRequestDTO request) {
        TournamentFormat format = parseFormat(request.getFormat());
        String name = request.getName().trim();
        if (tournamentRepository.existsByNameIgnoreCase(name)) {
            throw nameTaken(name);
        }

        Tournament tournament = new Tournament();
        tournament.setName(name);
        tournament.setFormat(format);
        tournament.setStatus(TournamentStatus.DRAFT);
        tournament.setCreatedAt(LocalDateTime.now());

        return toDTO(tournamentRepository.save(tournament));
    }

    @Transactional(readOnly = true)
    public List<TournamentDTO> findAll() {
        return tournamentRepository.findAllByOrderByNameAsc().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TournamentDTO findById(Long id) {
        return toDTO(tournamentRepository.findById(id).orElseThrow(() -> tournamentNotFound(id)));
    }

    /**
     * Changes name and format of a tournament that has not started yet.
     */
    @Transactional
    public TournamentDTO update(Long id, TournamentRequestDTO request) {
        Tournament tournament = lockTournament(id);
        requireNotLegacy(tournament);
        requireDraft(tournament);

        TournamentFormat format = parseFormat(request.getFormat());
        String name = request.getName().trim();
        if (tournamentRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw nameTaken(name);
        }

        tournament.setName(name);
        tournament.setFormat(format);
        return toDTO(tournamentRepository.save(tournament));
    }

    /**
     * Deletes a tournament that has not started yet, together with its
     * enrollments. The teams themselves are never touched.
     */
    @Transactional
    public void delete(Long id) {
        Tournament tournament = lockTournament(id);
        requireNotLegacy(tournament);
        requireDraft(tournament);

        tournamentTeamRepository.deleteByTournamentId(id);
        tournamentRepository.delete(tournament);
    }

    /**
     * Enrolls a team in a tournament that has not started yet. A team may be
     * enrolled in several tournaments, but only once in each one.
     */
    @Transactional
    public EquipoDTO enrollTeam(Long tournamentId, Long teamId) {
        Tournament tournament = lockTournament(tournamentId);
        requireEnrollmentAllowed(tournament);

        Equipo team = equipoRepository.findById(teamId)
                .orElseThrow(() -> new BusinessException("Fuera de juego! No se ha seleccionado un o unos equipos existentes."));
        if (tournamentTeamRepository.existsByTournamentIdAndTeamId(tournamentId, teamId)) {
            throw new BusinessException("El equipo " + team.getNombre() + " ya está inscrito en este torneo.");
        }

        TournamentTeam enrollment = new TournamentTeam();
        enrollment.setTournament(tournament);
        enrollment.setTeam(team);
        enrollment.setEnrolledAt(LocalDateTime.now());
        tournamentTeamRepository.save(enrollment);

        return equipoMapper.toDTO(team);
    }

    /**
     * Removes the enrollment of a team in this tournament only; the team and its
     * enrollments in other tournaments are untouched.
     */
    @Transactional
    public void withdrawTeam(Long tournamentId, Long teamId) {
        Tournament tournament = lockTournament(tournamentId);
        requireEnrollmentAllowed(tournament);

        TournamentTeam enrollment = tournamentTeamRepository.findByTournamentIdAndTeamId(tournamentId, teamId)
                .orElseThrow(() -> new NotFoundException("Fuera de juego! El equipo no está inscrito en este torneo."));
        tournamentTeamRepository.delete(enrollment);
    }

    @Transactional(readOnly = true)
    public List<EquipoDTO> findTeams(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw tournamentNotFound(tournamentId);
        }
        return tournamentTeamRepository.findTeamsByTournamentId(tournamentId).stream()
                .map(equipoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Moves a DRAFT tournament to IN_PROGRESS. From here on its format and its
     * enrolled teams are locked.
     */
    @Transactional
    public TournamentDTO start(Long id) {
        Tournament tournament = lockTournament(id);
        requireNotLegacy(tournament);
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new BusinessException("Solo se puede iniciar un torneo en borrador (estado actual: "
                    + tournament.getStatus() + ").");
        }
        long teamCount = tournamentTeamRepository.countByTournamentId(id);
        if (teamCount < MINIMUM_TEAMS_TO_START) {
            throw new BusinessException("Para iniciar el torneo se necesitan al menos " + MINIMUM_TEAMS_TO_START
                    + " equipos inscritos (hay " + teamCount + ").");
        }

        tournament.setStatus(TournamentStatus.IN_PROGRESS);
        tournament.setStartedAt(LocalDateTime.now());
        return toDTO(tournamentRepository.save(tournament));
    }

    /**
     * Closes an IN_PROGRESS tournament. Every match must be finished first: a
     * postponed match is either played or deleted before closing.
     */
    @Transactional
    public TournamentDTO finish(Long id) {
        Tournament tournament = lockTournament(id);
        requireNotLegacy(tournament);
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new BusinessException("El torneo no está en curso: solo se puede finalizar un torneo en curso (estado actual: "
                    + tournament.getStatus() + ").");
        }
        long pending = partidoRepository.countByTorneoIdAndEstadoNot(id, Partido.Estado.FINALIZADO);
        if (pending > 0) {
            throw new BusinessException("No se puede finalizar el torneo: faltan por finalizar " + pending
                    + " partido(s). Juégalos o elimínalos primero.");
        }

        tournament.setStatus(TournamentStatus.FINISHED);
        return toDTO(tournamentRepository.save(tournament));
    }

    @Transactional(readOnly = true)
    public List<PartidoDTO> findMatches(Long tournamentId) {
        if (!tournamentRepository.existsById(tournamentId)) {
            throw tournamentNotFound(tournamentId);
        }
        return partidoRepository.findByTorneoIdOrderByFechaAscHoraAsc(tournamentId).stream()
                .map(partidoMapper::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Decides the tournament of a new match. Without an id the match goes to the
     * protected "Histórico" tournament; otherwise the tournament must exist, be
     * IN_PROGRESS and have both teams enrolled. Any pair of enrolled teams is
     * valid in every format, which is what allows hand-made matches (for
     * example A vs C when A vs B is postponed).
     *
     * Reads the tournament with a lock, so the caller must run inside a
     * transaction that also saves the match: that way a match cannot slip into
     * a tournament that is being finished at the same moment.
     */
    public Tournament resolveForNewMatch(Long tournamentId, Equipo local, Equipo visitor) {
        if (tournamentId == null) {
            return tournamentRepository.findFirstByFormat(TournamentFormat.LEGACY)
                    .orElseThrow(() -> new IllegalStateException("No existe el torneo Histórico (formato LEGACY)."));
        }

        Tournament tournament = tournamentRepository.findByIdForUpdate(tournamentId)
                .orElseThrow(() -> new BusinessException("Fuera de juego! El torneo indicado no existe."));
        if (tournament.getFormat() == TournamentFormat.LEGACY) {
            return tournament;
        }
        if (tournament.getStatus() != TournamentStatus.IN_PROGRESS) {
            throw new BusinessException("El torneo no está en curso: solo se pueden crear partidos en un torneo en curso (estado actual: "
                    + tournament.getStatus() + ").");
        }
        validateTeamsEnrolled(tournament, local, visitor);
        return tournament;
    }

    /**
     * Both teams must be enrolled, except in the "Histórico" tournament, which
     * does not use enrollment.
     */
    public void validateTeamsEnrolled(Tournament tournament, Equipo local, Equipo visitor) {
        if (tournament.getFormat() == TournamentFormat.LEGACY) {
            return;
        }
        for (Equipo team : new Equipo[]{local, visitor}) {
            if (!tournamentTeamRepository.existsByTournamentIdAndTeamId(tournament.getId(), team.getId())) {
                throw new BusinessException("El equipo " + team.getNombre() + " no está inscrito en el torneo "
                        + tournament.getName() + ".");
            }
        }
    }

    // ------------------------------------------------------------------ helpers

    private Tournament lockTournament(Long id) {
        return tournamentRepository.findByIdForUpdate(id).orElseThrow(() -> tournamentNotFound(id));
    }

    // The "Histórico" tournament (internal LEGACY format) keeps the old matches
    // and cannot be edited, deleted, started or finished.
    private void requireNotLegacy(Tournament tournament) {
        if (tournament.getFormat() == TournamentFormat.LEGACY) {
            throw new BusinessException("El torneo Histórico es un torneo protegido: no se puede editar, eliminar, iniciar ni finalizar.");
        }
    }

    private void requireEnrollmentAllowed(Tournament tournament) {
        if (tournament.getFormat() == TournamentFormat.LEGACY) {
            throw new BusinessException("El torneo Histórico no usa inscripción de equipos.");
        }
        requireDraft(tournament);
    }

    private void requireDraft(Tournament tournament) {
        if (tournament.getStatus() != TournamentStatus.DRAFT) {
            throw new BusinessException("El torneo ya inició: solo se puede hacer esto mientras está en borrador.");
        }
    }

    private TournamentFormat parseFormat(String format) {
        try {
            TournamentFormat parsed = TournamentFormat.valueOf(format.trim().toUpperCase());
            if (parsed != TournamentFormat.LEGACY) {
                return parsed;
            }
        } catch (IllegalArgumentException ex) {
            // falls through to the common error below
        }
        throw new BusinessException("Formato de torneo inválido: " + format
                + ". Los formatos permitidos son LEAGUE, GROUPS_KNOCKOUT y KNOCKOUT.");
    }

    private TournamentDTO toDTO(Tournament tournament) {
        return mapper.toDTO(tournament, tournamentTeamRepository.countByTournamentId(tournament.getId()));
    }

    private NotFoundException tournamentNotFound(Long id) {
        return new NotFoundException("Fuera de juego! El torneo con Id " + id + " no existe.");
    }

    private BusinessException nameTaken(String name) {
        return new BusinessException("Ya existe un torneo con el nombre \"" + name + "\".");
    }
}
