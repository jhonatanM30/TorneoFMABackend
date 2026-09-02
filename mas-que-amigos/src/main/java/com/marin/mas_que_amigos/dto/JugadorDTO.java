package com.marin.mas_que_amigos.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.marin.mas_que_amigos.model.Jugador.Posicion;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor  // Constructor vacío necesario para serialización
@AllArgsConstructor // Constructor con todos los campos
public class JugadorDTO {

    private Long id;

    @NotBlank(message = "El nombre del jugador es obligatorio.")
    private String nombre;

    @NotNull(message = "La posición del jugador es obligatoria.")
    private Posicion posicion;

    @Min(value = 1, message = "La edad debe ser mayor a 0.")
    private int edad;

    @Min(value = 1, message = "El dorsal debe ser mayor a 0.")
    private int dorsal;

    private String indicadorRespuesta;
    private String mensaje;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Long idEquipo;

    // 🔹 Solo se completa al responder (JugadorMapper.toDTO); el servidor
    // nunca lo lee al crear/editar un jugador (se usa idEquipo para eso), así
    // que se marca de solo lectura. Esto es justo lo que provocaba el error
    // de deserialización al enviar "equipo": "Macalister" como texto: ahora
    // Jackson simplemente ignora ese campo en el request en vez de fallar.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private EquipoDTO equipo;

    public JugadorDTO(String indicadorRespuesta, String mensaje) {
        this.indicadorRespuesta = indicadorRespuesta;
        this.mensaje = mensaje;
    }
}
