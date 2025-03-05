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

@Service
public class JugadorService {

    private final JugadorRepository jugadorRepository;
    private final JugadorMapper jugadorMapper;
    private final EquipoRepository equipoRepository;

    public JugadorService(JugadorRepository jugadorRepository, JugadorMapper jugadorMapper, EquipoRepository equipoRepository) {
        this.jugadorRepository = jugadorRepository;
        this.jugadorMapper = jugadorMapper;
        this.equipoRepository = equipoRepository;
    }

    public List<JugadorDTO> listarJugadores() {
        return jugadorRepository.findAll()
                .stream()
                .map(jugadorMapper::toDTO)
                .collect(Collectors.toList());
    }

    public JugadorDTO obtenerJugadorPorId(int dorsal) {

        Jugador jugador = jugadorRepository.findByDorsal(dorsal)
                .orElseThrow(() -> new JugadorNotFoundException("El jugador con ID " + dorsal + " no fue encontrado."));
        return jugadorMapper.toDTO(jugador);
    }

    public JugadorDTO guardarJugador(JugadorDTO jugadorDTO) {

        Jugador jugador = jugadorMapper.toEntity(jugadorDTO);

        if (jugadorDTO.getIdEquipo() != null) {            
            validarDorsalEnEquipo(jugadorDTO);
            Equipo equipo = equipoRepository.findById(jugadorDTO.getIdEquipo())
                    .orElseThrow(() -> new RuntimeException("Equipo no encontrado con ID: " + jugadorDTO.getIdEquipo()));
            jugador.setEquipo(equipo); // 🔹 Asignar el equipo antes de guardar
        }

        jugador = jugadorRepository.save(jugador);

        return jugadorMapper.toDTO(jugador);
    }

    public void eliminarJugador(Long id) {
         Jugador jugador = jugadorRepository.findById(id)
                .orElseThrow(() -> new JugadorNotFoundException("El jugador con ID No existe, no se puede eliminar"));
       
        jugadorRepository.deleteById(id);
    }

    public void validarDorsalEnEquipo(JugadorDTO jugadorDTO) {
        boolean existeDorsal = jugadorRepository.existsByDorsalAndEquipoId(jugadorDTO.getDorsal(), jugadorDTO.getIdEquipo());
        if (existeDorsal) {
            throw new BusinessException("El dorsal " + jugadorDTO.getDorsal() + " ya está asignado en el equipo.");
        }       
    }
}
