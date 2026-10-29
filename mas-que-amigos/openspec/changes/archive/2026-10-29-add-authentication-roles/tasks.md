# Tasks

> Nota para todos los pasos que compilan o prueban: usar siempre `./mvnw clean ...`. El IDE deja clases a medio compilar en `target/` y sin `clean` Maven puede decir "Nothing to compile" o fallar con `ClassFormatError`.

## 0. Preparación: Crear el feature branch (OBLIGATORIO - PRIMER PASO)

- [x] 0.1 Crear el feature branch `feature/add-authentication-roles-backend` a partir de `master` y verificar con `git branch --show-current` que quedó activo
- [x] 0.2 Verificar el working tree con `git status` y confirmar que ningún archivo sin commitear pertenece a este change antes de empezar

## 1. Backend: Dependencias, configuración y migración

- [x] 1.1 Agregar `spring-boot-starter-security` y `spring-boot-starter-oauth2-resource-server` al `pom.xml` y verificar que `./mvnw clean compile` termina en `BUILD SUCCESS`
- [x] 1.2 Agregar a `src/test/resources/application.properties` el secreto del JWT (32 bytes o más), el usuario y la contraseña del administrador sembrado y la vigencia del token, y a `src/main/resources/application.properties` la vigencia por defecto `PT3M` con comentarios que indiquen que el secreto y las credenciales del admin vienen de variables de entorno sin valor por defecto; verificar que ambos archivos quedan válidos al arrancar los tests en el grupo 8
- [x] 1.3 Crear `src/main/resources/db/migration/V8__app_user.sql` con la tabla `app_user` (`id`, `username` único, `password_hash`, `role`, `team_id` nulo con llave foránea a `equipos` y restricción `UNIQUE`, `created_at`) y verificar con `./mvnw clean compile` que el archivo se empaqueta (su aplicación sobre MySQL se verifica en el grupo 9)

## 2. Backend: Tokens de sesión (TDD) — spec `authentication`

- [x] 2.1 Crear `JwtTokenServiceTest` con tests en rojo para: emitir un token con `sub`, `uid`, `role` y vigencia configurada; que un token vencido se rechaza sin tolerancia de reloj (vence exactamente a los 3 minutos); que un token alterado o firmado con otro secreto se rechaza; que un secreto de menos de 32 bytes o ausente hace fallar la configuración; verificar que compilan y fallan
- [x] 2.2 Implementar `SecurityProperties` (secreto, vigencia, usuario y contraseña del admin) y `JwtTokenService` con `NimbusJwtEncoder`/`NimbusJwtDecoder` HS256 y `JwtTimestampValidator` con tolerancia cero, y verificar que los tests de 2.1 pasan en verde

## 3. Backend: Cuentas de usuario y equipo del DT (TDD) — spec `user-accounts`

- [x] 3.1 Crear `UserServiceTest` y `UserCreateRequestDTOValidationTest` con tests en rojo para: crear sin rol (queda `DT`), crear con rol `ADMIN`, rol inválido (400), usuario duplicado sin importar mayúsculas (400), usuario guardado en minúsculas, contraseña guardada con hash y nunca devuelta, formato de usuario (3 a 50 caracteres permitidos) y contraseña de al menos 8 caracteres con mensajes en español, y listado ordenado por usuario con su equipo; verificar que fallan
- [x] 3.2 Agregar a `UserServiceTest` tests en rojo para el equipo del DT: crear DT con equipo libre, equipo inexistente (400), equipo ya asignado a otro DT (400), `ADMIN` con equipo (400), asignar equipo a un DT sin equipo, cambiar de equipo dejando libre el anterior, quitar el equipo con `teamId` nulo, asignar a una cuenta `ADMIN` (400) y a una cuenta inexistente (404); verificar que fallan
- [x] 3.3 Crear `UserRole`, `AppUser` (con la relación opcional a `Equipo`), `AppUserRepository`, `UserCreateRequestDTO`, `UserTeamRequestDTO`, `UserDTO` y `UserService` con el bean `PasswordEncoder` (BCrypt), y verificar que los tests de 3.1 y 3.2 pasan en verde
- [x] 3.4 Crear `AdminSeederTest` con tests en rojo para: crea el admin cuando no existe (rol `ADMIN`, contraseña con hash), no crea ni cambia nada si ya existe, y con configuración incompleta no crea nada y registra una advertencia; verificar que fallan
- [x] 3.5 Implementar `AdminSeeder` (`ApplicationRunner`) y verificar que los tests de 3.4 pasan en verde

## 4. Backend: Login, renovación y sesión actual (TDD) — spec `authentication`

- [x] 4.1 Crear `AuthServiceTest` y `LoginRequestDTOValidationTest` con tests en rojo para: login correcto (token, tipo `Bearer`, segundos de vigencia y datos del usuario con su equipo o nulo), usuario con otras mayúsculas, contraseña incorrecta y usuario inexistente con el mismo mensaje en español, campos vacíos, renovación que relee el rol vigente desde la base de datos, renovación cuando la cuenta ya no existe (401) y consulta de la sesión actual; verificar que fallan
- [x] 4.2 Implementar `AuthService` (comparación contra un hash ficticio cuando el usuario no existe), `LoginRequestDTO`, `AuthResponseDTO` y `UserSummaryDTO`, y verificar que los tests de 4.1 pasan en verde

## 5. Backend: Traspasos del DT (TDD) — spec `player-transfers`

- [x] 5.1 Agregar a `PlayerTransferServiceTest` tests en rojo para: un administrador solicita hacia cualquier equipo, un DT solicita hacia su equipo (201), un DT hacia otro equipo (403 con mensaje en español), un DT sin equipo (403), y que el equipo del DT se relee de la cuenta al solicitar y no de un dato del token; agregar a `GlobalExceptionHandlerTest` un test en rojo para el 403 de la nueva excepción; verificar que fallan
- [x] 5.2 Crear `AuthenticatedUser`, `ForbiddenOperationException` y su manejador 403 en `GlobalExceptionHandler`, ajustar `PlayerTransferService#requestTransfer` para recibir a quien solicita, y actualizar los tests existentes de `requestTransfer`; verificar que los tests de 5.1 y los existentes pasan en verde

## 6. Backend: Autorización HTTP y endpoints (TDD) — specs `access-control` y `authentication`

- [x] 6.1 Crear `SecurityRulesTest` (`@SpringBootTest` + `MockMvc` + H2, con tokens reales de `JwtTokenService`) con tests en rojo para la matriz visitante/DT/ADMIN: consultas públicas en equipos, jugadores, partidos, estadísticas y traspasos (en cualquier estado); escrituras sin sesión → 401; DT: consulta 200, solicitar traspaso hacia su equipo permitido y hacia otro equipo 403, confirmar/rechazar y demás escrituras → 403, `/api/users` → 403; ADMIN: escrituras, `/api/users` y asignar equipos permitidos; token vencido y token alterado → 401 con el mensaje de sesión expirada o de inicio de sesión; cuerpo `{indicadorRespuesta, mensaje}` en 401 y 403; solo `X-Director-Tecnico-Key` → 401; preflight con `Authorization` permitido; login público; `refresh` y `me` con y sin token; verificar que fallan
- [x] 6.2 Implementar `SecurityConfig` (cadena sin estado, CSRF desactivado, `http.cors()` sobre el `CorsFilter` existente, reglas por URL y método del diseño, conversor del claim `role` a `ROLE_*`, decoder con tolerancia cero), `AuthenticationEntryPoint` y `AccessDeniedHandler` con `ErrorResponseDTO` en español, y verificar que los tests de 6.1 que no dependen de los controllers nuevos pasan
- [x] 6.3 Crear `AuthController` (`POST /api/auth/login`, `POST /api/auth/refresh`, `GET /api/auth/me`) y `UserController` (`POST /api/users`, `GET /api/users`, `PUT /api/users/{id}/team`), y ajustar `PlayerTransferController` para pasar a quien solicita al service, con las anotaciones Swagger/OpenAPI de los demás controllers y mensajes en español; verificar que `SecurityRulesTest` completo pasa en verde
- [x] 6.4 Eliminar `DirectorTecnicoInterceptor`, `DirectorTecnicoWebConfig`, `DirectorTecnicoController` y la propiedad `app.director-tecnico.clave`, y verificar con una búsqueda en `src/` y con `./mvnw clean test` que nada los referencia y los tests siguen en verde
- [x] 6.5 Agregar el esquema de seguridad `bearerAuth` a `OpenApiConfig` y verificar con un test que `/v3/api-docs` es público y lo incluye

## 7. Backend: Revisar y actualizar los tests unitarios existentes (OBLIGATORIO)

- [x] 7.1 Revisar `MasQueAmigosApplicationTests`, `JugadorServiceTest`, `EquipoServiceTest`, `AlineacionServiceTest`, `PartidoServiceTest`, `EstadisticaServiceTest`, `PlayerTransferServiceTest`, `GlobalExceptionHandlerTest` y los tests de mappers y DTOs, y actualizar los que se rompan por las dependencias, la configuración de seguridad o la firma nueva de `requestTransfer`; confirmar que ningún test existente fue debilitado
- [x] 7.2 Revisar que los tests nuevos cubren cada escenario de `specs/authentication/spec.md`, `specs/user-accounts/spec.md`, `specs/access-control/spec.md` y `specs/player-transfers/spec.md`, y registrar la correspondencia escenario → test en el reporte del grupo 8

## 8. Backend: Ejecutar los tests unitarios y verificar el estado de la base de datos (OBLIGATORIO)

- [x] 8.1 Capturar el baseline de la base de datos MySQL previo a los tests: conteo de filas de `jugador`, `equipos`, `alineacion`, `estadistica` y `player_transfer`, última versión de Flyway y existencia de `app_user`
- [x] 8.2 Ejecutar los tests unitarios focalizados de seguridad, cuentas, autenticación y traspasos con `./mvnw clean test -Dtest=...` y confirmar 0 fallas
- [x] 8.3 Ejecutar la suite completa de tests unitarios con `./mvnw clean test` y registrar el total de tests, fallas y tiempo de ejecución
- [x] 8.4 Verificar el estado de la base de datos después de los tests (mismos conteos del baseline; los tests usan H2) y restaurarlo si hubo mutaciones
- [x] 8.5 Crear el reporte `openspec/changes/add-authentication-roles/specs/reports/YYYY-MM-DD-step-8-unit-test-and-db-verification.md` con comandos, resultados, comparación pre/post de la base de datos y la correspondencia escenario → test
- [x] 8.6 Marcar el paso como completado solo después de que los tests pasen y el reporte exista

## 9. Backend: Testing manual de endpoints con curl (OBLIGATORIO - EL AGENTE DEBE EJECUTAR)

- [x] 9.1 Iniciar el backend local en un puerto libre (por ejemplo 57076) contra el MySQL de `TorneoContainer`, con las variables `APP_SECURITY_JWT_SECRET`, `APP_SECURITY_ADMIN_USERNAME` y `APP_SECURITY_ADMIN_PASSWORD` de prueba, y verificar que Flyway aplica `V8` y que el administrador queda sembrado
- [x] 9.2 Probar `POST /api/auth/login` (credenciales correctas, contraseña incorrecta, usuario inexistente con el mismo mensaje, campos vacíos, usuario con otras mayúsculas) y `GET /api/auth/me` y `POST /api/auth/refresh` con y sin token
- [x] 9.3 Reiniciar el backend con una vigencia corta (por ejemplo `APP_SECURITY_JWT_TTL=PT10S`) y verificar que el token vence a los 10 segundos (401 con el mensaje de sesión expirada), que no se puede renovar un token vencido y que renovar a tiempo extiende la sesión
- [x] 9.4 Probar `POST /api/users`, `GET /api/users` y `PUT /api/users/{id}/team` como administrador (crear DT por defecto, crear otro administrador, rol inválido, usuario duplicado con otras mayúsculas, contraseña corta, DT con equipo, equipo ya asignado, equipo inexistente, equipo a un administrador, asignar, cambiar y quitar equipo) y verificar que ninguna respuesta incluye contraseña ni hash
- [x] 9.5 Probar los permisos con curl: visitante (consultas 200, escrituras 401, `/api/users` 401), DT con equipo (consultas 200, solicitar traspaso hacia su equipo 201, hacia otro equipo 403, confirmar y rechazar 403, crear jugador 403, `/api/users` 403), DT sin equipo (solicitar traspaso 403) y ADMIN (crear y borrar un jugador de prueba, solicitar hacia cualquier equipo, confirmar y rechazar un traspaso); probar que solo el header `X-Director-Tecnico-Key` ya no autoriza y que `POST /api/director-tecnico/validar` ya no existe
- [x] 9.6 Verificar `/v3/api-docs` (esquema `bearerAuth`, rutas nuevas) y `/swagger-ui/index.html` sin sesión
- [x] 9.7 Restaurar la base de datos: eliminar los traspasos, jugadores y cuentas de prueba (incluidas las de `app_user`, dejando la tabla vacía), verificar que los conteos coinciden con el baseline de 8.1 y detener el backend de prueba
- [x] 9.8 Documentar todos los comandos curl y las respuestas en `openspec/changes/add-authentication-roles/specs/reports/YYYY-MM-DD-step-9-manual-curl-testing.md`

## 10. Frontend: Testing E2E con Playwright MCP (No aplica)

- [x] 10.1 Confirmar y documentar que este change no incluye cambios de frontend (vive en el repositorio aparte `MasQueAmigos-Torneo`) y que por lo tanto el testing E2E con Playwright MCP no aplica aquí

## 11. Documentación técnica (OBLIGATORIO)

- [x] 11.1 Actualizar `docs/api-spec.yml`: reemplazar `directorTecnicoKey` por `bearerAuth` (global y por operación), retirar `/api/director-tecnico/validar` y la descripción de la clave fija, documentar `/api/auth/*`, `/api/users` y `PUT /api/users/{id}/team` con sus schemas, el 403 de traspasos del DT y los errores 401 y 403; verificar que el YAML es válido
- [x] 11.2 Actualizar `docs/data-model.md` con la entidad `AppUser` (tabla `app_user`, roles `ADMIN` y `DT`, usuario en minúsculas y único, equipo opcional y único)
- [x] 11.3 Agregar a `docs/backend-standards.md` una sección de seguridad: roles y matriz de permisos, regla de que los endpoints de escritura nuevos exigen `ADMIN` por defecto, autorización por URL (no `@PreAuthorize`) y por qué, variables de entorno nuevas (`APP_SECURITY_JWT_SECRET`, `APP_SECURITY_ADMIN_USERNAME`, `APP_SECURITY_ADMIN_PASSWORD`, `APP_SECURITY_JWT_TTL`) y la nota de CORS con `Authorization`
- [x] 11.4 Preparar y mostrar al usuario el diff propuesto para el `docker-compose.yml` y el `.env.example` del repositorio padre con las variables nuevas (en el compose, `APP_SECURITY_JWT_SECRET`, `APP_SECURITY_ADMIN_USERNAME` y `APP_SECURITY_ADMIN_PASSWORD` son obligatorias con `${VARIABLE:?mensaje}` para que el despliegue falle de forma clara si falta alguna, y `APP_SECURITY_JWT_TTL` es opcional con `${APP_SECURITY_JWT_TTL:-PT3M}`; el código conserva el arranque en modo solo consulta cuando falta el admin, la exigencia vive solo en el compose; en `.env.example` solo marcadores, nunca valores reales), sin aplicarlo; documentar que la reconstrucción de la imagen (skill `docker-rebuild`) requiere esas variables y se hace solo cuando el usuario lo pida
- [x] 11.5 Actualizar el `README.md` del repositorio padre: reemplazar la sección de la clave fija `X-Director-Tecnico-Key` / `app.director-tecnico.clave` (valor `director2025`) por el login con JWT (`/api/auth/login`, `/refresh`, `/me`, `/api/users`), los roles `ADMIN`/`DT`/visitante y las variables `APP_SECURITY_JWT_SECRET`, `APP_SECURITY_ADMIN_USERNAME`, `APP_SECURITY_ADMIN_PASSWORD` y `APP_SECURITY_JWT_TTL` (solo nombres, nunca valores); verificar con `grep` que no queda ninguna mención a la clave fija

## 12. Prompt para el front (OBLIGATORIO al cerrar cada change del backend)

- [x] 12.1 Redactar el prompt para la IA que adapta el front `MasQueAmigos-Torneo` (pantalla de login, envío del token, renovación por actividad en pantalla, manejo de `401` y `403`, equipo del DT, traspasos del DT, retiro del modo con clave fija), con el contrato del API, los cambios incompatibles, los errores y los criterios de aceptación; guardarlo en `docs/frontend-prompts/add-authentication-roles.md` y presentarlo en el chat con la línea "Written for: ..." indicando la audiencia
