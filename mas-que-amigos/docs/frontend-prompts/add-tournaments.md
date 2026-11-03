# Prompt para el front: torneos (`add-tournaments`)

> Para la IA que trabaja el repositorio `MasQueAmigos-Torneo` (HTML, CSS y JavaScript planos, sin framework ni build). No hagas commit: deja los cambios en el árbol de trabajo para que el usuario los revise.

## Contexto

El backend (`mas-que-amigos`, puerto 57075 por defecto) incorporó **torneos**: el administrador crea un torneo, define cómo se juega (liga, grupos con eliminatoria o eliminatoria directa), inscribe equipos, lo inicia y arma partidos dentro de él. Todo partido pertenece ahora a un torneo.

**No hay cambios incompatibles.** Todo lo que el front hace hoy sigue funcionando: un partido creado sin indicar torneo queda en el torneo protegido "Histórico". Lo nuevo es opcional de adoptar, pero es el objetivo de esta tarea.

Alcance de este primer paso (no lo anticipes en la interfaz): el torneo ya guarda su formato y lo bloquea al iniciar, pero **aún no hay tabla de posiciones, calendario automático, grupos ni llaves**. Esas funciones llegan en dos cambios siguientes del backend. Por ahora, todos los partidos de un torneo los crea el administrador a mano.

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
