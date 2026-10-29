package com.marin.mas_que_amigos.controller;

import com.marin.mas_que_amigos.dto.TransferDTO;
import com.marin.mas_que_amigos.dto.TransferRequestDTO;
import com.marin.mas_que_amigos.security.AuthenticatedUser;
import com.marin.mas_que_amigos.service.PlayerTransferService;
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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transfers")
@Validated
@Tag(name = "Traspasos", description = "Solicitudes de traspaso de jugadores entre equipos y su historial")
public class PlayerTransferController {

    private final PlayerTransferService transferService;

    public PlayerTransferController(PlayerTransferService transferService) {
        this.transferService = transferService;
    }

    @Operation(summary = "Solicitar traspaso",
            description = "Crea una solicitud de traspaso en estado PENDING hacia otro equipo. El jugador no cambia de equipo "
                    + "hasta que un organizador la confirme. El costo es el costo vigente del jugador. "
                    + "Puede haber varias solicitudes pendientes del mismo jugador. Pueden solicitarla un ADMIN (hacia cualquier "
                    + "equipo) y un DT (solo hacia su equipo asignado).")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Solicitud creada",
                content = @Content(schema = @Schema(implementation = TransferDTO.class))),
        @ApiResponse(responseCode = "400", description = "Datos inválidos, equipo inexistente, mismo equipo o dorsal ocupado", content = @Content),
        @ApiResponse(responseCode = "401", description = "Sin sesión o sesión vencida", content = @Content),
        @ApiResponse(responseCode = "403", description = "Un DT sin equipo, o que pide un traspaso hacia un equipo que no es el suyo", content = @Content),
        @ApiResponse(responseCode = "404", description = "El jugador no existe", content = @Content)
    })
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferDTO requestTransfer(@Valid @RequestBody TransferRequestDTO request,
            @AuthenticationPrincipal Jwt jwt) {
        return transferService.requestTransfer(request, AuthenticatedUser.fromJwt(jwt));
    }

    @Operation(summary = "Confirmar traspaso",
            description = "Solo ADMIN. Confirma una solicitud pendiente: el jugador pasa al equipo destino y las demás solicitudes "
                    + "pendientes del mismo jugador se rechazan automáticamente.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Traspaso confirmado",
                content = @Content(schema = @Schema(implementation = TransferDTO.class))),
        @ApiResponse(responseCode = "400", description = "La solicitud ya no está pendiente o el dorsal está ocupado", content = @Content),
        @ApiResponse(responseCode = "404", description = "La solicitud no existe", content = @Content)
    })
    @PostMapping("/{id}/confirm")
    public TransferDTO confirmTransfer(
            @Parameter(description = "Id de la solicitud de traspaso") @PathVariable @Min(1) Long id) {
        return transferService.confirmTransfer(id);
    }

    @Operation(summary = "Rechazar traspaso",
            description = "Solo ADMIN. Rechaza una solicitud pendiente sin modificar al jugador ni las demás solicitudes.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Traspaso rechazado",
                content = @Content(schema = @Schema(implementation = TransferDTO.class))),
        @ApiResponse(responseCode = "400", description = "La solicitud ya no está pendiente", content = @Content),
        @ApiResponse(responseCode = "404", description = "La solicitud no existe", content = @Content)
    })
    @PostMapping("/{id}/reject")
    public TransferDTO rejectTransfer(
            @Parameter(description = "Id de la solicitud de traspaso") @PathVariable @Min(1) Long id) {
        return transferService.rejectTransfer(id);
    }

    @Operation(summary = "Consultar traspasos",
            description = "Lista traspasos, del más reciente al más antiguo, con filtros opcionales. "
                    + "Bandeja de pendientes: status=PENDING. Historial de equipos de un jugador: status=CONFIRMED&playerId={id}.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Listado de traspasos (puede ser vacío)",
                content = @Content(schema = @Schema(implementation = TransferDTO.class))),
        @ApiResponse(responseCode = "400", description = "Estado de filtro inválido", content = @Content)
    })
    @GetMapping
    public List<TransferDTO> findTransfers(
            @Parameter(description = "PENDING, CONFIRMED o REJECTED") @RequestParam(required = false) String status,
            @Parameter(description = "Id del jugador") @RequestParam(required = false) Long playerId) {
        return transferService.findTransfers(status, playerId);
    }
}
