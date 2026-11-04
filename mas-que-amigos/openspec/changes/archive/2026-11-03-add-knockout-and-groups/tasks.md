# Tasks

## 0. Preparación: Crear el feature branch (OBLIGATORIO - PRIMER PASO)

- [x] 0.1 Crear el feature branch `feature/add-knockout-and-groups-backend` a partir de `master` y verificar con `git branch --show-current` que quedó activo
- [x] 0.2 Verificar el working tree con `git status` y confirmar que ningún archivo sin commitear pertenece a este change antes de empezar (el `.claude/settings.json` sin versionar no es de este change)

## 1. Backend: Migración de base de datos

- [x] 1.1 Crear `src/main/resources/db/migration/V10__knockout_and_groups.sql` con `ALTER TABLE partido ADD COLUMN penales_local INT NULL, ADD COLUMN penales_visitante INT NULL`, `ALTER TABLE tournament ADD COLUMN qualifiers_per_group INT NULL`, `CREATE TABLE tournament_group` (`id`, `tournament_id` FK, `name`, único `(tournament_id, name)`), `ALTER TABLE tournament_team ADD COLUMN group_id BIGINT NULL` con FK a `tournament_group`, y `CREATE TABLE bracket_tie` (`id`, `tournament_id` FK, `kind`, `round_number`, `position_in_round`, `home_team_id`, `away_team_id`, `winner_team_id` FKs a `equipos` NULL, `match_id` FK a `partido(id_partido)` NULL, `winner_next_tie_id` y `loser_next_tie_id` FKs a la misma tabla NULL con sus `..._is_home`, `scheduled_date`, `scheduled_time`, único `(tournament_id, kind, round_number, position_in_round)`, índice en `match_id`), en el estilo de `V9` (comentario de idioma, `ENGINE=InnoDB DEFAULT CHARSET=utf8mb4`)
- [x] 1.2 Respaldar la base real con `mysqldump`, crear una copia nueva de la base real (que ya está en V9), arrancar el backend contra ella y verificar que Flyway aplica `V10` sin errores (`flyway_schema_history` en `V10`), que las tablas y columnas existen y que los conteos de `partido`, `tournament` y `tournament_team` no cambian

## 2. Backend: Helper compartido de choques de fecha (TDD)

- [x] 2.1 Crear `MatchDateConflictsTest` (Mockito, sin Spring) con tests en rojo para un componente `MatchDateConflicts#requireFree(date, team)`: no lanza si el equipo no juega esa fecha y lanza `BusinessException` en español que nombra al equipo y la fecha si ya juega; verificar que falla por ausencia de la clase
- [x] 2.2 Crear `MatchDateConflicts` sobre `PartidoRepository#existePartidoEnFechaParaEquipo` y reemplazar con él el bucle de choques de `TournamentService#generateSchedule`; verificar que los tests de 2.1 y los 14 de `TournamentServiceTest` del calendario siguen en verde sin cambiar sus expectativas

## 3. Backend: Penales y ganador de un partido (TDD) — spec `tournament-knockout`

- [x] 3.1 Crear `MatchWinnerTest` (sin Spring) con tests en rojo para `MatchWinner#of(partido)`: gana el de más goles, con el marcador igual gana el de más penales, con marcador igual y sin penales o con penales iguales no hay ganador (devuelve vacío), y devuelve también el perdedor; verificar que fallan por ausencia de la clase
- [x] 3.2 Agregar `penalesLocal` y `penalesVisitante` (`Integer`, columnas `penales_local` y `penales_visitante`, nulas) a `Partido`, a `PartidoDTO` (solo lectura, solo cuando existen) y a `PartidoMapper#toDTO`, con tests en `PartidoMapperTest`; implementar `MatchWinner` y verificar que los tests de 3.1 y los del mapper pasan en verde
- [x] 3.3 Crear `PenaltiesRequestDTO` (campos `homePenalties` y `awayPenalties` con `@JsonProperty("penalesLocal")` y `("penalesVisitante")`, obligatorios, `>= 0`, mensajes en español) con su test de validación en el patrón de `ScheduleRequestDTOValidationTest`; verificar que pasa
- [x] 3.4 En `PartidoServiceTest`, agregar tests en rojo para `registrarPenales(id, request)`: penales válidos 5-4 en un partido en curso que exige ganador (el marcador no cambia), penales iguales (400), partido que no exige ganador (400), partido programado o finalizado (400) y partido inexistente (404); la pregunta "exige ganador" se delega en `BracketService#requiresWinner` (mock)
- [x] 3.5 Agregar `PartidoService#registrarPenales` y `PUT /api/partidos/{id}/penales` en `PartidoController` con Swagger y mensajes en español; crear `BracketService` con su dependencia mínima (`requiresWinner` devuelve falso hasta el grupo 7, donde se completa) y verificar que los tests de 3.4 pasan en verde

## 4. Backend: Armado puro del cuadro (TDD) — specs `tournament-knockout` y `tournament-groups`

- [x] 4.1 Crear `BracketBuilderTest` (sin Spring) con tests en rojo para `build(pairings, thirdPlace)`: 8 equipos dan 3 rondas con 4, 2 y 1 cruces; los ganadores de los cruces `2k` y `2k+1` alimentan al cruce `k` de la ronda siguiente con el lado correcto (local el primero, visitante el segundo); la final no tiene cruce siguiente; con tercer puesto los perdedores de las dos semifinales alimentan al cruce `THIRD_PLACE`; sin tercer puesto no existe; con 2 equipos hay una sola ronda y no hay tercer puesto; con 4 equipos las semifinales son la primera ronda; verificar que fallan por ausencia de la clase
- [x] 4.2 Agregar tests en rojo para `groupPairings(qualifiersByGroup)`: 2 grupos con 2 clasificados dan `1.º A vs 2.º B` y `1.º B vs 2.º A`; 4 grupos con 2 clasificados dan, en orden, `1A-2B, 1C-2D, 1B-2A, 1D-2C`; 4 grupos con 1 clasificado dan `1A-1B, 1C-1D`; 2 grupos con 1 clasificado dan `1A-1B`; y un test que comprueba que en el caso de 8 clasificados dos equipos del mismo grupo quedan en mitades opuestas del cuadro
- [x] 4.3 Implementar `BracketBuilder` (devuelve el plan con rondas, posiciones, enlaces y tercer puesto, sin base de datos) y verificar que los tests de 4.1 y 4.2 pasan en verde

## 5. Backend: Reparto puro de grupos (TDD) — spec `tournament-groups`

- [x] 5.1 Crear `GroupDrawTest` (sin Spring) con tests en rojo: 8 equipos en 2 grupos dan 4 y 4; 9 equipos en 2 grupos dan 5 y 4 (diferencia máxima 1); 10 en 4 grupos dan 3, 3, 2 y 2; cada equipo aparece una sola vez; con la misma semilla el sorteo se repite y con otra cambia; los nombres de grupo son A, B, C...; y la validación de una composición manual (omite equipos, repite uno, incluye uno ajeno, cantidad de grupos distinta) devuelve el motivo
- [x] 5.2 Implementar `GroupDraw` con `java.util.Random` inyectable y verificar que los tests de 5.1 pasan en verde

## 6. Backend: Generación de las llaves de eliminatoria directa (TDD) — spec `tournament-knockout`

- [x] 6.1 Crear las entidades `BracketTie` y `TournamentGroup` (y el enum `BracketTieKind` con `MAIN` y `THIRD_PLACE`), agregar `group` a `TournamentTeam` y `qualifiersPerGroup` a `Tournament`, y los repositorios `BracketTieRepository` y `TournamentGroupRepository`; verificar con `./mvnw clean compile` y que el contexto sigue arrancando (`MasQueAmigosApplicationTests`)
- [x] 6.2 Crear `BracketServiceTest` (Mockito + AssertJ) con tests en rojo para `generateBracket` en `KNOCKOUT`: 8 equipos crean 3 rondas, 4 partidos de cuartos programados con fase `ELIMINACION_DIRECTA`, fecha y hora de la ronda 1 y `torneo` asignado; las fechas de las rondas son `inicio + (r−1) × intervalo` con la final y el tercer puesto el mismo día; 2 equipos crean solo la final con fase `FINAL`; formato `LEAGUE`, Histórico o torneo no en curso (400); llaves ya generadas (400); 5 equipos (400 con el mensaje de potencia de 2 y repechaje); un equipo eliminado en un repechaje finalizado no participa; repechaje sin finalizar (400); fecha de inicio no posterior al último partido (400); torneo inexistente (404); choque de fecha de un equipo en la primera ronda (400 sin crear nada)
- [x] 6.3 Agregar tests en rojo para el sorteo y los cruces manuales: con un `Random` con semilla el sorteo reparte a todos los equipos una sola vez y es repetible; con `pairings` válidos la primera ronda queda exactamente así y en ese orden; `pairings` incompletos, con un equipo repetido, eliminado o no inscrito (400)
- [x] 6.4 Crear `BracketRequestDTO` (`startDate` y `time` obligatorios, `intervalDays` opcional 1 a 30, `thirdPlaceMatch` opcional con valor por defecto `true`, `pairings` opcional con `homeTeamId` y `awayTeamId`) con su test de validación, y agregar a `PartidoRepository` `findMaxFechaByTorneoId` y las consultas de partidos por torneo y fase necesarias (repechaje y fase de grupos); verificar con `./mvnw clean compile`
- [x] 6.5 Implementar `BracketService#generateBracket` para `KNOCKOUT` (`@Transactional`, bloqueo del torneo, validaciones en el orden del design, `BracketBuilder`, persistencia de los cruces con sus enlaces, partidos de la primera ronda) y verificar que los tests de 6.2 y 6.3 pasan en verde

## 7. Backend: Avance, tercer puesto y protección (TDD) — spec `tournament-knockout`

- [x] 7.1 En `BracketServiceTest`, agregar tests en rojo para `requiresWinner(partido)` (partido enlazado a un cruce, `REPECHAJE` en `KNOCKOUT`, y casos que no lo exigen: liga, grupos, partido manual normal) y para `requireDecided(partido)` (marcador distinto pasa; empate con penales distintos pasa; empate sin penales o con penales iguales lanza `BusinessException` en español)
- [x] 7.2 Agregar tests en rojo para `onMatchFinished(partido)`: el ganador ocupa su lugar en el cruce siguiente y el partido siguiente aún no se crea si falta el rival; cuando se completa el cruce se crea el partido programado con la fecha y la hora de su ronda y fase `ELIMINACION_DIRECTA` (`FINAL` para la final); el perdedor de una semifinal pasa al tercer puesto y este se crea con la fecha de la final al conocerse ambos; sin tercer puesto el perdedor no se coloca en ningún lado; la final finalizada deja al campeón; un partido que no es de las llaves no hace nada; los partidos creados no verifican choque de fecha
- [x] 7.3 Agregar tests en rojo para la consulta `getBracket(id)`: rondas con su nombre en español (octavos de final, cuartos de final, semifinales, final), cruces con equipos vacíos si no se conocen, marcador, penales y ganador, tercer puesto y campeón; torneo sin llaves devuelve rondas vacías; torneo inexistente (404); y para `isBracketMatch(partidoId)` y `requireChampion(tournament)`
- [x] 7.4 Crear los DTOs de la consulta (`BracketDTO`, `BracketRoundDTO`, `BracketTieDTO` y `TeamRefDTO`, nombres en inglés) e implementar `requiresWinner`, `requireDecided`, `onMatchFinished`, `getBracket`, `isBracketMatch` y `requireChampion` en `BracketService`; verificar que los tests de 7.1 a 7.3 pasan en verde

## 8. Backend: Grupos y eliminatoria posterior (TDD) — spec `tournament-groups`

- [x] 8.1 Crear `GroupStageServiceTest` con tests en rojo para `createGroups`: 8 equipos en 2 grupos con 2 clasificados crean los grupos A y B de 4 equipos, 6 partidos por grupo en 3 fechas compartidas, fase `FASE_DE_GRUPOS`, hora indicada, `group` asignado a cada inscripción y `qualifiersPerGroup` guardado; 9 equipos dan 5 y 4; composición manual válida; composición manual inválida (400); cantidad de grupos que no es potencia de 2, grupo con menos de 2 equipos, clasificados fuera de 1 o 2 o no menores al grupo más pequeño (400 con la regla); formato que no es `GROUPS_KNOCKOUT`, torneo no en curso, que ya tiene grupos o partidos (400); choque de fecha (400 sin crear nada); torneo inexistente (404)
- [x] 8.2 Agregar tests en rojo para `getGroups(id)`: la tabla de un grupo cuenta solo los partidos `FINALIZADO` de fase `FASE_DE_GRUPOS` entre equipos del grupo (incluido un partido manual), los de las llaves no cuentan, el juego limpio usa la consulta filtrada por fase, los clasificados por grupo se informan, y un torneo sin grupos o que no es `GROUPS_KNOCKOUT` devuelve lista vacía; y para `qualifiers(id)` (los primeros de cada tabla en orden de grupo)
- [x] 8.3 Agregar a `EstadisticaRepository` la consulta de tarjetas por equipo filtrada por fase (`sumarTarjetasPorEquipoEnTorneoYFase`) y a `PartidoRepository` las consultas de partidos por torneo, estado y fase; crear `GroupsRequestDTO` (`groupCount`, `qualifiersPerGroup`, `startDate`, `time`, `intervalDays` opcional, `groups` opcional con `teamIds`) y `GroupDTO` con su test de validación; verificar con `./mvnw clean compile`
- [x] 8.4 Implementar `GroupStageService` (`createGroups` atómico con `GroupDraw`, `LeagueScheduleGenerator`, `MatchDateConflicts`; `getGroups` y `qualifiers` con `LeagueStandingsCalculator`) y verificar que los tests de 8.1 y 8.2 pasan en verde
- [x] 8.5 En `BracketServiceTest`, agregar tests en rojo para `generateBracket` en `GROUPS_KNOCKOUT`: 2 grupos con 2 clasificados dan semifinales `1A-2B` y `1B-2A`; 4 grupos con 2 clasificados dan los 4 cuartos en el orden de la spec; 1 clasificado por grupo; grupos con partidos sin finalizar (400 con la cantidad); sin grupos o llaves ya generadas (400); `pairings` enviados (400); fecha de inicio no posterior al último partido (400); y que estas llaves avanzan igual que las de eliminatoria directa
- [x] 8.6 Extender `BracketService#generateBracket` para `GROUPS_KNOCKOUT` usando `GroupStageService#qualifiers` y `BracketBuilder#groupPairings`, y verificar que los tests de 8.5 pasan en verde

## 9. Backend: Integración con partidos y finalización de torneos (TDD)

- [x] 9.1 En `PartidoServiceTest`, agregar tests en rojo: `finalizarPartido` rechaza un empate sin penales cuando el partido exige ganador, finaliza un partido con ganador en el marcador o con penales y llama a `BracketService#onMatchFinished` después de finalizar; un partido que no exige ganador finaliza como siempre; `eliminar` rechaza un partido de las llaves con mensaje en español; `actualizarPartido` rechaza cambiar los equipos de un partido de las llaves pero acepta cambiar fecha y hora
- [x] 9.2 Integrar `BracketService` en `PartidoService` (`finalizarPartido` transaccional con `requireDecided` antes y `onMatchFinished` después, `eliminar` y `actualizarPartido` con `isBracketMatch`) y verificar que los tests de 9.1 y los existentes de `PartidoServiceTest` pasan en verde
- [x] 9.3 En `TournamentServiceTest`, agregar tests en rojo para `finish`: un torneo `KNOCKOUT` o `GROUPS_KNOCKOUT` sin llaves o con la final sin finalizar se rechaza con el mensaje de campeón aunque no queden partidos pendientes; con la final finalizada se finaliza; `LEAGUE` y los demás casos no cambian; implementar la regla y verificar que pasan en verde junto con los 14 tests de finalización existentes

## 10. Backend: Endpoints

- [x] 10.1 Agregar a `TournamentController` `POST /{id}/bracket` (201), `GET /{id}/bracket`, `POST /{id}/groups` (201) y `GET /{id}/groups` con las anotaciones Swagger/OpenAPI de los demás endpoints y mensajes en español; verificar con `./mvnw clean compile` y que aparecen en `/v3/api-docs`, junto con `PUT /api/partidos/{id}/penales` y los campos `penalesLocal` y `penalesVisitante` de `PartidoDTO`

## 11. Backend: Seguridad de las rutas nuevas (TDD) — specs `tournament-knockout` y `tournament-groups`

- [x] 11.1 Agregar a `SecurityRulesTest` casos para `POST /api/tournaments/{id}/bracket`, `POST /api/tournaments/{id}/groups` y `PUT /api/partidos/{id}/penales` (visitante 401, `DT` 403, `ADMIN` no bloqueado) y para `GET /api/tournaments/{id}/bracket` y `/groups` (públicos, 404 en un id inexistente y no 401/403); verificar que pasan sin modificar `SecurityFilterChain`

## 12. Backend: Revisar y actualizar los tests unitarios existentes (OBLIGATORIO)

- [x] 12.1 Revisar `TournamentServiceTest`, `PartidoServiceTest`, `PartidoMapperTest`, `EstadisticaServiceTest` y `SecurityRulesTest` y actualizar los que se rompan por las dependencias nuevas (`BracketService`, `MatchDateConflicts`) o por los campos de penales; confirmar que ningún test existente fue debilitado
- [x] 12.2 Revisar que los tests nuevos cubren cada escenario de `specs/tournament-knockout/spec.md`, `specs/tournament-groups/spec.md` y `specs/tournaments/spec.md` (incluidos "gana el de más penales", "los de un mismo grupo en mitades opuestas", "los partidos de las llaves no cuentan en la tabla del grupo" y "eliminatoria sin campeón definido")

## 13. Backend: Ejecutar los tests unitarios y verificar el estado de la base de datos (OBLIGATORIO)

- [x] 13.1 Capturar el baseline de la base de datos previo a los tests (conteo de `partido`, `equipos`, `estadistica`, `tournament`, `tournament_team`, `tournament_group`, `bracket_tie`, `app_user`, estado de Flyway) sobre la copia de la base real, no sobre la real
- [x] 13.2 Ejecutar los tests unitarios focalizados (`./mvnw clean test -Dtest=BracketBuilderTest,GroupDrawTest,MatchWinnerTest,BracketServiceTest,GroupStageServiceTest,TournamentServiceTest,PartidoServiceTest,MatchDateConflictsTest,SecurityRulesTest`) y confirmar 0 fallas
- [x] 13.3 Ejecutar la suite completa (`./mvnw clean test`) y registrar total de tests, fallas y tiempo de ejecución
- [x] 13.4 Verificar el estado de la base de datos después de los tests (mismos conteos del baseline) y restaurarlo si hubo mutaciones
- [x] 13.5 Crear el reporte `openspec/changes/add-knockout-and-groups/specs/reports/YYYY-MM-DD-step-13-unit-test-and-db-verification.md` con comandos, resultados y comparación pre/post de la base de datos
- [x] 13.6 Marcar este paso como completado solo después de que los tests pasen y el reporte exista

## 14. Backend: Testing manual de endpoints con curl (OBLIGATORIO - EL AGENTE DEBE EJECUTAR)

- [x] 14.1 Asegurar que el servidor backend esté corriendo contra la copia de la base (puerto distinto de 57075, `APP_SECURITY_*` definidos), iniciar sesión como `ADMIN` y crear una cuenta `DT` temporal; anotar el estado de `partido`, `tournament`, `tournament_team`, `tournament_group` y `bracket_tie`
- [x] 14.2 Penales: en un partido de torneo en curso que exige ganador registrar penales válidos (200), iguales (400) y negativos (400); en un partido de liga o de grupos (400); en uno programado o finalizado (400); partido inexistente (404); sin token (401) y con token `DT` (403)
- [x] 14.3 Eliminatoria con 8 equipos: generar las llaves con sorteo (201) y verificar con `GET /bracket` los 4 cuartos, las rondas y las fechas; generar de nuevo (400); fecha de inicio anterior al último partido (400); sin fecha y hora (400); intervalo fuera de rango (400); y con token `DT` (403)
- [x] 14.4 Eliminatoria con 5 equipos: intentar generar (400 con el mensaje de potencia de 2), armar un partido de `REPECHAJE`, finalizarlo con un empate que exige penales (400 sin penales, 200 con penales) y generar las llaves con los 4 restantes; probar también `pairings` manuales (válidos, incompletos y con un equipo repetido)
- [x] 14.5 Avance: iniciar y finalizar los partidos de cuartos (con la preparación de datos que haga falta, documentando qué se hizo por SQL y qué por API), verificar con `GET /bracket` que los ganadores pasan, que los partidos de semifinales se crean al completarse los cruces, que los perdedores de semifinales juegan el tercer puesto, que una final empatada exige penales, y que el campeón aparece; probar que eliminar o cambiar los equipos de un partido de las llaves da 400 y que cambiar su fecha da 200
- [x] 14.6 Finalizar el torneo: con la final sin finalizar (400 por campeón no definido), y con todo finalizado (200)
- [x] 14.7 Grupos con 8 equipos en 2 grupos: formar los grupos con sorteo (201, 4 equipos por grupo y 6 partidos por grupo en 3 fechas), con composición manual, con 9 equipos en 2 grupos (5 y 4), y los casos de error (cantidad de grupos que no es potencia de 2, clasificados inválidos, grupos ya formados, choque de fecha sin crear nada); `GET /groups` con las tablas en cero y, tras finalizar partidos de grupo, con puntos, diferencia, juego limpio y el partido manual de reemplazo contando
- [x] 14.8 Eliminatoria tras los grupos: con partidos de grupo sin finalizar (400 con la cantidad), con todos finalizados generar las llaves (201) y verificar el cruce `1.º A vs 2.º B` y `1.º B vs 2.º A`; con `pairings` (400); con 4 grupos y 2 clasificados verificar el orden de los 4 cuartos de la spec
- [x] 14.9 Documentar todos los comandos curl y las respuestas (tokens como `<token>`, contraseñas como `***`) en `openspec/changes/add-knockout-and-groups/specs/reports/YYYY-MM-DD-step-14-manual-curl-testing.md`
- [x] 14.10 Restaurar la copia de la base al baseline (eliminar partidos, estadísticas, cruces, grupos, inscripciones, torneos y cuentas de prueba) y verificar que los conteos coinciden con los de 14.1

## 15. Frontend: Testing E2E con Playwright MCP (NO APLICA en este change)

- [x] 15.1 Dejar constancia en el reporte del paso 14 de que no aplica Playwright: el front vive en el repositorio aparte `MasQueAmigos-Torneo` y este change no lo modifica; el contrato queda cubierto por la Parte 3 del prompt de la tarea 16.3

## 16. Actualizar la documentación técnica (OBLIGATORIO)

- [x] 16.1 Actualizar `docs/api-spec.yml` con `PUT /api/partidos/{id}/penales`, `POST` y `GET /api/tournaments/{id}/bracket`, `POST` y `GET /api/tournaments/{id}/groups`, los campos de penales de `PartidoDTO` y los esquemas nuevos; y `docs/data-model.md` con las tablas `tournament_group` y `bracket_tie`, las columnas nuevas y las reglas de avance y de finalización; verificar que el YAML sigue siendo válido
- [x] 16.2 Actualizar el `README.md` de la carpeta padre (eliminatoria directa, repechaje, penales, grupos, clasificación, tercer puesto y la regla de finalización), sin valores secretos
- [x] 16.3 Completar la Parte 3 de `docs/frontend-prompts/tournaments-module.md` con endpoints, formas de respuesta, reglas, pantallas de llaves y grupos, cambios sobre las Partes 1 y 2 y criterios de aceptación; **mantener** el aviso de "no entregar todavía" hasta que el usuario confirme que se entrega, actualizar la tabla de estado (change 3 implementado en el backend) y recordar que con esto los tres changes del backend quedan listos; en español y con "no hagas commit"
