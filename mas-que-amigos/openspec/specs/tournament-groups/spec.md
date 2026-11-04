# Tournament Groups Specification

## Purpose

Da comportamiento propio al formato de fase de grupos y eliminatoria: forma los grupos, genera el calendario de cada uno, calcula la tabla de cada grupo con las reglas de la liga y arma la eliminatoria con los clasificados.

## Requirements

### Requirement: Formación de grupos y calendario
El sistema SHALL permitir al administrador formar los grupos de un torneo `GROUPS_KNOCKOUT` en curso con `POST /api/tournaments/{id}/groups`, indicando la cantidad de grupos, cuántos equipos clasifican por grupo (1 o 2), una fecha de inicio, una hora y, opcionalmente, un intervalo en días entre jornadas (7 por defecto, de 1 a 30). La cantidad de grupos SHALL ser una potencia de 2 de al menos 2, cada grupo SHALL tener al menos 2 equipos, y los clasificados por grupo SHALL ser menos que los equipos del grupo más pequeño. Los equipos SHALL repartirse en grupos nombrados A, B, C..., con tamaños que difieren como máximo en 1. Al formar los grupos, el sistema SHALL generar el calendario de cada grupo a una vuelta, todos contra todos, con las mismas reglas del calendario de la liga: una jornada por fecha (la jornada `k` de todos los grupos cae el mismo día), partidos programados con la hora indicada y fase `FASE_DE_GRUPOS`, y descanso cuando un grupo tiene cantidad impar de equipos. La formación SHALL ser atómica y SHALL responder con estado 201 y los grupos.

#### Scenario: Ocho equipos en dos grupos
- **WHEN** se forman 2 grupos con 8 equipos inscritos, 2 clasificados por grupo, fecha de inicio 2027-03-06 y hora 15:00
- **THEN** el sistema crea los grupos A y B con 4 equipos cada uno, crea 6 partidos por grupo en 3 jornadas (2027-03-06, 2027-03-13 y 2027-03-20) y responde con estado 201

#### Scenario: Grupos desiguales
- **WHEN** se forman 2 grupos con 9 equipos
- **THEN** un grupo queda con 5 equipos y el otro con 4

#### Scenario: Sorteo aleatorio
- **WHEN** se forman los grupos sin indicar su composición
- **THEN** cada equipo inscrito queda en exactamente un grupo, repartidos al azar

#### Scenario: Grupos manuales
- **WHEN** el administrador indica la composición de cada grupo
- **THEN** los grupos quedan exactamente con esos equipos, siempre que incluyan a todos los inscritos una sola vez y respeten los tamaños permitidos

#### Scenario: Composición manual inválida
- **WHEN** la composición indicada omite equipos, repite un equipo, incluye un equipo no inscrito o no coincide con la cantidad de grupos
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear nada

#### Scenario: Cantidad de grupos o de clasificados inválida
- **WHEN** la cantidad de grupos no es una potencia de 2, hay un grupo con menos de 2 equipos, los clasificados no son 1 o 2, o los clasificados no son menos que los equipos del grupo más pequeño
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica la regla incumplida, sin crear nada

#### Scenario: Equipo ocupado en una fecha
- **WHEN** un equipo ya tiene otro partido (de cualquier torneo) en la fecha de una jornada
- **THEN** el sistema responde con estado 400 con un mensaje que nombra al equipo y la fecha, sin crear grupos ni partidos

#### Scenario: Precondiciones del torneo
- **WHEN** se intentan formar grupos en un torneo que no es `GROUPS_KNOCKOUT`, que no está en curso, que ya tiene grupos o que ya tiene partidos
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear nada

#### Scenario: Datos inválidos
- **WHEN** se forman los grupos sin fecha de inicio, sin hora, sin cantidad de grupos o sin clasificados, o con un intervalo fuera de 1 a 30
- **THEN** el sistema responde con estado 400 y un mapa campo → mensaje en español

#### Scenario: Torneo inexistente
- **WHEN** se intentan formar los grupos de un torneo que no existe
- **THEN** el sistema responde con estado 404 y un mensaje en español, con el mismo formato que los demás recursos de partidos y equipos (`{"indicadorRespuesta":"Sin Registros","mensaje":"..."}`)

### Requirement: Tabla de cada grupo
El sistema SHALL ofrecer, sin sesión, los grupos de un torneo con `GET /api/tournaments/{id}/groups`: cada grupo con su nombre, sus equipos y su tabla de posiciones, con las mismas columnas, puntuación (3-1-0), criterios de desempate y posiciones consecutivas de la tabla de liga. La tabla de un grupo SHALL contar solo los partidos de fase `FASE_DE_GRUPOS` finalizados entre equipos de ese grupo, incluidos los armados a mano, y los puntos de juego limpio SHALL contar solo las tarjetas de esos partidos. La respuesta SHALL indicar cuántos equipos clasifican por grupo.

#### Scenario: Tabla de un grupo
- **WHEN** se finalizan partidos del grupo A y se consultan los grupos
- **THEN** el grupo A muestra su tabla con puntos, diferencia de goles y posiciones calculadas solo con sus partidos de grupo

#### Scenario: Los partidos de las llaves no cuentan
- **WHEN** se finalizan partidos de eliminatoria entre equipos del mismo grupo
- **THEN** no suman a la tabla del grupo

#### Scenario: Partido manual de reemplazo en un grupo
- **WHEN** un partido de grupo se aplaza y el administrador arma a mano otro entre dos equipos del mismo grupo con fase `FASE_DE_GRUPOS` y lo finaliza
- **THEN** cuenta en la tabla del grupo igual que los demás

#### Scenario: Torneo sin grupos
- **WHEN** se consultan los grupos de un torneo que aún no los tiene o que no es `GROUPS_KNOCKOUT`
- **THEN** el sistema responde con estado 200 y una lista de grupos vacía

#### Scenario: Torneo inexistente
- **WHEN** se consultan los grupos de un torneo que no existe
- **THEN** el sistema responde con estado 404 y un mensaje en español, con el mismo formato que los demás recursos de partidos y equipos (`{"indicadorRespuesta":"Sin Registros","mensaje":"..."}`)

### Requirement: Eliminatoria con los clasificados de los grupos
El sistema SHALL permitir generar la eliminatoria de un torneo `GROUPS_KNOCKOUT` con `POST /api/tournaments/{id}/bracket`, con los mismos datos de fecha, hora, intervalo y tercer puesto que en la eliminatoria directa, cuando existen los grupos y todos los partidos de fase de grupos están finalizados. Los clasificados SHALL ser los primeros equipos de la tabla de cada grupo, según los clasificados por grupo definidos al formar los grupos. Con 2 clasificados por grupo, los cruces de la primera ronda SHALL enfrentar al primero de un grupo contra el segundo de otro grupo, emparejando los grupos de dos en dos (A con B, C con D...) de modo que los equipos de un mismo grupo queden en mitades opuestas del cuadro; con 1 clasificado por grupo, SHALL enfrentar a los primeros de los grupos emparejados. Estas llaves SHALL avanzar, tener tercer puesto, consultarse y protegerse igual que las de la eliminatoria directa. La fecha de inicio SHALL ser posterior a la del último partido del torneo. Los cruces de esta eliminatoria NO SHALL poder indicarse a mano.

#### Scenario: Cuatro clasificados de dos grupos
- **WHEN** se generan las llaves con los grupos A y B terminados, 2 clasificados por grupo, y quedan 1.º A, 2.º A, 1.º B y 2.º B
- **THEN** la primera ronda (semifinales) es 1.º A vs 2.º B y 1.º B vs 2.º A

#### Scenario: Ocho clasificados de cuatro grupos
- **WHEN** se generan las llaves con 4 grupos A, B, C y D y 2 clasificados por grupo
- **THEN** los cruces de la primera ronda son 1.º A vs 2.º B, 1.º C vs 2.º D, 1.º B vs 2.º A y 1.º D vs 2.º C, en ese orden, de modo que 1.º A y 2.º A solo pueden verse en la final

#### Scenario: Un clasificado por grupo
- **WHEN** se generan las llaves con 4 grupos y 1 clasificado por grupo
- **THEN** los cruces de la primera ronda son 1.º A vs 1.º B y 1.º C vs 1.º D

#### Scenario: Grupos sin terminar
- **WHEN** se intentan generar las llaves con algún partido de fase de grupos programado o en curso
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica cuántos partidos faltan, sin crear nada

#### Scenario: Sin grupos o llaves ya generadas
- **WHEN** se intentan generar las llaves de un torneo `GROUPS_KNOCKOUT` sin grupos, o que ya las tiene
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear nada

#### Scenario: Cruces manuales no permitidos
- **WHEN** se envía una lista de cruces al generar la eliminatoria de un torneo `GROUPS_KNOCKOUT`
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que los cruces los define la clasificación de los grupos

### Requirement: Permisos de la fase de grupos
El sistema SHALL permitir consultar los grupos sin sesión y SHALL exigir una sesión de administrador para formarlos.

#### Scenario: Sin sesión
- **WHEN** un visitante sin sesión intenta formar los grupos
- **THEN** el sistema responde con estado 401

#### Scenario: Director Técnico
- **WHEN** un usuario con rol `DT` intenta formar los grupos
- **THEN** el sistema responde con estado 403
