package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.NotFoundException;
import com.marin.mas_que_amigos.mapper.EquipoMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final EquipoMapper mapper;

    public EquipoService(EquipoRepository equipoRepository, EquipoMapper equipoMapper) {
        this.equipoRepository = equipoRepository;
        this.mapper = equipoMapper;
    }

    public List<EquipoDTO> listarEquipos() {

        return equipoRepository.findAll()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());

    }

    public EquipoDTO obtenerEquipoPorNombre(String nombre) {

        EquipoDTO rspEquipo = Optional.ofNullable(equipoRepository.findByNombreIgnoreCase(nombre))
                .map(mapper::toDTO) // Si el equipo existe, lo convierte a DTO
                .orElseThrow(() -> {
            return new NotFoundException("Fuera de juego! No se encontró registros de equipo con Nombre " + nombre + ".");
        });

        return rspEquipo;
    }

    public EquipoDTO guardarEquipo(EquipoDTO equipo) {

        if (equipoRepository.existsByNombre(equipo.getNombre())) {
            throw new BusinessException("Tarjeta Amarilla! El Equipo " + equipo.getNombre() + " ya existe en la base de datos.");
        }

        Equipo rspEquipo = equipoRepository.save(mapper.toEntity(equipo));
        return mapper.toRSPDTO(rspEquipo.getId(), "Success", "Gooool! El equipo " + equipo.getNombre() + " se guardó en la base de datos.");

    }

    public EquipoDTO eliminarEquipo(Long id) {

        Equipo rspEquipo = equipoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fuera de juego! El equipo que deseas eliminar no existe en la base de datos"));

        equipoRepository.delete(rspEquipo);

        return mapper.toRSPDTO(id, "Success", "Fin del juego! El equipo " + rspEquipo.getNombre() + " ha sido eliminado correctamente.");
    }

    public EquipoDTO actualizarEquipo(EquipoDTO equipo) {

        if (!equipoRepository.existsById(equipo.getId())) {
            throw new NotFoundException("El equipo " + equipo.getNombre() + " no existe, no se puede actualizar.");
        }

         equipoRepository.save(mapper.toEntity(equipo));

        return mapper.toRSPDTO(equipo.getId(), "Success", "Gooool! El equipo " + equipo.getNombre() + " se guardó en la base de datos.");
    }

}
