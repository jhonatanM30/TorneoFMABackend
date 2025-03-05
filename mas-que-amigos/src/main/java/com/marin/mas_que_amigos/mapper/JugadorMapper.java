package com.marin.mas_que_amigos.mapper;

import com.marin.mas_que_amigos.dto.JugadorDTO;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Jugador;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class JugadorMapper {

     private final EquipoMapper equipoMapper;

    // Inyección con @Lazy para evitar la referencia circular
    public JugadorMapper(@Lazy EquipoMapper equipoMapper) {
        this.equipoMapper = equipoMapper;
    }
    
    public JugadorDTO toDTO(Jugador jugador) {
    
    JugadorDTO dto = new JugadorDTO();
    
    dto.setId(jugador.getId());
    dto.setNombre(jugador.getNombre());
    dto.setPosicion(jugador.getPosicion());
    dto.setDorsal(jugador.getDorsal());
    dto.setEdad(jugador.getEdad());

    // Solo incluir el ID del equipo en vez de toda la entidad para evitar recursión infinita
    dto.setIdEquipo(jugador.getEquipo().getId());
    dto.setEquipo(equipoMapper.toDTOOfPlayer(jugador.getEquipo()));

    return dto;
}


    public Jugador toEntity(JugadorDTO dto) {
        Jugador jugador = new Jugador();
        jugador.setNombre(dto.getNombre());
        jugador.setPosicion(dto.getPosicion());
        jugador.setEdad(dto.getEdad());
        jugador.setDorsal(dto.getDorsal());
        
        // Evita una consulta extra a la base de datos, pero asigna una referencia
        Equipo equipo = new Equipo();
        equipo.setId(dto.getIdEquipo());
        jugador.setEquipo(equipo); 
        
        return jugador;
    }
    
     public JugadorDTO toDTOOfTeam(Jugador jugador) {
    
    JugadorDTO dto = new JugadorDTO();
    
    dto.setId(jugador.getId());
    dto.setNombre(jugador.getNombre());
    dto.setPosicion(jugador.getPosicion());
    dto.setDorsal(jugador.getDorsal());
    dto.setEdad(jugador.getEdad());
  
    return dto;
}
}
