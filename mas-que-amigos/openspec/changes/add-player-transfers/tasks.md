# Tasks

## 0. Preparación: Crear el feature branch (OBLIGATORIO - PRIMER PASO)

- [x] 0.1 Crear el feature branch `feature/add-player-transfers-backend` a partir de `master` y verificar con `git branch --show-current` que quedó activo
- [x] 0.2 Verificar el working tree con `git status` y confirmar que ningún archivo sin commitear pertenece a este change antes de empezar

## 1. Backend: Migración de base de datos

- [x] 1.1 Crear `src/main/resources/db/migration/V7__player_cost_and_transfers.sql` con `ALTER TABLE jugador ADD COLUMN costo DECIMAL(12,2) NOT NULL DEFAULT 0` y `CREATE TABLE player_transfer` (`id`, `player_id`, `origin_team_id`, `destination_team_id`, `cost`, `status`, `destination_dorsal`, `requested_at`, `resolved_at`, FKs a `jugador` y `equipos`, índices `(player_id, status)` y `status`), y verificar que Flyway la aplica al arrancar sin errores

## 2. Backend: Costo del jugador (TDD) — spec `player-cost`

- [x] 2.1 En `JugadorServiceTest`, agregar tests en rojo para: crear con costo, crear sin costo (queda 0), editar sin costo (conserva el existente) y mapeo de `costo` a la respuesta; verificar que fallan por la ausencia del campo
- [x] 2.2 Agregar un test de validación de `JugadorDTO` (patrón de `EstadisticaDTOValidationTest`) para costo negativo y con más de dos decimales, con mensaje en español, y verificar que falla en rojo
- [x] 2.3 Agregar `costo` (`BigDecimal`, columna `costo`) a `Jugador`, a `JugadorDTO` (nullable, `@DecimalMin("0.00")`, `@Digits(integer = 10, fraction = 2)`) y a `JugadorMapper#toEntity/toDTO/toDTOExt`; ajustar `JugadorService#guardarJugador` (null → 0) y `#actualizarJugador` (null → conservar), y verificar que los tests de 2.1 y 2.2 pasan en verde

## 3. Backend: Solicitud de traspaso (TDD) — spec `player-transfers`

- [x] 3.1 Crear `PlayerTransferServiceTest` (Mockito + AssertJ, patrón de `PartidoServiceTest`) con tests en rojo para `requestTransfer`: solicitud válida (PENDING, con costo snapshot y equipo origen), destino igual al equipo actual, jugador inexistente (404), equipo destino inexistente (400), varias solicitudes pendientes para el mismo jugador, dorsal ocupado en destino sin dorsal nuevo, y dorsal nuevo libre; verificar que compilan y fallan
- [x] 3.2 Crear el enum `TransferStatus`, la entidad `PlayerTransfer`, el `PlayerTransferRepository` y los DTOs `TransferRequestDTO` / `TransferDTO` (nombres en inglés, mensajes de validación en español) y verificar con `mvnw compile`
- [x] 3.3 Implementar `PlayerTransferService#requestTransfer` y verificar que los tests de 3.1 pasan en verde

## 4. Backend: Confirmación y rechazo (TDD) — spec `player-transfers`

- [x] 4.1 Agregar a `PlayerTransferServiceTest` tests en rojo para `confirmTransfer`: confirmación exitosa (jugador cambia de equipo y de dorsal si aplica, estado CONFIRMED con `resolved_at`), dorsal ocupado al confirmar (queda PENDING y no cambia nada), solicitud no pendiente (400), solicitud inexistente (404), rechazo automático de las demás PENDING del mismo jugador, otras PENDING de otros jugadores intactas, y sin rechazos automáticos si la confirmación falla
- [x] 4.2 Agregar tests en rojo para `rejectTransfer`: rechazo exitoso (no toca al jugador ni a otras solicitudes), solicitud no pendiente (400) e inexistente (404)
- [x] 4.3 Crear `TransferNotFoundException` y su handler 404 (`{"error": ...}`) en `GlobalExceptionHandler`, con test en `GlobalExceptionHandlerTest`, y verificar que pasa
- [x] 4.4 Agregar al repositorio la consulta de rechazo en bloque de las PENDING del jugador (excluyendo la confirmada) y el acceso con bloqueo pesimista a la fila del jugador; implementar `confirmTransfer` (`@Transactional`) y `rejectTransfer`; verificar que los tests de 4.1 y 4.2 pasan en verde

## 5. Backend: Consulta de solicitudes e historial (TDD) — spec `player-transfers`

- [x] 5.1 Agregar tests en rojo para `findTransfers(status, playerId)`: solo PENDING, historial CONFIRMED de un jugador, jugador sin traspasos (lista vacía), sin filtros (todos), orden por `requested_at` descendente, y estado inválido (400 en español)
- [x] 5.2 Implementar `PlayerTransferService#findTransfers` y la consulta del repositorio con filtros opcionales, y verificar que los tests de 5.1 pasan en verde

## 6. Backend: Endpoints

- [x] 6.1 Crear `PlayerTransferController` en `/api/transfers` con `POST /` (201), `POST /{id}/confirm`, `POST /{id}/reject` y `GET /?status=&playerId=`, con las anotaciones Swagger/OpenAPI usadas en los demás controllers y mensajes al usuario en español; verificar con `mvnw compile` y que los endpoints aparecen en Swagger
- [x] 6.2 Verificar que las rutas `POST` nuevas exigen el header `X-Director-Tecnico-Key` (401 sin clave) y que `GET` queda libre, sin tocar el interceptor

## 7. Backend: Revisar y actualizar los tests unitarios existentes (OBLIGATORIO)

- [x] 7.1 Revisar `JugadorServiceTest`, `EquipoServiceTest`, `AlineacionServiceTest`, `PartidoServiceTest`, `EstadisticaServiceTest` y `GlobalExceptionHandlerTest` y actualizar los que se rompan por el campo `costo` o por los cambios en `JugadorMapper`/`GlobalExceptionHandler`; confirmar que ningún test existente fue debilitado
- [x] 7.2 Revisar que `PlayerTransferServiceTest` y los tests de `player-cost` cubren cada escenario de `specs/player-cost/spec.md` y `specs/player-transfers/spec.md` (incluido "editar el costo no altera traspasos ya registrados")

## 8. Backend: Ejecutar los tests unitarios y verificar el estado de la base de datos (OBLIGATORIO)

- [x] 8.1 Capturar el baseline de la base de datos previo a los tests (conteo de filas de `jugador`, `equipos`, `alineacion`, `estadistica`, y existencia de `player_transfer`/columna `costo`)
- [x] 8.2 Ejecutar los tests unitarios focalizados (`mvnw test -Dtest=PlayerTransferServiceTest,JugadorServiceTest,GlobalExceptionHandlerTest`) y confirmar 0 fallas
- [x] 8.3 Ejecutar la suite completa de tests unitarios (`mvnw test`) y registrar total de tests, fallas y tiempo de ejecución
- [x] 8.4 Verificar el estado de la base de datos después de los tests (mismos conteos del baseline) y restaurarlo si hubo mutaciones
- [x] 8.5 Crear el reporte `openspec/changes/add-player-transfers/specs/reports/YYYY-MM-DD-step-8-unit-test-and-db-verification.md` con comandos, resultados y comparación pre/post de la base de datos
- [x] 8.6 Marcar este paso como completado solo después de que los tests pasen y el reporte exista

## 9. Backend: Testing manual de endpoints con curl (OBLIGATORIO - EL AGENTE DEBE EJECUTAR)

- [x] 9.1 Asegurar que el servidor backend esté corriendo (iniciarlo si es necesario, con la clave de Director Técnico configurada) y anotar el estado de `jugador` y `player_transfer`
- [x] 9.2 Probar `POST /api/jugadores` y `PUT /api/jugadores` con y sin `costo`, con costo negativo (400) y verificar `costo` en `GET /api/jugadores`; restaurar los datos creados/editados
- [x] 9.3 Probar `POST /api/transfers`: solicitud válida (201), dos solicitudes pendientes del mismo jugador, mismo equipo (400), jugador inexistente (404), equipo inexistente (400), dorsal ocupado y dorsal nuevo; sin clave de Director Técnico (401)
- [x] 9.4 Probar `POST /api/transfers/{id}/confirm` con un jugador con varias solicitudes pendientes: verificar con `GET /api/jugadores` que cambió de equipo, que las demás quedaron `REJECTED` y no aparecen con `GET /api/transfers?status=PENDING`; probar confirmar de nuevo (400) e id inexistente (404)
- [x] 9.5 Probar `POST /api/transfers/{id}/reject` (200, y 400 si ya no está pendiente) y `GET /api/transfers` con `status`, `playerId`, sin filtros, jugador sin traspasos y `status` inválido (400)
- [x] 9.6 Restaurar la base de datos (devolver el jugador de prueba a su equipo/dorsal originales y eliminar los traspasos y registros de prueba) y verificar que el estado coincide con el de 9.1
- [x] 9.7 Documentar todos los comandos curl y las respuestas en `openspec/changes/add-player-transfers/specs/reports/YYYY-MM-DD-step-9-manual-curl-testing.md`

## 10. Frontend: Testing E2E con Playwright MCP (No aplica)

- [x] 10.1 Confirmar y documentar que este change no incluye cambios de frontend (vive en el repositorio aparte `MasQueAmigos-Torneo`) y que por lo tanto el testing E2E con Playwright MCP no aplica aquí: se confirmó, no hay cambios de frontend en este change (solo backend y documentación), por lo que el E2E con Playwright MCP no aplica

## 11. Documentación técnica (OBLIGATORIO)

- [x] 11.1 Documentar en `docs/api-spec.yml` el campo `costo` de `JugadorDTO` y los endpoints de `/api/transfers` (request, respuestas, estados, rechazo automático) con sus schemas `TransferRequestDTO` y `TransferDTO`
- [x] 11.2 Actualizar `docs/data-model.md`: campo `costo` en Jugador, nueva entidad `PlayerTransfer` (tabla `player_transfer`) y su relación en el diagrama ER, e incluir la nota sobre alineaciones y eliminación de jugadores con traspasos
