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
                .orElseThrow(() -> new JugadorNotFoundException("Fuera de juego! No se encontró registros de jugador con Dorsal " + dorsal + "."));
        return jugadorMapper.toDTO(jugador);
    }

    public JugadorDTO guardarJugador(JugadorDTO jugadorDTO) {

        Jugador jugador = jugadorMapper.toEntity(jugadorDTO);

        if (jugadorDTO.getIdEquipo() != null) {            
            validarDorsalEnEquipo(jugadorDTO);
            Equipo equipo = equipoRepository.findById(jugadorDTO.getIdEquipo())
                    .orElseThrow(() -> new JugadorNotFoundException("Fuera de juego! No se encontró registros de equipo con Id " + jugadorDTO.getIdEquipo() + "."));
            jugador.setEquipo(equipo); // 🔹 Asignar el equipo antes de guardar
        }

        jugador = jugadorRepository.save(jugador);

        return new JugadorDTO("Success", "Gooool! El jugador " + jugador.getNombre() + "se guardó en la base de datos.");
    }

    public JugadorDTO eliminarJugador(Long id) {
         
        Jugador jugador = jugadorRepository.findById(id)
                .orElseThrow(() -> new JugadorNotFoundException("Fuera de juego! No se encontró registros de jugador con Id "  + id + "."));
       
        jugadorRepository.deleteById(id);
        
        return new JugadorDTO("Success", "Fin del juego! El jugador " + jugador.getNombre() + " ha sido eliminado correctamente de la base de datos.");
    }

    public void validarDorsalEnEquipo(JugadorDTO jugadorDTO) {
        boolean existeDorsal = jugadorRepository.existsByDorsalAndEquipoId(jugadorDTO.getDorsal(), jugadorDTO.getIdEquipo());
        if (existeDorsal) {
            throw new BusinessException("Cambio! El dorsal " + jugadorDTO.getDorsal() + " ya está asignado en el equipo.");
        }       
    }
    
      public JugadorDTO actualizarEquipo(JugadorDTO jugadorDTO) {

         jugadorRepository.findById(jugadorDTO.getId())
                .orElseThrow(() -> new JugadorNotFoundException("El jugador con ID " + jugadorDTO.getId() + " no existe, no se puede actualizar."));

        jugadorRepository.save(jugadorMapper.toEntity(jugadorDTO));
        
         return new JugadorDTO("Success", "Gooool! El jugador " + jugadorDTO.getNombre() + "se guardó en la base de datos.");
    }
}
