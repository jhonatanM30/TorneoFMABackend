package com.marin.mas_que_amigos.service;

import com.marin.mas_que_amigos.dto.ImagenGaleriaDTO;
import com.marin.mas_que_amigos.exception.BusinessException;
import com.marin.mas_que_amigos.exception.NotFoundException;
import com.marin.mas_que_amigos.mapper.ImagenGaleriaMapper;
import com.marin.mas_que_amigos.model.ImagenGaleria;
import com.marin.mas_que_amigos.repository.ImagenGaleriaRepository;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * Hallazgo post-despliegue (fase9): "Poder desde configuraciones subir
 * fotos y que aparezcan alli y se guarden donde estan las actuales que se
 * muestran... en especie de carrusel". Reutiliza exactamente el mismo
 * mecanismo de subida de archivos que EquipoService#actualizarImagenEquipo
 * (validar tipo/extension, guardar en disco bajo app.uploads.dir, servir
 * desde /uploads/** via StaticResourceConfig), pero en vez de actualizar
 * una imagen existente crea una fila nueva por cada foto subida: la
 * galeria es una lista de fotos, no una sola imagen como el escudo de un
 * equipo.
 *
 * @author JhonatanAlexanderCue
 */
@Service
public class ImagenGaleriaService {

    private final ImagenGaleriaRepository imagenGaleriaRepository;
    private final ImagenGaleriaMapper mapper;

    @Value("${app.uploads.dir:uploads}")
    private String uploadsDir;

    @Value("${app.base-url:http://localhost:57075}")
    private String baseUrl;

    private static final List<String> EXTENSIONES_PERMITIDAS = Arrays.asList("jpg", "jpeg", "png", "webp", "gif");

    public ImagenGaleriaService(ImagenGaleriaRepository imagenGaleriaRepository, ImagenGaleriaMapper mapper) {
        this.imagenGaleriaRepository = imagenGaleriaRepository;
        this.mapper = mapper;
    }

    public List<ImagenGaleriaDTO> listarTodas() {
        return imagenGaleriaRepository.findAllByOrderByFechaSubidaDesc()
                .stream()
                .map(mapper::toDTO)
                .collect(Collectors.toList());
    }

    public ImagenGaleriaDTO subirImagen(MultipartFile archivo) {

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

        ImagenGaleria imagen = new ImagenGaleria();

        try {
            Path carpetaGaleria = Paths.get(uploadsDir, "galeria");
            Files.createDirectories(carpetaGaleria);

            String nombreArchivo = UUID.randomUUID() + "." + extension;
            Path destino = carpetaGaleria.resolve(nombreArchivo);
            Files.copy(archivo.getInputStream(), destino, StandardCopyOption.REPLACE_EXISTING);

            imagen.setImagenUrl(baseUrl + "/uploads/galeria/" + nombreArchivo);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo guardar la imagen en disco.", e);
        }

        imagen.setFechaSubida(LocalDateTime.now());

        ImagenGaleria guardada = imagenGaleriaRepository.save(imagen);

        ImagenGaleriaDTO respuesta = mapper.toDTO(guardada);
        respuesta.setMensaje("Imagen agregada a la galería correctamente.");
        return respuesta;
    }

    // Ademas de borrar el registro, se borra el archivo fisico en disco:
    // como aqui cada eliminacion es definitiva (no un "reemplazo" como en
    // Equipo, donde la imagen vieja se sobreescribe), dejar el archivo
    // huerfano en uploads/galeria acumularia espacio sin motivo.
    public void eliminar(Long id) {
        ImagenGaleria imagen = imagenGaleriaRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("La imagen de galería con id " + id + " no existe."));

        imagenGaleriaRepository.deleteById(id);

        try {
            String rutaRelativa = URI.create(imagen.getImagenUrl()).getPath();
            String nombreArchivo = rutaRelativa.substring(rutaRelativa.lastIndexOf('/') + 1);
            Path archivo = Paths.get(uploadsDir, "galeria", nombreArchivo);
            Files.deleteIfExists(archivo);
        } catch (IOException | IllegalArgumentException e) {
            // No se considera un error fatal: el registro ya se borró de la
            // base de datos, que es lo que realmente afecta al carrusel que
            // ve el usuario. Un archivo huérfano en disco no rompe nada.
        }
    }

    private String extraerExtension(String nombreOriginal) {
        if (nombreOriginal == null || !nombreOriginal.contains(".")) {
            return "";
        }
        return nombreOriginal.substring(nombreOriginal.lastIndexOf('.') + 1).toLowerCase();
    }
}
