# Design

## Context

Estado actual verificado en el código (ver `proposal.md` para la motivación):

- Spring Boot 2.7.18, Java 8, sin Spring Security. La única protección es `DirectorTecnicoInterceptor` (registrado en `DirectorTecnicoWebConfig` sobre `/api/**`): exige el header `X-Director-Tecnico-Key` en todo método distinto de `GET/HEAD/OPTIONS` y responde `401` con `ErrorResponseDTO {indicadorRespuesta, mensaje}`. Además existe `POST /api/director-tecnico/validar` solo para comprobar la clave.
- `CorsConfig` ya registra un bean `CorsFilter` (orígenes `5500` y `5173`, `allowedHeaders "*"`, `allowCredentials true`).
- `GlobalExceptionHandler` tiene un `@ExceptionHandler(Exception.class)` que responde `500` a cualquier excepción no prevista. Esto condiciona cómo se debe autorizar (ver Decisión 4).
- Los tests actuales son Mockito sobre services más un `MasQueAmigosApplicationTests` que levanta el contexto con H2 en memoria (`src/test/resources/application.properties`, Flyway desactivado, `ddl-auto=update`).
- El esquema se versiona con Flyway; la última migración es `V7`. Las convenciones de idioma (`docs/base-standards.md` §2): clases, tablas, columnas y rutas nuevas en inglés; mensajes al usuario en español.
- El front es un repositorio aparte (HTML/CSS/JS planos), servido desde orígenes distintos al backend.

## Goals / Non-Goals

**Goals:**
- Identidad real (usuario y contraseña) y sesión que se cierra a los 3 minutos sin renovación.
- Autorización por rol aplicada de forma central, sin que un endpoint de escritura nuevo pueda quedar abierto por olvido (la misma garantía que daba el interceptor).
- Mantener el formato de error `{indicadorRespuesta, mensaje}` para facilitar la adaptación del front.

**Non-Goals:**
- Permisos de alineaciones del DT, solicitudes con aprobación, notificaciones y cierre de torneo (changes siguientes).
- Cambio y recuperación de contraseña, bloqueo por intentos fallidos, deshabilitar cuentas, revocación de tokens en el servidor y límite absoluto de duración de sesión.
- Cambios de frontend.

## Decisions

**1. Token JWT de corta vida, sin sesión en el servidor, en el header `Authorization: Bearer`.**
Tokens firmados con HS256 y un secreto de configuración, vigencia 3 minutos (`app.security.jwt.ttl`, `PT3M` por defecto). Claims: `sub` (usuario), `uid`, `role`, `iat`, `exp`. La "inactividad" la mide el front (actividad en pantalla): mientras hay actividad llama a `POST /api/auth/refresh`; si deja de renovar, el token vence solo. El servidor no guarda estado de sesión.
*Alternativas descartadas*: sesión HTTP con cookie (cookies entre orígenes distintos del front y el backend traen complicaciones de `SameSite`/CSRF y el front usa orígenes de desarrollo variables); token de larga vida con refresh token (más superficie y estado a guardar para un requisito que se resuelve con tokens cortos).

**2. Spring Security 5.7 con `SecurityFilterChain` y soporte JWT nativo de Spring** (`spring-boot-starter-security` + `spring-boot-starter-oauth2-resource-server`, que trae Nimbus). Emisión con `NimbusJwtEncoder` y validación con `NimbusJwtDecoder` (HS256).
Dos detalles que importan para que "3 minutos" sea 3 minutos: (a) el validador de tiempo de Spring tolera por defecto 60 segundos de desfase de reloj, así que se configura `JwtTimestampValidator(Duration.ZERO)`; (b) el rol se mapea desde el claim `role` a la autoridad `ROLE_<ROL>` con un `JwtAuthenticationConverter`.
*Alternativa descartada*: librería `jjwt`: obliga a escribir a mano el filtro de autenticación y los manejadores de errores que Spring ya trae.

**3. Cadena sin estado, CSRF desactivado y CORS reutilizando el `CorsFilter` existente.**
`SessionCreationPolicy.STATELESS`; sin cookies no hay superficie CSRF. `http.cors()` toma el bean `corsFilter` ya definido en `CorsConfig`, así que los orígenes siguen en un solo lugar y las solicitudes previas (`OPTIONS`) se resuelven antes de la autorización. `allowedHeaders "*"` ya cubre `Authorization`.

**4. Autorización por URL y método en la cadena de filtros, no con `@PreAuthorize`.**
Razón: una `AccessDeniedException` lanzada dentro de un controller la captura el `@ExceptionHandler(Exception.class)` y se convertiría en `500`; las reglas por URL se evalúan en el filtro y llegan a los manejadores de `401`/`403`. Es además el equivalente del interceptor actual (una sola tabla central). Reglas, en este orden:
1. `OPTIONS /**` y `/error` → permitidos (si `/error` quedara protegido, cualquier error se vería como `401`).
2. `POST /api/auth/login` → público. `POST /api/auth/refresh` y `GET /api/auth/me` → autenticado.
3. `/api/users/**` (cualquier método) → `ADMIN`.
4. `POST /api/transfers` → `ADMIN` o `DT` (la regla "el DT solo hacia su equipo" se aplica en el service, ver Decisión 9); `POST /api/transfers/*/confirm` y `/reject` → `ADMIN`.
5. `GET /api/**`, `GET /uploads/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html` → públicos.
6. Cualquier otra ruta de `/api/**` (los `POST/PUT/DELETE` restantes) → `ADMIN`.
7. Todo lo demás → denegado.
La regla 6 es la red de seguridad: un endpoint de escritura nuevo exige `ADMIN` por defecto y hay que abrirlo expresamente.

**5. Respuestas `401`/`403` en JSON con el formato existente.**
Un `AuthenticationEntryPoint` y un `AccessDeniedHandler` escriben `ErrorResponseDTO` en español (`"No autorizado"` / `"Prohibido"` como `indicadorRespuesta`). El entry point distingue token vencido ("Tu sesión expiró…") de falta o invalidez de token ("Debes iniciar sesión…") mirando el error de validación del JWT.

**6. Modelo de usuario: tabla `app_user` (inglés), migración `V8`.**
Columnas: `id`, `username VARCHAR(50)` único y guardado en minúsculas (se normaliza al escribir y al buscar, de modo que la unicidad no dependa del collation), `password_hash VARCHAR(100)`, `role VARCHAR(20)` (`ADMIN`|`DT`, enum como texto igual que `Posicion`), `created_at`. Entidad `AppUser`, enum `UserRole`, repositorio con `findByUsername` y `existsByUsername`. Además lleva `team_id BIGINT NULL` con llave foránea a `equipos` y restricción `UNIQUE`: un equipo tiene como máximo una cuenta y, al ser una sola columna, una cuenta tiene como máximo un equipo (MySQL admite varios `NULL` en un índice único, así que muchos DT pueden estar sin equipo). Una regla de servicio impide asignar equipo a cuentas `ADMIN`. El texto `Equipo.directorTecnico` no se sincroniza con la cuenta en este change; sigue siendo un dato que mantiene el administrador.

**7. Contraseñas con BCrypt.** `BCryptPasswordEncoder` (costo por defecto). En el login, cuando el usuario no existe se compara igual contra un hash ficticio para que el tiempo de respuesta no revele si el usuario existe, y se devuelve el mismo mensaje en ambos casos.

**8. Administrador inicial con `ApplicationRunner` y configuración sin valores por defecto inseguros.**
Propiedades `app.security.admin.username` y `app.security.admin.password` (variables `APP_SECURITY_ADMIN_USERNAME` y `APP_SECURITY_ADMIN_PASSWORD`). Si el usuario no existe se crea con rol `ADMIN`; si existe no se toca; si falta la configuración, se registra una advertencia y la app arranca (solo consulta). El secreto del JWT (`app.security.jwt.secret`, `APP_SECURITY_JWT_SECRET`) no tiene valor por defecto y debe tener al menos 32 bytes: si falta o es corto, **la app no arranca** (fallo explícito, igual que con las variables del datasource). Los tests lo proveen en `src/test/resources/application.properties`.

**9. Servicios, controladores y DTOs nuevos (inglés).**
`AuthController` (`/api/auth`: `login`, `refresh`, `me`) con `AuthService` y `JwtTokenService`; `UserController` (`/api/users`: `POST`, `GET`) con `UserService`. DTOs: `LoginRequestDTO`, `AuthResponseDTO {token, tokenType, expiresInSeconds, user}`, `UserSummaryDTO`, `UserCreateRequestDTO {username, password, role?}`, `UserDTO`. `PUT /api/users/{id}/team` (cuerpo `UserTeamRequestDTO {teamId}`; `teamId` nulo quita el equipo) lo usa el administrador para asignar, cambiar o quitar el equipo de un DT. `UserSummaryDTO` y `UserDTO` incluyen `teamId` (y el nombre del equipo en `UserDTO`). Para la regla de traspasos, el controller toma `uid` y `role` del token y los pasa al service como un objeto `AuthenticatedUser`; el service **relee la cuenta en la base de datos** para obtener el equipo vigente (no se usa un dato del token, porque el equipo puede asignarse después de iniciar sesión) y, si el rol es `DT`, exige que el equipo destino sea ese. Si no coincide, o el DT no tiene equipo, lanza `ForbiddenOperationException`, que `GlobalExceptionHandler` convierte en `403` con `ErrorResponseDTO` en español. `refresh` vuelve a leer al usuario por el `sub` del token: así el rol del nuevo token es el vigente en base de datos, y si la cuenta ya no existe responde `401`.

**10. Retiro de la clave fija.** Se eliminan `DirectorTecnicoInterceptor`, `DirectorTecnicoWebConfig`, `DirectorTecnicoController` y la propiedad `app.director-tecnico.clave`. El front actual deja de poder escribir hasta adaptarse; no se mantiene un modo de compatibilidad para no dejar la clave fija como puerta trasera.

**11. Documentación OpenAPI.** `OpenApiConfig` declara el esquema `bearerAuth` (HTTP bearer, JWT). En `docs/api-spec.yml` se reemplaza `directorTecnicoKey` por `bearerAuth`, se retira `/api/director-tecnico/validar` y se documentan las rutas nuevas.

**12. Estrategia de pruebas.** Tests unitarios con Mockito para `AuthService`, `UserService` y el sembrado del administrador; tests del servicio de tokens (emisión, vigencia, vencimiento exacto sin tolerancia, token alterado, firma de otro emisor); validación de DTOs; y una clase `@SpringBootTest` + `MockMvc` con H2 que recorre la matriz rol × operación (visitante, DT, ADMIN) sobre endpoints representativos, usando tokens reales del propio servicio. Un token vencido se fabrica con una fecha de expiración pasada, sin esperar tiempo real.

## Risks / Trade-offs

- **Tolerancia de 60 s en la validación de la expiración** → el token duraría 4 minutos en vez de 3. Mitigación: `JwtTimestampValidator` con tolerancia cero y un test que lo verifica.
- **Sesión que no caduca mientras haya actividad** (sin límite absoluto) → una pestaña activa renueva sin fin. Mitigación: aceptado por ahora; un límite absoluto (por ejemplo 8 horas) queda como seguimiento.
- **Token guardado en el navegador** → expuesto si hay XSS. Mitigación: vida de 3 minutos y recomendación al front de mantenerlo en memoria, sin `localStorage`; sin cookies no hay CSRF.
- **Cambios de rol o cuentas dadas de baja se notan en la siguiente renovación** (hasta 3 minutos) → inherente a tokens sin revocación en el servidor; aceptable con una vida tan corta.
- **Sin protección contra fuerza bruta en el login** → BCrypt encarece cada intento pero no lo impide. Mitigación: seguimiento (bloqueo temporal o límite de intentos).
- **`Equipo.directorTecnico` (texto) y la cuenta del DT pueden quedar desalineados** → el administrador mantiene ambos. Mitigación: documentado; el change de solicitudes de equipo vincula el equipo aprobado con la cuenta.
- **Los equipos existentes no tienen DT con cuenta** → hasta que el administrador cree las cuentas y asigne equipos, ningún DT podrá solicitar traspasos. Mitigación: es parte de la puesta en marcha (ver Migration Plan).
- **El `@ExceptionHandler(Exception.class)` existente enmascara errores como `500`** → por eso la autorización vive en la cadena de filtros. Quien agregue autorización por método en el futuro deberá añadir un manejador para `AccessDeniedException`.
- **Despliegue**: sin `APP_SECURITY_JWT_SECRET` la aplicación no arranca, y el contenedor actual no tiene esas variables. Mitigación: agregar las variables al `docker-compose.yml`/`.env` **antes** de reconstruir la imagen (ver Migration Plan).
- **Cambio incompatible para el front** → mientras no se adapte, las pantallas de escritura devolverán `401`. Mitigación: coordinar el despliegue con el cambio del front; las consultas siguen funcionando.

## Migration Plan

1. Preparar la configuración: definir `APP_SECURITY_JWT_SECRET` (mínimo 32 caracteres aleatorios), `APP_SECURITY_ADMIN_USERNAME` y `APP_SECURITY_ADMIN_PASSWORD`, y opcionalmente `APP_SECURITY_JWT_TTL`, en el `docker-compose.yml`/`.env` del repositorio padre. Se muestra el diff al usuario antes de aplicarlo.
2. Desplegar el backend: Flyway aplica `V8` al arrancar y el sembrado crea el administrador si no existe.
3. Verificar: inicio de sesión del administrador, una escritura con token, una escritura sin token (`401`) y las consultas públicas.
4. Crear las cuentas de DT de los equipos existentes con `POST /api/users` (con `teamId`) o asignarlas con `PUT /api/users/{id}/team`.
5. Adaptar y desplegar el front (login, envío del token, renovación por actividad, manejo de `401`/`403`, equipo del DT).
6. Rollback: volver a la imagen anterior (ignora la tabla `app_user` y recupera la clave fija); si hace falta, `DROP TABLE app_user` y retirar las variables nuevas.
