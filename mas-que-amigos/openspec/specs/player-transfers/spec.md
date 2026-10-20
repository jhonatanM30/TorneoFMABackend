# Player Transfers Specification

## Purpose

Permite trasladar a un jugador de un equipo a otro mediante solicitudes que un organizador confirma o rechaza, registrando el costo de cada traspaso y conservando el historial de equipos por los que pasó el jugador.

## Requirements

### Requirement: Solicitud de traspaso
El sistema SHALL permitir solicitar el traspaso de un jugador a un equipo destino distinto de su equipo actual. La solicitud SHALL crearse en estado pendiente, SHALL registrar el equipo origen (el equipo actual del jugador) y el costo vigente del jugador en ese momento, y NO SHALL cambiar el equipo del jugador.

#### Scenario: Solicitud válida
- **WHEN** se solicita el traspaso de un jugador existente a un equipo destino existente distinto de su equipo actual
- **THEN** el sistema responde con estado 201 y una solicitud en estado pendiente con jugador, equipo origen, equipo destino, costo y fecha de solicitud, y el jugador sigue en su equipo actual

#### Scenario: El costo de la solicitud es una copia del costo del jugador
- **WHEN** se solicita un traspaso y luego se edita el costo del jugador
- **THEN** la solicitud conserva el costo que tenía el jugador al momento de solicitarla

#### Scenario: Equipo destino igual al equipo actual
- **WHEN** se solicita el traspaso de un jugador al mismo equipo en el que ya está
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear la solicitud

#### Scenario: Jugador o equipo destino inexistente
- **WHEN** se solicita un traspaso con un jugador o un equipo destino que no existe
- **THEN** el sistema responde con un error (404 si no existe el jugador, 400 si no existe el equipo, igual que el resto del API) con un mensaje en español, sin crear la solicitud

#### Scenario: Varias solicitudes pendientes para el mismo jugador
- **WHEN** ya existe una solicitud pendiente para un jugador y se solicita otro traspaso de ese jugador (al mismo u otro equipo destino)
- **THEN** el sistema crea la nueva solicitud y ambas quedan pendientes

#### Scenario: Dorsal ocupado en el equipo destino
- **WHEN** se solicita un traspaso y el dorsal del jugador ya está asignado a otro jugador del equipo destino, y la solicitud no indica un dorsal nuevo
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear la solicitud

#### Scenario: Dorsal nuevo indicado en la solicitud
- **WHEN** se solicita un traspaso indicando un dorsal nuevo libre en el equipo destino
- **THEN** el sistema crea la solicitud con ese dorsal, que se aplicará al jugador si el traspaso se confirma

### Requirement: Confirmación de traspaso
El sistema SHALL permitir confirmar una solicitud pendiente. Al confirmarla, el jugador SHALL pasar al equipo destino (y al dorsal indicado, si lo hubo) de forma atómica, y la solicitud SHALL quedar en estado confirmado con su fecha de resolución.

#### Scenario: Confirmación exitosa
- **WHEN** se confirma una solicitud pendiente cuyo dorsal sigue disponible en el equipo destino
- **THEN** el sistema responde con estado 200, la solicitud queda confirmada con fecha de resolución, y el jugador pertenece al equipo destino

#### Scenario: Dorsal ocupado al momento de confirmar
- **WHEN** se confirma una solicitud pendiente y, entre la solicitud y la confirmación, el dorsal quedó ocupado en el equipo destino
- **THEN** el sistema responde con estado 400 y un mensaje en español, la solicitud sigue pendiente y el jugador no cambia de equipo

#### Scenario: Solicitud que ya no está pendiente
- **WHEN** se intenta confirmar una solicitud que ya fue confirmada o rechazada
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin cambios

#### Scenario: Solicitud inexistente
- **WHEN** se intenta confirmar o rechazar una solicitud que no existe
- **THEN** el sistema responde con estado 404 y un mensaje en español

### Requirement: Rechazo automático de las demás solicitudes del jugador
Al confirmarse un traspaso, el sistema SHALL rechazar automáticamente todas las demás solicitudes pendientes del mismo jugador, en la misma operación atómica de la confirmación, y esas solicitudes SHALL dejar de aparecer entre las solicitudes pendientes.

#### Scenario: Se confirma una solicitud y hay otras pendientes del mismo jugador
- **WHEN** un jugador tiene tres solicitudes pendientes y se confirma una de ellas
- **THEN** las otras dos quedan rechazadas con fecha de resolución y ya no aparecen al consultar las solicitudes pendientes

#### Scenario: Las solicitudes pendientes de otros jugadores no se ven afectadas
- **WHEN** se confirma una solicitud de un jugador
- **THEN** las solicitudes pendientes de cualquier otro jugador siguen pendientes

#### Scenario: La confirmación falla
- **WHEN** la confirmación no puede completarse (por ejemplo, dorsal ocupado)
- **THEN** ninguna otra solicitud del jugador se rechaza

### Requirement: Rechazo manual de traspaso
El sistema SHALL permitir rechazar una solicitud pendiente sin modificar al jugador ni las demás solicitudes.

#### Scenario: Rechazo exitoso
- **WHEN** se rechaza una solicitud pendiente
- **THEN** el sistema responde con estado 200, la solicitud queda rechazada con fecha de resolución, el jugador sigue en su equipo y las demás solicitudes del jugador no cambian

#### Scenario: Rechazar una solicitud que no está pendiente
- **WHEN** se intenta rechazar una solicitud ya confirmada o rechazada
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin cambios

### Requirement: Consulta de solicitudes e historial de traspasos
El sistema SHALL exponer una consulta de solo lectura de traspasos que permita filtrar por estado y por jugador, devolviendo para cada traspaso: identificador, jugador, equipo origen, equipo destino, costo, estado, fecha de solicitud y fecha de resolución (nula mientras esté pendiente). Los resultados SHALL ordenarse del más reciente al más antiguo por fecha de solicitud.

#### Scenario: Bandeja de solicitudes pendientes
- **WHEN** se consultan los traspasos filtrando por estado pendiente
- **THEN** el sistema responde con estado 200 y solo las solicitudes pendientes, sin incluir las confirmadas ni las rechazadas (manual o automáticamente)

#### Scenario: Historial de equipos de un jugador
- **WHEN** se consultan los traspasos de un jugador filtrando por estado confirmado
- **THEN** el sistema responde con estado 200 y la lista de sus traspasos confirmados, cada uno con equipo origen, equipo destino y costo pagado

#### Scenario: Jugador sin traspasos
- **WHEN** se consultan los traspasos de un jugador que nunca fue traspasado
- **THEN** el sistema responde con estado 200 y una lista vacía

#### Scenario: Consulta sin filtros
- **WHEN** se consultan los traspasos sin filtros
- **THEN** el sistema responde con estado 200 y todos los traspasos en cualquier estado

#### Scenario: Estado de filtro inválido
- **WHEN** se consultan los traspasos con un estado que no existe
- **THEN** el sistema responde con estado 400 y un mensaje en español
