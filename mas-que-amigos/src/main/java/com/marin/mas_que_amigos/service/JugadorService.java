package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.JugadorBatchItemResultDTO;
import com.marin.mas_que_amigos.dto.JugadorBatchResponseDTO;
import com.marin.mas_que_amigos.dto.JugadorDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.JugadorNotFoundException;
import com.marin.mas_que_amigos.mapper.JugadorMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.JugadorRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JugadorService {

    // Tope defensivo del lote: evita peticiones desproporcionadas (payloads
    // enormes) sin ser tan bajo que estorbe el caso real de uso (cargar la
    // plantilla completa de un equipo, ~20-30 jugadores).
    private static final int TAMANO_MAXIMO_LOTE = 50;

    private final JugadorRepository jugadorRepository;
    private final EquipoRepository equipoRepository;
    private final JugadorMapper mapper;
    private final Validator validator;

    @Autowired
    private ValidationCommonService validacionService;

    public JugadorService(JugadorRepository jugadorRepository, EquipoRepository equipoRepository, JugadorMapper mapper, Validator validator) {
        this.jugadorRepository = jugadorRepository;
        this.equipoRepository = equipoRepository;
        this.mapper = mapper;
        this.validator = validator;
    }

    public List<JugadorDTO> listarJugadores() {
        return jugadorRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public List<JugadorDTO> obtenerJugadorPorNombre(String nombre) {

        List<Jugador> jugadores = jugadorRepository.findByNombreContainingIgnoreCase(nombre);

        if (jugadores.isEmpty()) {
            throw new BusinessException("No se encontraron jugadores con el nombre: " + nombre);
        }

        return jugadores.stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public JugadorDTO guardarJugador(JugadorDTO jugadorDTO) {       
        
        // Se resuelve la entidad Equipo real (ya persistida) en vez de dejar que
        // el mapper construya un Equipo "shell" con solo el id: así el jugador
        // guardado queda con la asociación completamente hidratada y la
        // respuesta incluye los datos reales del equipo, no campos en null.
        Equipo equipoReal = equipoRepository.findById(jugadorDTO.getIdEquipo())
                .orElseThrow(() -> new BusinessException("Fuera de juego! No se ha seleccionado un o unos equipos existentes."));

        validarDorsalDuplicado(jugadorDTO.getDorsal(), jugadorDTO.getIdEquipo(), -1L);  // 🔹 -1 significa "no excluir ningún jugador"        
        Jugador jugador = mapper.toEntity(jugadorDTO);
        jugador.setEquipo(equipoReal);
        Jugador guardado = jugadorRepository.save(jugador);

        JugadorDTO respuesta = mapper.toDTO(guardado);
        respuesta.setMensaje("Gooool! El jugador " + guardado.getNombre() + " se guardó en la base de datos.");
        return respuesta;
    }

    /**
     * Crea varios jugadores en una sola petición con procesamiento
     * "optimista": cada jugador se valida y se guarda de forma
     * independiente, en el orden recibido. Si uno falla (datos inválidos,
     * equipo inexistente, dorsal duplicado), no afecta a los demás - el
     * lote sigue procesando el resto y el resultado detalla éxito/fallo por
     * cada posición del arreglo original.
     *
     * Importante para que el "sigue con los demás" funcione de verdad: este
     * método deliberadamente NO es @Transactional. jugadorRepository.save()
     * confirma cada jugador en su propia transacción (comportamiento por
     * defecto de Spring Data JPA); si este método fuera transaccional, una
     * excepción en el jugador 5 revertiría también a los jugadores 1-4 ya
     * guardados, que es justo lo que no queremos.
     *
     * La verificación de dorsal duplicado se reconsulta contra la base de
     * datos en cada jugador del lote (no una sola vez al principio): como
     * cada guardado exitoso se confirma antes de procesar el siguiente,
     * esto detecta automáticamente dos jugadores del mismo lote que
     * compitan por el mismo dorsal en el mismo equipo, sin necesitar lógica
     * de deduplicación aparte.
     */
    public JugadorBatchResponseDTO guardarJugadoresEnLote(List<JugadorDTO> jugadores) {

        if (jugadores == null || jugadores.isEmpty()) {
            throw new BusinessException("El lote de jugadores no puede estar vacío.");
        }
        if (jugadores.size() > TAMANO_MAXIMO_LOTE) {
            throw new BusinessException("El lote no puede tener más de " + TAMANO_MAXIMO_LOTE
                    + " jugadores (se recibieron " + jugadores.size() + ").");
        }

        List<JugadorBatchItemResultDTO> resultados = new ArrayList<>();
        int exitosos = 0;

        for (int indice = 0; indice < jugadores.size(); indice++) {
            JugadorDTO dto = jugadores.get(indice);
            try {
                // Bean validation manual: al llamar al servicio directamente
                // (sin pasar por @Valid del controlador) hay que validar cada
                // jugador aquí, para que uno con datos inválidos no aborte el
                // resto de la petición.
                Set<ConstraintViolation<JugadorDTO>> violaciones = validator.validate(dto);
                if (!violaciones.isEmpty()) {
                    String mensajeError = violaciones.stream()
                            .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                            .collect(Collectors.joining("; "));
                    resultados.add(new JugadorBatchItemResultDTO(indice, false, null, mensajeError));
                    continue;
                }

                JugadorDTO creado = guardarJugador(dto);
                resultados.add(new JugadorBatchItemResultDTO(indice, true, creado, null));
                exitosos++;
            } catch (RuntimeException ex) {
                // Cubre BusinessException (equipo inexistente, dorsal
                // duplicado) y cualquier otro error en tiempo de ejecución
                // (por ejemplo una violación de la restricción UNIQUE de BD
                // ante una carrera con otra petición concurrente): el lote
                // sigue con el siguiente jugador en vez de abortar.
                resultados.add(new JugadorBatchItemResultDTO(indice, false, null, ex.getMessage()));
            }
        }

        return new JugadorBatchResponseDTO(jugadores.size(), exitosos, jugadores.size() - exitosos, resultados);
    }

    public void eliminarJugador(Long id) {

        Jugador rspJugador = jugadorRepository.findById(id)
                .orElseThrow(() -> new JugadorNotFoundException("Fuera de juego! No se encontró registros de jugador con Id " + id + "."));

        jugadorRepository.delete(rspJugador);
    }

    public JugadorDTO actualizarJugador(JugadorDTO jugadorDTO) {

        validacionService.validarEquipo(jugadorDTO.getIdEquipo());
        validarJugadorExiste(jugadorDTO.getId(), jugadorDTO.getIdEquipo());
        validarDorsalDuplicado(jugadorDTO.getDorsal(), jugadorDTO.getIdEquipo(), jugadorDTO.getId());  // 🔹 -1 significa "no excluir ningún jugador"

        // Igual que en Equipo: se carga la entidad administrada existente y solo
        // se mutan sus campos, en vez de guardar una entidad nueva sin id (lo que
        // provocaba un INSERT duplicado en cada "actualización" en vez de un UPDATE).
        Jugador jugadorExistente = jugadorRepository.findById(jugadorDTO.getId())
                .orElseThrow(() -> new JugadorNotFoundException("Fuera de juego! No se encontró registros de jugador con Id " + jugadorDTO.getId() + "."));

        Equipo equipoReal = equipoRepository.findById(jugadorDTO.getIdEquipo())
                .orElseThrow(() -> new BusinessException("Fuera de juego! No se ha seleccionado un o unos equipos existentes."));

        jugadorExistente.setNombre(jugadorDTO.getNombre());
        jugadorExistente.setPosicion(jugadorDTO.getPosicion());
        jugadorExistente.setEdad(jugadorDTO.getEdad());
        jugadorExistente.setDorsal(jugadorDTO.getDorsal());
        jugadorExistente.setEquipo(equipoReal);

        Jugador actualizado = jugadorRepository.save(jugadorExistente);

        JugadorDTO respuesta = mapper.toDTO(actualizado);
        respuesta.setMensaje("Gooool! El jugador " + actualizado.getNombre() + " se actualizó en la base de datos.");
        return respuesta;
    }

    private void validarJugadorExiste(Long idJugador, Long idEquipo) {
        if (!jugadorRepository.existsByIdAndEquipoId(idJugador, idEquipo)) {
            throw new BusinessException("Falta! El jugador no existe en este equipo.");
        }
    }

    private void validarDorsalDuplicado(int dorsal, Long idEquipo, Long idJugador) {
        if (jugadorRepository.existsByDorsalAndEquipoIdAndIdNot(dorsal, idEquipo, idJugador)) {
            throw new BusinessException("Cambio! El dorsal " + dorsal + " ya está asignado en el equipo.");
        }
    }

}
