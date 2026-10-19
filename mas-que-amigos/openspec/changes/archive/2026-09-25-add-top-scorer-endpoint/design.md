# Design

## Context

Ver `proposal.md` - Why. Restricciones técnicas relevantes:
- No existe entidad `Torneo`; el torneo es implícito y se agrega sobre toda la tabla `estadistica`.
- `estadistica` ya garantiza un único registro por `(id_jugador, id_partido)`, así que `SUM(goles)` por jugador no duplica goles de un mismo partido.
- El patrón existente en `EstadisticaRepository` es Spring Data JPA con `@Query` JPQL (ver `sumarGolesPorEquipo`); no hay capa de dominio separada de la infraestructura.

## Goals / Non-Goals

**Goals:**
- Calcular el ranking de goleadores con una consulta de agregación, sin cargar en memoria todas las filas de `estadistica`.
- Manejar el caso de empate en el máximo devolviendo todos los jugadores empatados, sin lógica ad-hoc en el controller.

**Non-Goals:**
- No se introduce una entidad `Torneo` ni un concepto de temporada/torneo múltiple.
- No se implementa un ranking completo paginado (top 10, top N); solo el máximo (y sus empates).
- No se cachea el resultado; se calcula en cada solicitud (volumen de datos bajo, un torneo amistoso).

## Decisions

- **Consulta de dos pasos (máximo + empatados) en vez de una sola query con `HAVING`**: se usa una consulta JPQL que agrupa por jugador y ordena por total de goles descendente (similar a `sumarGolesPorEquipo`), y en `EstadisticaService.obtenerGoleadores()` se toma el primer valor como el máximo y se filtran en memoria los jugadores cuyo total sea igual a ese máximo. Alternativa considerada: una única query con `HAVING SUM(goles) = (SELECT MAX(...))` (subquery correlacionada). Se descarta por ser menos legible en JPQL y no aportar ventaja de rendimiento medible dado el volumen de datos del torneo (decenas de jugadores, no miles).
- **DTO de proyección (`GoleadorDTO`) en vez de reutilizar `EstadisticaDTO`**: `EstadisticaDTO` representa un registro individual jugador+partido (incluye tarjetas, asistencias, minuto), que no aplica a un total agregado. Se crea `GoleadorDTO` con solo los campos relevantes: id de jugador, nombre, equipo actual, total de goles.
- **Nuevo endpoint bajo el controller existente `EstadisticaController` (`GET /api/estadisticas/goleador`)** en vez de un controller nuevo: el dato fuente es la tabla `estadistica` y el resto de operaciones de estadísticas ya viven ahí; no se justifica una nueva capa de controller para una sola operación de lectura agregada.

## Risks / Trade-offs

- [Cálculo del máximo en memoria con Java Streams sobre el resultado de la query] → Mitigación: el volumen de filas devueltas es una fila por jugador con goles (no por partido), acotado al tamaño del roster del torneo; no representa un riesgo de rendimiento.
- [Ausencia de entidad `Torneo`: si en el futuro se manejan varios torneos, este endpoint mezclaría goles de todos] → Mitigación: fuera de alcance de este change; documentado explícitamente en el proposal como comportamiento actual (torneo único implícito).
