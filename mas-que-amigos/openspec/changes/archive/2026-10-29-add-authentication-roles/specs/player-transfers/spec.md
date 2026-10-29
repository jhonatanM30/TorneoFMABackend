# Spec Delta

## ADDED Requirements

### Requirement: Permisos sobre los traspasos
El sistema SHALL permitir solicitar un traspaso a un administrador y a un DT, y SHALL reservar al administrador la confirmación y el rechazo de traspasos. El visitante sin sesión NO SHALL poder solicitar, confirmar ni rechazar. La consulta de traspasos SHALL seguir abierta a cualquier persona, con o sin sesión, en cualquier estado, porque no modifica datos.

#### Scenario: Un administrador solicita un traspaso
- **WHEN** un administrador con sesión vigente solicita el traspaso de un jugador hacia cualquier equipo
- **THEN** el sistema crea la solicitud pendiente con el comportamiento ya definido para solicitar un traspaso

#### Scenario: Un DT intenta confirmar o rechazar
- **WHEN** un DT con sesión vigente intenta confirmar o rechazar un traspaso
- **THEN** el sistema responde con estado 403 y no modifica la solicitud ni al jugador

#### Scenario: Un administrador confirma o rechaza
- **WHEN** un administrador con sesión vigente confirma o rechaza un traspaso pendiente
- **THEN** el sistema ejecuta la operación con el comportamiento ya definido para confirmar o rechazar

#### Scenario: Un visitante intenta solicitar, confirmar o rechazar
- **WHEN** una persona sin sesión intenta solicitar, confirmar o rechazar un traspaso
- **THEN** el sistema responde con estado 401 y no crea ni modifica ninguna solicitud

#### Scenario: Consulta abierta de traspasos
- **WHEN** una persona sin sesión, un DT o un administrador consulta los traspasos, con o sin filtros
- **THEN** el sistema responde con estado 200 y los traspasos que coinciden con los filtros, en cualquier estado, con el comportamiento ya definido para la consulta

### Requirement: Un DT solicita traspasos solo hacia su equipo
El sistema SHALL permitir a un DT solicitar únicamente traspasos cuyo equipo destino sea el equipo asignado a su cuenta, tomado de la cuenta y no del token ni de lo que envíe el cliente. Un DT sin equipo asignado NO SHALL poder solicitar traspasos.

#### Scenario: Un DT solicita un traspaso hacia su equipo
- **WHEN** un DT con equipo asignado solicita el traspaso de un jugador de otro equipo hacia su propio equipo
- **THEN** el sistema responde con estado 201 y crea la solicitud pendiente con el comportamiento ya definido para solicitar un traspaso

#### Scenario: Un DT solicita un traspaso hacia otro equipo
- **WHEN** un DT con equipo asignado solicita un traspaso cuyo equipo destino no es el suyo
- **THEN** el sistema responde con estado 403 y un mensaje en español que indica que solo puede solicitar traspasos hacia su equipo, sin crear la solicitud

#### Scenario: Un DT sin equipo solicita un traspaso
- **WHEN** un DT que no tiene equipo asignado solicita un traspaso
- **THEN** el sistema responde con estado 403 y un mensaje en español que indica que no tiene un equipo asignado, sin crear la solicitud

#### Scenario: El equipo se toma de la cuenta y no del token
- **WHEN** a un administrador le asigna un equipo a un DT después de que este inició sesión y el DT solicita un traspaso hacia ese equipo con el mismo token
- **THEN** el sistema acepta la solicitud, porque el equipo se consulta en la cuenta al momento de solicitar
