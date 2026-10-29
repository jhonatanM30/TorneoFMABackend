# Authentication Specification

## Purpose

Permite que una persona con cuenta inicie sesión con usuario y contraseña y use la API con una sesión de corta vida que se renueva mientras haya actividad y se cierra sola por inactividad.

## Requirements

### Requirement: Inicio de sesión con usuario y contraseña
El sistema SHALL permitir iniciar sesión enviando usuario y contraseña. Cuando las credenciales son correctas, SHALL responder con un token de sesión, su tipo, los segundos de vigencia y los datos básicos del usuario (identificador, usuario, rol y equipo asignado, nulo si no tiene). El usuario SHALL compararse sin distinguir mayúsculas de minúsculas.

#### Scenario: Credenciales correctas
- **WHEN** se inicia sesión con un usuario existente y su contraseña correcta
- **THEN** el sistema responde con estado 200, un token, el tipo `Bearer`, la vigencia en segundos y el identificador, el usuario, el rol y el equipo asignado de la persona

#### Scenario: Usuario escrito con otras mayúsculas
- **WHEN** se inicia sesión con el usuario `ADMIN` y existe la cuenta `admin` con esa contraseña
- **THEN** el sistema inicia la sesión normalmente

#### Scenario: Contraseña incorrecta
- **WHEN** se inicia sesión con un usuario existente y una contraseña incorrecta
- **THEN** el sistema responde con estado 401 y el mensaje en español "Usuario o contraseña incorrectos", sin entregar token

#### Scenario: Usuario inexistente
- **WHEN** se inicia sesión con un usuario que no existe
- **THEN** el sistema responde con estado 401 y exactamente el mismo mensaje que para una contraseña incorrecta, de modo que no se pueda saber si el usuario existe

#### Scenario: Datos obligatorios vacíos
- **WHEN** se inicia sesión sin usuario o sin contraseña
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica el campo faltante

### Requirement: Sesión de corta vida
El sistema SHALL emitir tokens de sesión con una vigencia limitada, de 3 minutos por defecto y configurable. Un token SHALL identificar a la persona y su rol, y SHALL dejar de ser aceptado al vencer o si fue alterado.

#### Scenario: Token vigente
- **WHEN** se llama a una operación protegida con un token vigente
- **THEN** el sistema atiende la solicitud con la identidad y el rol de ese token

#### Scenario: Token vencido
- **WHEN** se llama a una operación protegida con un token cuya vigencia terminó
- **THEN** el sistema responde con estado 401 y un mensaje en español que indica que la sesión expiró

#### Scenario: Token alterado o inválido
- **WHEN** se llama a una operación protegida con un token modificado, mal formado o firmado por otro emisor
- **THEN** el sistema responde con estado 401 y no atiende la solicitud

#### Scenario: Sesión cerrada por inactividad
- **WHEN** pasan más minutos que la vigencia configurada sin que se renueve el token
- **THEN** el token deja de ser válido y para continuar hay que iniciar sesión de nuevo

### Requirement: Renovación de la sesión
El sistema SHALL permitir renovar la sesión con un token todavía vigente, entregando un token nuevo con una vigencia completa. No SHALL ser posible renovar con un token vencido.

#### Scenario: Renovación con token vigente
- **WHEN** se solicita renovar la sesión con un token vigente
- **THEN** el sistema responde con estado 200 y un token nuevo con una vigencia completa para la misma persona y rol

#### Scenario: Renovación con token vencido
- **WHEN** se solicita renovar la sesión con un token vencido
- **THEN** el sistema responde con estado 401 y no entrega un token nuevo

#### Scenario: Renovación sin token
- **WHEN** se solicita renovar la sesión sin enviar token
- **THEN** el sistema responde con estado 401

#### Scenario: Renovación con una cuenta que ya no existe
- **WHEN** se solicita renovar la sesión con un token vigente cuya cuenta fue eliminada
- **THEN** el sistema responde con estado 401 y no entrega un token nuevo

### Requirement: Consulta de la sesión actual
El sistema SHALL permitir a una persona autenticada consultar quién es según su token, devolviendo identificador, usuario, rol y equipo asignado, sin exponer datos de contraseña.

#### Scenario: Consulta con token vigente
- **WHEN** se consulta la sesión actual con un token vigente
- **THEN** el sistema responde con estado 200 y el identificador, el usuario, el rol y el equipo asignado de la persona

#### Scenario: Consulta sin token
- **WHEN** se consulta la sesión actual sin token
- **THEN** el sistema responde con estado 401

#### Scenario: Consulta con una cuenta que ya no existe
- **WHEN** se consulta la sesión actual con un token vigente cuya cuenta fue eliminada
- **THEN** el sistema responde con estado 401
