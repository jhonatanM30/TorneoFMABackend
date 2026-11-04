# Tournaments Specification

## Purpose

Permite al administrador crear torneos, definir antes de iniciarlos cómo se juegan (liga, grupos con eliminatoria o eliminatoria directa) y llevarlos por su ciclo de vida (borrador, en curso, finalizado). El formato queda bloqueado al iniciar para que el sistema se comporte según lo definido.

## Requirements

### Requirement: Creación de torneo
El sistema SHALL permitir crear un torneo indicando un nombre y un formato. El formato SHALL ser obligatorio y SHALL ser uno de `LEAGUE` (liga), `GROUPS_KNOCKOUT` (fase de grupos y eliminatoria) o `KNOCKOUT` (eliminatoria directa). El torneo SHALL crearse en estado `DRAFT`. El nombre SHALL ser obligatorio, tener entre 3 y 100 caracteres y ser único sin distinguir mayúsculas.

#### Scenario: Creación válida
- **WHEN** se crea un torneo con nombre "Copa Apertura" y formato `LEAGUE`
- **THEN** el sistema responde con estado 201 y el torneo con su id, nombre, formato, estado `DRAFT` y sin equipos inscritos

#### Scenario: Formato ausente o inválido
- **WHEN** se crea un torneo sin formato, o con un formato que no es `LEAGUE`, `GROUPS_KNOCKOUT` ni `KNOCKOUT`
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear el torneo

#### Scenario: El formato interno del torneo Histórico no se puede elegir
- **WHEN** se crea un torneo con formato `LEGACY`
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear el torneo

#### Scenario: Nombre inválido
- **WHEN** se crea un torneo con nombre vacío o de menos de 3 caracteres o de más de 100
- **THEN** el sistema responde con estado 400 y un mapa campo → mensaje en español

#### Scenario: Nombre repetido
- **WHEN** se crea un torneo cuyo nombre ya usa otro torneo (sin importar mayúsculas)
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear el torneo

### Requirement: Consulta de torneos
El sistema SHALL permitir listar los torneos y consultar uno por su id. La consulta SHALL ser pública, sin sesión.

#### Scenario: Listado
- **WHEN** se consultan los torneos
- **THEN** el sistema responde con estado 200 y la lista de torneos con id, nombre, formato, estado y cantidad de equipos inscritos

#### Scenario: Consulta por id
- **WHEN** se consulta un torneo existente por su id
- **THEN** el sistema responde con estado 200 y sus datos, incluidas las fechas de creación e inicio (esta última vacía si aún no inició)

#### Scenario: Torneo inexistente
- **WHEN** se consulta, edita, inicia, finaliza o elimina un torneo que no existe
- **THEN** el sistema responde con estado 404 y un mensaje en español, con el mismo formato que los demás recursos de partidos y equipos (`{"indicadorRespuesta":"Sin Registros","mensaje":"..."}`)

### Requirement: Edición de torneo en borrador
El sistema SHALL permitir cambiar el nombre y el formato de un torneo solo mientras esté en estado `DRAFT`, con las mismas validaciones de la creación. Un torneo `IN_PROGRESS` o `FINISHED` SHALL conservar su nombre y su formato.

#### Scenario: Cambio de formato en borrador
- **WHEN** se edita un torneo `DRAFT` cambiando su formato de `LEAGUE` a `KNOCKOUT`
- **THEN** el sistema responde con estado 200 y el torneo con el formato nuevo

#### Scenario: Edición de un torneo ya iniciado
- **WHEN** se intenta editar el nombre o el formato de un torneo `IN_PROGRESS` o `FINISHED`
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que el torneo ya inició, sin cambiar nada

### Requirement: Eliminación de torneo en borrador
El sistema SHALL permitir eliminar un torneo solo mientras esté en estado `DRAFT`, retirando también sus inscripciones. Un torneo `IN_PROGRESS` o `FINISHED` SHALL conservarse.

#### Scenario: Eliminación de un borrador
- **WHEN** se elimina un torneo `DRAFT` con equipos inscritos
- **THEN** el sistema responde con estado 204, el torneo deja de existir y sus inscripciones también, y los equipos no se eliminan

#### Scenario: Eliminación de un torneo ya iniciado
- **WHEN** se intenta eliminar un torneo `IN_PROGRESS` o `FINISHED`
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin eliminarlo

### Requirement: Inicio de torneo
El sistema SHALL permitir iniciar un torneo `DRAFT` que tenga al menos 2 equipos inscritos. Al iniciarlo, su estado SHALL pasar a `IN_PROGRESS`, SHALL registrarse la fecha de inicio y su formato y sus equipos inscritos SHALL quedar bloqueados.

#### Scenario: Inicio válido
- **WHEN** se inicia un torneo `DRAFT` con 4 equipos inscritos
- **THEN** el sistema responde con estado 200 y el torneo en estado `IN_PROGRESS` con su fecha de inicio

#### Scenario: Equipos insuficientes
- **WHEN** se intenta iniciar un torneo `DRAFT` con menos de 2 equipos inscritos
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica el mínimo, y el torneo sigue en `DRAFT`

#### Scenario: Torneo que no está en borrador
- **WHEN** se intenta iniciar un torneo que ya está `IN_PROGRESS` o `FINISHED`
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin cambiar nada

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

### Requirement: Torneo Histórico
El sistema SHALL contar con un torneo llamado "Histórico", de formato interno `LEGACY` y estado `IN_PROGRESS`, que agrupa los partidos creados antes de existir los torneos y los que se creen sin indicar torneo. Este torneo SHALL ser protegido: no se SHALL poder editar, eliminar, iniciar ni finalizar, y no SHALL exigir inscripción de equipos.

#### Scenario: Existencia tras la migración
- **WHEN** se consultan los torneos después de aplicar la migración
- **THEN** aparece el torneo "Histórico" con formato `LEGACY` y estado `IN_PROGRESS`, y todos los partidos previos pertenecen a él

#### Scenario: Operaciones no permitidas sobre el Histórico
- **WHEN** se intenta editar, eliminar, iniciar o finalizar el torneo Histórico
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que es un torneo protegido, sin cambiar nada

### Requirement: Permisos sobre los torneos
El sistema SHALL permitir consultar torneos sin sesión y SHALL exigir una sesión de administrador para crear, editar, eliminar, iniciar, finalizar torneos y para inscribir o retirar equipos.

#### Scenario: Consulta sin sesión
- **WHEN** un visitante sin sesión consulta los torneos
- **THEN** el sistema responde con estado 200

#### Scenario: Escritura sin sesión
- **WHEN** un visitante sin sesión intenta crear un torneo
- **THEN** el sistema responde con estado 401

#### Scenario: Escritura de un Director Técnico
- **WHEN** un usuario con rol `DT` intenta crear, editar, iniciar o finalizar un torneo, o inscribir un equipo
- **THEN** el sistema responde con estado 403
