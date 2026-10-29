---
description: Estándares de desarrollo backend, buenas prácticas y convenciones de la aplicación Java/Spring Boot de Más que Amigos, incluida la arquitectura en capas, el diseño de la API y las prácticas de testing.
globs: ["src/main/java/**/*.java", "src/test/java/**/*.java", "src/main/resources/**", "pom.xml"]
alwaysApply: true
---

## Tabla de contenidos

- [Descripción general](#descripción-general)
- [Stack tecnológico](#stack-tecnológico)
- [Arquitectura en capas](#arquitectura-en-capas)
- [Convenciones de nomenclatura](#convenciones-de-nomenclatura)
- [Entidades (JPA)](#entidades-jpa)
- [DTOs](#dtos)
- [Mappers](#mappers)
- [Repositorios](#repositorios)
- [Servicios](#servicios)
- [Controladores](#controladores)
- [Manejo de errores](#manejo-de-errores)
- [Validación](#validación)
- [Documentación de la API (OpenAPI/Swagger)](#documentación-de-la-api-openapiswagger)
- [Migraciones de base de datos (Flyway)](#migraciones-de-base-de-datos-flyway)
- [CORS](#cors)
- [Seguridad (login y roles)](#seguridad-login-y-roles)
- [Subida de archivos](#subida-de-archivos)
- [Testing](#testing)
- [Flujo de trabajo con git](#flujo-de-trabajo-con-git)

## Descripción general

Este backend es una API REST en Spring Boot para gestionar una liga deportiva
amateur: equipos, jugadores, partidos, alineaciones, estadísticas y el
contenido informativo de la página de Inicio. El frontend vive en un
repositorio aparte (`MasQueAmigos-Torneo`, ver `frontend-standards.md`) y
consume esta API vía HTTP/JSON.

## Stack tecnológico

- **Java 8**, **Spring Boot 2.7** (`spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`)
- **MySQL** en desarrollo/producción, **H2** en pruebas
- **Flyway** para versionar el esquema de base de datos
- **Lombok** para reducir código repetitivo (`@Getter`, `@Setter`, `@Data`, `@RequiredArgsConstructor`, etc.)
- **springdoc-openapi-ui** para generar Swagger UI y `/v3/api-docs` a partir de las anotaciones de los controladores
- **JUnit 5** + **Mockito** + **AssertJ** para pruebas
- Validación con `javax.validation` (Bean Validation), namespace `javax.*` (Spring Boot 2.7, previo a Jakarta EE 9)

## Arquitectura en capas

```
Controller → Service → Repository (Spring Data JPA)
              ↓
            Mapper (Entity ↔ DTO)
```

- **Controller**: expone los endpoints REST, valida el shape de la petición (`@Valid`, `@Validated`, `@Min`, etc.) y delega toda regla de negocio al Service. No debe contener lógica de negocio.
- **Service**: contiene las reglas de negocio (ver ejemplos reales: cupo de titulares en `AlineacionService`, cálculo de goles en `PartidoService`, dorsal duplicado en `JugadorService`). Lanza `BusinessException` o `NotFoundException` cuando corresponda.
- **Repository**: interfaces `JpaRepository<Entidad, Long>` de Spring Data JPA. Sin lógica; solo consultas derivadas (`findByNombre`, etc.) o `@Query` cuando el nombre del método no basta.
- **Mapper**: convierte entre entidad JPA y DTO. Nunca se salta esta capa exponiendo una entidad directamente en un controlador.

## Convenciones de nomenclatura

Ver la sección de idioma completa en `docs/base-standards.md` (2.2 y 2.4).
Resumen aplicado al backend:

- El dominio existente está en español (`Jugador`, `Equipo`, `Partido`,
  `Alineacion`, `Estadistica`, `CambioJugador`, `RegistroInformativo`,
  `ImagenGaleria`) y así se mantiene: no se renombra de forma oportunista.
- Las clases nuevas y su código interno (métodos, variables, comentarios,
  logs) van en inglés, salvo que extiendan una clase existente en español
  (ahí se respeta el idioma de esa clase).
- Los sufijos de capa siempre en inglés, sin importar el idioma del
  sustantivo: `Controller`, `Service`, `Repository`, `Mapper`, `DTO`,
  `Config`, `Exception` (p. ej. `JugadorController`, `EquipoRepository`).
- Los identificadores del dominio en español van sin tildes ni ñ
  (`Alineacion`, no `Alineación`).
- Los mensajes que ve el usuario final (`ErrorResponseDTO.mensaje`, mensajes
  de validación en los DTOs) van en español. Los mensajes de log van en
  inglés.

## Entidades (JPA)

- Anotadas con `@Entity` + `@Table(name = "...")`; el nombre de la tabla no
  siempre coincide con el de la clase (ver `Equipo` → tabla `equipos`,
  `Jugador` → tabla `jugador`).
- `@Id` con `@GeneratedValue(strategy = GenerationType.IDENTITY)`.
- Lombok `@Getter @Setter` en vez de getters/setters manuales.
- Enums de dominio como enum interno anidado en la entidad
  (`Equipo.TipoClasificacion`, `Jugador.Posicion`, `Partido.Fase`,
  `Partido.Estado`), persistidos con `@Enumerated(EnumType.STRING)` para
  que la base de datos guarde el nombre legible, no el ordinal.
- Relaciones con `@ManyToOne`/`@OneToMany` explícitas y `@JoinColumn`
  nombrando la columna FK en snake_case (`id_equipo`, `id_partido`).
- Una llave compuesta se modela con `@EmbeddedId` + una clase `@Embeddable`
  aparte (ver `Alineacion`/`AlineacionId`), no con `@IdClass`.
- Todo cambio de esquema (columna nueva, tabla nueva, cambio de tipo) va
  acompañado de una migración Flyway (ver sección correspondiente), nunca
  solo de la anotación JPA.

## DTOs

- Nunca se expone una entidad JPA directamente en un controlador: cada
  entidad tiene su DTO (`EquipoDTO`, `JugadorDTO`, etc.).
- Constructor vacío + `@AllArgsConstructor` de Lombok cuando el DTO se
  serializa/deserializa (`@NoArgsConstructor @AllArgsConstructor`); `@Data`
  cuando no hace falta ese control fino.
- Campos calculados en el servidor, o que solo tiene sentido leer en la
  respuesta, se marcan `@JsonProperty(access = JsonProperty.Access.READ_ONLY)`
  para que Jackson los ignore silenciosamente si el cliente los envía en el
  body, en vez de fallar la deserialización (ver `PartidoDTO.golesLocal`,
  `JugadorDTO.equipo`).
- Validación de Bean Validation directamente en el campo del DTO
  (`@NotBlank`, `@NotNull`, `@Size`, `@Min`), con `message` en español —
  es el mensaje que termina viéndolo el usuario final vía
  `GlobalExceptionHandler#handleValidationExceptions`.
- Un DTO de respuesta de error propio y compartido: `ErrorResponseDTO`
  (`indicadorRespuesta` + `mensaje`).

## Mappers

- Un `@Component` por entidad (`EquipoMapper`, `JugadorMapper`, etc.), con
  métodos `toEntity(DTO)` y `toDTO(Entidad)`.
- Cuando dos entidades se referencian mutuamente (`Equipo` ↔ `Jugador`), el
  mapper "externo" tiene un método adicional (`toDTOExt`) que arma el DTO
  sin la relación anidada, para cortar la recursión infinita al serializar.
- Referencias circulares entre mappers se resuelven con constructor
  explícito (sin `@Autowired` en el campo), nunca con `@Lazy` como parche.

## Repositorios

- `interface XxxRepository extends JpaRepository<Xxx, Long>`.
- Métodos derivados por nombre (`findByNombreIgnoreCase`,
  `findByNombreContainingIgnoreCase`) para búsquedas simples; `@Query` solo
  cuando la derivación por nombre no alcanza.

## Servicios

- Anotados `@Service`, con las dependencias inyectadas por constructor
  (Lombok `@RequiredArgsConstructor` cuando los campos son `final`, o
  constructor explícito si hace falta lógica adicional en él).
- Toda regla de negocio vive aquí, nunca en el controlador ni en el mapper:
  validar existencia (`NotFoundException`), reglas de dominio como cupo de
  titulares o duplicados (`BusinessException`), y cálculos derivados como
  el marcador del partido a partir de `Estadistica`.
- Un método de servicio nunca retorna ni recibe una entidad JPA hacia/desde
  el controlador: siempre DTO de entrada y salida, convertido con el
  Mapper correspondiente.

## Controladores

- `@RestController` + `@RequestMapping("/api/<recurso-en-plural>")` +
  `@Validated` a nivel de clase (para que los `@Min`/`@Size` en
  `@PathVariable`/`@RequestParam` se evalúen).
- Un `@Tag` de Swagger por controlador, con nombre y descripción en
  español (es lo que ve quien consulta `/swagger-ui.html`).
- Cada endpoint documentado con `@Operation` (resumen + descripción de la
  regla de negocio) y `@ApiResponses` para cada código de estado posible,
  incluidos los de error — ver `docs/api-spec.yml`, que se genera leyendo
  estas mismas anotaciones.
- Subida de archivos con `consumes = "multipart/form-data"` y
  `@RequestParam("imagen") MultipartFile imagen`.
- Un recurso anidado en otro (por ejemplo, cambios de jugador dentro de un
  partido) usa la ruta anidada del padre
  (`/api/partidos/{idPartido}/cambios`), no un recurso de primer nivel.

## Manejo de errores

Centralizado en `GlobalExceptionHandler` (`@RestControllerAdvice`):

- `NotFoundException` / excepciones específicas de "no encontrado" (como
  `JugadorNotFoundException`) → `404`.
- `BusinessException` (violación de una regla de negocio, no de formato) →
  `400`, cuerpo `ErrorResponseDTO`.
- `MethodArgumentNotValidException` (falla un `@Valid` del DTO) → `400`,
  cuerpo con el mapa `campo → mensaje` de cada error de validación.
- Cualquier otra excepción no controlada → `500`.

Al agregar una regla de negocio nueva, se lanza `BusinessException` con un
mensaje en español, no un `IllegalArgumentException` genérico ni un
`return null`.

## Validación

- Bean Validation (`javax.validation`) en los DTOs, evaluada por Spring al
  usar `@Valid @RequestBody` en el controlador.
- Reglas que dependen de datos ya guardados (dorsal duplicado dentro de un
  equipo, fecha de partido ya ocupada, cupo de titulares) no se validan con
  anotaciones: se verifican en el Service contra el Repository, y se
  reportan con `BusinessException`.

## Documentación de la API (OpenAPI/Swagger)

- Se genera en caliente a partir de las anotaciones de los controladores
  (`springdoc-openapi-ui`), visible en `/swagger-ui.html` y `/v3/api-docs`
  con la app corriendo.
- `docs/api-spec.yml` es la versión estática de esa misma documentación,
  para revisarla sin levantar la aplicación. Al agregar, cambiar o eliminar
  un endpoint, actualiza ambos: las anotaciones del controlador (fuente de
  verdad en caliente) y `docs/api-spec.yml` (referencia estática).

## Migraciones de base de datos (Flyway)

- Cambios de esquema versionados con Flyway, no con
  `spring.jpa.hibernate.ddl-auto=update` en producción.
- Antes de introducir una migración que sea incompatible con datos
  existentes (por ejemplo, restringir un `String` libre a un `enum`), la
  migración o sus notas deben indicar qué hacer con las filas existentes
  que no cumplan el nuevo valor permitido (ver el comentario de
  `Equipo.TipoClasificacion` en el modelo, que documenta justo este caso).

## CORS

- Configurado en `CorsConfig` con una lista explícita de orígenes
  permitidos (hoy: `http://127.0.0.1:5500`, `http://localhost:5500`,
  `http://localhost:5173`, los puertos típicos de Live Server/Vite para
  servir el frontend en desarrollo).
- Al agregar un nuevo origen de desarrollo o el dominio de producción del
  frontend, se agrega a esa lista explícita; nunca se usa `"*"` cuando
  `allowCredentials` está en `true` (no es válido según la spec CORS).

## Seguridad (login y roles)

- La API usa Spring Security con tokens JWT de corta vida (3 minutos por
  defecto, `app.security.jwt.ttl`) enviados en `Authorization: Bearer <token>`.
  No hay sesión en el servidor ni cookies (por eso no hay superficie CSRF). El
  front renueva el token con `POST /api/auth/refresh` mientras el usuario tiene
  actividad en pantalla; si pasan 3 minutos sin renovar, la sesión se cierra.
- **Roles**: visitante (sin cuenta, solo `GET`), `DT` (consulta y solicita
  traspasos hacia su propio equipo) y `ADMIN` (todo). Las cuentas las crea un
  administrador (`POST /api/users`); el administrador inicial se siembra al
  arrancar desde la configuración.
- **Autorización por URL y método** en `SecurityConfig`, no con `@PreAuthorize`:
  una `AccessDeniedException` lanzada dentro de un controller la captura el
  `@ExceptionHandler(Exception.class)` de `GlobalExceptionHandler` y se
  convertiría en un `500`. En la cadena de filtros, en cambio, la responden
  `JsonAuthenticationEntryPoint` (`401`) y `JsonAccessDeniedHandler` (`403`),
  ambos con el cuerpo `{indicadorRespuesta, mensaje}` en español. Las reglas de
  negocio que dependen de quién llama (por ejemplo, el DT solo solicita
  traspasos hacia su equipo) se validan en el service y lanzan
  `ForbiddenOperationException` (`403`).
- **Un endpoint de escritura nuevo exige `ADMIN` por defecto**: la última regla
  de `/api/**` es la red de seguridad. Para abrirlo a otro rol hay que agregarlo
  expresamente en `SecurityConfig` y cubrirlo en `SecurityRulesTest`.
- **Un token inválido o vencido se rechaza con `401` aunque la operación sea de
  consulta pública**; el cliente debe dejar de enviarlo cuando la sesión se cierra.
- **Datos que cambian después de iniciar sesión** (el equipo del DT, el rol) se
  leen de la cuenta en la base de datos, no del token. El token solo dice quién
  es la persona y su rol al momento de emitirlo; al renovarlo se vuelve a leer.
- **Configuración (variables de entorno, sin valores por defecto)**:
  - `APP_SECURITY_JWT_SECRET`: secreto de firma, al menos 32 caracteres. La app
    **no arranca** si falta o es corto.
  - `APP_SECURITY_ADMIN_USERNAME` y `APP_SECURITY_ADMIN_PASSWORD`: administrador
    inicial. Si faltan, la app arranca sin crearlo (solo consulta) y registra una
    advertencia. Nunca se modifica una cuenta existente.
  - `APP_SECURITY_JWT_TTL` (opcional): vigencia del token en formato ISO-8601
    (por ejemplo `PT3M`).
  Con Docker Compose se pasan como `environment` del servicio `backend` y se
  definen en el `.env` del repositorio padre (que no se versiona).
- La clave fija `X-Director-Tecnico-Key` y su interceptor ya no existen.
- **Contraseñas**: BCrypt, nunca en claro ni en respuestas ni en el log. El
  login responde igual si el usuario no existe o la contraseña es incorrecta
  y compara contra un hash ficticio para no revelar por tiempo si el usuario existe.
- **Pruebas**: `SecurityRulesTest` recorre la matriz rol por operación sobre la
  cadena real con tokens reales (H2 en memoria); los tests con credenciales usan
  los valores de `src/test/resources/application.properties`.

## Subida de archivos

- Las imágenes (escudo de equipo, foto de jugador, fotos de galería) se
  reciben como `multipart/form-data`, se validan por extensión
  (`jpg`, `jpeg`, `png`, `webp`, `gif`) y se guardan en disco bajo
  `app.uploads.dir` (por defecto `uploads`), sirviéndose después desde
  `/uploads/**` (ver `StaticResourceConfig`).
- La URL pública devuelta se arma con `app.base-url`
  (por defecto `http://localhost:57075`).

## Testing

- JUnit 5 (`org.junit.jupiter`) + Mockito (`@ExtendWith(MockitoExtension.class)`,
  `@Mock`) + AssertJ (`assertThat`, `assertThatThrownBy`) para pruebas
  unitarias de Service y Mapper.
- Un Service se prueba con sus dependencias (`Repository`, `Mapper`)
  mockeadas, construyendo el Service manualmente en `@BeforeEach`
  (no se levanta el contexto de Spring para una prueba unitaria).
- H2 en memoria para las pruebas que sí necesiten un contexto con base de
  datos real (ver `src/test/resources/application.properties`).
- Nombre de método de prueba descriptivo en español o inglés según el
  idioma del código que prueba, en formato
  `metodoQueSePrueba_comportamientoEsperado`
  (ver `guardarEquipo_devuelveRepresentacionCompletaDelEquipoGuardado` en
  `EquipoServiceTest`).

## Flujo de trabajo con git

- Mensajes de commit en inglés, Conventional Commits, con el prefijo de
  fase/hallazgo cuando aplique (ver el historial real del repo:
  `feat(fase9-back): ...`, `fix(db): ...`, `docs(readme): ...`).
- Cambios pequeños y enfocados (ver `docs/base-standards.md`, principio de
  "tareas pequeñas, una a la vez").
