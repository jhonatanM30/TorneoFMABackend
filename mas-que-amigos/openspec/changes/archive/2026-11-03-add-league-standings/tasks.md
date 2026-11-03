# Tasks

## 0. Preparación: Crear el feature branch (OBLIGATORIO - PRIMER PASO)

- [x] 0.1 Crear el feature branch `feature/add-league-standings-backend` a partir de `master` y verificar con `git branch --show-current` que quedó activo
- [x] 0.2 Verificar el working tree con `git status` y confirmar que ningún archivo sin commitear pertenece a este change antes de empezar (el `.claude/settings.json` sin versionar no es de este change)

## 1. Backend: Clasificación de la liga (TDD) — spec `league-standings`

- [x] 1.1 Crear `LeagueStandingsCalculatorTest` (JUnit + AssertJ, sin Spring) con tests en rojo para la puntuación y las columnas: el ejemplo de 3 equipos A/B/C de la spec (partidos jugados, ganados, empatados, perdidos, goles, diferencia y puntos), equipo sin partidos con todo en 0, victoria 3 / empate 1 / derrota 0; verificar que fallan por ausencia de la clase
- [x] 1.2 Agregar tests en rojo para el desempate: dos equipos por enfrentamiento directo aunque el rival tenga mejor diferencia total, tres equipos con mini tabla (orden B, A, C), enfrentamiento directo sin diferencia (pasa a diferencia de goles total), diferencia de goles y luego goles a favor, juego limpio (menos puntos queda arriba) y empate total por nombre
- [x] 1.3 Agregar tests en rojo para las posiciones consecutivas (1..n, sin repetir, lista ordenada) y para un conjunto de partidos vacío (todos en 0, ordenados por nombre)
- [x] 1.4 Crear `StandingDTO` (`position`, `teamId`, `teamName`, `played`, `won`, `drawn`, `lost`, `goalsFor`, `goalsAgainst`, `goalDifference`, `points`, `fairPlayPoints`) y `LeagueStandingsCalculator` con constantes nombradas para los puntos (3-1-0) e implementar la ordenación en una sola pasada con mini tabla de los empatados (Decisión 2 del design); verificar que los tests de 1.1 a 1.3 pasan en verde

## 2. Backend: Consulta de la tabla (TDD) — spec `league-standings`

- [x] 2.1 En `TournamentServiceTest`, agregar tests en rojo para `standings(tournamentId)`: torneo `LEAGUE` con partidos finalizados (solo cuentan los `FINALIZADO`), partidos programados o en curso ignorados, partido manual que cuenta igual que los demás, equipos inscritos sin partidos presentes, puntos de juego limpio (amarilla 1, roja 2) tomados de la consulta agregada, formato que no es `LEAGUE` (incluido el Histórico) rechazado con 400 y torneo inexistente con 404
- [x] 2.2 Agregar a `PartidoRepository` la consulta de partidos de un torneo por estado (`findByTorneoIdAndEstado`) y a `EstadisticaRepository` la consulta agregada de tarjetas por equipo para los partidos finalizados de un torneo (proyección con equipo, amarillas y rojas); verificar con `./mvnw clean compile`
- [x] 2.3 Implementar `TournamentService#standings` (solo lectura) reuniendo los datos y delegando en `LeagueStandingsCalculator`; verificar que los tests de 2.1 pasan en verde
- [x] 2.4 Agregar `GET /{id}/standings` a `TournamentController` con las anotaciones Swagger/OpenAPI de los demás endpoints y mensajes en español; verificar que aparece en `/v3/api-docs`

## 3. Backend: Generación del calendario (TDD) — spec `league-schedule`

- [x] 3.1 Crear `LeagueScheduleGeneratorTest` (sin Spring) con tests en rojo: 4 equipos dan 6 partidos en 3 jornadas con cada equipo una vez por jornada, 5 equipos dan 10 partidos en 5 jornadas con un descanso por equipo, cada pareja aparece exactamente una vez, y para `n = 2..12` la diferencia local−visitante de cada equipo es ≤ 1; verificar que fallan por ausencia de la clase
- [x] 3.2 Implementar `LeagueScheduleGenerator` (método del círculo con descanso ficticio para `n` impar y localía de Berger, devolviendo parejas por jornada) y verificar que los tests de 3.1 pasan en verde; si el reparto de localía no cumple el ≤ 1, ajustar el algoritmo y no la spec
- [x] 3.3 En `TournamentServiceTest`, agregar tests en rojo para `generateSchedule`: generación válida (6 partidos en 4 equipos con fechas `inicio + k × intervalo`, hora indicada, estado programado, fase `FASE_DE_GRUPOS`, torneo asignado, intervalo por defecto 7), formato que no es `LEAGUE` (400), torneo `DRAFT`/`FINISHED` (400), torneo ya con partidos (400, sin crear nada), menos de 2 equipos (400), torneo inexistente (404), choque de fecha de un equipo con partido de otro torneo (400 nombrando equipo y fecha, sin `saveAll`)
- [x] 3.4 Crear `ScheduleRequestDTO` (`startDate` y `time` obligatorios, `intervalDays` opcional entre 1 y 30, mensajes en español) con su test de validación en el patrón de `TournamentRequestDTOValidationTest`; agregar a `PartidoRepository` `countByTorneoId` y `existePartidoEnFechaParaEquipo(fecha, idEquipo)`; verificar con `./mvnw clean compile`
- [x] 3.5 Implementar `TournamentService#generateSchedule` (`@Transactional`, bloqueo del torneo, orden de validaciones del design, `saveAll` solo al final) y verificar que los tests de 3.3 y 3.4 pasan en verde
- [x] 3.6 Agregar `POST /{id}/schedule` (201) a `TournamentController` con Swagger y verificar que aparece en `/v3/api-docs`
- [x] 3.7 Agregar tests en `PartidoServiceTest` que fijen que un partido generado se edita y se elimina como cualquier otro (cambio de fecha conserva torneo y equipos; eliminación de uno programado) y verificar que pasan

## 4. Backend: Seguridad de las rutas nuevas (TDD) — specs `league-standings` y `league-schedule`

- [x] 4.1 Agregar a `SecurityRulesTest` casos para `GET /api/tournaments/{id}/standings` (público, 404 en un id inexistente y no 401/403) y `POST /api/tournaments/{id}/schedule` (visitante 401, `DT` 403, `ADMIN` no bloqueado), y verificar que pasan sin modificar `SecurityFilterChain`

## 5. Backend: Revisar y actualizar los tests unitarios existentes (OBLIGATORIO)

- [x] 5.1 Revisar `TournamentServiceTest`, `PartidoServiceTest`, `EstadisticaServiceTest` y `SecurityRulesTest` y actualizar los que se rompan por las dependencias o consultas nuevas; confirmar que ningún test existente fue debilitado
- [x] 5.2 Revisar que los tests nuevos cubren cada escenario de `specs/league-standings/spec.md` y `specs/league-schedule/spec.md` (incluidos "un partido manual cuenta como cualquier otro", "tres equipos con mini tabla" y "equipo ocupado en una fecha")

## 6. Backend: Ejecutar los tests unitarios y verificar el estado de la base de datos (OBLIGATORIO)

- [x] 6.1 Capturar el baseline de la base de datos previo a los tests (conteo de `partido`, `equipos`, `estadistica`, `tournament`, `tournament_team`, `app_user`, estado de Flyway) sobre una copia de la base real, no sobre la real
- [x] 6.2 Ejecutar los tests unitarios focalizados (`./mvnw clean test -Dtest=LeagueStandingsCalculatorTest,LeagueScheduleGeneratorTest,TournamentServiceTest,PartidoServiceTest,SecurityRulesTest`) y confirmar 0 fallas
- [x] 6.3 Ejecutar la suite completa (`./mvnw clean test`) y registrar total de tests, fallas y tiempo de ejecución
- [x] 6.4 Verificar el estado de la base de datos después de los tests (mismos conteos del baseline) y restaurarlo si hubo mutaciones
- [x] 6.5 Crear el reporte `openspec/changes/add-league-standings/specs/reports/YYYY-MM-DD-step-6-unit-test-and-db-verification.md` con comandos, resultados y comparación pre/post de la base de datos
- [x] 6.6 Marcar este paso como completado solo después de que los tests pasen y el reporte exista

## 7. Backend: Testing manual de endpoints con curl (OBLIGATORIO - EL AGENTE DEBE EJECUTAR)

- [x] 7.1 Asegurar que el servidor backend esté corriendo contra una copia de la base (puerto distinto de 57075, `APP_SECURITY_*` definidos), iniciar sesión como `ADMIN` y anotar el estado de `partido`, `tournament` y `tournament_team`
- [x] 7.2 Preparar un torneo de liga con 4 equipos inscritos e iniciado y probar `POST /api/tournaments/{id}/schedule`: válido (201, 6 partidos en 3 fechas, cada equipo una vez por fecha), con intervalo, de nuevo (400 por ya tener partidos), sin fecha u hora (400), intervalo fuera de rango (400), sin token (401) y con token `DT` (403)
- [x] 7.3 Probar precondiciones del calendario: torneo `KNOCKOUT` (400), torneo en `DRAFT` (400), el Histórico (400), torneo inexistente (404) y un choque de fecha con un partido de otro torneo (400 con equipo y fecha, sin crear ningún partido)
- [x] 7.4 Probar `GET /api/tournaments/{id}/standings`: sin partidos finalizados (4 equipos en 0, ordenados por nombre), después de jugar y finalizar partidos con estadísticas (goles y tarjetas) verificar puntos, diferencia, juego limpio y el orden de desempate; un partido manual de reemplazo que cuenta; un torneo que no es de liga (400); torneo inexistente (404); y sin token (200)
- [x] 7.5 Documentar todos los comandos curl y las respuestas (tokens como `<token>`, contraseñas como `***`) en `openspec/changes/add-league-standings/specs/reports/YYYY-MM-DD-step-7-manual-curl-testing.md`
- [x] 7.6 Restaurar la copia de la base al baseline (eliminar partidos, estadísticas, inscripciones, torneos y cuentas de prueba) y verificar que los conteos coinciden con los de 7.1

## 8. Frontend: Testing E2E con Playwright MCP (NO APLICA en este change)

- [x] 8.1 Dejar constancia en el reporte del paso 7 de que no aplica Playwright: el front vive en el repositorio aparte `MasQueAmigos-Torneo` y este change no lo modifica; el contrato queda cubierto por la Parte 2 del prompt de la tarea 9.3

## 9. Actualizar la documentación técnica (OBLIGATORIO)

- [x] 9.1 Actualizar `docs/api-spec.yml` con `GET /api/tournaments/{id}/standings` y `POST /api/tournaments/{id}/schedule` y sus esquemas (`StandingDTO`, `ScheduleRequestDTO`), y `docs/data-model.md` con la nota de que la tabla y la jornada no se guardan; verificar que el YAML sigue siendo válido
- [x] 9.2 Actualizar el `README.md` de la carpeta padre (tabla de posiciones, criterios de desempate, calendario de una vuelta y estado de los changes), sin valores secretos
- [x] 9.3 Completar la Parte 2 de `docs/frontend-prompts/tournaments-module.md` con endpoints, forma de la tabla, calendario, reglas de desempate, cambios sobre las pantallas de la Parte 1 y criterios de aceptación; **mantener** el aviso de "no entregar todavía" (el front se entrega solo cuando los tres changes del backend estén listos), actualizar su tabla de estado (change 2 implementado) y agregar una Parte 3 pendiente para el change 3 (grupos y eliminatoria); en español y con "no hagas commit"
