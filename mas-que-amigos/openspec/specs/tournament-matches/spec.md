# Tournament Matches Specification

## Purpose

Hace que todo partido pertenezca a un torneo y permite al administrador armar partidos manualmente en cualquier formato (por ejemplo, cuando el partido que correspondía a la fecha no se pudo jugar), de modo que cuenten para la puntuación del torneo como cualquier otro partido.

## Requirements

### Requirement: Todo partido pertenece a un torneo
El sistema SHALL asociar cada partido a exactamente un torneo. Al crear un partido SHALL aceptarse el torneo de forma opcional; cuando no se indica, el partido SHALL asignarse al torneo Histórico. Los partidos existentes antes de los torneos SHALL pertenecer al torneo Histórico y conservar sin cambios sus datos, alineaciones y estadísticas.

#### Scenario: Partido creado sin torneo
- **WHEN** se crea un partido sin indicar torneo
- **THEN** el sistema responde con estado 201 y el partido pertenece al torneo Histórico, igual que antes de existir los torneos

#### Scenario: Partidos previos a la migración
- **WHEN** se consultan los partidos existentes después de aplicar la migración
- **THEN** cada uno conserva su fecha, hora, equipos, goles, fase y estado, y pertenece al torneo Histórico

#### Scenario: Torneo indicado inexistente
- **WHEN** se crea un partido indicando un torneo que no existe
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear el partido

### Requirement: Armado manual de partidos
El sistema SHALL permitir al administrador crear un partido de un torneo `IN_PROGRESS` entre dos equipos inscritos en él, en cualquier formato (`LEAGUE`, `GROUPS_KNOCKOUT` o `KNOCKOUT`), eligiendo libremente los equipos, la fecha y la hora. El sistema NO SHALL distinguir un partido manual de cualquier otro partido del torneo: SHALL tener el mismo ciclo de vida (programado, en curso, finalizado), las mismas alineaciones y estadísticas, y SHALL contar para la puntuación del torneo de la misma manera.

#### Scenario: Partido de reemplazo cuando uno se aplaza
- **WHEN** en un torneo en curso el partido A vs B no se puede jugar y el administrador crea manualmente A vs C, con A y C inscritos
- **THEN** el sistema responde con estado 201 y el partido A vs C pertenece al torneo, en estado programado

#### Scenario: Un partido manual cuenta como cualquier otro
- **WHEN** un partido creado manualmente en un torneo se juega y se finaliza con sus estadísticas
- **THEN** sus goles y su resultado quedan registrados igual que los de cualquier partido del torneo y están disponibles para calcular la puntuación

#### Scenario: Formato no condiciona el armado manual
- **WHEN** se crea un partido manual en un torneo `KNOCKOUT`, en uno `LEAGUE` y en uno `GROUPS_KNOCKOUT`, todos en curso
- **THEN** el sistema acepta los tres, siempre que los equipos estén inscritos

#### Scenario: Equipo no inscrito en el torneo
- **WHEN** se crea un partido de un torneo con un equipo que no está inscrito en él
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica qué equipo no está inscrito, sin crear el partido

#### Scenario: Torneo que no está en curso
- **WHEN** se crea un partido de un torneo `DRAFT` o `FINISHED`
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que el torneo no está en curso, sin crear el partido

#### Scenario: Reglas generales de partido se mantienen
- **WHEN** se crea un partido de un torneo con el mismo equipo como local y visitante, o con un equipo que ya tiene otro partido ese día
- **THEN** el sistema responde con estado 400 y el mismo mensaje en español que ya se usaba antes de los torneos

#### Scenario: Torneo Histórico sin exigencia de inscripción
- **WHEN** se crea un partido en el torneo Histórico (o sin indicar torneo) con equipos existentes
- **THEN** el sistema lo acepta aunque los equipos no estén inscritos, porque el Histórico no usa inscripción

### Requirement: El torneo de un partido no cambia
El sistema SHALL conservar el torneo de un partido durante toda su vida. Al editar un partido, el torneo SHALL permanecer igual, y los equipos nuevos de un partido de un torneo con inscripción SHALL estar inscritos en él.

#### Scenario: Edición que reemplaza un equipo
- **WHEN** se edita un partido programado de un torneo cambiando un equipo por otro inscrito en el mismo torneo
- **THEN** el sistema acepta la edición y el partido sigue en el mismo torneo

#### Scenario: Edición con un equipo no inscrito
- **WHEN** se edita un partido de un torneo con inscripción cambiando un equipo por uno no inscrito
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin cambiar el partido

#### Scenario: Intento de mover el partido a otro torneo
- **WHEN** una edición de partido envía un torneo distinto al del partido
- **THEN** el sistema ignora ese dato y el partido sigue en su torneo original

### Requirement: Consulta de partidos de un torneo
El sistema SHALL permitir consultar, sin sesión, los partidos de un torneo ordenados por fecha y hora.

#### Scenario: Torneo con partidos
- **WHEN** se consultan los partidos de un torneo con 3 partidos
- **THEN** el sistema responde con estado 200 y los 3 partidos, ordenados por fecha y hora, con la misma estructura que `GET /api/partidos`

#### Scenario: Torneo sin partidos
- **WHEN** se consultan los partidos de un torneo sin partidos
- **THEN** el sistema responde con estado 200 y una lista vacía

#### Scenario: Torneo inexistente
- **WHEN** se consultan los partidos de un torneo que no existe
- **THEN** el sistema responde con estado 404 y un mensaje en español, con el mismo formato que los demás recursos de partidos y equipos (`{"indicadorRespuesta":"Sin Registros","mensaje":"..."}`)
