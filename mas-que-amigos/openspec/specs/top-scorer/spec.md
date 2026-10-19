# Top Scorer Specification

## Purpose

Permite consultar quién es el goleador del torneo actual: el jugador o los jugadores con más goles anotados, sumando los goles registrados en todos los partidos del torneo, sin importar el equipo por el que jugaron.

## Requirements

### Requirement: Cálculo del goleador del torneo
El sistema SHALL calcular el goleador del torneo sumando, por jugador, los goles registrados en todos los partidos existentes, sin filtrar por equipo ni por estado del partido.

#### Scenario: Un jugador tiene más goles que todos los demás
- **WHEN** se consulta el goleador del torneo y un jugador tiene un total de goles estrictamente mayor al de cualquier otro jugador
- **THEN** el sistema devuelve únicamente a ese jugador junto con su total de goles

#### Scenario: Los goles se suman sin importar el equipo ni el partido
- **WHEN** se calcula el total de goles de un jugador
- **THEN** el sistema suma los goles de ese jugador en todos los partidos en los que participó, incluyendo partidos con distintos equipos rivales y sin excluir partidos por su estado

#### Scenario: Un jugador sin goles registrados no es candidato a goleador
- **WHEN** un jugador no tiene ningún registro de estadística con goles mayores a cero
- **THEN** ese jugador no aparece en el resultado del goleador del torneo

### Requirement: Empate en el máximo de goles
El sistema SHALL devolver a todos los jugadores empatados cuando más de uno alcance el máximo total de goles del torneo.

#### Scenario: Dos o más jugadores empatan en el máximo de goles
- **WHEN** dos o más jugadores comparten el total de goles más alto del torneo
- **THEN** el sistema devuelve una lista con todos esos jugadores empatados, cada uno con su total de goles, sin elegir arbitrariamente a uno solo

### Requirement: Endpoint de consulta del goleador del torneo
El sistema SHALL exponer un endpoint HTTP de solo lectura que devuelva el goleador o los goleadores del torneo, identificando a cada jugador y su total de goles.

#### Scenario: Consulta exitosa con datos de goles registrados
- **WHEN** un cliente realiza una solicitud GET al endpoint del goleador del torneo y existen goles registrados
- **THEN** el sistema responde con estado 200 y una lista de uno o más jugadores, cada uno con su identificador, nombre y total de goles

#### Scenario: Consulta cuando no hay goles registrados en el torneo
- **WHEN** un cliente realiza una solicitud GET al endpoint del goleador del torneo y no existe ningún gol registrado en el torneo
- **THEN** el sistema responde con estado 200 y una lista vacía
