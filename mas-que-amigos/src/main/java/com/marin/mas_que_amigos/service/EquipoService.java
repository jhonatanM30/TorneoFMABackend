package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.EquipoDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.NotFoundException;
import com.marin.mas_que_amigos.mapper.EquipoMapper;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EquipoService {

    private final EquipoRepository equipoRepository;
    private final EquipoMapper mapper;

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    @Value("${app.base-url:http://localhost:57075}")
    private String baseUrl;

    private static final List<String> EXTENSIONES_PERMITIDAS = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");

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

    // FRONTEND_VISION.md Fase1: busqueda por coincidencia parcial (endpoint
    // nuevo GET /api/equipos/buscar?nombre=...), separado de
    // obtenerEquipoPorNombre (match exacto) para no cambiar su contrato.
    public List<EquipoDTO> buscarEquiposPorNombreParcial(String nombre) {

        if (nombre == null || nombre.trim().isEmpty()) {
            return Collections.emptyList();
        }

        return equipoRepository.findByNombreContainingIgnoreCase(nombre.trim())
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

        Equipo guardado = equipoRepository.save(mapper.toEntity(equipo));

        EquipoDTO respuesta = mapper.toDTO(guardado);
        respuesta.setMensaje("Gooool! El equipo " + guardado.getNombre() + " se guardó en la base de datos.");
        return respuesta;
    }

    // FRONTEND_VISION.md Fase1: "boton de carga que me permita subir
    // imagenes desde el dispositivo y que esta se guarde donde estan las
    // otras existentes para el escudo del equipo". No existia ninguna
    // capacidad de archivos en el backend antes de este hallazgo (ver
    // config/StaticResourceConfig para el servido de /uploads/**).
    public EquipoDTO actualizarImagenEquipo(Long id, MultipartFile archivo) {

        Equipo equipo = equipoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fuera de juego! El equipo indicado no existe, no se puede actualizar su imagen."));

        if (archivo == null || archivo.isEmpty()) {
            throw new BusinessException("Debes seleccionar un archivo de imagen para subir.");
        }

        String contentType = archivo.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new BusinessException("El archivo debe ser una imagen (jpg, png, webp o gif).");
        }

        String extension = extraerExtension(archivo.getOriginalFilename());
        if (!EXTENSIONES_PERMITIDAS.contains(extension)) {
            throw new BusinessException("Formato de imagen no soportado. Usa jpg, png, webp o gif.");
        }

        try {
            Path carpetaEquipos = Paths.get(uploadsDir, "equipos");
            Files.createDirectories(carpetaEquipos);

            String nombreArchivo = UUID.randomUUID() + "." + extension;
            Path destino = carpetaEquipos.resolve(nombreArchivo);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            equipo.setImagenUrl(baseUrl + "/uploads/equipos/" + nombreArchivo);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen del equipo en disco.", e);
        }

        Equipo actualizado = equipoRepository.save(equipo);

        EquipoDTO respuesta = mapper.toDTO(actualizado);
        respuesta.setMensaje("Gooool! El escudo del equipo " + actualizado.getNombre() + " se actualizó correctamente.");
        return respuesta;
    }

    private String extraerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return "";
        }
        return nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1).toLowerCase();
    }

    public void eliminarEquipo(Long id) {

        Equipo rspEquipo = equipoRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Fuera de juego! El equipo que deseas eliminar no existe en la base de datos"));

        equipoRepository.delete(rspEquipo);
    }

    public EquipoDTO actualizarEquipo(EquipoDTO equipo) {

        // Se busca la entidad administrada (managed) por JPA en lugar de construir
        // una nueva y hacer save() sobre ella: eso evitaría el @Id y forzaría un
        // merge() sobre una instancia "shell" con la colección jugadores vacía,
        // lo que dispararía el orphanRemoval=true y borraría todos los jugadores
        // del equipo en cada edición. Aquí solo se mutan los campos editables.
        Equipo equipoExistente = equipoRepository.findById(equipo.getId())
                .orElseThrow(() -> new NotFoundException("El equipo " + equipo.getNombre() + " no existe, no se puede actualizar."));

        equipoExistente.setNombre(equipo.getNombre());
        equipoExistente.setDirectorTecnico(equipo.getDirectorTecnico());
        equipoExistente.setImagenUrl(equipo.getImagenUrl());
        equipoExistente.setTitulos(equipo.getTitulos());
        equipoExistente.setTipoClasificacion(equipo.getTipoClasificacion());

        Equipo actualizado = equipoRepository.save(equipoExistente);

        EquipoDTO respuesta = mapper.toDTO(actualizado);
        respuesta.setMensaje("Gooool! El equipo " + actualizado.getNombre() + " se actualizó en la base de datos.");
        return respuesta;
    }

}
