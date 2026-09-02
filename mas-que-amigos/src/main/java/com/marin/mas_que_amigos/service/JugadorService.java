package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.JugadorDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.JugadorNotFoundException;
import com.marin.mas_que_amigos.mapper.JugadorMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.JugadorRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class JugadorService {

    private final JugadorRepository jugadorRepository;
    private final EquipoRepository equipoRepository;
    private final JugadorMapper mapper;

    @Autowired
    private ValidationCommonService validacionService;

    public JugadorService(JugadorRepository jugadorRepository, EquipoRepository equipoRepository, JugadorMapper mapper) {
        this.jugadorRepository = jugadorRepository;
        this.equipoRepository = equipoRepository;
        this.mapper = mapper;
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
