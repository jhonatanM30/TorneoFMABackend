# Proposal

## Why

El change `add-tournaments` dejó los torneos con su formato definido y bloqueado, pero un torneo de liga todavía no se comporta como liga: no hay tabla de posiciones y el administrador debe crear a mano cada partido. Ahora que todo partido pertenece a un torneo, se puede calcular la tabla sobre los partidos finalizados (incluidos los armados a mano para reemplazar uno aplazado) y generar el calendario todos contra todos, que es lo que hace que `LEAGUE` signifique algo. Este es el segundo de tres changes; el tercero (grupos y eliminatoria) reutilizará esta lógica de tabla.

## What Changes

- **Tabla de posiciones** de un torneo `LEAGUE`: `GET /api/tournaments/{id}/standings` (pública). Incluye todos los equipos inscritos, incluso los que aún no jugaron, y calcula solo con partidos `FINALIZADO` del torneo, creados como se hayan creado (los manuales cuentan igual).
- **Puntuación** victoria 3, empate 1, derrota 0.
- **Desempate**, en este orden: puntos, enfrentamiento directo, diferencia de goles, goles a favor, juego limpio (menos puntos de tarjeta; amarilla 1, roja 2) y, como último recurso, nombre del equipo. Las posiciones son consecutivas, sin empates compartidos.
- **Calendario automático** de una vuelta: `POST /api/tournaments/{id}/schedule` (solo `ADMIN`) crea los partidos todos contra todos de un torneo `LEAGUE` en curso y sin partidos, una jornada por fecha, a partir de una fecha de inicio, una hora y un intervalo en días. Con número impar de equipos, uno descansa en cada jornada.
- Los partidos generados son partidos normales: se pueden editar, eliminar y combinar con partidos manuales.
- Documentación y la Parte 2 del prompt unificado del front (`docs/frontend-prompts/tournaments-module.md`).

Fuera de alcance:

- **Change 3**: grupos, clasificación y llaves de eliminatoria; la tabla para `GROUPS_KNOCKOUT` y `KNOCKOUT`.
- Ida y vuelta, y reprogramación automática de partidos aplazados.
- Filtrar el goleador y otras estadísticas por torneo.

## Capabilities

### New Capabilities
- `league-standings`: tabla de posiciones de un torneo de liga con puntuación 3-1-0, criterios de desempate (incluido el enfrentamiento directo y el juego limpio) y posiciones consecutivas.
- `league-schedule`: generación del calendario de una vuelta para un torneo de liga en curso, con sus precondiciones, atomicidad y convivencia con partidos manuales.

### Modified Capabilities
<!-- Ninguna: tournament-matches ya establece que un partido manual es un partido normal; este change solo agrega consultas y la generación sobre esa base. -->

## Impact

- **Base de datos**: sin migración. La tabla se calcula en cada consulta desde `partido` y `estadistica`; no se guarda.
- **Código nuevo (inglés)**: `LeagueStandingsCalculator`, `StandingDTO`, `ScheduleRequestDTO`, métodos `standings` y `generateSchedule` en `TournamentService`, y consultas nuevas en `PartidoRepository` y `EstadisticaRepository`.
- **API**: `GET /api/tournaments/{id}/standings` y `POST /api/tournaments/{id}/schedule`. Sin cambios incompatibles.
- **Seguridad**: sin cambios en `SecurityFilterChain` (consulta pública, escritura `ADMIN`); se agregan casos a `SecurityRulesTest`.
- **Documentación**: `docs/api-spec.yml`, `docs/data-model.md`, README y Parte 2 de `docs/frontend-prompts/tournaments-module.md`.
