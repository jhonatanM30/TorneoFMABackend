# Proposal

## Why

Hoy el backend no tiene la noción de torneo: todos los partidos viven en una sola bolsa, `Partido` solo guarda una `fase` (enum) y las estadísticas, como el goleador, se calculan sobre todos los partidos sin distinguir competencia. El administrador necesita crear los torneos que quiera, decidir **antes de iniciar** cómo se juega cada uno (liga, grupos con eliminatoria o eliminatoria directa), inscribir los equipos que participan y poder armar partidos a mano cuando el que correspondía no se pudo jugar (por ejemplo, A vs B se aplaza y se juega A vs C), sin que esa decisión rompa la puntuación.

Este change es el **primero de tres** y construye la base. El formato queda definido y bloqueado desde aquí, pero la lógica propia de cada formato llega en los changes siguientes.

## What Changes

- Nueva entidad **Torneo** con nombre, **formato** (`LEAGUE`, `GROUPS_KNOCKOUT`, `KNOCKOUT`) y estado (`DRAFT` → `IN_PROGRESS` → `FINISHED`). El formato es obligatorio al crear, se puede cambiar solo mientras el torneo está en `DRAFT` y queda bloqueado al iniciarlo.
- **Inscripción de equipos** a un torneo (agregar y quitar), permitida solo en `DRAFT`. Para iniciar se exigen al menos 2 equipos inscritos.
- **Iniciar y finalizar** un torneo. Finalizar exige que todos sus partidos estén finalizados.
- **Partidos manuales en todos los formatos**: el administrador crea un partido de un torneo en curso entre dos equipos inscritos cualquiera. Un partido manual es un partido normal del torneo, sin bandera que lo distinga: **cuenta para la puntuación** igual que cualquier otro.
- Todo partido pertenece a un torneo. La migración crea un torneo **"Histórico"** (formato interno `LEGACY`, en curso) al que se asignan los partidos existentes; los clientes que crean partidos sin indicar torneo siguen funcionando, porque caen en el Histórico.
- Endpoints nuevos bajo `/api/tournaments` (consulta pública, escritura solo `ADMIN`, por la regla ya existente de `/api/**`).
- Documentación y prompt para la IA del front (`docs/frontend-prompts/add-tournaments.md`).

Fuera de alcance (changes siguientes de la misma hoja de ruta):

- **Change 2 – liga**: tabla de posiciones (3-1-0, desempate por diferencia de goles, goles a favor y juego limpio por tarjetas) y generación automática del calendario todos contra todos.
- **Change 3 – grupos y eliminatoria**: sorteo/armado de grupos, clasificación y llaves.
- Filtrar goleador y estadísticas por torneo.

## Capabilities

### New Capabilities
- `tournaments`: crear, consultar, editar y eliminar torneos; formato obligatorio y bloqueado al iniciar; ciclo de vida `DRAFT` → `IN_PROGRESS` → `FINISHED`; torneo "Histórico" protegido.
- `tournament-enrollment`: inscribir y retirar equipos de un torneo mientras está en `DRAFT`, y consultar los equipos inscritos.
- `tournament-matches`: pertenencia de todo partido a un torneo, creación manual de partidos en cualquier formato (con validación de inscripción y estado) y consulta de los partidos de un torneo.

### Modified Capabilities
<!-- Ninguna: la regla "toda escritura bajo /api/** es ADMIN" de access-control ya cubre /api/tournaments/**. -->

## Impact

- **Base de datos**: migración `V9__tournaments.sql` (tablas `tournament` y `tournament_team`; columna `id_torneo` en `partido`, con backfill al torneo Histórico y luego `NOT NULL`).
- **Código nuevo (inglés)**: `Tournament`, `TournamentTeam`, `TournamentFormat`, `TournamentStatus`, repositorios, `TournamentService`, `TournamentController`, DTOs.
- **Código existente modificado (español, se respeta el idioma del archivo)**: `Partido` (campo `torneo`), `PartidoDTO` (`idTorneo`), `PartidoMapper`, `PartidoService#guardar` (resuelve el torneo y valida inscripción/estado) y `PartidoRepository`.
- **API**: `/api/tournaments/**` nuevo; `POST /api/partidos` acepta `idTorneo` opcional (compatible hacia atrás). Sin cambios incompatibles.
- **Seguridad**: sin cambios en `SecurityFilterChain`; se agrega un test que fija las reglas para la ruta nueva.
- **Documentación**: `docs/data-model.md`, `docs/api-spec.yml`, README y `docs/frontend-prompts/add-tournaments.md`.
