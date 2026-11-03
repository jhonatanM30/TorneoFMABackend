# Design

## Context

Estado actual verificado en el código (ver `proposal.md` para la motivación):

- No existe la entidad Torneo. `Partido` (tabla `partido`, columnas en español) tiene `fase` como enum (`FASE_DE_GRUPOS`, `REPECHAJE`, `ELIMINACION_DIRECTA`, `FINAL`) y `estado` (`PROGRAMADO`, `EN_CURSO`, `FINALIZADO`). `PartidoMapper` asume `FASE_DE_GRUPOS` cuando el cliente no envía fase.
- `PartidoService#guardar` ya valida: equipos existentes, equipos distintos y que ningún equipo tenga otro partido ese día (`existePartidoEnFechaParaEquipos`, global, sin distinguir torneo). Los goles se calculan desde `Estadistica` (`recalcularGoles`), nunca se escriben a mano. Un partido `FINALIZADO` no se edita ni se elimina.
- Spring Security ya está activo (change `add-authentication-roles`): consulta `GET /api/**` pública y "cualquier otra escritura bajo `/api/**`" exige `ADMIN`, así que `/api/tournaments/**` queda protegido sin tocar la cadena de filtros.
- Convenciones de error vigentes: `404` de `NotFoundException` (la que usan partidos y equipos) → `{indicadorRespuesta:"Sin Registros", mensaje}`; otras excepciones de no encontrado (jugador, traspaso, usuario) responden `{"error": ...}` y no se usan aquí; regla de negocio `400` → `{indicadorRespuesta:"Error", mensaje}` (`BusinessException`), validación `400` → mapa campo → mensaje.
- Idioma (`docs/base-standards.md` §2): clases, tablas, columnas y rutas nuevas en inglés; en archivos existentes se respeta su idioma. `partido` usa español, así que su columna nueva es `id_torneo`.
- Flyway versiona el esquema; la última migración es `V8`. Los tests actuales son Mockito sobre services, más tests de seguridad y de DTO.

## Goals / Non-Goals

**Goals:**
- Que "torneo" y "formato" existan como datos de primera clase y que el formato quede definido y bloqueado antes de jugar.
- Que todo partido, creado de cualquier manera, tenga torneo y sea tratado igual (base para que la tabla de posiciones de los changes 2 y 3 sea una consulta sobre los partidos del torneo, sin casos especiales para los manuales).
- Compatibilidad hacia atrás: el front y las pruebas actuales siguen creando partidos sin torneo.

**Non-Goals:**
- Tabla de posiciones, puntos, desempate y juego limpio (change 2).
- Generación automática de calendario, grupos y llaves (changes 2 y 3).
- Mínimos de equipos propios de cada formato (p. ej. cuántos hacen falta para grupos); aquí solo se exige 2 para cualquier formato.
- Filtrar goleador/estadísticas por torneo; migrar `fase` a otro modelo.

## Decisions

**1. Un solo "origen" de partidos: no existe bandera de partido manual.**
El requisito "los manuales cuentan para la puntuación" se cumple por construcción: un partido manual es una fila de `partido` más, con `id_torneo`. La puntuación de los changes siguientes será una consulta sobre los partidos `FINALIZADO` del torneo, sin importar cómo se crearon. En este change, además, todos los partidos son "manuales" (la generación automática llega después), así que el camino es el mismo `POST /api/partidos` con `idTorneo`.
*Alternativa descartada*: columna `manual BOOLEAN` o un endpoint aparte para manuales: crearía dos clases de partido y obligaría a decidir en cada cálculo si cuentan.

**2. Modelo de datos (migración `V9__tournaments.sql`).**
- `tournament`: `id`, `name` (único), `format` (`VARCHAR(20)`), `status` (`VARCHAR(20)`), `created_at`, `started_at` NULL.
- `tournament_team`: `id`, `tournament_id`, `team_id`, `enrolled_at`, `UNIQUE (tournament_id, team_id)`, FKs a `tournament` y `equipos`. Un equipo puede aparecer en varios torneos.
- `partido.id_torneo` `BIGINT`, FK a `tournament`, índice. La migración, en este orden: crea las tablas, inserta el torneo "Histórico" (`LEGACY`, `IN_PROGRESS`), agrega la columna como NULL, hace `UPDATE partido SET id_torneo = <histórico>`, y recién entonces la pasa a `NOT NULL` con su FK.
- Formato y estado se guardan como texto (patrón de `player_transfer`), mapeados con `@Enumerated(EnumType.STRING)`.

**3. `LEGACY` como valor interno del formato, no seleccionable por la API.**
El torneo Histórico necesita un formato válido en la tabla; usar `LEAGUE` mentiría (no tiene calendario ni tabla) y dejar `format` nulo complica todas las lecturas. `TournamentFormat` tiene cuatro valores, pero la validación de crear/editar acepta solo los tres reales. El Histórico se identifica por `format = LEGACY` (no por nombre, que es editable en teoría y es solo una etiqueta).
*Alternativa descartada*: sin torneo Histórico, `id_torneo` nullable: obliga a que cada consulta y cada regla futura trate el "sin torneo" como caso especial; el usuario eligió el torneo por defecto.

**4. Reglas de partido de torneo dentro de `PartidoService`, delegando la parte del torneo a `TournamentService`.**
`PartidoService#guardar` llama a `TournamentService#resolveForNewMatch(idTorneo, idLocal, idVisitante)`, que: si `idTorneo` es nulo devuelve el Histórico; si no, exige que exista, que esté `IN_PROGRESS` y, salvo `LEGACY`, que ambos equipos estén inscritos. `actualizarPartido` ignora `idTorneo` del request (el torneo de un partido no cambia) y valida inscripción si cambian los equipos. `TournamentService` usa `PartidoRepository` (consulta de partidos pendientes) pero `PartidoService` no es usado por él, así que no hay dependencia circular. Las demás reglas (mismo equipo, un partido por día, alineaciones) quedan como están.

**5. Ciclo de vida lineal con bloqueo de fila.**
`DRAFT → IN_PROGRESS → FINISHED`, sin retrocesos. Las operaciones que cambian estado o dependen de él (inscribir, retirar, iniciar, finalizar, crear partido) corren en `@Transactional` y leen el torneo con bloqueo pesimista (`findByIdForUpdate`, mismo patrón que en traspasos), para que, por ejemplo, un partido no se cree en el instante en que el torneo se finaliza. El costo es mínimo: hay un solo administrador a la vez.

**6. Finalizar exige que no queden partidos pendientes.**
`FINISHED` significa "no hay nada más por jugar". Si un partido aplazado (A vs B) sigue programado, hay que jugarlo o eliminarlo (la eliminación de partidos no finalizados ya existe) antes de cerrar. Evita torneos "terminados" con partidos colgando y resuelve el caso de uso del aplazamiento sin una pantalla extra.

**7. Un equipo en varios torneos; la regla de "un partido por día" sigue siendo global.**
La inscripción permite que un equipo participe en una liga y una copa a la vez. La regla ya existente impide que un equipo tenga dos partidos el mismo día aunque sean de torneos distintos; es lo realista (no puede jugar dos partidos a la vez) y no se toca.

**8. API en `/api/tournaments`, request con `String` para el formato.**
`POST/GET /api/tournaments`, `GET/PUT/DELETE /api/tournaments/{id}`, `POST /{id}/start`, `POST /{id}/finish`, `GET/POST /{id}/teams`, `DELETE /{id}/teams/{teamId}`, `GET /{id}/matches`. El formato llega como `String` y un conversor lanza `BusinessException` en español si no es válido (mismo patrón que la `fase` en `PartidoMapper`), en vez de dejar que Jackson devuelva un error genérico. Equipos y partidos se responden con los DTO existentes (`EquipoDTO`, `PartidoDTO`) para no duplicar estructuras.

**9. Sin cambios en seguridad.**
La regla `GET` pública / escritura `ADMIN` ya cubre las rutas nuevas. Se agregan casos a `SecurityRulesTest` para fijarlo (visitante 401, `DT` 403, `ADMIN` pasa; `GET` público).

## Risks / Trade-offs

- [La migración toca todas las filas de `partido` y cambia una columna a `NOT NULL`] → Es un `UPDATE` único sobre pocas filas (decenas), ejecutado antes del `NOT NULL`; se prueba contra una copia de la base antes de aplicar, y se anota el conteo previo y posterior de `partido`.
- [DDL de MySQL no es transaccional: si `V9` falla a medias queda un esquema parcial] → La migración es corta y ordenada para que cada paso sea reintentable; en desarrollo se revierte con `DROP` manual de las tablas nuevas y de la columna, y se limpia la fila de `flyway_schema_history`.
- [Eliminar un equipo inscrito o con partidos falla hoy por llaves foráneas (ya pasaba con `partido`)] → No se cambia en este change; queda documentado y las inscripciones se suman a esa restricción.
- [`Partido.fase` y el formato del torneo pueden contradecirse (p. ej. `FINAL` en una liga)] → Aquí `fase` sigue libre; los changes 2 y 3 definen qué fases admite cada formato.
- [Un torneo `IN_PROGRESS` sin calendario generado depende de que el administrador cree cada partido a mano] → Es la expectativa de este primer change; la generación automática es el change 2.
- [Mínimo de 2 equipos es laxo para `GROUPS_KNOCKOUT`] → Se endurece por formato en el change 3 sin cambiar este contrato (solo agrega validación).

## Migration Plan

1. Respaldar (o anotar) el conteo de `partido` y confirmar que Flyway está en `V8`.
2. Aplicar `V9__tournaments.sql` al arrancar el backend. Verificar: tablas creadas, torneo "Histórico" presente, `SELECT COUNT(*) FROM partido WHERE id_torneo IS NULL` = 0 y mismo total de partidos.
3. Reconstruir la imagen Docker solo cuando el usuario lo confirme (skill `docker-rebuild`).
4. Entregar `docs/frontend-prompts/add-tournaments.md` a la IA del front. Mientras el front no cambie, todo sigue funcionando: los partidos nuevos caen en el Histórico.
5. Rollback: no hay migración inversa automática; en desarrollo, restaurar el respaldo o `DROP` de lo creado como se indica en Riesgos.
