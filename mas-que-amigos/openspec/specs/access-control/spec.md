# Access Control Specification

## Purpose

Define qué puede hacer cada rol (visitante sin cuenta, Director Técnico y Administrador) sobre la API y cómo responde el sistema cuando el acceso se niega.

## Requirements

### Requirement: El visitante solo consulta
El sistema SHALL permitir a cualquier persona, sin iniciar sesión, ejecutar las operaciones de consulta (`GET`) sobre equipos, jugadores, partidos, alineaciones, estadísticas, galería, registros informativos y traspasos, así como ver las imágenes subidas y la documentación de la API. El visitante NO SHALL poder crear, modificar ni eliminar nada.

#### Scenario: Consulta sin sesión
- **WHEN** una persona sin sesión consulta el listado de equipos, jugadores o partidos
- **THEN** el sistema responde con estado 200 y los datos, sin pedir credenciales

#### Scenario: Escritura sin sesión
- **WHEN** una persona sin sesión intenta crear, editar o eliminar un equipo, un jugador, un partido, una alineación, una estadística, una imagen de galería o un registro informativo
- **THEN** el sistema responde con estado 401 y no modifica ningún dato

### Requirement: El administrador tiene acceso total
El sistema SHALL permitir a una persona autenticada con rol `ADMIN` ejecutar todas las operaciones de consulta y de escritura de la API.

#### Scenario: Escritura de un administrador
- **WHEN** un administrador con sesión vigente crea un jugador, edita un equipo o inicia un partido
- **THEN** el sistema ejecuta la operación con el mismo resultado que tenía antes de existir el login

#### Scenario: Gestión de usuarios de un administrador
- **WHEN** un administrador con sesión vigente consulta o crea cuentas de usuario
- **THEN** el sistema atiende la solicitud

### Requirement: Permisos del Director Técnico en esta etapa
El sistema SHALL permitir a una persona autenticada con rol `DT` las mismas operaciones de consulta que al visitante y, además, solicitar traspasos hacia su propio equipo. NO SHALL permitirle ninguna otra operación de escritura ni la gestión de usuarios hasta que se habiliten sus permisos propios (alineaciones de su equipo, solicitudes de equipo y de jugador) en cambios posteriores.

#### Scenario: Consulta de un DT
- **WHEN** un DT con sesión vigente consulta equipos, jugadores o partidos
- **THEN** el sistema responde con estado 200 y los datos

#### Scenario: Solicitud de traspaso de un DT
- **WHEN** un DT con sesión vigente solicita un traspaso hacia su propio equipo
- **THEN** el sistema atiende la solicitud

#### Scenario: Otras escrituras de un DT
- **WHEN** un DT con sesión vigente intenta crear, editar o eliminar un equipo, un jugador, un partido o una alineación
- **THEN** el sistema responde con estado 403 y no modifica ningún dato

#### Scenario: Gestión de usuarios de un DT
- **WHEN** un DT con sesión vigente intenta consultar o crear cuentas de usuario
- **THEN** el sistema responde con estado 403

### Requirement: La gestión de usuarios es exclusiva del administrador
El sistema SHALL reservar la consulta y la creación de cuentas de usuario a los administradores, incluso para operaciones de consulta.

#### Scenario: Listado de usuarios sin sesión
- **WHEN** una persona sin sesión intenta listar los usuarios
- **THEN** el sistema responde con estado 401

### Requirement: Formato de los errores de acceso
El sistema SHALL responder `401` cuando falta, venció o es inválido el token en una operación que lo exige, y `403` cuando la persona está autenticada pero su rol no alcanza para la operación. En ambos casos el cuerpo SHALL ser un objeto con `indicadorRespuesta` y `mensaje` en español.

#### Scenario: Falta el token
- **WHEN** se llama a una operación protegida sin token
- **THEN** el sistema responde con estado 401 y un cuerpo con `indicadorRespuesta` y un `mensaje` en español que pide iniciar sesión

#### Scenario: Rol insuficiente
- **WHEN** una persona autenticada ejecuta una operación para la que su rol no tiene permiso
- **THEN** el sistema responde con estado 403 y un cuerpo con `indicadorRespuesta` y un `mensaje` en español que indica que no tiene permisos para esa acción

### Requirement: Retiro de la clave fija de Director Técnico
El sistema NO SHALL aceptar la clave fija de Director Técnico (header `X-Director-Tecnico-Key`) como forma de autorización y SHALL exigir un token de sesión para toda operación que no sea de consulta.

#### Scenario: Escritura con la clave fija
- **WHEN** se llama a una operación de escritura enviando solo el header `X-Director-Tecnico-Key`, aunque su valor sea el de la clave anterior
- **THEN** el sistema responde con estado 401 y no modifica ningún dato

### Requirement: El front puede enviar el token de sesión
El sistema SHALL aceptar peticiones desde los orígenes permitidos del front que incluyan el header `Authorization`, incluidas las solicitudes previas (preflight) que lo anuncian.

#### Scenario: Preflight con Authorization
- **WHEN** un origen permitido del front envía una solicitud previa que anuncia el header `Authorization` y el método `POST`
- **THEN** el sistema responde permitiendo ese origen, método y header
