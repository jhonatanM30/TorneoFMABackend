# Proposal

## Why

Hoy no existe ninguna forma de consultar quién es el goleador del torneo: `EstadisticaController` solo expone operaciones CRUD sobre un registro individual de `Estadistica` (por jugador y partido), y no hay ningún endpoint que agregue goles entre partidos. El torneo necesita mostrar esta información (por ejemplo en la galería o en un tablero de estadísticas del frontend), por lo que se requiere un endpoint de solo lectura que calcule el ranking de goleadores sumando los goles de todos los partidos.

## What Changes

- Se agrega un endpoint de solo lectura `GET /api/estadisticas/goleador` que devuelve el/los jugador(es) con más goles del torneo, sumando el campo `goles` de la tabla `estadistica` en todos los partidos (sin filtrar por equipo ni por estado del partido).
- El torneo actual es implícito: no existe una entidad `Torneo`, así que la agregación se hace sobre todos los registros de `estadistica` existentes en la base de datos.
- Si dos o más jugadores empatan en el máximo de goles, el endpoint devuelve **todos** los jugadores empatados (respuesta siempre en forma de lista), en vez de elegir uno arbitrariamente.
- Un jugador sin ningún registro de `estadistica` no participa en el cálculo (no se le asume 0 goles como candidato a goleador).
- Se agrega un nuevo método de agregación en `EstadisticaRepository` (consulta `SUM(goles) GROUP BY id_jugador`) y un método `EstadisticaService.obtenerGoleadores()` que lo consume, además de un DTO nuevo `GoleadorDTO` (id de jugador, nombre, equipo actual, total de goles).

## Capabilities

### New Capabilities
- `top-scorer`: consulta de solo lectura que expone el/los goleador(es) del torneo (jugador o jugadores con más goles sumando todos los partidos), incluyendo el manejo de empates.

### Modified Capabilities
(ninguna; no se modifica el comportamiento de los endpoints existentes de `estadisticas`, `partidos` ni `jugadores`)

## Impact

- **Backend** (`src/main/java/com/marin/mas_que_amigos/`):
  - `repository/EstadisticaRepository.java`: nueva consulta `@Query` de agregación por jugador.
  - `service/EstadisticaService.java`: nuevo método `obtenerGoleadores()`.
  - `dto/`: nuevo `GoleadorDTO`.
  - `mapper/`: posible mapeo de proyección JPQL a `GoleadorDTO`.
  - `controller/EstadisticaController.java`: nuevo endpoint `GET /api/estadisticas/goleador`, documentado con las anotaciones Swagger/OpenAPI que ya usa el controller.
- **Tests**: nuevo `EstadisticaServiceTest.java` (no existe todavía) siguiendo el patrón JUnit 5 + Mockito + AssertJ de `PartidoServiceTest.java`, cubriendo caso sin goles registrados, caso con un único goleador y caso de empate.
- **Frontend / consumidores del API** (`MasQueAmigos-Torneo`, repo aparte): nuevo contrato de respuesta a documentar; no rompe endpoints existentes.
- **Base de datos**: sin cambios de esquema; se reutiliza la tabla `estadistica` existente.
