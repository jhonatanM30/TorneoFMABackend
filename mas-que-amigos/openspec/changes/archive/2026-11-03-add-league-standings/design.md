# Design

## Context

Estado actual verificado en el código (ver `proposal.md` para la motivación):

- `add-tournaments` ya está en `master`: `Partido.torneo` (`id_torneo`, obligatorio), `Tournament` con `format` (`LEAGUE`, `GROUPS_KNOCKOUT`, `KNOCKOUT`, `LEGACY`) y `status`, `TournamentTeam` (inscripciones), y `TournamentService` con bloqueo pesimista de fila (`findByIdForUpdate`) y `resolveForNewMatch`.
- El marcador de un partido (`golesLocal`, `golesVisitante`) está guardado en `Partido` y se recalcula desde `Estadistica` cada vez que se crea, edita o elimina una estadística (`PartidoService#recalcularGoles`). Las tarjetas viven en `Estadistica` (`tarjetasAmarillas`, `tarjetasRojas`, por jugador y partido) y la pertenencia a un equipo se resuelve por `Estadistica.jugador.equipo`, igual que ya hace el cálculo de goles por equipo.
- `PartidoService#guardar` impone que un equipo no tenga dos partidos el mismo día (`existePartidoEnFechaParaEquipos`, global entre torneos). `Partido.fase` es un enum sin valor propio de liga; `FASE_DE_GRUPOS` es el valor por defecto del mapper.
- Errores: `NotFoundException` → `404 {indicadorRespuesta:"Sin Registros", mensaje}`; `BusinessException` → `400 {indicadorRespuesta:"Error", mensaje}`; validación → `400` mapa campo → mensaje. La regla de seguridad "GET público / escritura `ADMIN`" ya cubre `/api/tournaments/**`.
- Idioma: clases, métodos y DTOs nuevos en inglés; mensajes al usuario en español.

## Goals / Non-Goals

**Goals:**
- Una tabla de posiciones que sea una consulta pura sobre los partidos del torneo, sin casos especiales para los partidos manuales.
- Una lógica de clasificación aislada y reutilizable por el change 3 (grupos), fácil de probar sin base de datos.
- Un calendario generado atómicamente que no deje el torneo a medias.

**Non-Goals:**
- Guardar la tabla, la jornada o los puntos en la base de datos (sin migración).
- Tabla para `GROUPS_KNOCKOUT`/`KNOCKOUT`, ida y vuelta, reprogramación automática de aplazados y filtros de goleador por torneo.
- Reaplicar el enfrentamiento directo de forma recursiva (ver Decisión 2).

## Decisions

**1. La tabla se calcula en cada consulta, con una clase pura `LeagueStandingsCalculator`.**
Recibe los equipos inscritos, los partidos finalizados (equipos y goles) y los puntos de juego limpio por equipo, y devuelve las filas ordenadas con su posición. No depende de Spring ni de repositorios; el servicio solo reúne los datos. Razones: el cálculo es barato (decenas de partidos), siempre refleja correcciones de estadísticas sin sincronizar nada, y el change 3 podrá llamarla por grupo.
*Alternativa descartada*: guardar la tabla y actualizarla al finalizar cada partido: exige recalcular cuando se edita una estadística de un partido ya finalizado y abre la puerta a tablas desincronizadas.

**2. Desempate en una sola pasada, con mini tabla de los empatados.**
Orden por puntos descendente; luego, por cada grupo de equipos con los mismos puntos, se construye una mini tabla solo con los partidos jugados entre ellos (puntos, diferencia de goles y goles a favor dentro de ese subconjunto) y se ordena el grupo por esos tres valores; los que siguen empatados se ordenan por diferencia de goles total, goles a favor total, puntos de juego limpio ascendentes y, al final, nombre. Es una sola pasada: no se vuelve a aplicar el enfrentamiento directo a un subgrupo que sigue empatado (algunas ligas sí lo hacen). Es un criterio determinista, documentado y suficiente para una liga amateur; ampliarlo después no cambia el contrato.
*Alternativa descartada*: recursión completa del enfrentamiento directo: más fiel a ciertos reglamentos, pero con 3 o más equipos añade complejidad que hoy no se necesita.

**3. Puntos de juego limpio con una consulta agregada por torneo.**
Una sola consulta suma amarillas y rojas por equipo para los partidos `FINALIZADO` del torneo (`group by e.jugador.equipo.id`), y el servicio calcula `amarillas × 1 + rojas × 2`. Los pesos son constantes nombradas. Como el equipo se toma de `jugador.equipo`, las tarjetas se atribuyen al equipo **actual** del jugador, igual que ya ocurre con los goles por equipo.

**4. Calendario con el método del círculo (Berger), sin guardar la jornada.**
Con `n` equipos (se agrega un descanso ficticio si `n` es impar) se generan `n−1` jornadas (o `n` si es impar) en las que cada equipo juega una vez. La localía se reparte con la regla de Berger (el equipo fijo alterna y los demás alternan por posición y jornada); un test recorre `n = 2..12` y comprueba que cada equipo tiene diferencia local−visitante ≤ 1. Cada jornada tiene su propia fecha (`inicio + k × intervalo`), así que "jornada" equivale a "fecha" y no hace falta una columna: el front agrupa por fecha. Los partidos nacen `PROGRAMADO` con `fase = FASE_DE_GRUPOS`, el único valor existente sin conflicto de significado para una liga.
*Alternativa descartada*: agregar `jornada` a `partido`: obliga a una migración y a mantenerla en partidos manuales y editados, sin que ninguna regla actual la necesite.

**5. Generación atómica, con las mismas precondiciones que protegen al torneo.**
`generateSchedule` corre en `@Transactional` y lee el torneo con bloqueo (`findByIdForUpdate`), de modo que no puede cruzarse con la creación de un partido manual (que también bloquea la fila). Orden de validaciones: existe → formato `LEAGUE` → `IN_PROGRESS` → al menos 2 equipos → sin partidos → choques de fecha. Los choques se comprueban con una consulta nueva `existePartidoEnFechaParaEquipo(fecha, idEquipo)` para poder nombrar al equipo y la fecha. Solo si todo pasa se llama a `saveAll`. Exigir "sin partidos" evita duplicados al repetir la llamada y evita mezclar calendario con partidos manuales previos.

**6. API.**
`GET /api/tournaments/{id}/standings` (público) devuelve `List<StandingDTO>` (`position`, `teamId`, `teamName`, `played`, `won`, `drawn`, `lost`, `goalsFor`, `goalsAgainst`, `goalDifference`, `points`, `fairPlayPoints`); `POST /api/tournaments/{id}/schedule` (`ADMIN`) recibe `ScheduleRequestDTO` (`startDate`, `time`, `intervalDays` opcional, 1 a 30, por defecto 7) y responde 201 con los `PartidoDTO` creados. Los pesos, el mínimo de equipos y el intervalo por defecto son constantes. Sin cambios en `SecurityFilterChain`.

## Risks / Trade-offs

- [Las tarjetas y los goles por equipo siguen a `jugador.equipo`: un traspaso confirmado después de un partido mueve su historial al equipo nuevo] → Es una limitación ya existente en el cálculo de goles por equipo; no se corrige aquí y queda anotada para una mejora futura (guardar el equipo en la estadística o en la alineación).
- [Desempate en una sola pasada puede dar un orden distinto al de un reglamento con recursión] → Documentado en la spec y el diseño; el orden es determinista y siempre termina en el nombre.
- [Una liga con muchos equipos genera muchas consultas de choque de fecha (equipos × jornadas)] → Con las ligas del proyecto (decenas de equipos como máximo) son pocas decenas de consultas dentro de una transacción; aceptable.
- [Un partido generado y luego eliminado deja la liga con una pareja sin jugar] → El administrador puede crear un partido manual para esa pareja; la tabla solo cuenta lo finalizado.
- [`FASE_DE_GRUPOS` en una liga puede confundir a quien lea el dato] → Se documenta; los formatos con fases reales llegan en el change 3.

## Migration Plan

1. No hay migración de base de datos.
2. Reconstruir la imagen Docker solo cuando el usuario lo confirme (skill `docker-rebuild`).
3. Completar la Parte 2 de `docs/frontend-prompts/tournaments-module.md`, que desbloquea la entrega única del front.
4. Rollback: revertir el commit; no hay datos nuevos que limpiar.

## Open Questions

- Ninguna que cambie las specs. Si más adelante se quiere ida y vuelta o reprogramación de aplazados, se agrega como parámetro del calendario sin romper el contrato.
