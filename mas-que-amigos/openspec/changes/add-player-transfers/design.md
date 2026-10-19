# Design

## Context

Estado actual verificado en el código (ver `proposal.md` para la motivación):

- `Jugador` (`jugador`, código en español) tiene `@ManyToOne equipo` obligatorio y una restricción `UNIQUE (id_equipo, dorsal)`. `JugadorService#actualizarJugador` llama a `validarJugadorExiste(id, idEquipo)`, que exige que el jugador ya esté en ese equipo: hoy **no hay ningún camino para cambiar de equipo**. El traspaso será el único.
- No existe presupuesto/saldo en `Equipo`; el usuario decidió que el pago es solo un monto registrado.
- Errores: `BusinessException` → 400 (`ErrorResponseDTO`), `JugadorNotFoundException` → 404 (`{"error": ...}`). El esquema se versiona con Flyway (`V1`…`V6`); la última migración es `V6__imagen_galeria.sql`.
- `DirectorTecnicoInterceptor` protege **todo** `POST/PUT/DELETE/PATCH` de `/api/**` con el header `X-Director-Tecnico-Key`, así que las rutas nuevas quedan protegidas sin código adicional. Las consultas `GET` son libres.
- Reglas de idioma (`docs/base-standards.md` §2): clases, tablas, columnas y rutas **nuevas** en inglés; la columna nueva en `jugador` sigue el idioma de esa tabla (español); los textos al usuario van en español.

## Goals / Non-Goals

**Goals:**
- Traspaso en dos pasos (solicitud → confirmación/rechazo) con atomicidad al confirmar y rechazo automático de las demás solicitudes del mismo jugador.
- Historial auditable de traspasos con el costo vigente al solicitar.
- Compatibilidad hacia atrás del API de jugadores (el campo `costo` es opcional).

**Non-Goals:**
- Presupuesto, saldos, divisas o pagos reales entre equipos.
- Roles/login: la confirmación/rechazo usa la clave de Director Técnico existente como placeholder hasta que exista el rol organizador.
- Ventana de traspasos por torneo (no hay entidad torneo) y notificaciones a organizadores.
- Reasignar alineaciones/estadísticas históricas al cambiar de equipo (ver Riesgos).
- Cambios de frontend.

## Decisions

**1. Entidad y tabla nuevas `PlayerTransfer` / `player_transfer` (inglés), no columnas en `jugador`.**
Columnas: `id`, `player_id` (FK `jugador.id_jugador`), `origin_team_id` y `destination_team_id` (FK `equipos.id`), `cost DECIMAL(12,2)`, `status VARCHAR(20)`, `destination_dorsal INT NULL`, `requested_at DATETIME`, `resolved_at DATETIME NULL`. Índice sobre `(player_id, status)` y sobre `status`.
*Alternativa descartada*: una tabla de "pertenencia" (`player_team_history`) con rangos de fechas. Es más fiel a "equipos por los que ha pasado", pero duplica el modelo, exige backfill de jugadores existentes y el usuario pidió explícitamente el costo *por traspaso*. El historial se deriva de los traspasos `CONFIRMED`.

**2. Estado como enum `TransferStatus { PENDING, CONFIRMED, REJECTED }` guardado como texto** (mismo patrón que `Posicion` y `TipoClasificacion`).
Las solicitudes rechazadas automáticamente **no se borran**: pasan a `REJECTED` con `resolved_at`. Se interpreta "eliminadas de las solicitudes a los administradores" como "salen de la bandeja `PENDING`", conservando el registro para auditoría. *Alternativa descartada*: borrado físico, que pierde trazabilidad sin ganar nada visible para el administrador.

**3. `costo` en `jugador` como `BigDecimal` (`DECIMAL(12,2) NOT NULL DEFAULT 0`)**, nunca `double`. En `JugadorDTO` es nullable con `@DecimalMin("0.00")` y `@Digits(integer = 10, fraction = 2)`: `null` en crear → `0`; `null` en editar → se conserva el costo existente. Mensaje de validación en español. El costo del traspaso es un **snapshot** copiado de `jugador.costo` al solicitar; no se envía en el cuerpo de la solicitud (el usuario pidió que se pague "el valor que cuesta el jugador").

**4. Un servicio nuevo `PlayerTransferService` con la regla de negocio; `JugadorService` solo gana el campo `costo`.**
- `requestTransfer(playerId, destinationTeamId, destinationDorsal?)`: valida jugador (`JugadorNotFoundException` → 404), equipo destino (`BusinessException` → 400, igual que `ValidationCommonService#validarEquipo`), destino ≠ equipo actual, y dorsal libre en el destino (`existsByDorsalAndEquipoIdAndIdNot`, con el dorsal indicado o el actual). No impone unicidad de solicitudes `PENDING` por jugador.
- `confirmTransfer(id)` (`@Transactional`): carga la solicitud (404 si no existe), exige `PENDING`, **bloquea la fila del jugador** (`@Lock(PESSIMISTIC_WRITE)`) para serializar confirmaciones concurrentes del mismo jugador, revalida que `origin_team_id` siga siendo el equipo actual y el dorsal en el destino, actualiza `equipo` (y `dorsal`), marca `CONFIRMED` y rechaza en bloque las demás `PENDING` del jugador con una sola consulta `UPDATE ... SET status='REJECTED', resolved_at=? WHERE player_id=? AND status='PENDING' AND id<>?`. Si algo falla, todo se revierte (la solicitud sigue `PENDING`, nada se rechaza).
- `rejectTransfer(id)`: exige `PENDING`, marca `REJECTED`.
*Alternativa descartada*: unicidad de `PENDING` por jugador. El usuario pidió explícitamente permitir varias antes del inicio del torneo.

**5. API bajo `/api/transfers` (inglés), controlador y DTOs nuevos (`TransferRequestDTO`, `TransferDTO`).**
- `POST /api/transfers` — solicita (201).
- `POST /api/transfers/{id}/confirm` y `POST /api/transfers/{id}/reject` — verbos de acción sobre el recurso (200).
- `GET /api/transfers?status=&playerId=` — bandeja e historial con filtros opcionales, orden `requested_at` descendente.
Se evita colgar rutas bajo `/api/jugadores/**` porque `GET /api/jugadores/{nombre}` ya captura cualquier segmento. `status` inválido → 400 con mensaje en español (conversión manual del parámetro para devolver el formato de error del proyecto en vez del error genérico de Spring).
`TransferDTO` incluye ids y nombres de jugador y de ambos equipos para que la UI no necesite consultas adicionales.

**6. Errores**: nuevo `TransferNotFoundException` → 404 con el mismo formato `{"error": ...}` que `JugadorNotFoundException`, agregado a `GlobalExceptionHandler`. No se reutiliza `NotFoundException` porque su handler responde con un `EquipoDTO`.

**7. Migración `V7__player_cost_and_transfers.sql`**: `ALTER TABLE jugador ADD COLUMN costo DECIMAL(12,2) NOT NULL DEFAULT 0` y `CREATE TABLE player_transfer` con las FKs. Es aditiva, no requiere backfill y los jugadores existentes quedan con costo 0.

## Risks / Trade-offs

- **Alineaciones/estadísticas ligadas al jugador, no al equipo** → un jugador traspasado conserva sus estadísticas históricas (es lo deseable para el goleador del torneo) pero queda convocado en alineaciones de partidos programados de su equipo anterior. Mitigación: no se toca la alineación en este change; se documenta y se deja como seguimiento si el negocio quiere limpiarlas al confirmar.
- **Eliminar un jugador con traspasos** → falla por FK (hoy ocurre igual con alineaciones/estadísticas). Mitigación: documentarlo; no se agrega `ON DELETE CASCADE` para no perder historial por accidente.
- **Costo sin divisa** → el monto es adimensional. Mitigación: documentar en el API que es un valor único de la liga; agregar divisa sería un change aparte.
- **Confirmación sin rol organizador** → mientras no exista el login, quien tenga la clave de Director Técnico puede confirmar traspasos. Mitigación: el interceptor central ya lo restringe; al llegar los roles se cambia la regla de autorización sin tocar la lógica de negocio.
- **Concurrencia en confirmaciones** → dos organizadores confirmando solicitudes distintas del mismo jugador. Mitigación: bloqueo pesimista de la fila del jugador; la segunda confirmación encuentra su solicitud ya `REJECTED` y responde 400.
- **Dorsal** → la validación previa puede pasar y aun así fallar al confirmar. Mitigación: se revalida al confirmar y la restricción `UNIQUE` de BD es la red de seguridad final; el error se traduce a mensaje en español.

## Migration Plan

1. Desplegar el backend: Flyway aplica `V7` al arrancar (columna con `DEFAULT 0` y tabla nueva; sin bloqueos largos).
2. Verificar que `GET /api/jugadores` devuelve `costo: 0` para los jugadores existentes.
3. Rollback: revertir el despliegue del backend y, si hace falta, `DROP TABLE player_transfer` y `ALTER TABLE jugador DROP COLUMN costo` (los datos nuevos se pierden; el resto del modelo no depende de ellos).
