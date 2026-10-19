# Proposal

## Why

Hoy un jugador pertenece a un único equipo y no hay forma de moverlo a otro: `PUT /api/jugadores` exige que el jugador ya exista en el equipo enviado, así que el equipo es de facto inmutable. Tampoco existe el concepto de "costo" de un jugador ni un historial de los equipos por los que ha pasado. La liga necesita traspasos entre equipos, con el valor del jugador registrado en cada uno, y con un paso de aprobación por parte de los organizadores (rol que se creará con el futuro login/permisos).

## What Changes

- El **jugador** gana un campo `costo` (valor monetario, `>= 0`). Es opcional al crear/editar (por defecto `0`) para no romper al frontend ni a los jugadores ya existentes.
- Nuevo flujo de **traspaso** (`transfer`) de un jugador de su equipo actual a otro equipo:
  - Se **solicita** un traspaso: queda en estado `PENDING` y guarda una copia (snapshot) del `costo` del jugador en ese momento. El jugador no cambia de equipo todavía.
  - **Pueden coexistir varias solicitudes `PENDING` para el mismo jugador** (por ejemplo, de distintos equipos interesados antes del inicio de un torneo).
  - Un organizador **confirma** una solicitud (`CONFIRMED`): el jugador pasa al equipo destino de forma atómica y **todas las demás solicitudes `PENDING` de ese jugador se rechazan automáticamente** (`REJECTED`) y dejan de aparecer en la bandeja de solicitudes pendientes de los administradores.
  - Un organizador también puede **rechazar** una solicitud puntual (`REJECTED`): no cambia nada más.
  - Si el equipo destino ya usa el dorsal del jugador, la solicitud puede traer un dorsal nuevo; el dorsal se valida al solicitar y otra vez al confirmar (por la restricción `UNIQUE (id_equipo, dorsal)`).
- Nuevo **historial de traspasos** consultable por jugador, con equipo origen, equipo destino, costo y fechas. Las solicitudes rechazadas (manual o automáticamente) se conservan como registro, pero no cuentan como paso por un equipo.
- El "pago" **solo se registra como monto** en el traspaso: no hay presupuesto ni saldo por equipo (decisión del usuario). No se valida disponibilidad de fondos.
- La autorización real (rol organizador) queda fuera: hasta que exista login, las operaciones de escritura (incluido confirmar/rechazar) quedan protegidas por la clave de Director Técnico que ya aplica a todo `POST/PUT/DELETE` de `/api/**`.

## Capabilities

### New Capabilities
- `player-cost`: el jugador tiene un costo (valor) validado, persistido y expuesto en el API de jugadores.
- `player-transfers`: solicitud, confirmación y rechazo (manual y automático) de traspasos entre equipos, bandeja de solicitudes pendientes y consulta del historial de traspasos de un jugador.

### Modified Capabilities
<!-- Ninguna: no existen specs previas del módulo de jugadores en openspec/specs/ (solo top-scorer, que no cambia). -->

## Impact

- **Base de datos**: migración Flyway `V7` — `ALTER TABLE jugador ADD COLUMN costo` y nueva tabla `player_transfer` con FKs a `jugador` y `equipos`.
- **Código backend** (Spring Boot 2.7 / Java 8): `Jugador`, `JugadorDTO`, `JugadorMapper`, `JugadorService` (campo `costo`); clases nuevas en inglés para el traspaso (entidad, repositorio, servicio, controlador, DTOs, mapper); `GlobalExceptionHandler` solo si hace falta un 404 propio.
- **API**: rutas nuevas bajo `/api/transfers`; `/api/jugadores` agrega el campo `costo` (compatible hacia atrás). Se actualiza `docs/api-spec.yml` y `docs/data-model.md`.
- **Riesgo conocido**: eliminar un jugador con traspasos registrados chocará con la FK (mismo comportamiento que hoy con alineaciones/estadísticas); se documenta en el diseño.
- **Fuera de alcance**: ventana de traspasos "antes del inicio de cada torneo" (no existe una entidad torneo; no se valida la fecha), roles/login, y el frontend (`MasQueAmigos-Torneo`, repo aparte).
