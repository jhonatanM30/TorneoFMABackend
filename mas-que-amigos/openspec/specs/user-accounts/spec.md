# User Accounts Specification

## Purpose

Define las cuentas de usuario de la aplicación, con rol de Administrador o Director Técnico, el administrador inicial sembrado por configuración y cómo un administrador crea otras cuentas.

## Requirements

### Requirement: Roles de las cuentas
El sistema SHALL manejar dos roles de cuenta: `ADMIN` (administrador de la organización) y `DT` (director técnico). Cada cuenta SHALL tener un usuario único, sin distinguir mayúsculas de minúsculas, un rol y una fecha de creación. El visitante no tiene cuenta ni rol almacenado.

#### Scenario: Usuario único sin importar mayúsculas
- **WHEN** ya existe la cuenta `entrenador1` y se intenta crear otra con el usuario `Entrenador1`
- **THEN** el sistema rechaza la creación porque el usuario ya existe

### Requirement: Administrador inicial sembrado por configuración
Al arrancar, el sistema SHALL crear la cuenta de administrador inicial a partir de un usuario y una contraseña tomados de la configuración, solo si no existe ya una cuenta con ese usuario. No SHALL crear ni modificar la cuenta cuando ya existe, y SHALL arrancar sin administrador, registrando una advertencia, cuando falta esa configuración.

#### Scenario: Primer arranque con configuración de administrador
- **WHEN** el sistema arranca, no existe la cuenta del administrador y la configuración trae usuario y contraseña
- **THEN** se crea la cuenta con rol `ADMIN` y con la contraseña guardada de forma protegida, y esa persona puede iniciar sesión

#### Scenario: Arranque cuando el administrador ya existe
- **WHEN** el sistema arranca y la cuenta del administrador ya existe
- **THEN** no se crea otra cuenta y no se cambia la contraseña guardada, aunque la configuración traiga otra

#### Scenario: Arranque sin configuración de administrador
- **WHEN** el sistema arranca sin usuario o sin contraseña de administrador en la configuración
- **THEN** el sistema arranca sin crear ninguna cuenta y deja una advertencia en el registro, y las operaciones de solo consulta siguen disponibles

### Requirement: Contraseñas protegidas
El sistema SHALL guardar las contraseñas únicamente de forma protegida (hash con sal) y NO SHALL devolver contraseñas ni su representación protegida en ninguna respuesta ni registro.

#### Scenario: Las respuestas de usuarios no exponen contraseñas
- **WHEN** se crea una cuenta o se lista a los usuarios
- **THEN** ninguna respuesta incluye la contraseña ni su hash

### Requirement: Creación de cuentas por un administrador
El sistema SHALL permitir a un administrador crear cuentas indicando usuario, contraseña y, opcionalmente, el rol (`ADMIN` o `DT`) y el equipo (solo para `DT`). Cuando no se indica el rol, la cuenta SHALL crearse con rol `DT`. El usuario SHALL tener entre 3 y 50 caracteres entre letras, números, punto, guion y guion bajo, y la contraseña al menos 8 caracteres.

#### Scenario: Creación de una cuenta de DT
- **WHEN** un administrador crea una cuenta con un usuario libre y una contraseña de al menos 8 caracteres, sin indicar rol
- **THEN** el sistema responde con estado 201 y el identificador, el usuario (en minúsculas), el rol `DT`, sin equipo asignado y la fecha de creación

#### Scenario: Creación de otro administrador
- **WHEN** un administrador crea una cuenta indicando el rol `ADMIN`
- **THEN** el sistema responde con estado 201 y la cuenta queda con rol `ADMIN`, con acceso total una vez que inicie sesión

#### Scenario: Rol inexistente
- **WHEN** un administrador crea una cuenta indicando un rol distinto de `ADMIN` o `DT`
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear la cuenta

#### Scenario: Usuario ya existente
- **WHEN** un administrador crea una cuenta con un usuario que ya existe
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que el usuario ya existe, sin crear la cuenta

#### Scenario: Usuario o contraseña inválidos
- **WHEN** un administrador crea una cuenta con un usuario fuera del formato permitido o con una contraseña de menos de 8 caracteres
- **THEN** el sistema responde con estado 400 y mensajes en español por cada campo inválido, sin crear la cuenta

#### Scenario: La cuenta creada puede iniciar sesión
- **WHEN** un administrador crea una cuenta y luego esa persona inicia sesión con su usuario y contraseña
- **THEN** el sistema inicia la sesión con el rol con el que se creó la cuenta

### Requirement: Equipo del Director Técnico
El sistema SHALL permitir asociar a una cuenta `DT` como máximo un equipo, y a un equipo como máximo una cuenta `DT`. Un administrador SHALL poder indicar el equipo al crear la cuenta y asignarlo, cambiarlo o quitarlo después. Una cuenta `DT` SHALL poder existir sin equipo, por ejemplo mientras espera la aprobación del equipo que solicitó. Las cuentas `ADMIN` NO SHALL tener equipo.

#### Scenario: Crear un DT con equipo
- **WHEN** un administrador crea una cuenta de DT indicando un equipo existente que no tiene DT asignado
- **THEN** el sistema responde con estado 201 y la cuenta queda asociada a ese equipo

#### Scenario: Equipo inexistente
- **WHEN** un administrador crea una cuenta o asigna un equipo que no existe
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin cambios

#### Scenario: Equipo que ya tiene DT
- **WHEN** un administrador asigna a una cuenta un equipo que ya está asignado a otra cuenta de DT
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin cambios

#### Scenario: Equipo en una cuenta de administrador
- **WHEN** un administrador crea una cuenta `ADMIN` con equipo, o asigna un equipo a una cuenta `ADMIN`
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin cambios

#### Scenario: Asignar equipo a un DT existente
- **WHEN** un administrador asigna un equipo libre a una cuenta de DT que no tenía equipo
- **THEN** el sistema responde con estado 200 y la cuenta queda asociada a ese equipo

#### Scenario: Cambiar el equipo de un DT
- **WHEN** un administrador asigna otro equipo libre a un DT que ya tenía uno
- **THEN** el sistema responde con estado 200, la cuenta queda con el equipo nuevo y el equipo anterior queda libre para otra cuenta

#### Scenario: Quitar el equipo de un DT
- **WHEN** un administrador quita el equipo de una cuenta de DT
- **THEN** el sistema responde con estado 200 y la cuenta queda sin equipo

#### Scenario: Cuenta inexistente
- **WHEN** un administrador asigna un equipo a una cuenta que no existe
- **THEN** el sistema responde con estado 404 y un mensaje en español

### Requirement: Listado de usuarios por un administrador
El sistema SHALL permitir a un administrador listar las cuentas existentes, devolviendo para cada una identificador, usuario, rol, equipo asignado (nulo si no tiene) y fecha de creación, ordenadas por usuario.

#### Scenario: Listado
- **WHEN** un administrador consulta los usuarios
- **THEN** el sistema responde con estado 200 y la lista de cuentas, incluida la del administrador inicial, ordenadas por usuario
