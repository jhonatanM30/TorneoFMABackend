package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.RegistroInformativoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.mapper.RegistroInformativoMapper;
import com.marin.mas_que_amigos.model.RegistroInformativo;
import com.marin.mas_que_amigos.repository.RegistroInformativoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Fase 6 - Configuracion: permite crear y eliminar registros informativos
 * (tipo blog) que luego se muestran en la pagina de Inicio (Fase 5, hallazgo 2).
 *
 * @author JhonatanAlexanderCue
 */
@Service
@RequiredArgsConstructor
public class RegistroInformativoService {

    private final RegistroInformativoRepository registroInformativoRepository;
    private final RegistroInformativoMapper registroInformativoMapper;

    public List<RegistroInformativoDTO> listarTodos() {
        return registroInformativoRepository.findAllByOrderByFechaPublicacionDesc()
                .stream()
                .map(registroInformativoMapper::toDTO)
                .collect(Collectors.toList());
    }

    public RegistroInformativoDTO guardar(RegistroInformativoDTO registroDTO) {
        RegistroInformativo registro = registroInformativoMapper.toEntity(registroDTO);
        registro.setFechaPublicacion(LocalDateTime.now());
        registro = registroInformativoRepository.save(registro);
        return registroInformativoMapper.toDTO(registro);
    }

    public void eliminar(Long id) {
        if (!registroInformativoRepository.existsById(id)) {
            throw new BusinessException("El registro informativo con id " + id + " no existe.");
        }
        registroInformativoRepository.deleteById(id);
    }
}
