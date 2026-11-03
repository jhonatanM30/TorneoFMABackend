package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.dto.PartidoDTO;
import com.marin.mas_que_amigos.dto.ScheduleRequestDTO;
import com.marin.mas_que_amigos.dto.StandingDTO;
import com.marin.mas_que_amigos.dto.TournamentDTO;
import com.marin.mas_que_amigos.dto.TournamentRequestDTO;
import com.marin.mas_que_amigos.dto.TournamentTeamRequestDTO;
import com.marin.mas_que_amigos.service.TournamentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tournaments")
@Validated
@Tag(name = "Torneos", description = "Torneos con formato definido, inscripción de equipos y sus partidos")
public class TournamentController {

    private final TournamentService tournamentService;

    public TournamentController(TournamentService tournamentService) {
        this.tournamentService = tournamentService;
    }

    @Operation(summary = "Crear torneo",
            description = "Solo ADMIN. Crea un torneo en estado DRAFT. El formato es obligatorio: LEAGUE (liga), "
                    + "GROUPS_KNOCKOUT (fase de grupos y eliminatoria) o KNOCKOUT (eliminatoria directa).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Torneo creado",
                content = @Content(schema = @Schema(implementation = TournamentDTO.class))),
        @ApiResponse(responseCode = "400", description = "Nombre o formato inválido, o nombre repetido", content = @Content),
        @ApiResponse(responseCode = "401", description = "Sin sesión o sesión vencida", content = @Content),
        @ApiResponse(responseCode = "403", description = "El usuario no es administrador", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TournamentDTO create(@Valid @RequestBody TournamentRequestDTO request) {
        return tournamentService.create(request);
    }

    @Operation(summary = "Listar torneos", description = "Consulta pública. Incluye el torneo protegido \"Histórico\".")
    @ApiResponse(responseCode = "200", description = "Listado de torneos ordenado por nombre",
            content = @Content(schema = @Schema(implementation = TournamentDTO.class)))
    @GetMapping
    public List<TournamentDTO> findAll() {
        return tournamentService.findAll();
    }

    @Operation(summary = "Consultar un torneo", description = "Consulta pública por id.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Torneo encontrado",
                content = @Content(schema = @Schema(implementation = TournamentDTO.class))),
        @ApiResponse(responseCode = "404", description = "El torneo no existe", content = @Content)
    })
    @GetMapping("/{id}")
    public TournamentDTO findById(@Parameter(description = "Id del torneo") @PathVariable @Min(1) Long id) {
        return tournamentService.findById(id);
    }

    @Operation(summary = "Editar torneo",
            description = "Solo ADMIN. Cambia el nombre y el formato mientras el torneo está en DRAFT. "
                    + "Un torneo iniciado y el torneo Histórico no se pueden editar.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Torneo actualizado",
                content = @Content(schema = @Schema(implementation = TournamentDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos, torneo ya iniciado o protegido", content = @Content),
        @ApiResponse(responseCode = "404", description = "El torneo no existe", content = @Content)
    })
    @PutMapping("/{id}")
    public TournamentDTO update(@Parameter(description = "Id del torneo") @PathVariable @Min(1) Long id,
            @Valid @RequestBody TournamentRequestDTO request) {
        return tournamentService.update(id, request);
    }

    @Operation(summary = "Eliminar torneo",
            description = "Solo ADMIN. Elimina un torneo en DRAFT junto con sus inscripciones; los equipos no se eliminan.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Torneo eliminado", content = @Content),
        @ApiResponse(responseCode = "400", description = "Torneo ya iniciado o protegido", content = @Content),
        @ApiResponse(responseCode = "404", description = "El torneo no existe", content = @Content)
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@Parameter(description = "Id del torneo") @PathVariable @Min(1) Long id) {
        tournamentService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Iniciar torneo",
            description = "Solo ADMIN. Pasa un torneo DRAFT con al menos 2 equipos inscritos a IN_PROGRESS; "
                    + "desde ahí el formato y los equipos inscritos quedan bloqueados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Torneo iniciado",
                content = @Content(schema = @Schema(implementation = TournamentDTO.class))),
        @ApiResponse(responseCode = "400", description = "Menos de 2 equipos, no está en DRAFT o es el Histórico", content = @Content),
        @ApiResponse(responseCode = "404", description = "El torneo no existe", content = @Content)
    })
    @PostMapping("/{id}/start")
    public TournamentDTO start(@Parameter(description = "Id del torneo") @PathVariable @Min(1) Long id) {
        return tournamentService.start(id);
    }

    @Operation(summary = "Finalizar torneo",
            description = "Solo ADMIN. Pasa un torneo IN_PROGRESS a FINISHED; todos sus partidos deben estar finalizados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Torneo finalizado",
                content = @Content(schema = @Schema(implementation = TournamentDTO.class))),
        @ApiResponse(responseCode = "400", description = "Partidos pendientes, no está en curso o es el Histórico", content = @Content),
        @ApiResponse(responseCode = "404", description = "El torneo no existe", content = @Content)
    })
    @PostMapping("/{id}/finish")
    public TournamentDTO finish(@Parameter(description = "Id del torneo") @PathVariable @Min(1) Long id) {
        return tournamentService.finish(id);
    }

    @Operation(summary = "Equipos inscritos", description = "Consulta pública. Equipos inscritos en el torneo, ordenados por nombre.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Equipos inscritos (puede ser vacío)",
                content = @Content(schema = @Schema(implementation = EquipoDTO.class))),
        @ApiResponse(responseCode = "404", description = "El torneo no existe", content = @Content)
    })
    @GetMapping("/{id}/teams")
    public List<EquipoDTO> findTeams(@Parameter(description = "Id del torneo") @PathVariable @Min(1) Long id) {
        return tournamentService.findTeams(id);
    }

    @Operation(summary = "Inscribir equipo",
            description = "Solo ADMIN. Inscribe un equipo en un torneo DRAFT. Un equipo puede estar en varios torneos, pero una sola vez en cada uno.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Equipo inscrito",
                content = @Content(schema = @Schema(implementation = EquipoDTO.class))),
        @ApiResponse(responseCode = "400", description = "Equipo inexistente o ya inscrito, torneo iniciado o Histórico", content = @Content),
        @ApiResponse(responseCode = "404", description = "El torneo no existe", content = @Content)
    })
    @PostMapping("/{id}/teams")
    @ResponseStatus(HttpStatus.CREATED)
    public EquipoDTO enrollTeam(@Parameter(description = "Id del torneo") @PathVariable @Min(1) Long id,
            @Valid @RequestBody TournamentTeamRequestDTO request) {
        return tournamentService.enrollTeam(id, request.getTeamId());
    }

    @Operation(summary = "Retirar equipo",
            description = "Solo ADMIN. Quita la inscripción del equipo en este torneo DRAFT; el equipo no se elimina.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Inscripción retirada", content = @Content),
        @ApiResponse(responseCode = "400", description = "Torneo iniciado o Histórico", content = @Content),
        @ApiResponse(responseCode = "404", description = "El torneo no existe o el equipo no está inscrito", content = @Content)
    })
    @DeleteMapping("/{id}/teams/{teamId}")
    public ResponseEntity<Void> withdrawTeam(@Parameter(description = "Id del torneo") @PathVariable @Min(1) Long id,
            @Parameter(description = "Id del equipo") @PathVariable @Min(1) Long teamId) {
        tournamentService.withdrawTeam(id, teamId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Generar calendario de liga",
            description = "Solo ADMIN. Crea los partidos todos contra todos (una vuelta) de un torneo LEAGUE en curso y sin partidos. "
                    + "Cada jornada tiene su propia fecha: la primera es startDate y cada siguiente intervalDays después (7 por defecto, "
                    + "de 1 a 30). Con equipos impares, uno descansa en cada jornada. Los partidos nacen PROGRAMADO y son partidos "
                    + "normales (se pueden editar, eliminar y combinar con partidos manuales). Es atómico: si un equipo ya tiene "
                    + "partido en alguna fecha, no se crea ninguno.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Partidos creados",
                content = @Content(schema = @Schema(implementation = PartidoDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos, torneo que no es de liga o no está en curso, "
                + "sin equipos suficientes, ya tiene partidos, o un equipo ya juega en una fecha de jornada", content = @Content),
        @ApiResponse(responseCode = "401", description = "Sin sesión o sesión vencida", content = @Content),
        @ApiResponse(responseCode = "403", description = "El usuario no es administrador", content = @Content),
        @ApiResponse(responseCode = "404", description = "El torneo no existe", content = @Content)
    })
    @PostMapping("/{id}/schedule")
    @ResponseStatus(HttpStatus.CREATED)
    public List<PartidoDTO> generateSchedule(@Parameter(description = "Id del torneo") @PathVariable @Min(1) Long id,
            @Valid @RequestBody ScheduleRequestDTO request) {
        return tournamentService.generateSchedule(id, request);
    }

    @Operation(summary = "Tabla de posiciones",
            description = "Consulta pública, solo para torneos LEAGUE. Se calcula en cada consulta con los partidos FINALIZADO del "
                    + "torneo (los partidos armados a mano cuentan igual): 3 puntos por victoria, 1 por empate. Desempate: enfrentamiento "
                    + "directo (mini tabla entre los empatados), diferencia de goles, goles a favor, juego limpio (amarilla 1, roja 2; menos "
                    + "es mejor) y nombre. Incluye a todos los equipos inscritos, aunque no hayan jugado.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Tabla ordenada por posición (1..n)",
                content = @Content(schema = @Schema(implementation = StandingDTO.class))),
        @ApiResponse(responseCode = "400", description = "El torneo no es de formato LEAGUE", content = @Content),
        @ApiResponse(responseCode = "404", description = "El torneo no existe", content = @Content)
    })
    @GetMapping("/{id}/standings")
    public List<StandingDTO> standings(@Parameter(description = "Id del torneo") @PathVariable @Min(1) Long id) {
        return tournamentService.standings(id);
    }

    @Operation(summary = "Partidos del torneo",
            description = "Consulta pública. Partidos del torneo ordenados por fecha y hora. Para crear un partido "
                    + "(incluido uno armado a mano) se usa POST /api/partidos con idTorneo.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Partidos del torneo (puede ser vacío)",
                content = @Content(schema = @Schema(implementation = PartidoDTO.class))),
        @ApiResponse(responseCode = "404", description = "El torneo no existe", content = @Content)
    })
    @GetMapping("/{id}/matches")
    public List<PartidoDTO> findMatches(@Parameter(description = "Id del torneo") @PathVariable @Min(1) Long id) {
        return tournamentService.findMatches(id);
    }
}
