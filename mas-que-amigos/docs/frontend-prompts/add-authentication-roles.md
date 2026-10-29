# Prompt para el front: login y roles (`add-authentication-roles`)

> Para la IA que trabaja el repositorio `MasQueAmigos-Torneo` (HTML, CSS y JavaScript planos, sin framework ni build). No hagas commit: deja los cambios en el árbol de trabajo para que el usuario los revise.

## Contexto

El backend (`mas-que-amigos`, puerto 57075 por defecto) reemplazó la clave fija `X-Director-Tecnico-Key` por login real con roles. Mientras el front no se adapte, **toda pantalla que escribe (crear, editar, borrar) responde `401`**; las consultas siguen funcionando sin sesión.

Debes: agregar el login, enviar el token en cada escritura, renovarlo por actividad, manejar `401` y `403`, adaptar la interfaz al rol, y retirar todo uso de la clave fija.

## Cambios incompatibles

- Se eliminó el header `X-Director-Tecnico-Key` y el endpoint `POST /api/director-tecnico/validar` (ahora responde 404). Borra del front cualquier pantalla, campo o llamada que los use.
- Toda escritura (`POST`, `PUT`, `DELETE`) exige `Authorization: Bearer <token>`. Las consultas (`GET`) siguen abiertas.
- Los DT ya no pueden crear ni editar equipos, jugadores, partidos, alineaciones, estadísticas, galería ni registros informativos: solo el administrador.

## Roles y permisos

| Rol | Consultas (`GET`) | Solicitar traspaso | Confirmar/rechazar traspaso | Demás escrituras | `/api/users` |
|---|---|---|---|---|---|
| Visitante (sin sesión) | Sí | No (401) | No (401) | No (401) | No (401) |
| `DT` | Sí | Solo hacia su equipo | No (403) | No (403) | No (403) |
| `ADMIN` | Sí | Hacia cualquier equipo | Sí | Sí | Sí |

## Contrato del API

Todas las rutas con base `http://localhost:57075` (configurable).

### `POST /api/auth/login` (público)
Cuerpo: `{"username": "...", "password": "..."}`. El usuario no distingue mayúsculas.
Respuesta `200`:
```json
{
  "token": "<jwt>",
  "tokenType": "Bearer",
  "expiresInSeconds": 180,
  "user": { "id": 2, "username": "admin", "role": "ADMIN", "teamId": null, "teamName": null }
}
```
Errores: `401` `{"indicadorRespuesta":"No autorizado","mensaje":"Usuario o contraseña incorrectos"}` (mismo mensaje para usuario inexistente y contraseña incorrecta); `400` con campos vacíos: `{"username":"El usuario es obligatorio.","password":"La contraseña es obligatoria."}`.

### `POST /api/auth/refresh` (requiere token vigente, sin cuerpo)
Devuelve la misma forma que el login, con un token nuevo de vigencia completa y el rol vigente en la base de datos. Con token vencido o ausente: `401`; no se puede renovar un token vencido.

### `GET /api/auth/me` (requiere token)
Devuelve `{"id","username","role","teamId","teamName"}`. Úsalo para recuperar al usuario si ya tienes un token.

### `POST /api/users`, `GET /api/users`, `PUT /api/users/{id}/team` (solo `ADMIN`)
- Crear: `{"username","password","role"?,"teamId"?}`. `role` es `DT` por defecto o `ADMIN`. Usuario: 3 a 50 caracteres (letras, números, punto, guion y guion bajo), se guarda en minúsculas y es único. Contraseña: 8 a 72 caracteres. Responde `201` con `{"id","username","role","teamId","teamName","createdAt"}`; nunca devuelve contraseña ni hash.
- Listar: arreglo de lo mismo, ordenado por usuario.
- Equipo del DT: `PUT /api/users/{id}/team` con `{"teamId": 3}` asigna o cambia; `{"teamId": null}` lo quita.

### Traspasos
- `POST /api/transfers` (`ADMIN` y `DT`): `{"playerId","destinationTeamId","destinationDorsal"?}`. Un DT solo puede pedir traspasos hacia **su** equipo (el servidor lo comprueba contra su cuenta, no contra lo que envíe el front).
- `POST /api/transfers/{id}/confirm` y `/reject`: solo `ADMIN`.
- `GET /api/transfers`: público.

## Formato de los errores

| Código | Cuándo | Cuerpo |
|---|---|---|
| `401` | Falta, venció o es inválido el token | `{"indicadorRespuesta":"No autorizado","mensaje":"..."}` |
| `403` | Sesión válida pero el rol no alcanza | `{"indicadorRespuesta":"Prohibido","mensaje":"..."}` |
| `400` | Regla de negocio | `{"indicadorRespuesta":"Error","mensaje":"..."}` |
| `400` | Validación de campos | mapa campo → mensaje, por ejemplo `{"password":"La contraseña debe tener al menos 8 caracteres."}` |
| `404` | No existe el recurso | `{"error":"..."}` (ojo: la clave es `error`, no `mensaje`) |

Mensajes `401` relevantes: `"Debes iniciar sesión para realizar esta acción."` (sin token o token inválido) y `"Tu sesión expiró. Inicia sesión de nuevo."` (token vencido). Mensajes `403` relevantes: `"No tienes permisos para realizar esta acción."`, `"Solo puedes solicitar traspasos hacia tu equipo."` y `"No tienes un equipo asignado, por eso no puedes solicitar traspasos. Pídele a la organización que te asigne uno."`. Muestra siempre el `mensaje` del servidor al usuario; ya viene en español.

## Qué construir

1. **Pantalla de login** (usuario y contraseña). Si el servidor responde `401`, muestra su mensaje sin redirigir. Al entrar, guarda el token **en memoria** (una variable de módulo), no en `localStorage`, y el objeto `user` para decidir qué mostrar.
2. **Cliente HTTP central**: un solo lugar que agregue `Authorization: Bearer <token>` cuando haya sesión y que trate las respuestas de acceso. Las consultas pueden enviarse con o sin token.
3. **Renovación por actividad del usuario**: el token dura 3 minutos (`expiresInSeconds`). Mientras el usuario interactúe (clics, teclas, movimiento, enfoque de la pestaña), llama a `POST /api/auth/refresh` antes de que venza (por ejemplo, cuando queden unos 60 segundos) y reemplaza el token. **No renueves por temporizador sin actividad**: si el usuario deja de interactuar, el token debe vencer y la sesión cerrarse sola. No reintentes un refresh que respondió `401`.
4. **Manejo de `401`** (fuera del login): borra token y usuario, y lleva al login con el mensaje del servidor ("Tu sesión expiró…"). Conserva la pantalla de destino para volver tras iniciar sesión, si es sencillo.
5. **Manejo de `403`**: no cierres la sesión; muestra el `mensaje` del servidor.
6. **Interfaz por rol** (el backend es la autoridad; esto es solo comodidad):
   - Visitante: oculta o deshabilita todas las acciones de escritura y ofrece "Iniciar sesión".
   - `ADMIN`: ve todas las acciones, incluida una pantalla de usuarios (listar, crear DT o administrador, asignar/cambiar/quitar equipo del DT).
   - `DT`: solo consulta y la acción "Solicitar traspaso". Muestra el equipo del DT (`user.teamName`). **Si `user.teamId` es `null`, oculta o deshabilita "Solicitar traspaso"** y avisa que aún no tiene equipo asignado; el servidor igualmente responde `403` si lo intenta.
   - En el formulario de traspaso del DT, fija el equipo destino a su equipo (no editable). El `ADMIN` elige cualquier destino y es el único que ve "Confirmar" y "Rechazar".
7. **Cerrar sesión**: borra token y usuario en memoria y vuelve a modo visitante.
8. **Retirar la clave fija**: elimina cualquier campo, constante, pantalla o header relacionado con `X-Director-Tecnico-Key` y con `/api/director-tecnico/validar`.

## Criterios de aceptación

- Sin sesión se pueden consultar equipos, jugadores, partidos, traspasos y galería, y no aparece ninguna acción de escritura.
- Un `ADMIN` inicia sesión y puede crear un jugador, solicitar y confirmar un traspaso, y crear un DT con equipo.
- Un `DT` con equipo ve solo la acción de solicitar traspaso hacia su equipo; si fuerza otro destino, el servidor responde `403` y se muestra su mensaje.
- Un `DT` sin equipo no ve la acción de solicitar traspaso.
- Con actividad en pantalla la sesión se mantiene más de 3 minutos; sin actividad por más de 3 minutos la siguiente acción lleva al login con el mensaje de sesión expirada.
- Ninguna parte del front envía ni menciona `X-Director-Tecnico-Key`.
- El token no se guarda en `localStorage` ni `sessionStorage` y no aparece en consola ni en la URL.

## Para probar contra el backend

Orígenes permitidos por CORS: los de desarrollo del front (`5500` y `5173`); el header `Authorization` ya está permitido. Credenciales del administrador: las que definió el dueño del backend en su `.env` (`APP_SECURITY_ADMIN_USERNAME` y `APP_SECURITY_ADMIN_PASSWORD`); pídeselas, no las inventes. Las cuentas de DT las crea el administrador desde la pantalla de usuarios o con `POST /api/users`.
