# Player Cost Specification

## Purpose

Permite que cada jugador tenga un costo (su valor monetario), que se usa como monto de referencia al traspasarlo a otro equipo.

## Requirements

### Requirement: Costo del jugador
El sistema SHALL almacenar un costo por jugador, expresado como un valor monetario mayor o igual a cero con hasta dos decimales, y SHALL exponerlo en todas las respuestas del API de jugadores.

#### Scenario: Crear un jugador con costo
- **WHEN** se crea un jugador enviando un costo válido
- **THEN** el sistema guarda el jugador con ese costo y la respuesta incluye el costo guardado

#### Scenario: Crear un jugador sin enviar costo
- **WHEN** se crea un jugador sin enviar el costo
- **THEN** el sistema lo guarda con costo 0 y la creación no falla

#### Scenario: Editar un jugador sin enviar costo
- **WHEN** se edita un jugador sin enviar el costo
- **THEN** el sistema conserva el costo que el jugador ya tenía

#### Scenario: Jugadores existentes antes del cambio
- **WHEN** se consulta un jugador registrado antes de que existiera el costo
- **THEN** el sistema lo devuelve con costo 0

#### Scenario: Costo negativo rechazado
- **WHEN** se crea o edita un jugador con un costo menor a cero
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que el costo no puede ser negativo, sin guardar cambios

#### Scenario: Editar el costo no altera traspasos ya registrados
- **WHEN** se edita el costo de un jugador que tiene traspasos registrados
- **THEN** los traspasos existentes conservan el costo con el que fueron solicitados
