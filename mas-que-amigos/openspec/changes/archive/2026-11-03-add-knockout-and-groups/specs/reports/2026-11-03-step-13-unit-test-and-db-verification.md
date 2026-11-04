# Reporte del Paso 13 - Tests unitarios y verificación de la base de datos

- Fecha: 2026-11-03
- Change: add-knockout-and-groups
- Agente: Claude (Sonnet 5.5)

## Comandos ejecutados
- `./mvnw -o clean test -Dtest='BracketBuilderTest,GroupDrawTest,MatchWinnerTest,BracketServiceTest,GroupStageServiceTest,TournamentServiceTest,PartidoServiceTest,MatchDateConflictsTest,SecurityRulesTest'`
- `./mvnw -o clean test`
- Conteos de la base de datos sobre la **copia** `masqueamigos_t10` del MySQL de `TorneoContainer` (consulta de filas por tabla con `docker exec ... mysql`).

## Resultados de los tests unitarios
- Tests focalizados: 227 pasaron, 0 fallaron, 0 omitidos (99 s, casi todo en `SecurityRulesTest`, que levanta el contexto con H2).
- Suite completa: **388 pasaron, 0 fallaron, 0 omitidos** (BUILD SUCCESS). Se corrió dos veces: 387 (104 s) y, tras corregir la fecha que muestra un cruce con el partido editado (hallazgo de los curl del paso 14, con un test nuevo), 388 (143 s). Antes del change eran 246; los 142 nuevos: `BracketServiceTest` 57, `GroupStageServiceTest` 23, `BracketBuilderTest` 16, `GroupDrawTest` 11, `MatchWinnerTest` 8, `BracketRequestDTOValidationTest` 5, `PenaltiesRequestDTOValidationTest` 4, `MatchDateConflictsTest` 2, `TournamentServiceTest` +3, `PartidoServiceTest` +11 y `PartidoMapperTest` +2. `SecurityRulesTest` suma casos dentro de tests ya existentes (rutas nuevas en la matriz de roles).
- Notas: sin tests inestables. El contexto de Spring con H2 (`MasQueAmigosApplicationTests` y `SecurityRulesTest`) valida el mapeo de las entidades nuevas (`BracketTie`, `TournamentGroup`, columnas nuevas) y las consultas JPQL. `java.util.Random` se inyecta con semilla para repetir sorteos; `var` de Java 10 se reemplazó porque el proyecto es Java 8.
- Tests existentes modificados (no debilitados): `PartidoServiceTest` y `TournamentServiceTest` (constructores con `BracketService` y `MatchDateConflicts`; los 14 tests del calendario de liga pasan sin cambiar sus expectativas), y `SecurityRulesTest`.
- Refactor cubierto por los tests existentes: el chequeo de choques de fecha pasó a `MatchDateConflicts` y los pesos de juego limpio a `LeagueStandingsCalculator`.

## Verificación del estado de la base de datos
- Nota: los tests usan H2 en memoria y no tocan MySQL. La migración `V10` se probó contra una copia de la base real (`masqueamigos_t10`, volcado de `masqueamigos`, que ya estaba en `V9`), con un respaldo previo de la base real. La base real no se modificó.
- Validación de `V10` al arrancar el backend contra la copia: `flyway_schema_history` en v10 con `success = 1`; existen `tournament_group` y `bracket_tie`; `partido.penales_local`, `partido.penales_visitante`, `tournament.qualifiers_per_group` y `tournament_team.group_id` existen; `GET /api/partidos` devuelve los 2 partidos con `idTorneo` 1 y sin penales; `GET /api/tournaments/1/bracket` devuelve rondas vacías.
- Baseline previo: partido 2, equipos 5, jugador 29, alineacion 21, estadistica 2, player_transfer 7, app_user 3, tournament 1, tournament_team 0, tournament_group 0, bracket_tie 0, Flyway 10.
- Validación posterior a los tests: idéntica al baseline.
- Estado restaurado: Sí (sin mutaciones).
- Acciones de restauración: ninguna necesaria.

## Resultado
- Estado del Paso 13: PASS
- Problemas bloqueantes: ninguno
