# League Schedule Specification

## Purpose

Genera el calendario todos contra todos, a una vuelta, de un torneo de liga en curso, sin quitar al administrador la posibilidad de editar partidos o armar otros a mano.

## ADDED Requirements

### Requirement: Generación del calendario de una vuelta
El sistema SHALL permitir al administrador generar los partidos de un torneo de formato `LEAGUE` indicando una fecha de inicio, una hora y, opcionalmente, un intervalo en días entre jornadas (7 por defecto). La generación SHALL crear un partido entre cada pareja de equipos inscritos exactamente una vez, repartidos en jornadas de modo que cada equipo juegue como máximo una vez por jornada. Cada jornada SHALL tener su propia fecha: la primera la fecha de inicio y cada siguiente el intervalo después de la anterior. Los partidos SHALL crearse programados, con la hora indicada, y la localía SHALL repartirse de modo que, para cada equipo, la diferencia entre sus partidos de local y de visitante no supere 1. El sistema SHALL responder con estado 201 y los partidos creados.

#### Scenario: Cuatro equipos
- **WHEN** se genera el calendario de un torneo de liga en curso con 4 equipos, fecha de inicio 2027-03-06, hora 15:00 y sin intervalo
- **THEN** el sistema crea 6 partidos en 3 jornadas con fechas 2027-03-06, 2027-03-13 y 2027-03-20, cada pareja de equipos juega una sola vez, cada equipo juega una vez por jornada y todos quedan programados a las 15:00

#### Scenario: Número impar de equipos
- **WHEN** se genera el calendario con 5 equipos
- **THEN** el sistema crea 10 partidos en 5 jornadas, cada pareja juega una sola vez y en cada jornada un equipo descansa, de modo que cada equipo descansa exactamente una jornada

#### Scenario: Localía repartida
- **WHEN** se genera el calendario con cualquier cantidad de equipos entre 2 y 12
- **THEN** ningún equipo tiene más de un partido de diferencia entre los que juega de local y los que juega de visitante

#### Scenario: Intervalo indicado
- **WHEN** se genera el calendario indicando un intervalo de 3 días
- **THEN** las fechas de las jornadas están separadas 3 días entre sí

#### Scenario: Los partidos generados son partidos normales
- **WHEN** se consultan los partidos del torneo después de generar el calendario
- **THEN** los partidos generados aparecen con la misma estructura que cualquier otro partido, en estado programado, y pertenecen al torneo

### Requirement: Precondiciones de la generación
El sistema SHALL permitir generar el calendario solo en un torneo de formato `LEAGUE` en estado `IN_PROGRESS`, con al menos 2 equipos inscritos y sin partidos creados. La fecha de inicio y la hora SHALL ser obligatorias y el intervalo, si se indica, SHALL estar entre 1 y 30 días. No SHALL generarse el calendario dos veces ni sobre un torneo que ya tiene partidos manuales.

#### Scenario: Torneo que no es de liga
- **WHEN** se intenta generar el calendario de un torneo `GROUPS_KNOCKOUT`, `KNOCKOUT` o del torneo Histórico
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que solo los torneos de liga tienen calendario automático, sin crear partidos

#### Scenario: Torneo que no está en curso
- **WHEN** se intenta generar el calendario de un torneo de liga en `DRAFT` o `FINISHED`
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que el torneo no está en curso, sin crear partidos

#### Scenario: Torneo que ya tiene partidos
- **WHEN** se intenta generar el calendario de un torneo de liga que ya tiene algún partido, sea generado o manual
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear partidos

#### Scenario: Datos inválidos
- **WHEN** se genera el calendario sin fecha de inicio o sin hora, o con un intervalo menor a 1 o mayor a 30
- **THEN** el sistema responde con estado 400 y un mapa campo → mensaje en español

#### Scenario: Torneo inexistente
- **WHEN** se intenta generar el calendario de un torneo que no existe
- **THEN** el sistema responde con estado 404 y un mensaje en español, con el mismo formato que los demás recursos de partidos y equipos (`{"indicadorRespuesta":"Sin Registros","mensaje":"..."}`)

### Requirement: Generación atómica y sin choques de fecha
El sistema SHALL crear todos los partidos del calendario o ninguno. Si un equipo ya tiene otro partido (de cualquier torneo) en la fecha de una jornada, el sistema SHALL responder con estado 400 y un mensaje en español que indique el equipo y la fecha, sin crear ningún partido.

#### Scenario: Equipo ocupado en una fecha
- **WHEN** un equipo del torneo ya tiene un partido en otro torneo el 2027-03-13 y se genera un calendario cuya segunda jornada cae en esa fecha
- **THEN** el sistema responde con estado 400 con un mensaje que nombra al equipo y la fecha, y el torneo sigue sin partidos

### Requirement: Convivencia con la edición y los partidos manuales
El sistema SHALL tratar los partidos generados como cualquier otro partido: SHALL poder editarse (por ejemplo, cambiar fecha u hora) y eliminarse mientras no estén finalizados, y el administrador SHALL poder crear partidos manuales adicionales en el torneo. Todos SHALL contar para la tabla de posiciones del mismo modo.

#### Scenario: Reemplazo de un partido aplazado
- **WHEN** un partido generado A vs B no se puede jugar, el administrador lo elimina y crea manualmente A vs C
- **THEN** A vs C es un partido del torneo y su resultado cuenta en la tabla como el de cualquier otro

#### Scenario: Cambio de fecha de un partido generado
- **WHEN** el administrador edita la fecha de un partido generado
- **THEN** el partido conserva su torneo y sus equipos, y solo cambia la fecha

### Requirement: Permisos de la generación
El sistema SHALL exigir una sesión de administrador para generar el calendario.

#### Scenario: Sin sesión
- **WHEN** un visitante sin sesión intenta generar el calendario
- **THEN** el sistema responde con estado 401

#### Scenario: Director Técnico
- **WHEN** un usuario con rol `DT` intenta generar el calendario
- **THEN** el sistema responde con estado 403
