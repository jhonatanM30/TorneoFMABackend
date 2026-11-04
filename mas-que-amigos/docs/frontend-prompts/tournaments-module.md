# Prompt para el front: módulo de torneos (3 changes del backend en una sola entrega)

> Para la IA que trabaja el repositorio `MasQueAmigos-Torneo` (HTML, CSS y JavaScript planos, sin framework ni build). No hagas commit: deja los cambios en el árbol de trabajo para que el usuario los revise.

> **Alcance de la entrega.** Este prompt reúne los tres changes del backend del módulo de torneos y se implementa **en una sola entrega**, en este orden: Parte 1 (torneos, inscripción y partidos manuales), Parte 2 (liga: tabla y calendario) y Parte 3 (eliminatoria directa, grupos y penales). El backend ya tiene los tres changes implementados.
>
> | Parte | Formato que cubre | Qué agrega |
> |---|---|---|
> | 1 | Todos | Torneos, formato, inscripción de equipos y partidos manuales |
> | 2 | `LEAGUE` | Tabla de posiciones y calendario automático |
> | 3 | `KNOCKOUT` y `GROUPS_KNOCKOUT` | Llaves, repechaje, penales, grupos y eliminatoria de los clasificados |
>
> Los endpoints de las Partes 2 y 3 responden `400` si el torneo no es del formato que corresponde: decide qué mostrar según el `format` del torneo.

# Parte 1 — Torneos, inscripción y partidos manuales (change 1)

## Contexto

El backend (`mas-que-amigos`, puerto 57075 por defecto) incorporó **torneos**: el administrador crea un torneo, define cómo se juega (liga, grupos con eliminatoria o eliminatoria directa), inscribe equipos, lo inicia y arma partidos dentro de él. Todo partido pertenece ahora a un torneo.

**No hay cambios incompatibles.** Todo lo que el front hace hoy sigue funcionando: un partido creado sin indicar torneo queda en el torneo protegido "Histórico". Lo nuevo es opcional de adoptar, pero es el objetivo de esta tarea.

Alcance de la Parte 1: el torneo guarda su formato y lo bloquea al iniciar, y todos los partidos se crean a mano con `POST /api/partidos` y `idTorneo`. La tabla de posiciones y el calendario automático de las ligas están en la Parte 2; las llaves, los grupos y los penales, en la Parte 3.

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

> Solo aplica a torneos con `format = "LEAGUE"`. Para `GROUPS_KNOCKOUT` y `KNOCKOUT` el servidor responde `400` en estos endpoints (esos formatos usan la Parte 3); no muestres la tabla ni el botón de calendario en esos torneos ni en el "Histórico".

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

# Parte 3 — Eliminatoria directa, grupos y penales (change 3)

> Aplica a los torneos `KNOCKOUT` (eliminatoria directa) y `GROUPS_KNOCKOUT` (fase de grupos y eliminatoria). Para `LEAGUE` y para el "Histórico" no muestres nada de esta parte.

## Qué construir

1. **Llaves de la eliminatoria**: en el detalle de un torneo `KNOCKOUT` o `GROUPS_KNOCKOUT`, un cuadro con las rondas en orden (octavos de final, cuartos de final, semifinales, final), cada cruce con sus equipos, el marcador, los penales y el ganador, el partido por el tercer puesto aparte y el campeón. Visible para todos (consulta pública). Muestra los cruces sin equipos conocidos como pendientes ("Por definir") y no los trates como error.
2. **Generar las llaves (solo `ADMIN`)**: botón "Generar llaves" con fecha de inicio, hora, intervalo entre rondas (opcional), casilla "Partido por el tercer puesto" (marcada por defecto) y, solo en `KNOCKOUT`, la opción de elegir los cruces a mano (si no, se sortean).
3. **Repechaje (solo `KNOCKOUT`)**: si los equipos inscritos no son una potencia de 2 (2, 4, 8, 16...), el servidor lo rechaza con un mensaje que pide armar un repechaje. Guía al administrador: crear un partido entre dos equipos con la fase `REPECHAJE` (con el formulario de partidos de la Parte 1, eligiendo esa fase), jugarlo, y volver a generar las llaves. El perdedor del repechaje queda eliminado y no entra al cuadro.
4. **Penales (solo `ADMIN`)**: en un partido en curso que exige ganador, un formulario para registrar los penales de cada equipo. Al finalizar un partido empatado de este tipo, si el servidor responde que no puede terminar empatado, abre ese formulario.
5. **Formar grupos (solo `ADMIN`, solo `GROUPS_KNOCKOUT`)**: botón "Formar grupos" con la cantidad de grupos, cuántos clasifican por grupo, fecha de inicio, hora, intervalo (opcional) y, opcionalmente, la composición de cada grupo elegida a mano (si no, se sortea).
6. **Tabla de cada grupo**: en `GROUPS_KNOCKOUT`, una tabla por grupo con las mismas columnas de la tabla de la liga (Parte 2). Marca visualmente a los que clasifican (los primeros `qualifiersCount` de cada tabla).
7. **Eliminatoria tras los grupos**: cuando todos los partidos de grupos terminaron, el mismo botón "Generar llaves" arma el cuadro con los clasificados; aquí **no** se eligen cruces a mano.

## Cómo funciona (lo que el servidor ya hace)

- **Partidos que exigen ganador**: los partidos de las llaves y los de fase `REPECHAJE` de un torneo `KNOCKOUT`. No se pueden finalizar empatados: con el marcador igual hay que registrar antes penales **distintos**. El ganador es el de más goles y, con el marcador igual, el de más penales. Los penales no cambian el marcador.
- **Avance automático**: al finalizar un partido de las llaves (`PUT /api/partidos/{id}/finalizar`, el mismo de siempre), el ganador pasa a la ronda siguiente y el partido siguiente se crea solo cuando se conocen sus dos equipos. Refresca las llaves después de finalizar un partido.
- **Tercer puesto**: los perdedores de las semifinales juegan entre sí en la fecha de la final (solo con al menos 4 equipos y si la casilla está marcada).
- **Fechas**: la ronda `r` cae en `startDate + (r − 1) × intervalDays` (7 por defecto). Los cruces muestran la fecha y la hora del partido cuando ya existe y las de su ronda mientras no. Un partido de ronda posterior puede chocar con otro de otro torneo: se resuelve editando su fecha.
- **Partidos de las llaves protegidos**: no se eliminan ni cambian de equipos; su fecha y hora sí se editan. No ofrezcas "Eliminar" ni "Cambiar equipos" en esos partidos.
- **Finalizar el torneo**: exige que la final esté finalizada (campeón definido). Si no, el servidor responde con el motivo.
- **Grupos**: la cantidad de grupos es una potencia de 2 (2, 4, 8...), cada grupo tiene al menos 2 equipos y los clasificados por grupo son 1 o 2 y menos que el grupo más pequeño. Los equipos se reparten en grupos A, B, C... con tamaños que difieren como máximo en 1. Se genera solo el calendario de cada grupo a una vuelta (todos contra todos): la jornada `k` de todos los grupos cae el mismo día.
- **Tabla de grupo**: cuenta solo los partidos de fase de grupos finalizados entre equipos del grupo (los armados a mano también), 3-1-0 y el mismo desempate de la liga. Los partidos de las llaves no cuentan.
- **Cruces tras los grupos**: con 2 clasificados por grupo, primero de un grupo contra segundo de otro (grupos emparejados A con B, C con D...), y los equipos de un mismo grupo quedan en mitades opuestas del cuadro; con 1 clasificado, se enfrentan los primeros de los grupos emparejados.

## Contrato del API

Base `http://localhost:57075` (configurable). Las escrituras llevan `Authorization: Bearer <token>` de un `ADMIN`.

### `GET /api/tournaments/{id}/bracket` (público)

```json
{
  "rounds": [
    {
      "round": 1,
      "name": "Cuartos de final",
      "ties": [
        {
          "id": 1001, "position": 0,
          "homeTeam": { "id": 8, "name": "Test Team 3" },
          "awayTeam": { "id": 10, "name": "Test Team 5" },
          "matchId": 8, "matchStatus": "FINALIZADO",
          "homeGoals": 1, "awayGoals": 1, "homePenalties": 5, "awayPenalties": 4,
          "winnerTeamId": 8, "date": "2027-05-01", "time": "18:00:00"
        }
      ]
    },
    { "round": 2, "name": "Semifinales", "ties": [ { "id": 1005, "position": 0, "homeTeam": { "id": 8, "name": "Test Team 3" }, "awayTeam": null, "matchId": null, "matchStatus": null, "date": "2027-05-08", "time": "18:00:00" } ] },
    { "round": 3, "name": "Final", "ties": [] }
  ],
  "thirdPlace": null,
  "champion": null
}
```

- `rounds` está vacía mientras no se han generado las llaves.
- En cada cruce, `homeTeam`, `awayTeam`, `matchId`, `matchStatus` y los goles y penales son `null` hasta que existen; `winnerTeamId` es `null` hasta que el partido se finaliza.
- `thirdPlace` tiene la misma forma que un cruce (o `null`); `champion` es `{id, name}` o `null` hasta que se finaliza la final.
- El nombre de la ronda viene en español; muéstralo tal cual. El orden de los cruces de una ronda es el del cuadro: los cruces `2k` y `2k + 1` alimentan al cruce `k` de la ronda siguiente.

### `POST /api/tournaments/{id}/bracket` (solo `ADMIN`)

```json
{
  "startDate": "2027-05-01",
  "time": "18:00:00",
  "intervalDays": 7,
  "thirdPlaceMatch": true,
  "pairings": [ { "homeTeamId": 8, "awayTeamId": 10 }, { "homeTeamId": 6, "awayTeamId": 7 } ]
}
```

- `startDate` y `time` obligatorios; `intervalDays` opcional (1 a 30, por defecto 7); `thirdPlaceMatch` opcional (por defecto `true`); `pairings` opcional y **solo en `KNOCKOUT`**: los cruces de la primera ronda en el orden del cuadro, que deben incluir a todos los equipos que participan exactamente una vez. Sin `pairings`, el servidor sortea.
- `startDate` debe ser posterior a la fecha del último partido del torneo (por ejemplo, tras el repechaje o la fase de grupos).
- Respuesta `201`: el mismo cuerpo del `GET`. Solo se crean los partidos de la primera ronda.
- Una sola vez por torneo.

### `POST /api/tournaments/{id}/groups` (solo `ADMIN`)

```json
{
  "groupCount": 2,
  "qualifiersPerGroup": 2,
  "startDate": "2027-02-06",
  "time": "15:00:00",
  "intervalDays": 7,
  "groups": [ { "teamIds": [6, 8, 11, 13] }, { "teamIds": [7, 9, 10, 12] } ]
}
```

- `groupCount`, `qualifiersPerGroup`, `startDate` y `time` obligatorios; `intervalDays` opcional; `groups` opcional (composición manual: todos los inscritos una sola vez, en el orden A, B, C...). Sin `groups`, el servidor sortea.
- Solo en un torneo `GROUPS_KNOCKOUT` en curso, sin grupos ni partidos. Es todo o nada: si un equipo ya juega en alguna fecha, no se crea nada y el mensaje nombra al equipo y la fecha.
- Respuesta `201`: arreglo de grupos (misma forma que el `GET`).

### `GET /api/tournaments/{id}/groups` (público)

```json
[
  {
    "id": 10, "name": "A", "qualifiersCount": 2,
    "teams": [ { "id": 6, "name": "Test Team 1" } ],
    "standings": [
      { "position": 1, "teamId": 6, "teamName": "Test Team 1", "played": 3, "won": 3, "drawn": 0, "lost": 0,
        "goalsFor": 6, "goalsAgainst": 0, "goalDifference": 6, "points": 9, "fairPlayPoints": 0 }
    ]
  }
]
```

- `standings` tiene la misma forma que la tabla de la liga (Parte 2) y ya viene ordenada por posición; no reordenes.
- Lista vacía si el torneo no tiene grupos.

### `PUT /api/partidos/{id}/penales` (solo `ADMIN`)

```json
{ "penalesLocal": 5, "penalesVisitante": 4 }
```

- Solo en un partido **en curso** que exige ganador (partidos de las llaves y de repechaje de un `KNOCKOUT`). Enteros `>= 0` y distintos entre sí.
- Respuesta `200`: el partido (`PartidoDTO`), que ahora incluye `penalesLocal` y `penalesVisitante` cuando existen. Esos campos también aparecen en `GET /api/partidos` y en las demás respuestas de partido.

### Partidos (`POST /api/partidos`)

Sigue igual que en la Parte 1. Para el repechaje, envía `"fase": "REPECHAJE"` y el `idTorneo` del torneo `KNOCKOUT` en curso, con dos equipos inscritos. Para reemplazar un partido de grupo aplazado, crea uno manual con `"fase": "FASE_DE_GRUPOS"` entre dos equipos del mismo grupo: cuenta en la tabla del grupo.

## Formato de los errores

| Código | Cuándo | Cuerpo |
|---|---|---|
| `400` | Regla de negocio (equipos que no son potencia de 2, repechaje sin finalizar, llaves ya generadas, cruces inválidos, grupos sin terminar, fecha de inicio no posterior, empate sin penales, penales iguales, partido de las llaves protegido, falta el campeón...) | `{"indicadorRespuesta":"Error","mensaje":"..."}` |
| `400` | Validación de campos | mapa campo → mensaje (por ejemplo `{"penalesLocal":"Los penales del equipo local no pueden ser negativos."}`; los nombres de campo de las respuestas de validación pueden venir en inglés, como `homePenalties`: muestra solo el mensaje) |
| `401` / `403` | Sin sesión o sesión vencida / rol que no alcanza | `{"indicadorRespuesta":"...","mensaje":"..."}` |
| `404` | El torneo o el partido no existe | `{"indicadorRespuesta":"Sin Registros","mensaje":"..."}` |

Muestra siempre el `mensaje` del servidor al usuario; ya viene en español.

## Flujos que debe poder hacer el administrador

**Eliminatoria directa**
1. Crear el torneo `KNOCKOUT`, inscribir los equipos e iniciarlo (Parte 1).
2. Si los equipos no son potencia de 2, armar y jugar uno o más partidos de `REPECHAJE` hasta que queden 2, 4, 8, 16... equipos.
3. "Generar llaves" (sorteo o cruces manuales).
4. Jugar los partidos de cada ronda como siempre (alineaciones, iniciar, estadísticas), registrar penales si el partido empata y finalizarlo; las llaves avanzan solas.
5. Al finalizar la final, aparece el campeón; finalizar el torneo.

**Grupos y eliminatoria**
1. Crear el torneo `GROUPS_KNOCKOUT`, inscribir los equipos e iniciarlo.
2. "Formar grupos" (sorteo o composición manual).
3. Jugar los partidos de grupo; si uno se aplaza, eliminarlo y crear uno manual de reemplazo entre equipos del mismo grupo.
4. Cuando todos terminaron, "Generar llaves" con los clasificados y seguir como en la eliminatoria directa.

## Criterios de aceptación de la Parte 3

- Un visitante, un `DT` y un `ADMIN` ven las llaves y las tablas de los grupos; solo el `ADMIN` ve "Generar llaves", "Formar grupos" y el formulario de penales.
- Con 8 equipos en `KNOCKOUT`, el `ADMIN` genera las llaves con sorteo y ve 3 rondas (cuartos de final, semifinales y final) con 4, 2 y 1 cruces, y el partido por el tercer puesto aparte.
- Con 5 equipos, intentar generar muestra el mensaje del servidor que pide un repechaje; tras jugarlo, se generan las llaves con 4 equipos.
- Un partido de las llaves empatado no se finaliza sin penales: el formulario de penales se abre y, con penales distintos, el partido se finaliza y el ganador aparece en la ronda siguiente.
- Al completarse dos cruces consecutivos, el cruce siguiente muestra a sus dos equipos y su partido.
- Los perdedores de las semifinales aparecen en el cruce por el tercer puesto; sin la casilla marcada, no existe.
- Al finalizar la final se muestra el campeón y el torneo se puede finalizar; antes, el servidor lo rechaza y se muestra su mensaje.
- Los partidos de las llaves no ofrecen "Eliminar" ni cambiar equipos; la fecha se edita y el cruce muestra la nueva fecha.
- Con 8 equipos en `GROUPS_KNOCKOUT`, "Formar grupos" con 2 grupos y 2 clasificados crea los grupos A y B de 4 equipos con su calendario; formarlos de nuevo muestra el mensaje del servidor.
- Las tablas de los grupos respetan el orden recibido, marcan a los clasificados y un partido manual de reemplazo cuenta.
- Con los partidos de grupo sin terminar, "Generar llaves" muestra cuántos faltan; con todos terminados, los cruces son 1.º A vs 2.º B y 1.º B vs 2.º A (con 2 grupos y 2 clasificados).
- En un torneo con grupos no se pueden elegir cruces a mano.
- El front no ofrece repechaje ni cruces manuales en `GROUPS_KNOCKOUT`, ni muestra la tabla de liga o las llaves en un torneo `LEAGUE` o en el "Histórico".
