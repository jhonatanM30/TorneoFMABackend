# Prompt para el front: módulo de torneos (3 changes del backend en una sola entrega)

> Para la IA que trabaja el repositorio `MasQueAmigos-Torneo` (HTML, CSS y JavaScript planos, sin framework ni build). No hagas commit: deja los cambios en el árbol de trabajo para que el usuario los revise.

> **Estado del documento — no entregar todavía.** Este prompt unifica en una sola implementación del front los tres changes del backend del módulo de torneos. El front se hará una vez, **cuando los tres estén listos**. Si ves este aviso, no ejecutes el prompt.
>
> | Change | Contenido | Estado |
> |---|---|---|
> | 1. `add-tournaments` | Torneos, formato, inscripción de equipos y partidos manuales | Implementado y archivado (Parte 1 completa) |
> | 2. `add-league-standings` | Tabla de posiciones y calendario de la liga | Implementado en el backend (Parte 2 completa) |
> | 3. Grupos y eliminatoria | Grupos, clasificación y llaves | Sin planificar (Parte 3 pendiente) |
>
> Al cerrar el change 3, completa la Parte 3, ajusta lo que cambie en las Partes 1 y 2 y **quita este aviso** antes de entregarlo. Este archivo reemplaza al anterior `add-tournaments.md`, que no se ejecutará por separado.

# Parte 1 — Torneos, inscripción y partidos manuales (change 1)

## Contexto

El backend (`mas-que-amigos`, puerto 57075 por defecto) incorporó **torneos**: el administrador crea un torneo, define cómo se juega (liga, grupos con eliminatoria o eliminatoria directa), inscribe equipos, lo inicia y arma partidos dentro de él. Todo partido pertenece ahora a un torneo.

**No hay cambios incompatibles.** Todo lo que el front hace hoy sigue funcionando: un partido creado sin indicar torneo queda en el torneo protegido "Histórico". Lo nuevo es opcional de adoptar, pero es el objetivo de esta tarea.

Alcance de la Parte 1: el torneo guarda su formato y lo bloquea al iniciar, y todos los partidos se crean a mano. La tabla de posiciones y el calendario automático de las ligas están en la Parte 2; los grupos y las llaves, en la Parte 3 (pendiente). No muestres nada de eso hasta que su parte esté completa.

## Lo que debes construir

1. **Sección de torneos** visible para todos (consulta pública): lista de torneos con nombre, formato, estado y cantidad de equipos, y un detalle con los equipos inscritos y los partidos del torneo.
2. **Gestión de torneos solo para `ADMIN`** (oculta o deshabilita para visitante y `DT`; el backend es la autoridad y responde `401`/`403`):
   - Crear torneo (nombre y formato).
   - Editar nombre y formato mientras esté en borrador (`DRAFT`).
   - Eliminar un torneo en borrador.
   - Inscribir y retirar equipos mientras esté en borrador.
   - Iniciar el torneo y finalizarlo.
3. **Crear partido dentro de un torneo**: en el formulario de partidos del administrador, agrega un selector de torneo (solo torneos `IN_PROGRESS`; el "Histórico" también aparece) y envía `idTorneo`. Al elegir torneo, los selectores de equipo local y visitante deben mostrar solo los equipos inscritos en él.
4. **Partido manual**: es simplemente crear un partido en el torneo entre dos equipos inscritos cualesquiera, en **cualquier** formato. Ejemplo: si A vs B no se pudo jugar, el administrador crea A vs C. No existe una opción ni un campo aparte para "manual", y no debes inventar uno: es un partido normal del torneo y cuenta igual para la puntuación cuando esta exista.
5. **Equipo en varios torneos**: un mismo equipo puede estar inscrito en varios torneos a la vez (por ejemplo, una liga y una copa). No lo muestres como un error.

## Formatos y estados

| Formato (`format`) | Significado |
|---|---|
| `LEAGUE` | Liga |
| `GROUPS_KNOCKOUT` | Fase de grupos y eliminatoria |
| `KNOCKOUT` | Eliminatoria directa |
| `LEGACY` | Solo lo tiene el torneo "Histórico". **No lo ofrezcas** al crear ni al editar |

| Estado (`status`) | Qué se puede hacer |
|---|---|
| `DRAFT` | Editar nombre y formato, eliminar, inscribir y retirar equipos, iniciar |
| `IN_PROGRESS` | Crear partidos del torneo; finalizarlo. Formato e inscripciones bloqueados |
| `FINISHED` | Solo consulta |

El ciclo es lineal (`DRAFT` → `IN_PROGRESS` → `FINISHED`), sin retroceso. Muestra el formato y el estado con textos en español (por ejemplo, "Liga", "En curso").

El torneo **"Histórico"** (`format = LEGACY`, siempre `IN_PROGRESS`) agrupa los partidos anteriores a los torneos. Está protegido: no ofrezcas editar, eliminar, iniciar, finalizar ni inscribir equipos en él. No usa inscripción, así que en él se puede crear un partido con cualquier pareja de equipos.

## Contrato del API

Base `http://localhost:57075` (configurable). Las escrituras llevan `Authorization: Bearer <token>` de un `ADMIN` (ver el prompt `add-authentication-roles`).

### Consultas (públicas)

- `GET /api/tournaments` → arreglo de torneos ordenado por nombre.
- `GET /api/tournaments/{id}` → un torneo.
- `GET /api/tournaments/{id}/teams` → equipos inscritos (mismo formato que `GET /api/equipos`), ordenados por nombre; lista vacía si no hay.
- `GET /api/tournaments/{id}/matches` → partidos del torneo (mismo formato que `GET /api/partidos`), ordenados por fecha y hora.

Forma de un torneo:

```json
{
  "id": 2,
  "name": "Liga Apertura",
  "format": "LEAGUE",
  "status": "IN_PROGRESS",
  "teamCount": 4,
  "createdAt": "2026-11-02T18:46:02",
  "startedAt": "2026-11-02T18:47:56"
}
```

`startedAt` es `null` mientras el torneo está en `DRAFT`.

### Escrituras (solo `ADMIN`)

| Operación | Cuerpo | Respuesta |
|---|---|---|
| `POST /api/tournaments` | `{"name": "...", "format": "LEAGUE"}` | `201` con el torneo en `DRAFT` |
| `PUT /api/tournaments/{id}` | `{"name": "...", "format": "KNOCKOUT"}` | `200` con el torneo (solo en `DRAFT`) |
| `DELETE /api/tournaments/{id}` | — | `204` (solo en `DRAFT`) |
| `POST /api/tournaments/{id}/teams` | `{"teamId": 3}` | `201` con el equipo inscrito (solo en `DRAFT`) |
| `DELETE /api/tournaments/{id}/teams/{teamId}` | — | `204` (solo en `DRAFT`) |
| `POST /api/tournaments/{id}/start` | — | `200` con el torneo en `IN_PROGRESS` (mínimo 2 equipos) |
| `POST /api/tournaments/{id}/finish` | — | `200` con el torneo en `FINISHED` (todos los partidos finalizados) |

Reglas de validación: el nombre tiene entre 3 y 100 caracteres y no se repite (sin distinguir mayúsculas); el formato es obligatorio y solo `LEAGUE`, `GROUPS_KNOCKOUT` o `KNOCKOUT`.

### Partidos

`POST /api/partidos` acepta un campo nuevo y opcional, `idTorneo`; el resto del cuerpo es el de siempre (`idEquipoLocal`, `idEquipoVisitante`, `fecha`, `hora`, `fase`):

```json
{
  "idEquipoLocal": 1,
  "idEquipoVisitante": 3,
  "fecha": "2027-03-08",
  "hora": "15:00:00",
  "fase": "FASE_DE_GRUPOS",
  "idTorneo": 2
}
```

- Con `idTorneo`: el torneo debe estar `IN_PROGRESS` y **ambos** equipos inscritos en él; si no, `400` con un mensaje.
- Sin `idTorneo`: el partido va al torneo "Histórico" (comportamiento anterior).
- Las respuestas de partido (`GET`, `POST`, `PUT`) ahora incluyen `idTorneo`.
- `PUT /api/partidos` **ignora** `idTorneo`: el torneo de un partido no cambia. Si cambias un equipo del partido por otro, el nuevo debe estar inscrito en el torneo.
- Se mantienen las reglas de siempre: equipos distintos y un equipo no puede tener dos partidos el mismo día (aunque sean de torneos distintos).
- Un partido de un torneo no finalizado se puede eliminar con `DELETE /api/partidos/{id}`; es la forma de descartar un partido aplazado, porque el torneo solo se puede finalizar cuando todos sus partidos están finalizados.

## Formato de los errores

| Código | Cuándo | Cuerpo |
|---|---|---|
| `400` | Regla de negocio (torneo ya iniciado, menos de 2 equipos, equipo no inscrito, partidos pendientes, nombre repetido, formato inválido, torneo protegido) | `{"indicadorRespuesta":"Error","mensaje":"..."}` |
| `400` | Validación de campos | mapa campo → mensaje, por ejemplo `{"name":"El nombre del torneo debe tener entre 3 y 100 caracteres."}` |
| `401` / `403` | Sin sesión o sesión vencida / rol que no alcanza | `{"indicadorRespuesta":"...","mensaje":"..."}` |
| `404` | El torneo no existe, o el equipo no está inscrito al retirarlo | `{"indicadorRespuesta":"Sin Registros","mensaje":"..."}` |

Muestra siempre el `mensaje` del servidor al usuario; ya viene en español.

## Flujo que debe poder hacer el administrador

1. Crear el torneo y elegir su formato.
2. Inscribir los equipos.
3. Iniciarlo (el botón solo tiene sentido con 2 o más equipos).
4. Crear los partidos del torneo, incluido el de reemplazo cuando uno se aplaza.
5. Jugar los partidos con el flujo de siempre (alineaciones, iniciar, estadísticas, finalizar).
6. Finalizar el torneo cuando todos sus partidos estén finalizados. Si el servidor responde que faltan partidos, muestra cuántos y ofrece ir a jugarlos o eliminarlos.

## Criterios de aceptación

- Un visitante y un `DT` ven la lista y el detalle de los torneos, sin ninguna acción de escritura.
- Un `ADMIN` crea un torneo `LEAGUE`, lo edita a `KNOCKOUT` mientras es borrador, inscribe 3 equipos y lo inicia; después no puede cambiar el formato ni las inscripciones.
- Iniciar con menos de 2 equipos muestra el mensaje del servidor y el torneo sigue en borrador.
- Un `ADMIN` crea el partido A vs B y luego A vs C en el mismo torneo; un equipo no inscrito no aparece como opción y, si se fuerza, se muestra el `400` del servidor.
- El mismo equipo se inscribe en dos torneos distintos sin error.
- Crear un partido sin elegir torneo sigue funcionando y el partido aparece en el "Histórico".
- El torneo "Histórico" no ofrece editar, eliminar, iniciar, finalizar ni inscribir.
- Finalizar con partidos pendientes muestra el mensaje del servidor; tras jugarlos o eliminarlos, finaliza.
- El front no ofrece el formato `LEGACY` ni muestra un campo o interruptor de "partido manual".

---

# Parte 2 — Liga: tabla de posiciones y calendario (change 2)

> Solo aplica a torneos con `format = "LEAGUE"`. Para `GROUPS_KNOCKOUT` y `KNOCKOUT` el servidor responde `400` en estos endpoints (llegan en la Parte 3); no muestres la tabla ni el botón de calendario en esos torneos ni en el "Histórico".

## Qué construir

1. **Tabla de posiciones** en el detalle de un torneo de liga, visible para todos (consulta pública). Columnas: posición, equipo, PJ, G, E, P, GF, GC, DG, Pts y juego limpio. Muéstrala también con el torneo en borrador o recién iniciado: con todos los equipos en 0.
2. **Botón "Generar calendario"** solo para `ADMIN`, en un torneo de liga en curso que todavía no tiene partidos. Pide fecha de inicio, hora y, opcional, intervalo en días entre jornadas. Después de generarlo, muestra los partidos agrupados por jornada.
3. **Partidos agrupados por jornada**: el backend no guarda un número de jornada. **Una jornada es una fecha**: todos los partidos con la misma `fecha` son una jornada, y la jornada 1 es la fecha más antigua. Agrupa y numera en el front. Con equipos impares, un equipo descansa en cada jornada; puede mostrarlo como "Descansa: X" deduciéndolo de los equipos inscritos que no juegan esa fecha.
4. **Los partidos manuales de la Parte 1 siguen disponibles** después de generar el calendario: sirven para reemplazar un partido aplazado (elimina el aplazado con `DELETE /api/partidos/{id}` y crea A vs C con `POST /api/partidos` e `idTorneo`) o para añadir uno extra. Cuentan para la tabla igual que los generados; no los distingas.
5. Los partidos generados se editan (cambiar fecha u hora) y se eliminan como cualquier otro partido, con las mismas reglas de siempre.

## Contrato del API

### `GET /api/tournaments/{id}/standings` (público)

Arreglo ordenado por posición, con **todos los equipos inscritos** (incluso sin partidos):

```json
[
  {
    "position": 1,
    "teamId": 1,
    "teamName": "Macalister",
    "played": 3,
    "won": 2,
    "drawn": 1,
    "lost": 0,
    "goalsFor": 3,
    "goalsAgainst": 0,
    "goalDifference": 3,
    "points": 7,
    "fairPlayPoints": 0
  }
]
```

Reglas (el servidor ya las aplica; no recalcules ni reordenes en el front, solo muestra el orden recibido):

- Cuentan solo los partidos `FINALIZADO` del torneo; los programados y en curso no suman.
- Victoria 3 puntos, empate 1, derrota 0.
- Desempate con los mismos puntos, en este orden: enfrentamiento directo, diferencia de goles, goles a favor, juego limpio y nombre. `fairPlayPoints` es amarillas × 1 + rojas × 2: **menos es mejor** y solo se usa para desempatar.
- Las posiciones son únicas (1 a n), sin empates compartidos.
- La tabla se calcula en cada consulta: refréscala después de finalizar un partido o de corregir una estadística.

Errores: `400` `{"indicadorRespuesta":"Error","mensaje":"..."}` si el torneo no es de liga; `404` `{"indicadorRespuesta":"Sin Registros","mensaje":"..."}` si no existe.

### `POST /api/tournaments/{id}/schedule` (solo `ADMIN`)

Cuerpo:

```json
{ "startDate": "2027-03-06", "time": "15:00:00", "intervalDays": 7 }
```

- `startDate` y `time` son obligatorios; `intervalDays` es opcional (1 a 30, por defecto 7).
- Respuesta `201`: arreglo con los partidos creados (misma forma que `GET /api/partidos`, con `idTorneo`), todos `PROGRAMADO`.
- Una sola vuelta: cada pareja de equipos se enfrenta una vez. Cada jornada cae `intervalDays` después de la anterior (la primera en `startDate`) y todos los partidos usan la misma hora.
- Es todo o nada: si un equipo ya tiene un partido (de cualquier torneo) en la fecha de una jornada, no se crea ninguno y el mensaje nombra al equipo y la fecha; ofrece elegir otra fecha de inicio o intervalo.
- Solo funciona con el torneo `IN_PROGRESS` y sin partidos (generados ni manuales); si no, responde `400` con el motivo. No se puede generar dos veces.

Errores: `400` regla de negocio `{"indicadorRespuesta":"Error","mensaje":"..."}` (torneo que no es de liga, no está en curso, menos de 2 equipos, ya tiene partidos, equipo ocupado en una fecha); `400` validación mapa campo → mensaje (`startDate`, `time`, `intervalDays`); `401` sin sesión; `403` si el rol no es `ADMIN`; `404` si el torneo no existe. Muestra siempre el `mensaje` del servidor.

## Cambios sobre la Parte 1

- El detalle de un torneo `LEAGUE` suma la pestaña o sección "Tabla" y, para el `ADMIN`, el botón "Generar calendario". Los demás formatos no los muestran.
- Mientras un torneo de liga en curso no tenga partidos, ofrece "Generar calendario" como camino principal y deja "Crear partido" como alternativa manual.
- Al finalizar un partido, vuelve a pedir `GET /api/tournaments/{id}/standings`.

## Criterios de aceptación de la Parte 2

- Un visitante, un `DT` y un `ADMIN` ven la tabla de una liga; en un torneo `KNOCKOUT`, `GROUPS_KNOCKOUT` o en el Histórico no aparece.
- Una liga recién iniciada muestra todos sus equipos en 0, ordenados por nombre.
- Un `ADMIN` genera el calendario de una liga de 4 equipos: ve 6 partidos en 3 jornadas con fechas separadas por el intervalo, y cada equipo juega una vez por jornada.
- Con 5 equipos ve 10 partidos en 5 jornadas y un descanso por equipo.
- Intentar generar de nuevo muestra el mensaje del servidor y no crea partidos.
- Un choque de fecha con un partido de otro torneo muestra el equipo y la fecha, y no se crea ningún partido.
- Si un partido generado se aplaza, el `ADMIN` lo elimina, crea un partido manual de reemplazo y, al finalizarlo, la tabla lo cuenta igual que los demás.
- La tabla respeta el orden recibido del servidor, incluida la posición.
- Un `DT` y un visitante no ven "Generar calendario".

---

# Parte 3 — Grupos y eliminatoria (change 3) — PENDIENTE

> Esta parte se completa cuando el change 3 esté implementado. No la ejecutes mientras diga "pendiente".

Lo que se sabe hasta ahora: los formatos `GROUPS_KNOCKOUT` (fase de grupos y luego eliminatoria) y `KNOCKOUT` (eliminatoria directa) hoy solo se guardan y se bloquean al iniciar el torneo; la tabla y el calendario de la Parte 2 responden `400` para ellos. El change 3 definirá los grupos, la clasificación, las llaves, el avance de ronda y qué pasa cuando el número de equipos no es potencia de 2.

Por completar al cerrar el change 3: endpoints y formas de respuesta, reglas de clasificación, pantallas de grupos y llaves, cambios sobre las Partes 1 y 2, y criterios de aceptación.
