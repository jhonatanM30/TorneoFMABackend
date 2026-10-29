# Proposal

## Why

Hoy la API se protege con una única clave fija compartida (`X-Director-Tecnico-Key`): quien la conoce puede crear, editar y borrar todo, y no hay forma de saber quién hizo qué ni de darle a cada persona solo lo que le corresponde. La liga necesita cuentas reales con roles (Administrador, Director Técnico y un visitante sin cuenta) como base para los siguientes changes: permisos del DT sobre su equipo, solicitudes con aprobación y notificaciones. Este change es el primero de seis y entrega los cimientos: identidad, sesión, autorización por rol y el vínculo mínimo entre un DT y su equipo.

## What Changes

- **Login con usuario y contraseña** y sesión mediante token de corta vida (JWT, 3 minutos por defecto). El front lo renueva mientras el usuario tiene actividad en pantalla; si pasan 3 minutos sin renovación, la sesión se cierra y hay que volver a iniciar sesión.
- **Cuentas de usuario** con rol `ADMIN` o `DT`. Hay un administrador inicial sembrado por configuración (usuario y contraseña por variables de entorno). Solo un administrador puede crear cuentas (de DT, que es el rol por defecto, o de otros administradores) y listar usuarios. No hay auto-registro.
- **Equipo del DT**: una cuenta DT tiene como máximo un equipo y un equipo tiene como máximo un DT. El administrador asigna, cambia o quita el equipo (al crear la cuenta o después); un DT puede existir sin equipo. Login y sesión actual devuelven el equipo asignado.
- **Autorización por rol** sobre toda la API:
  - **Visitante** (sin login): solo operaciones de consulta (`GET`).
  - **DT**: consulta lo mismo que el visitante y además puede **solicitar traspasos hacia su propio equipo**; el resto de sus permisos de escritura (alineaciones de su equipo, solicitudes de equipo y de jugador) llegan en los changes siguientes.
  - **ADMIN**: acceso total, incluidas todas las escrituras que hoy hace la clave fija.
- **Traspasos**: solicitar queda disponible para ADMIN y DT; **confirmar y rechazar son exclusivos del ADMIN** (hoy están tras la clave fija como solución temporal). La consulta de traspasos sigue abierta a todos, en cualquier estado, porque no modifica datos. Un DT solo puede solicitar traspasos cuyo destino sea su equipo (tomado de su cuenta); sin equipo asignado no puede solicitarlos.
- **BREAKING**: se retira la clave fija. Desaparecen el header `X-Director-Tecnico-Key`, `DirectorTecnicoInterceptor`, la propiedad `app.director-tecnico.clave` y el endpoint `POST /api/director-tecnico/validar`. Las escrituras ahora exigen `Authorization: Bearer <token>` de un ADMIN. El front actual (modo Director Técnico) deja de funcionar para escrituras hasta que se adapte.
- **BREAKING**: los errores de acceso cambian: `401` cuando falta o venció el token y `403` cuando el rol no alcanza, ambos con el cuerpo `{indicadorRespuesta, mensaje}` en español, el mismo formato que usa hoy el `401` de la clave fija.
- Swagger/OpenAPI pasa a documentar el esquema de seguridad Bearer en lugar de la clave fija.

## Capabilities

### New Capabilities
- `authentication`: inicio de sesión con usuario y contraseña, token de corta vida, renovación mientras hay actividad, expiración por inactividad y consulta de la sesión actual.
- `user-accounts`: cuentas con rol `ADMIN` o `DT`, administrador inicial sembrado por configuración, creación de cuentas (DT u otros administradores), equipo asignado al DT (uno por DT y uno por equipo) y listado de usuarios por un administrador.
- `access-control`: reglas de qué rol puede ejecutar qué operaciones de la API (visitante, DT, ADMIN) y formato de los errores `401` y `403`.

### Modified Capabilities
- `player-transfers`: se agregan los permisos por rol (solicitar: ADMIN y DT, este último solo hacia su equipo; confirmar y rechazar: solo ADMIN) y se deja explícito que la consulta es abierta.

## Impact

- **Base de datos**: migración Flyway `V8` con la tabla nueva `app_user` (usuario único, hash de contraseña, rol, equipo opcional y único con llave foránea a `equipos`, fecha de creación).
- **Dependencias** (`pom.xml`): Spring Security y soporte JWT de Spring (`spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server`).
- **Código backend**: paquete de seguridad nuevo (configuración de la cadena de filtros, emisión y validación de tokens, siembra del administrador), `AuthController` y `UserController` con sus servicios, entidad y repositorio de usuario; `PlayerTransferController`/`PlayerTransferService` (el equipo destino de un DT debe ser el suyo) y `GlobalExceptionHandler` (respuesta 403 para reglas de negocio); `OpenApiConfig` (esquema Bearer). Se eliminan `DirectorTecnicoInterceptor`, `DirectorTecnicoWebConfig` y `DirectorTecnicoController`.
- **API**: rutas nuevas `/api/auth/login`, `/api/auth/refresh`, `/api/auth/me`, `/api/users` y `PUT /api/users/{id}/team`. Se actualiza `docs/api-spec.yml` y `docs/data-model.md`.
- **Configuración y despliegue**: variables de entorno nuevas (secreto del JWT, usuario y contraseña del admin sembrado, duración del token) que deben agregarse al `docker-compose.yml`/`.env`, archivos que viven en el repositorio padre y que no se modifican sin mostrar antes el diff.
- **Frontend** (`MasQueAmigos-Torneo`): fuera de alcance, pero requiere adaptación: pantalla de login, envío del token, renovación por actividad y manejo de `401`/`403`.
- **Fuera de alcance** (changes posteriores): permisos de alineaciones del DT, notificaciones, solicitudes de equipo y jugador con aprobación, ajustes sobre traspasos para el DT, cierre de torneo. Tampoco incluye cambio ni recuperación de contraseña, bloqueo por intentos fallidos, deshabilitar cuentas ni cerrar sesión en el servidor (el cierre es descartar el token en el cliente).
