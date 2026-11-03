# Tournament Enrollment Specification

## Purpose

Permite al administrador elegir qué equipos participan en un torneo, inscribiéndolos o retirándolos mientras el torneo no ha iniciado, y consultar los equipos inscritos. Un mismo equipo puede participar en varios torneos a la vez (por ejemplo, una liga y una copa).

## Requirements

### Requirement: Inscripción de equipos
El sistema SHALL permitir inscribir un equipo existente en un torneo solo mientras este esté en estado `DRAFT`. Un equipo SHALL poder estar inscrito simultáneamente en varios torneos, sin importar su formato ni su estado, pero no más de una vez en el mismo torneo.

#### Scenario: Inscripción válida
- **WHEN** se inscribe un equipo existente en un torneo `DRAFT`
- **THEN** el sistema responde con estado 201 y el equipo aparece entre los inscritos del torneo

#### Scenario: Equipo ya inscrito en ese torneo
- **WHEN** se inscribe en un torneo un equipo que ya está inscrito en él
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin duplicar la inscripción

#### Scenario: Equipo en varios torneos a la vez
- **WHEN** se inscribe en un torneo `DRAFT` un equipo que ya participa en otro torneo, incluso uno `IN_PROGRESS` de otro formato
- **THEN** el sistema crea la inscripción y el equipo participa en ambos torneos

#### Scenario: Equipo inexistente
- **WHEN** se inscribe un equipo que no existe
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear la inscripción

#### Scenario: Torneo ya iniciado
- **WHEN** se intenta inscribir un equipo en un torneo `IN_PROGRESS` o `FINISHED`
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que el torneo ya inició, sin crear la inscripción

#### Scenario: Torneo Histórico
- **WHEN** se intenta inscribir un equipo en el torneo Histórico
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que ese torneo no usa inscripción

### Requirement: Retiro de equipos
El sistema SHALL permitir retirar un equipo inscrito de un torneo solo mientras este esté en estado `DRAFT`. El retiro SHALL quitar únicamente la inscripción en ese torneo, sin eliminar el equipo ni tocar su inscripción en otros torneos.

#### Scenario: Retiro válido
- **WHEN** se retira un equipo inscrito de un torneo `DRAFT`
- **THEN** el sistema responde con estado 204, el equipo deja de aparecer entre los inscritos de ese torneo y el equipo sigue existiendo

#### Scenario: El retiro no afecta otros torneos
- **WHEN** se retira de un torneo `DRAFT` un equipo que también está inscrito en otro torneo
- **THEN** el equipo sigue inscrito en el otro torneo

#### Scenario: Equipo no inscrito
- **WHEN** se intenta retirar de un torneo un equipo que no está inscrito en él
- **THEN** el sistema responde con estado 404 y un mensaje en español, con el mismo formato que los demás recursos de partidos y equipos (`{"indicadorRespuesta":"Sin Registros","mensaje":"..."}`)

#### Scenario: Torneo ya iniciado
- **WHEN** se intenta retirar un equipo de un torneo `IN_PROGRESS` o `FINISHED`
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin quitar la inscripción

### Requirement: Consulta de equipos inscritos
El sistema SHALL permitir consultar, sin sesión, los equipos inscritos en un torneo, ordenados por nombre.

#### Scenario: Torneo con equipos
- **WHEN** se consultan los equipos inscritos de un torneo con 3 equipos
- **THEN** el sistema responde con estado 200 y los 3 equipos con su id y nombre, ordenados por nombre

#### Scenario: Torneo sin equipos
- **WHEN** se consultan los equipos inscritos de un torneo sin inscripciones
- **THEN** el sistema responde con estado 200 y una lista vacía

#### Scenario: Torneo inexistente
- **WHEN** se consultan los equipos inscritos de un torneo que no existe
- **THEN** el sistema responde con estado 404 y un mensaje en español, con el mismo formato que los demás recursos de partidos y equipos (`{"indicadorRespuesta":"Sin Registros","mensaje":"..."}`)
