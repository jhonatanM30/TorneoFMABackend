# Documentación del modelo de datos

Este documento describe el modelo de datos de **Más que Amigos**, una aplicación
para gestionar una liga deportiva amateur: equipos, jugadores, partidos,
alineaciones, estadísticas y contenido informativo del sitio.

## Diagrama entidad-relación

```mermaid
erDiagram
    EQUIPO ||--o{ JUGADOR : "tiene"
    EQUIPO ||--o{ PARTIDO : "juega como local"
    EQUIPO ||--o{ PARTIDO : "juega como visitante"
    PARTIDO ||--o{ ALINEACION : "convoca"
    JUGADOR ||--o{ ALINEACION : "es convocado en"
    PARTIDO ||--o{ ESTADISTICA : "registra"
    JUGADOR ||--o{ ESTADISTICA : "acumula"
    PARTIDO ||--o{ CAMBIO_JUGADOR : "registra"
    JUGADOR ||--o{ CAMBIO_JUGADOR : "sale/entra en"
    JUGADOR ||--o{ PLAYER_TRANSFER : "es traspasado en"
    EQUIPO ||--o{ PLAYER_TRANSFER : "origen"
    EQUIPO ||--o{ PLAYER_TRANSFER : "destino"
    EQUIPO |o--o| APP_USER : "tiene un DT con cuenta"
```

## Descripción de los modelos

### 1. Equipo (tabla `equipos`)
Representa un equipo participante en la liga.

**Campos:**
- `id`: identificador único (llave primaria, autoincremental)
- `nombre`: nombre del equipo (máx. 100 caracteres, obligatorio)
- `directorTecnico` (`director_tecnico`): nombre del director técnico (máx. 100 caracteres, obligatorio)
- `imagenUrl` (`imagen_url`): URL del escudo del equipo, servido desde `/uploads/**`
- `titulos`: número de títulos ganados
- `tipoClasificacion` (`tipo_clasificacion`): enum `ELIMINATORIA` | `REPECHAJE`, guardado como texto en BD

**Relaciones:**
- `jugadores`: uno a muchos con Jugador (`mappedBy = "equipo"`, cascada total, `orphanRemoval`)

### 2. Jugador (tabla `jugador`)
Representa un jugador perteneciente a un equipo.

**Campos:**
- `id` (`id_jugador`): identificador único
- `nombre`: nombre del jugador (máx. 100 caracteres, obligatorio)
- `posicion`: enum `PORTERO` | `DEFENSA` | `MEDIOCAMPISTA` | `DELANTERO`
- `edad`: edad del jugador (obligatorio)
- `dorsal`: número de camiseta (obligatorio)
- `imagenUrl` (`imagen_url`): URL de la foto del jugador, servida desde `/uploads/jugadores/**`
- `costo`: valor del jugador (`DECIMAL(12,2)`, `>= 0`, sin divisa; por defecto 0). Es el monto que se copia al solicitar un traspaso

**Relaciones:**
- `equipo`: muchos a uno con Equipo (`id_equipo`, obligatorio). El equipo de un jugador solo cambia mediante un traspaso confirmado (ver `PlayerTransfer`)

**Notas:**
- Un jugador con alineaciones, estadísticas o traspasos registrados no se puede eliminar (restricción de llave foránea; no hay `ON DELETE CASCADE`).
- Al confirmar un traspaso el jugador conserva sus estadísticas históricas, pero sus alineaciones de partidos ya programados con el equipo anterior no se modifican.

### 3. Partido (tabla `partido`)
Representa un encuentro entre dos equipos.

**Campos:**
- `id` (`id_partido`): identificador único
- `fecha`, `hora`: fecha y hora del partido
- `golesLocal` (`goles_local`), `golesVisitante` (`goles_visitante`): marcador, derivado de los goles registrados en Estadística
- `fase`: enum `FASE_DE_GRUPOS` | `REPECHAJE` | `ELIMINACION_DIRECTA` | `FINAL`
- `estado`: enum `PROGRAMADO` | `EN_CURSO` | `FINALIZADO` (por defecto `PROGRAMADO`; solo cambia vía `PartidoService#iniciarPartido`/`#finalizarPartido`, nunca por edición directa)

**Relaciones:**
- `equipoLocal`, `equipoVisitante`: muchos a uno con Equipo (ambos obligatorios)

### 4. Alineacion (tabla `alineacion`)
Convocatoria de un jugador a un partido específico, con llave compuesta.

**Llave compuesta (`AlineacionId`):** `idPartido` + `idJugador`

**Campos:**
- `titular`: booleano — indica si el jugador inicia como titular o suplente (editable entre partido programado y en curso)

**Relaciones:**
- `partido`, `jugador`: asociaciones de solo lectura (`insertable = false, updatable = false`) hacia los mismos IDs de la llave compuesta

### 5. Estadistica (tabla `estadistica`)
Estadísticas individuales de un jugador en un partido.

**Campos:**
- `id` (`id_estadistica`): identificador único
- `goles`, `tarjetasAmarillas` (`tarjetas_amarillas`), `tarjetasRojas` (`tarjetas_rojas`), `asistencias`: contadores (por defecto 0)
- `minuto`: minuto del partido en que ocurrió, opcional (nulo en registros cargados sin ese dato)

**Relaciones:**
- `jugador`, `partido`: muchos a uno, ambos obligatorios

### 6. CambioJugador (tabla `cambio_jugador`)
Sustitución de un jugador por otro durante un partido. Es un registro de auditoría/bitácora.

**Campos:**
- `id` (`id_cambio_jugador`): identificador único
- `minuto`: minuto del cambio (obligatorio)
- `fechaRegistro` (`fecha_registro`): fecha y hora en que se registró el cambio

**Relaciones:**
- `partido`: muchos a uno, obligatorio
- `jugadorSale` (`id_jugador_sale`), `jugadorEntra` (`id_jugador_entra`): ambos muchos a uno con Jugador, obligatorios

### 7. RegistroInformativo (tabla `registro_informativo`)
Contenido tipo blog mostrado en la página de Inicio, administrado desde Configuración.

**Campos:**
- `id` (`id_registro_informativo`): identificador único
- `titulo`: máx. 150 caracteres, obligatorio
- `contenido`: texto largo (`LONGTEXT` en BD), obligatorio
- `fechaPublicacion` (`fecha_publicacion`): fecha y hora de publicación

### 8. ImagenGaleria (tabla `imagen_galeria`)
Fotos administrables para el carrusel de la página de Inicio.

**Campos:**
- `id` (`id_imagen_galeria`): identificador único
- `imagenUrl` (`imagen_url`): URL de la imagen, obligatorio
- `fechaSubida` (`fecha_subida`): fecha y hora de subida

### 9. PlayerTransfer (tabla `player_transfer`)
Solicitud de traspaso de un jugador de su equipo actual a otro equipo. Cada fila es parte del historial: el costo es una copia del `costo` del jugador al momento de solicitarla y no cambia si luego se edita el jugador. Tabla y columnas nuevas, por eso van en inglés.

**Campos:**
- `id`: identificador único
- `cost`: costo del jugador al solicitar el traspaso (`DECIMAL(12,2)`)
- `status`: enum `PENDING` | `CONFIRMED` | `REJECTED`, guardado como texto en BD
- `destinationDorsal` (`destination_dorsal`): dorsal nuevo en el equipo destino, opcional (si es nulo se conserva el del jugador)
- `requestedAt` (`requested_at`): fecha y hora de la solicitud
- `resolvedAt` (`resolved_at`): fecha y hora en que se confirmó o rechazó; nulo mientras está `PENDING`

**Relaciones:**
- `player` (`player_id`): muchos a uno con Jugador, obligatorio
- `originTeam` (`origin_team_id`), `destinationTeam` (`destination_team_id`): muchos a uno con Equipo, ambos obligatorios

**Reglas:**
- Pueden coexistir varias solicitudes `PENDING` para el mismo jugador.
- Al confirmar una, el jugador pasa al equipo destino y las demás `PENDING` del mismo jugador pasan a `REJECTED` en la misma transacción (no se borran: quedan como registro y salen de la bandeja de pendientes).
- El historial de equipos de un jugador son sus traspasos `CONFIRMED`.
- Índices: `(player_id, status)` y `status`.

### 10. AppUser (tabla `app_user`)
Cuenta para iniciar sesión. Tabla nueva, por eso va en inglés. La crea un administrador (o el sistema, para el administrador inicial); no hay auto-registro.

**Campos:**
- `id`: identificador único
- `username`: usuario para iniciar sesión (3 a 50 caracteres: letras, números, punto, guion y guion bajo), único. Se guarda siempre en minúsculas y la comparación no distingue mayúsculas
- `passwordHash` (`password_hash`): contraseña protegida con BCrypt; nunca se guarda ni se devuelve en claro
- `role`: enum `ADMIN` | `DT`, guardado como texto en BD. El visitante no tiene cuenta ni rol
- `createdAt` (`created_at`): fecha y hora de creación

**Relaciones:**
- `team` (`team_id`): muchos a uno con Equipo, opcional y **único**. Un DT tiene como máximo un equipo y un equipo tiene como máximo una cuenta. Una cuenta `DT` puede estar sin equipo (por ejemplo, mientras espera la aprobación del que solicitó) y una cuenta `ADMIN` nunca tiene equipo

**Reglas:**
- El administrador inicial se crea al arrancar con `APP_SECURITY_ADMIN_USERNAME` y `APP_SECURITY_ADMIN_PASSWORD`, solo si no existe ya; nunca se modifica una cuenta existente.
- El texto `Equipo.directorTecnico` no se sincroniza con la cuenta del DT: lo mantiene el administrador.
- El equipo del DT se consulta siempre en esta tabla (no en el token de sesión), porque puede asignarse después de iniciar sesión.
