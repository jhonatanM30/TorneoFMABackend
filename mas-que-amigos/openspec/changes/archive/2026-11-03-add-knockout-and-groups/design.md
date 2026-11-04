# Design

## Context

Estado actual verificado en el código (ver `proposal.md` para la motivación):

- Los changes 1 y 2 ya están en `master`: `Tournament` (`format`, `status`), `TournamentTeam`, `Partido.torneo`, `TournamentService` con bloqueo pesimista (`findByIdForUpdate`), `LeagueStandingsCalculator` y `LeagueScheduleGenerator` (clases puras), y las consultas `existePartidoEnFechaParaEquipo`, `findByTorneoIdAndEstado` y `sumarTarjetasPorEquipoEnTorneo`.
- `Partido.Fase` ya tiene `FASE_DE_GRUPOS`, `REPECHAJE`, `ELIMINACION_DIRECTA` y `FINAL`; hoy ninguna regla las usa. El marcador (`golesLocal`, `golesVisitante`) se calcula desde estadísticas; `Partido` no tiene noción de ganador ni de penales.
- `PartidoService#finalizarPartido` cambia el estado `EN_CURSO` → `FINALIZADO` sin más reglas; `#eliminar` y `#actualizarPartido` ya existen y aplican "un equipo no juega dos veces el mismo día" (`guardar` y edición, global entre torneos).
- Errores: `NotFoundException` → `404 {indicadorRespuesta:"Sin Registros", mensaje}`; `BusinessException` → `400 {indicadorRespuesta:"Error", mensaje}`; validación → `400` mapa campo → mensaje. La regla de seguridad "GET público / escritura `ADMIN`" ya cubre `/api/tournaments/**` y `/api/partidos/**`.
- Idioma: clases, tablas, columnas y rutas nuevas en inglés; los archivos existentes de `Partido` conservan el español (`partido.penales_local`, `PartidoDTO.penalesLocal`).

## Goals / Non-Goals

**Goals:**
- Llaves que avanzan solas al finalizar partidos, sin que el administrador cree cada cruce a mano.
- Lógica de cuadros y de grupos aislada en clases puras fáciles de probar (como la liga).
- Reutilizar la tabla de la liga para los grupos y los partidos manuales como cualquier otro.

**Non-Goals:**
- Ida y vuelta, gol de visitante, mejores terceros, más de 2 clasificados por grupo y cruces manuales tras los grupos.
- Reubicar automáticamente a un equipo ni reprogramar partidos aplazados.

## Decisions

**1. Una tabla `bracket_tie` modela el cuadro; el `Partido` se crea cuando se conocen los dos equipos.**
Cada cruce guarda su torneo, tipo (`MAIN` o `THIRD_PLACE`), ronda, posición, equipos local y visitante (vacíos hasta que se conocen), el partido (vacío hasta crearse), el ganador, la fecha y la hora que le tocan por su ronda, y los enlaces al cruce siguiente del ganador (y, en semifinales, del perdedor, hacia el tercer puesto) con el lado que ocupa. Al generar las llaves se crean todos los cruces y solo los partidos de la primera ronda; el resto de partidos nace cuando el avance completa al cruce. Así `Partido` sigue exigiendo equipos, fecha y hora, sin cambiar su esquema para admitir "partidos pendientes de rival".
*Alternativa descartada*: crear todos los partidos desde el inicio con equipos nulos: obliga a volver nulos `equipo_local`/`equipo_visitante` y a revisar cada consulta existente.

**2. El avance vive en un enganche de `PartidoService#finalizarPartido`, con `BracketService` sin depender de `PartidoService`.**
Antes de finalizar, `BracketService#requireDecided` rechaza el empate de un partido que exige ganador sin penales distintos; después, `BracketService#onMatchFinished` calcula ganador y perdedor, los coloca en sus cruces siguientes y crea los partidos que ya tengan ambos equipos. Un partido exige ganador si está enlazado a un cruce o si su fase es `REPECHAJE` en un torneo `KNOCKOUT`. `BracketService` usa solo repositorios (y `TournamentRepository` para el bloqueo), así que no hay dependencia circular. El ganador es el de más goles y, con el marcador igual, el de más penales; esa regla vive en un solo método.

**3. Penales como dos columnas de `partido`, con su propio endpoint.**
`penales_local` y `penales_visitante` (`INT NULL`) más `PUT /api/partidos/{id}/penales`, solo para partidos en curso que exigen ganador y con valores ≥ 0 y distintos. No se mezclan con el marcador (que sigue calculándose desde estadísticas), de modo que `golesLocal`/`golesVisitante` no cambian de significado.
*Alternativa descartada*: que el administrador elija al ganador sin guardar la tanda: pierde el dato y obliga a un campo de ganador sin respaldo en el marcador.

**4. Repechaje con la fase `REPECHAJE` que ya existe.**
No hay entidad nueva: el administrador arma partidos `REPECHAJE` en un torneo `KNOCKOUT` con el `POST /api/partidos` existente, y el perdedor de uno finalizado queda eliminado. Al generar las llaves, participan los inscritos menos los eliminados; deben ser una potencia de 2 y no puede haber repechajes sin finalizar. Los eliminados se calculan al generar (no se guardan).

**5. Sorteo y cruces manuales en el mismo endpoint.**
`POST .../bracket` sortea la primera ronda al azar cuando no recibe `pairings`, y usa los indicados cuando sí (deben cubrir a todos los participantes una vez). El azar sale de un `java.util.Random` inyectable para poder probarlo con semilla. Los cruces manuales solo existen en `KNOCKOUT`; en `GROUPS_KNOCKOUT` la regla de cruce es fija.

**6. Grupos: `tournament_group`, `tournament_team.group_id` y `tournament.qualifiers_per_group`.**
La composición se reparte con una clase pura `GroupDraw`: mezcla (o toma la composición manual) y reparte en turnos, de modo que los tamaños difieran como máximo en 1. La cantidad de grupos es potencia de 2 (con 1 o 2 clasificados por grupo, el total de clasificados también lo es y las llaves cuadran sin pases) y los clasificados por grupo deben ser menos que el tamaño del grupo más pequeño. El calendario de cada grupo reutiliza `LeagueScheduleGenerator`: la jornada `k` de todos los grupos cae en la misma fecha, y es atómico con el mismo chequeo de choques de fecha de la liga.

**7. La tabla de un grupo reutiliza `LeagueStandingsCalculator`.**
Con los equipos del grupo, los partidos `FINALIZADO` de fase `FASE_DE_GRUPOS` entre ellos (incluidos los manuales) y los puntos de juego limpio de una consulta nueva filtrada por fase (para que las tarjetas de la eliminatoria no cuenten). La liga mantiene su consulta actual.

**8. Cruces de los clasificados con una función pura (`BracketBuilder`).**
Con 2 clasificados por grupo, los grupos se emparejan de dos en dos y la primera ronda lista primero los `1.º X vs 2.º Y` de cada pareja de grupos y luego los `1.º Y vs 2.º X`, de modo que dos equipos del mismo grupo quedan en mitades opuestas del cuadro. Con 1 clasificado, se enfrentan los primeros de los grupos emparejados. En la eliminatoria directa los ganadores de dos cruces consecutivos se enfrentan en la ronda siguiente. `BracketBuilder` recibe los cruces de la primera ronda y devuelve el plan completo (rondas, posiciones y enlaces, más el tercer puesto), sin tocar la base de datos.

**9. Fechas de las rondas y choques.**
La ronda `r` cae en `inicio + (r − 1) × intervalo`; la final y el tercer puesto comparten fecha. La fecha de inicio debe ser posterior al último partido del torneo (para que no se mezclen con la fase previa). Los choques de fecha se verifican para la primera ronda (atómico); los partidos de rondas posteriores se crean sin ese chequeo, porque nacen durante una finalización que no debe fallar por un partido de otro torneo. Si un equipo choca, el administrador edita la fecha del partido (que sí se valida en la edición).

**10. Protección de los partidos de las llaves y finalización del torneo.**
Un partido enlazado a un cruce no se elimina ni cambia de equipos (los cruces dependen de él), pero su fecha y su hora se pueden editar. Finalizar un torneo `KNOCKOUT` o `GROUPS_KNOCKOUT` exige además que las llaves existan y la final esté finalizada: sin esto, la regla de "sin partidos pendientes" cerraría el torneo entre rondas, cuando los cruces siguientes aún no tienen partido.

**11. Un helper compartido para los choques de fecha.**
La comprobación "este equipo ya juega esa fecha" ya existe en `TournamentService#generateSchedule` y se necesita en el calendario de grupos y en la primera ronda de las llaves. Se extrae a un componente pequeño (`MatchDateConflicts`) usado por los tres, en lugar de repetir el mismo bucle.

**12. Servicios separados.**
`BracketService` (llaves, avance, repechaje, penales, protección) y `GroupStageService` (grupos y su tabla) nacen aparte de `TournamentService`, que ya concentra torneos, inscripción, liga y partidos. `BracketService` usa a `GroupStageService` para obtener los clasificados; `TournamentService` usa a `BracketService` solo para validar el campeón al finalizar.

**13. API.**
`PUT /api/partidos/{id}/penales`; `POST` y `GET /api/tournaments/{id}/bracket`; `POST` y `GET /api/tournaments/{id}/groups`. El `GET` de llaves devuelve las rondas con su nombre en español (octavos de final, cuartos de final, semifinales, final), cada cruce con equipos, partido, marcador, penales y ganador, el tercer puesto y el campeón. Sin cambios en `SecurityFilterChain`.

## Risks / Trade-offs

- [Un partido de ronda posterior puede nacer en una fecha en la que un equipo ya juega en otro torneo] → Se documenta; el administrador cambia la fecha del partido (la edición sí valida el choque). Es el costo de no hacer fallar una finalización por un partido ajeno.
- [El avance corre dentro de la finalización de un partido: un error dejaría el partido sin finalizar] → `finalizarPartido` se vuelve transaccional junto con el avance; si algo falla, no cambia nada.
- [Eliminar un partido finalizado ya está prohibido, pero un partido de llaves programado tampoco se puede eliminar] → Es intencional; si hay que reemplazar un equipo, se corrige antes de generar las llaves o se edita la fecha. Se documenta en la API.
- [Los criterios de grupos usan `jugador.equipo` para las tarjetas, como la liga] → Mismo riesgo ya aceptado por traspasos posteriores al partido.
- [Mucha lógica nueva en un solo change] → Mitigado con clases puras probadas sin base de datos (`BracketBuilder`, `GroupDraw`), y los tests de servicio verifican cada regla por separado.
- [Con 1 o 2 clasificados por grupo no hay "mejores terceros"] → Está fuera de alcance; se puede añadir como parámetro sin romper el contrato.

## Migration Plan

1. Aplicar `V10__knockout_and_groups.sql` al arrancar: añade `partido.penales_local`/`penales_visitante` (nulos), `tournament.qualifiers_per_group` (nulo), `tournament_team.group_id` (nulo) y crea `tournament_group` y `bracket_tie`. Ningún dato existente cambia.
2. Respaldar la base real antes de reconstruir Docker (la migración no tiene reversa automática) y probar primero contra una copia.
3. Reconstruir Docker solo cuando el usuario lo confirme (skill `docker-rebuild`).
4. Completar la Parte 3 de `docs/frontend-prompts/tournaments-module.md`; el front se entrega solo cuando los tres changes estén listos.
5. Rollback en desarrollo: restaurar el respaldo o eliminar lo creado (tablas nuevas y columnas) y limpiar `flyway_schema_history`.

## Open Questions

- Ninguna que cambie las specs.
