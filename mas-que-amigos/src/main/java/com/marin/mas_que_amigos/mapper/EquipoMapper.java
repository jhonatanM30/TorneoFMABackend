package com.marin.mas_que_amigos.mapper;

import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.model.Equipo;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

@Component
public class EquipoMapper {

    private final JugadorMapper jugadorMapper;

    // Constructor sin @Autowired para evitar referencia circular
    public EquipoMapper(JugadorMapper jugadorMapper) {
        this.jugadorMapper = jugadorMapper;
    }

    public Equipo toEntity(EquipoDTO dto) {
        Equipo equipo = new Equipo();
        equipo.setNombre(dto.getNombre());
        equipo.setDirectorTecnico(dto.getDirectorTecnico());
        equipo.setImagenUrl(dto.getImagenUrl());
        equipo.setTitulos(dto.getTitulos());
        equipo.setTipoClasificacion(dto.getTipoClasificacion());
        return equipo;
    }

    public EquipoDTO toDTO(Equipo equipo) {
        EquipoDTO dto = new EquipoDTO();

        dto.setId(equipo.getId());
        dto.setNombre(equipo.getNombre());
        dto.setDirectorTecnico(equipo.getDirectorTecnico());
        dto.setImagenUrl(equipo.getImagenUrl());
        dto.setTitulos(equipo.getTitulos());
        dto.setTipoClasificacion(equipo.getTipoClasificacion());
        // Convertir lista de jugadores a DTO
        if (equipo.getJugadores() != null) {
            dto.setJugadores(equipo.getJugadores()
                    .stream()
                    .map(jugadorMapper::toDTOExt)
                    .collect(Collectors.toList()));
        }
        dto.setIndicadorRespuesta("Success");
        dto.setMensaje("");
        return dto;
    }

    public EquipoDTO toRSPDTO(Long id, String indicadorRespuesta, String mensaje) {

        return new EquipoDTO(id, indicadorRespuesta, mensaje);
            
    }

    public EquipoDTO toDTOExt(Equipo equipo) {
        EquipoDTO dto = new EquipoDTO();

        dto.setId(equipo.getId());
        dto.setNombre(equipo.getNombre());
        dto.setDirectorTecnico(equipo.getDirectorTecnico());
        dto.setImagenUrl(equipo.getImagenUrl());
        dto.setTitulos(equipo.getTitulos());
        dto.setTipoClasificacion(equipo.getTipoClasificacion());
        return dto;
    }
}
