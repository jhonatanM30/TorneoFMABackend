# Tournaments Specification

## MODIFIED Requirements

### Requirement: Finalización de torneo
El sistema SHALL permitir finalizar un torneo `IN_PROGRESS` solo si todos sus partidos están finalizados. Además, un torneo de formato `KNOCKOUT` o `GROUPS_KNOCKOUT` SHALL finalizarse solo cuando sus llaves ya están generadas y la final está finalizada (hay campeón). Al finalizarlo, su estado SHALL pasar a `FINISHED` y ya no SHALL admitir partidos nuevos.

#### Scenario: Finalización válida
- **WHEN** se finaliza un torneo `IN_PROGRESS` cuyos partidos están todos finalizados
- **THEN** el sistema responde con estado 200 y el torneo en estado `FINISHED`

#### Scenario: Partidos pendientes
- **WHEN** se intenta finalizar un torneo con partidos programados o en curso
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica cuántos partidos faltan por finalizar, y el torneo sigue `IN_PROGRESS`

#### Scenario: Torneo que no está en curso
- **WHEN** se intenta finalizar un torneo `DRAFT` o `FINISHED`
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin cambiar nada

#### Scenario: Eliminatoria con campeón
- **WHEN** se finaliza un torneo `KNOCKOUT` o `GROUPS_KNOCKOUT` cuya final está finalizada y cuyos partidos están todos finalizados
- **THEN** el sistema responde con estado 200 y el torneo en estado `FINISHED`

#### Scenario: Eliminatoria sin campeón definido
- **WHEN** se intenta finalizar un torneo `KNOCKOUT` o `GROUPS_KNOCKOUT` sin llaves generadas, o con llaves cuya final aún no está finalizada, aunque no queden partidos programados ni en curso
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que falta definir al campeón, y el torneo sigue `IN_PROGRESS`
