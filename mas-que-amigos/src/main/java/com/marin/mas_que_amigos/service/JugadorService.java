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
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class JugadorService {

    private final JugadorRepository jugadorRepository;
    private final JugadorMapper mapper;
    private final EquipoRepository equipoRepository;

    public JugadorService(JugadorRepository jugadorRepository, JugadorMapper mapper, EquipoRepository equipoRepository) {
        this.jugadorRepository = jugadorRepository;
        this.mapper = mapper;
        this.equipoRepository = equipoRepository;
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
        validarEquipo(jugadorDTO.getIdEquipo());       
        validarDorsalDuplicado( jugadorDTO.getDorsal(), jugadorDTO.getIdEquipo(), -1L);  // 🔹 -1 significa "no excluir ningún jugador"

        Jugador jugador = mapper.toEntity(jugadorDTO);
        jugadorRepository.save(jugador);

        return mapper.toRSPDTO("Success", "Gooool! El jugador " + jugadorDTO.getNombre() + " se guardó en la base de datos.");
    }

    public JugadorDTO eliminarJugador(Long id) {

        Jugador rspJugador = jugadorRepository.findById(id)
                .orElseThrow(() -> new JugadorNotFoundException("Fuera de juego! No se encontró registros de jugador con Id " + id + "."));

        jugadorRepository.delete(rspJugador);

        return new JugadorDTO("Success", "Fin del juego! El jugador " + rspJugador.getNombre() + " ha sido eliminado correctamente de la base de datos.");
    }

    public JugadorDTO actualizarJugador(JugadorDTO jugadorDTO) {

        validarEquipo(jugadorDTO.getIdEquipo());
        validarJugadorExiste(jugadorDTO.getId(), jugadorDTO.getIdEquipo());
        validarDorsalDuplicado( jugadorDTO.getDorsal(), jugadorDTO.getIdEquipo(), jugadorDTO.getId());  // 🔹 -1 significa "no excluir ningún jugador"

        Jugador jugador = mapper.toEntity(jugadorDTO);
        jugadorRepository.save(jugador);

        return mapper.toRSPDTO("Success", "Gooool! El jugador " + jugador.getNombre() + " se actualizó en la base de datos.");
    }

    private void validarEquipo(Long idEquipo) {
        if (idEquipo == null || !equipoRepository.existsById(idEquipo)) {
            throw new BusinessException("Fuera de juego! No se ha seleccionado un equipo válido.");
        }
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
