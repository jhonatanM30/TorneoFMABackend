package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.RegistroInformativoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.NotFoundException;
import com.marin.mas_que_amigos.mapper.RegistroInformativoMapper;
import com.marin.mas_que_amigos.model.RegistroInformativo;
import com.marin.mas_que_amigos.repository.RegistroInformativoRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Fase 6 - Configuracion: permite crear, editar y eliminar registros
 * informativos (tipo blog) que luego se muestran en la pagina de Inicio
 * (Fase 5, hallazgo 2).
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

    // Hallazgo post-despliegue (fase9): "deberia permitir editar". Antes el
    // controller solo exponia GET/POST/DELETE. Se edita solo titulo y
    // contenido; la fecha de publicacion original se conserva (no se trata
    // como una republicacion).
    public RegistroInformativoDTO actualizar(Long id, RegistroInformativoDTO registroDTO) {
        RegistroInformativo existente = registroInformativoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("El registro informativo con id " + id + " no existe."));

        existente.setTitulo(registroDTO.getTitulo());
        existente.setContenido(registroDTO.getContenido());

        RegistroInformativo actualizado = registroInformativoRepository.save(existente);
        return registroInformativoMapper.toDTO(actualizado);
    }

    public void eliminar(Long id) {
        if (!registroInformativoRepository.existsById(id)) {
            throw new BusinessException("El registro informativo con id " + id + " no existe.");
        }
        registroInformativoRepository.deleteById(id);
    }
}
