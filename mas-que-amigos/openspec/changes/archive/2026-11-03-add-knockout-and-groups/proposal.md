# Proposal

## Why

Con los changes `add-tournaments` y `add-league-standings`, solo `LEAGUE` tiene comportamiento propio. Los formatos `KNOCKOUT` (eliminatoria directa) y `GROUPS_KNOCKOUT` (fase de grupos y eliminatoria) se guardan y se bloquean al iniciar, pero el administrador debe armar y avanzar a mano cada cruce. Este es el tercer y último change del módulo de torneos: da vida a esos dos formatos con llaves que avanzan solas, penales para desempatar, partidos previos cuando los equipos no son potencia de 2, y grupos con su tabla y su clasificación.

## What Changes

- **Penales en el partido**: un partido guarda los penales de cada equipo (`penalesLocal`, `penalesVisitante`). Se registran con `PUT /api/partidos/{id}/penales` y solo sirven para desempatar partidos que exigen ganador.
- **Partidos que exigen ganador**: los partidos de las llaves y los partidos de fase `REPECHAJE` de un torneo `KNOCKOUT` no pueden finalizarse empatados; si el marcador es igual, hay que registrar penales distintos.
- **Repechaje previo a las llaves** (torneo `KNOCKOUT`): si los equipos no son potencia de 2, el administrador arma partidos de `REPECHAJE` entre dos equipos; el perdedor queda eliminado y los demás forman las llaves.
- **Eliminatoria directa**: `POST /api/tournaments/{id}/bracket` genera las llaves de un torneo `KNOCKOUT` en curso con equipos en potencia de 2, con **sorteo aleatorio** de los cruces o **cruces manuales** indicados por el administrador. Las llaves avanzan solas: al finalizar un partido, el ganador pasa a la ronda siguiente y el partido siguiente se crea cuando sus dos equipos ya se conocen. Incluye **partido por el tercer puesto** (activo por defecto). `GET /api/tournaments/{id}/bracket` devuelve las llaves y el campeón.
- **Fase de grupos** (torneo `GROUPS_KNOCKOUT`): `POST /api/tournaments/{id}/groups` forma los grupos (sorteo aleatorio o grupos manuales), define cuántos equipos clasifican por grupo (1 o 2) y genera el calendario de cada grupo a una vuelta. `GET /api/tournaments/{id}/groups` devuelve cada grupo con su tabla, calculada con las mismas reglas de la liga.
- **Eliminatoria tras los grupos**: con los partidos de grupos finalizados, `POST /api/tournaments/{id}/bracket` arma las llaves con los clasificados, cruzando primero contra segundo de otro grupo.
- **Finalizar un torneo de eliminatoria o de grupos** exige además que la final esté finalizada (hay campeón).
- Los partidos de las llaves quedan protegidos: no se eliminan ni cambian de equipos.
- Documentación y Parte 3 del prompt unificado del front (`docs/frontend-prompts/tournaments-module.md`), que se entrega solo cuando los tres changes estén listos.

Fuera de alcance:

- Ida y vuelta, gol de visitante y mejores terceros de grupo.
- Cruces manuales en la eliminatoria posterior a los grupos (siguen la regla de cruce fija).
- Más de 2 clasificados por grupo.
- Reprogramación automática de partidos aplazados y filtros de estadísticas por torneo.

## Capabilities

### New Capabilities
- `tournament-knockout`: penales, partidos que exigen ganador, repechaje previo, generación de las llaves (sorteo o manual), avance de ronda, tercer puesto, consulta de las llaves y protección de sus partidos.
- `tournament-groups`: formación de grupos, calendario por grupo, tabla de cada grupo, clasificación y armado de la eliminatoria posterior a los grupos.

### Modified Capabilities
- `tournaments`: el requisito "Finalización de torneo" agrega que un torneo `KNOCKOUT` o `GROUPS_KNOCKOUT` solo se finaliza con la final finalizada.

## Impact

- **Base de datos**: migración `V10__knockout_and_groups.sql`: `partido.penales_local` y `partido.penales_visitante`; `tournament.qualifiers_per_group`; tablas `tournament_group` y `bracket_tie`; y `tournament_team.group_id`.
- **Código nuevo (inglés)**: `BracketService`, `GroupStageService`, `BracketTie`, `TournamentGroup`, clases puras `BracketBuilder` y `GroupDraw`, DTOs de llaves, grupos y penales, y repositorios.
- **Código existente modificado**: `Partido`, `PartidoDTO`, `PartidoMapper`, `PartidoService` (reglas de finalización, avance, edición y eliminación), `PartidoController` (penales), `TournamentService` (finalizar), `TournamentTeam` y `TournamentController`.
- **API**: `PUT /api/partidos/{id}/penales`, `POST` y `GET /api/tournaments/{id}/bracket`, `POST` y `GET /api/tournaments/{id}/groups`. Sin cambios incompatibles: lo que hoy se hace a mano sigue funcionando; solo se endurece la finalización de torneos de eliminatoria y de grupos.
- **Seguridad**: sin cambios en `SecurityFilterChain` (consulta pública, escritura `ADMIN`); se agregan casos a `SecurityRulesTest`.
- **Documentación**: `docs/api-spec.yml`, `docs/data-model.md`, README y la Parte 3 del prompt del front.
