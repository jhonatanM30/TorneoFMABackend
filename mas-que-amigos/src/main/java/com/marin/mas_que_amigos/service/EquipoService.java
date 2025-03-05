package com.marin.mas_que_amigos.service;



import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.EquipoNotFoundException;
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
        
        if(equipoGuardado == null){
            throw new EquipoNotFoundException("El equipo con Nombre " + nombre + " No existe.");
        }
                
        return equipoMapper.toDTO(equipoGuardado);
    }

    public EquipoDTO guardarEquipo(EquipoDTO equipo) {  
        
        Equipo equipoGuardado = equipoRepository.findByNombre(equipo.getNombre());  
        
        if(equipoGuardado != null){
            throw new BusinessException("El Equipo " + equipo.getNombre() + " ya existe.");
        }
        
        equipoGuardado = equipoRepository.save(equipoMapper.toEntity(equipo));
        return equipoMapper.toDTO(equipoGuardado);
    }

    public void eliminarEquipo(Long id) {
        
        Optional<Equipo> equipoGuardado = equipoRepository.findById(id);
        
        if(equipoGuardado == null){
            throw new EquipoNotFoundException("El equipo No existe, no se pudo eliminar");
        }
        equipoRepository.deleteById(id);
    }
       
}
