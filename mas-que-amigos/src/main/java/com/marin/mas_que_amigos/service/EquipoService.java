package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.EquipoNotFoundException;
import com.marin.mas_que_amigos.mapper.EquipoMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final EquipoMapper equipoMapper;

    public EquipoService(EquipoRepository equipoRepository, EquipoMapper equipoMapper) {
        this.equipoRepository = equipoRepository;
        this.equipoMapper = equipoMapper;
    }

    public List<EquipoDTO> listarEquipos() {

        return equipoRepository.findAll()
                .stream()
                .map(equipoMapper::toDTO)
                .collect(Collectors.toList());

    }

    public EquipoDTO obtenerEquipoPorNombre(String nombre) {

        Equipo equipoGuardado = equipoRepository.findByNombre(nombre);

        if (equipoGuardado == null) {
            throw new EquipoNotFoundException("Fuera de juego! No se encontró registros de equipo con Nombre " + nombre + ".");
        }

        return equipoMapper.toDTO(equipoGuardado);
    }

    public EquipoDTO guardarEquipo(EquipoDTO equipo) {

        Equipo equipoGuardado = equipoRepository.findByNombre(equipo.getNombre());

        if (equipoGuardado != null) {
            throw new BusinessException("Tarjeta Amarilla! El Equipo " + equipo.getNombre() + " ya existe en la base de datos.");
        }

        equipoGuardado = equipoRepository.save(equipoMapper.toEntity(equipo));
        return new EquipoDTO("Success", "Gooool! El equipo " + equipoGuardado.getNombre() + " se guardó en la base de datos.");
    }

    public EquipoDTO eliminarEquipo(Long id) {

        Equipo equipoGuardado = equipoRepository.findById(id)
                .orElseThrow(() -> new EquipoNotFoundException("Fuera de juego! El equipo que deseas eliminar no existe en la base de datos"));

        equipoRepository.deleteById(id);

        return new EquipoDTO("Success", "Fin del juego! El equipo " + equipoGuardado.getNombre() + " ha sido eliminado correctamente de la base de datos.");
    }

    public EquipoDTO actualizarEquipo(EquipoDTO equipoDTO) {

         equipoRepository.findById(equipoDTO.getId())
                .orElseThrow(() -> new EquipoNotFoundException("El equipo con ID " + equipoDTO.getId() + " no existe, no se puede actualizar."));

        equipoRepository.save(equipoMapper.toEntity(equipoDTO));
        
         return new EquipoDTO("Success", "Gooool! El equipo " + equipoDTO.getNombre() + " se guardó en la base de datos.");
    }

}
