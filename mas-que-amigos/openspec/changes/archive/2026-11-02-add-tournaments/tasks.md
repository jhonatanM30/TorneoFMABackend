# Tasks

## 0. Preparación: Crear el feature branch (OBLIGATORIO - PRIMER PASO)

- [x] 0.1 Crear el feature branch `feature/add-tournaments-backend` a partir de `master` y verificar con `git branch --show-current` que quedó activo
- [x] 0.2 Verificar el working tree con `git status` y confirmar que ningún archivo sin commitear pertenece a este change antes de empezar

## 1. Backend: Migración de base de datos

- [x] 1.1 Crear `src/main/resources/db/migration/V9__tournaments.sql` con `CREATE TABLE tournament` (`id`, `name` único, `format`, `status`, `created_at`, `started_at` NULL) y `CREATE TABLE tournament_team` (`id`, `tournament_id`, `team_id`, `enrolled_at`, `UNIQUE (tournament_id, team_id)`, FKs a `tournament` y `equipos`), en el estilo de `V7` (comentario de idioma, `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`)
- [x] 1.2 En la misma migración, insertar el torneo "Histórico" (`format = 'LEGACY'`, `status = 'IN_PROGRESS'`), agregar `partido.id_torneo` como NULL, asignar todos los partidos existentes al Histórico y recién entonces pasar la columna a `NOT NULL` con su FK e índice; verificar con el conteo de `partido` antes/después y `SELECT COUNT(*) FROM partido WHERE id_torneo IS NULL` = 0
- [x] 1.3 Verificar que Flyway aplica `V9` sin errores al arrancar el backend contra la base local (`flyway_schema_history` en `V9`) y que el arranque del contexto sigue pasando en `MasQueAmigosApplicationTests`

## 2. Backend: Creación, consulta, edición y eliminación de torneos (TDD) — spec `tournaments`

- [x] 2.1 Crear `TournamentServiceTest` (Mockito + AssertJ, patrón de `PlayerTransferServiceTest`) con tests en rojo para `create`: creación válida (queda `DRAFT`), formato inválido, formato `LEGACY` rechazado y nombre repetido sin importar mayúsculas; verificar que fallan por ausencia de la clase
- [x] 2.2 Agregar un test de validación de `TournamentRequestDTO` (patrón de `LoginRequestDTOValidationTest`) para nombre vacío, de menos de 3 y de más de 100 caracteres y formato ausente, con mensajes en español, y verificar que falla en rojo
- [x] 2.3 Crear los enums `TournamentFormat` (`LEAGUE`, `GROUPS_KNOCKOUT`, `KNOCKOUT`, `LEGACY`) y `TournamentStatus` (`DRAFT`, `IN_PROGRESS`, `FINISHED`), las entidades `Tournament` y `TournamentTeam`, los repositorios `TournamentRepository` (con `findByIdForUpdate` con bloqueo pesimista, `existsByNameIgnoreCase` y búsqueda del Histórico por formato `LEGACY`) y `TournamentTeamRepository`, y los DTOs `TournamentRequestDTO` / `TournamentDTO` (nombres en inglés, mensajes en español); verificar con `./mvnw compile`
- [x] 2.4 Implementar `TournamentService#create` con el conversor del formato (`String` → `TournamentFormat`, `BusinessException` en español si no es válido ni `LEGACY`) y verificar que los tests de 2.1 y 2.2 pasan en verde
- [x] 2.5 Agregar tests en rojo para `findAll`, `findById` (404 con `NotFoundException`), `update` en `DRAFT` (nombre y formato), `update` de un torneo `IN_PROGRESS`/`FINISHED` (400) y `delete` de un `DRAFT` con inscripciones (se retiran las inscripciones, no los equipos) y de uno no `DRAFT` (400)
- [x] 2.6 Implementar `findAll`, `findById`, `update` y `delete` y verificar que los tests de 2.5 pasan en verde

## 3. Backend: Inscripción de equipos (TDD) — spec `tournament-enrollment`

- [x] 3.1 Agregar a `TournamentServiceTest` tests en rojo para `enrollTeam`: inscripción válida, equipo ya inscrito (400), equipo en otro torneo ya en curso (permitido), equipo inexistente (400), torneo `IN_PROGRESS`/`FINISHED` (400) y torneo Histórico (400)
- [x] 3.2 Agregar tests en rojo para `withdrawTeam`: retiro válido, el retiro no afecta la inscripción en otro torneo, equipo no inscrito (404) y torneo no `DRAFT` (400); y para `findTeams`: ordenados por nombre, lista vacía y torneo inexistente (404)
- [x] 3.3 Implementar `enrollTeam`, `withdrawTeam` y `findTeams` (los dos primeros con bloqueo de fila) y verificar que los tests de 3.1 y 3.2 pasan en verde

## 4. Backend: Inicio, finalización y torneo Histórico (TDD) — spec `tournaments`

- [x] 4.1 Agregar tests en rojo para `start`: inicio válido con ≥ 2 equipos (`IN_PROGRESS` y `started_at`), menos de 2 equipos (400, sigue `DRAFT`) y torneo que no está en `DRAFT` (400)
- [x] 4.2 Agregar tests en rojo para `finish`: finalización válida con todos los partidos `FINALIZADO`, partidos programados o en curso (400 con la cantidad en el mensaje, sigue `IN_PROGRESS`) y torneo que no está en curso (400)
- [x] 4.3 Agregar tests en rojo de protección del Histórico: `update`, `delete`, `start`, `finish` y `enrollTeam` sobre el torneo `LEGACY` responden 400 con mensaje en español
- [x] 4.4 Agregar a `PartidoRepository` la consulta que cuenta los partidos de un torneo que no están `FINALIZADO`; implementar `start`, `finish` y las protecciones del Histórico, y verificar que los tests de 4.1, 4.2 y 4.3 pasan en verde

## 5. Backend: Partidos del torneo (TDD) — spec `tournament-matches`

- [x] 5.1 En `PartidoServiceTest`, agregar tests en rojo para `guardar`: sin `idTorneo` (cae en el Histórico), con `idTorneo` de un torneo `IN_PROGRESS` y equipos inscritos (partido A vs C de reemplazo), torneo inexistente (400), torneo `DRAFT`/`FINISHED` (400), equipo no inscrito (400 indicando cuál), Histórico con equipos no inscritos (permitido) y las reglas generales intactas (mismo equipo y un partido por día con sus mensajes originales); verificar que fallan
- [x] 5.2 Agregar tests en rojo para `actualizarPartido`: el torneo del partido no cambia aunque el request traiga otro `idTorneo`, reemplazo de un equipo por otro inscrito (permitido) y por uno no inscrito (400)
- [x] 5.3 Agregar el campo `torneo` (`@ManyToOne`, columna `id_torneo`, `nullable = false`) a `Partido` y `idTorneo` a `PartidoDTO`; ajustar `PartidoMapper#toDTO/toEntity` y `PartidoMapperTest` (se respeta el idioma español de esos archivos)
- [x] 5.4 Implementar `TournamentService#resolveForNewMatch` y la validación de inscripción para equipos nuevos, e integrarlos en `PartidoService#guardar` y `#actualizarPartido`; verificar que los tests de 5.1 y 5.2 pasan en verde
- [x] 5.5 Agregar tests en rojo e implementar `TournamentService#findMatches` (ordenados por fecha y hora, lista vacía, torneo inexistente 404) con su consulta en `PartidoRepository`, y verificar que pasan en verde
- [x] 5.6 Agregar en `PartidoServiceTest` un test que fije que un partido de torneo no tiene diferenciador de "manual": se crea, se inicia y se finaliza por el mismo camino que cualquier otro y sus goles se calculan igual con `recalcularGoles`

## 6. Backend: Endpoints

- [x] 6.1 Crear `TournamentController` en `/api/tournaments` con `POST /` (201), `GET /`, `GET /{id}`, `PUT /{id}`, `DELETE /{id}` (204), `POST /{id}/start`, `POST /{id}/finish`, `GET /{id}/teams`, `POST /{id}/teams` (201), `DELETE /{id}/teams/{teamId}` (204) y `GET /{id}/matches`, con las anotaciones Swagger/OpenAPI usadas en los demás controllers y mensajes al usuario en español; verificar con `./mvnw compile` y que los endpoints aparecen en `/v3/api-docs`
- [x] 6.2 Documentar en Swagger el campo opcional `idTorneo` de `POST /api/partidos` y verificar que aparece en `PartidoDTO` dentro de `/v3/api-docs`

## 7. Backend: Seguridad de las rutas nuevas (TDD) — spec `tournaments`, requisito "Permisos sobre los torneos"

- [x] 7.1 Agregar a `SecurityRulesTest` casos para `/api/tournaments/**`: `GET` público (sin token), escritura sin token 401, escritura con rol `DT` 403 (crear, iniciar, inscribir) y con `ADMIN` permitida; verificar que pasan sin modificar `SecurityFilterChain` (si fallan, ajustar la regla y documentarlo)

## 8. Backend: Revisar y actualizar los tests unitarios existentes (OBLIGATORIO)

- [x] 8.1 Revisar `PartidoServiceTest`, `PartidoMapperTest`, `EstadisticaServiceTest`, `AlineacionServiceTest`, `SecurityRulesTest` y `GlobalExceptionHandlerTest` y actualizar los que se rompan por `Partido.torneo`/`PartidoDTO.idTorneo` o por las dependencias nuevas de `PartidoService`; confirmar que ningún test existente fue debilitado
- [x] 8.2 Revisar que los tests nuevos cubren cada escenario de `specs/tournaments/spec.md`, `specs/tournament-enrollment/spec.md` y `specs/tournament-matches/spec.md` (incluidos "equipo en varios torneos a la vez", "partido de reemplazo cuando uno se aplaza" y "intento de mover el partido a otro torneo")

## 9. Backend: Ejecutar los tests unitarios y verificar el estado de la base de datos (OBLIGATORIO)

- [x] 9.1 Capturar el baseline de la base de datos previo a los tests (conteo de `partido`, `equipos`, `alineacion`, `estadistica`, `player_transfer`, `app_user`, estado de Flyway y existencia de las tablas `tournament`/`tournament_team`)
- [x] 9.2 Ejecutar los tests unitarios focalizados (`./mvnw test -Dtest=TournamentServiceTest,PartidoServiceTest,PartidoMapperTest,SecurityRulesTest,TournamentRequestDTOValidationTest`) y confirmar 0 fallas
- [x] 9.3 Ejecutar la suite completa de tests unitarios (`./mvnw clean test`) y registrar total de tests, fallas y tiempo de ejecución
- [x] 9.4 Verificar el estado de la base de datos después de los tests (mismos conteos del baseline) y restaurarlo si hubo mutaciones
- [x] 9.5 Crear el reporte `openspec/changes/add-tournaments/specs/reports/YYYY-MM-DD-step-9-unit-test-and-db-verification.md` con comandos, resultados y comparación pre/post de la base de datos
- [x] 9.6 Marcar este paso como completado solo después de que los tests pasen y el reporte exista

## 10. Backend: Testing manual de endpoints con curl (OBLIGATORIO - EL AGENTE DEBE EJECUTAR)

- [x] 10.1 Asegurar que el servidor backend esté corriendo (iniciarlo en un puerto distinto del contenedor Docker si hace falta, con `APP_SECURITY_*` definidos), iniciar sesión como `ADMIN`, y anotar el estado de `partido`, `tournament` y `tournament_team`; confirmar que existe el torneo "Histórico" y que ningún partido tiene `id_torneo` nulo
- [x] 10.2 Probar los endpoints `GET` (`/api/tournaments`, `/{id}`, `/{id}/teams`, `/{id}/matches`) sin token (200) y con un id inexistente (404 con `{"indicadorRespuesta":"Sin Registros","mensaje":...}`, el formato de `NotFoundException` que ya usan partidos y equipos)
- [x] 10.3 Probar `POST /api/tournaments`: creación válida (201, `DRAFT`), formato ausente/inválido/`LEGACY` (400), nombre corto y repetido con otras mayúsculas (400); sin token (401) y con un token `DT` (403)
- [x] 10.4 Probar `PUT /api/tournaments/{id}` (cambio de formato en `DRAFT`, luego intento en un torneo iniciado y en el Histórico) y `DELETE` (borrador 204, iniciado y Histórico 400); restaurar los datos creados
- [x] 10.5 Probar `POST/DELETE /api/tournaments/{id}/teams`: inscribir, duplicado (400), equipo inexistente (400), el mismo equipo en dos torneos, retiro (204) y retiro de un no inscrito (404); restaurar
- [x] 10.6 Probar `start` (con menos de 2 equipos 400, con 2 o más 200, de nuevo 400) y verificar que luego inscribir y retirar dan 400
- [x] 10.7 Probar el flujo de partido manual: en un torneo en curso crear A vs B y luego A vs C con equipos inscritos (201), con un equipo no inscrito (400), en un torneo `DRAFT` (400) y con `idTorneo` inexistente (400); `POST /api/partidos` sin `idTorneo` (cae en el Histórico); verificar con `GET /api/tournaments/{id}/matches` y que el torneo del partido no cambia al editarlo con otro `idTorneo`
- [x] 10.8 Probar `finish`: con partidos pendientes 400 (con la cantidad), eliminar o finalizar los partidos de prueba y luego 200; probar que crear un partido en el torneo `FINISHED` da 400
- [x] 10.9 Documentar todos los comandos curl y las respuestas (con tokens como `<token>` y contraseñas como `***`) en `openspec/changes/add-tournaments/specs/reports/YYYY-MM-DD-step-10-manual-curl-testing.md`
- [x] 10.10 Restaurar la base de datos al baseline (eliminar partidos, inscripciones y torneos de prueba, dejando solo el Histórico) y verificar que los conteos coinciden con los de 10.1

## 11. Frontend: Testing E2E con Playwright MCP (NO APLICA en este change)

- [x] 11.1 Dejar constancia en el reporte del paso 10 de que no aplica Playwright: el front vive en el repositorio aparte `MasQueAmigos-Torneo` y este change no lo modifica; el contrato queda cubierto por el prompt de la tarea 12.3

## 12. Actualizar la documentación técnica (OBLIGATORIO)

- [x] 12.1 Actualizar `docs/data-model.md` con las tablas `tournament` y `tournament_team`, la columna `partido.id_torneo` y el torneo Histórico, y `docs/api-spec.yml` con los endpoints de `/api/tournaments` y el campo `idTorneo` de partidos; verificar que el YAML sigue siendo válido
- [x] 12.2 Actualizar el `README.md` de la carpeta padre (módulo de torneos, formatos, ciclo de vida, partidos manuales, torneo Histórico) y la nota de la hoja de ruta (changes 2 y 3 pendientes), sin incluir valores secretos
- [x] 12.3 Crear `docs/frontend-prompts/add-tournaments.md` con el prompt para la IA del front (`MasQueAmigos-Torneo`): qué cambia (nada incompatible), contrato de `/api/tournaments`, formato de errores, flujo de creación → inscripción → inicio → partidos → finalización, "partido manual" como partido normal del torneo, campo `idTorneo` y criterios de aceptación; en español y con "no hagas commit"
- [x] 12.4 Revisar la documentación de ayuda de pruebas (`GUIA_PRUEBAS.md`, `HERRAMIENTAS_TESTING.md`) y anotar, sin ampliar el alcance, si menciona partidos sin torneo, para dejarlo como pendiente conocido
