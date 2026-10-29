# MAS-QUE-AMIGOS

Backend de una aplicación deportiva para gestionar equipos, jugadores, partidos, alineaciones, estadísticas y registros informativos (tipo blog) de una competición o liga. La API está desarrollada con Spring Boot y expone endpoints REST para crear, consultar, actualizar y eliminar registros de un sistema de fútbol amateur o semiprofesional.

> **Auditoría de hallazgos (Fases 1 a 7):** este backend y su frontend
> (`MasQueAmigos-Torneo`, repo hermano) pasaron por una auditoría completa
> contra `FRONTEND_VISION.md` (32 hallazgos en 7 fases), que terminó con
> los 32 hallazgos en estado Completado. El detalle hallazgo por
> hallazgo — qué se resolvió y por qué — está en
> `DIAGNOSTICO_HALLAZGOS.md`, en el repo del frontend. Los cambios de
> backend que salieron de esa auditoría (subida de escudo y de foto de
> jugador, búsqueda parcial, edición de partidos, registros informativos,
> el control de acceso de escrituras (hoy login con JWT y roles), y el estado de partido con cambios de
> jugador) están documentados en las secciones de abajo.

## ¿Qué puede hacer este proyecto?

El proyecto es capaz de:

- Registrar y consultar equipos (búsqueda parcial por nombre, subida del
  escudo del equipo como imagen)
- Registrar y consultar jugadores asociados a un equipo (incluida carga
  en lote)
- Crear, editar y consultar partidos entre dos equipos (búsqueda parcial
  por equipo), exigiendo 7 titulares por equipo para iniciarlos y
  bloqueando la edición/eliminación de uno ya finalizado
- Registrar, editar y quitar alineaciones de jugadores en partidos,
  individualmente o en lote, mientras el partido no haya iniciado
- Registrar, editar y eliminar estadísticas por jugador y partido (un
  solo registro por jugador+partido), reflejando siempre el marcador del
  partido correspondiente
- Crear, editar y eliminar registros informativos (tipo blog) para la
  página de Inicio del frontend
- Administrar una galería de fotos para el carrusel de la página de
  Inicio (subir, listar y eliminar)
- Manejar errores con respuestas consistentes en JSON
- Validar datos de entrada antes de persistirlos
- Exponer la API para consumo desde frontend web o móvil
- Iniciar sesión con usuario y contraseña y exigir un rol (administrador o
  Director Técnico) en las operaciones de escritura
  (ver sección "Autenticación y roles" más abajo)

## Arquitectura

El proyecto sigue una arquitectura en capas con Spring Boot:

- Capa Controllers: expone endpoints REST
- Capa Services: contiene la lógica de negocio
- Capa Repositories: acceso a datos con Spring Data JPA
- Capa Models / Entities: mapeo de tablas de base de datos
- Capa DTOs: transporte de datos hacia y desde la API
- Capa Mapper: conversión entre entidades y DTOs
- Capa Exceptions: manejo centralizado de errores
- Capa Security: login con JWT y autorización por rol (Spring Security)
- Capa Config: configuración de CORS, archivos estáticos y OpenAPI

## Tecnologías utilizadas

- Java 8
- Spring Boot 2.7.18
- Spring Web (incluye MVC, validación e interceptores)
- Spring Data JPA
- Spring Validation
- Flyway (versionado del esquema, ver `src/main/resources/db/migration`)
- H2 Database (por defecto para pruebas y desarrollo)
- MySQL (soporte para entorno real)
- Lombok
- Maven

## Estructura principal

- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/controller](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/controller): endpoints REST
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/service](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/service): lógica de negocio
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/repository](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/repository): repositorios JPA
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/model](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/model): entidades
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/dto](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/dto): objetos de transferencia
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/exception](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/exception): manejo centralizado de errores
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/security](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/security): autenticación JWT, reglas de autorización por rol y sembrado del administrador
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/config](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/config): CORS, archivos estáticos (`/uploads/**`) y documentación OpenAPI
- [mas-que-amigos/src/main/resources/db/migration](mas-que-amigos/src/main/resources/db/migration): migraciones Flyway del esquema

## Requisitos

- JDK 8+
- Maven
- Git

## Ejecución

Desde la carpeta del proyecto:

```bash
./mvnw spring-boot:run
```

La aplicación queda disponible en:

- http://localhost:8080
- Consola H2: http://localhost:8080/h2-console

## Endpoints principales

### Equipos

- GET /api/equipos
- GET /api/equipos/{nombre} (coincidencia exacta)
- GET /api/equipos/buscar?nombre=... (coincidencia parcial, sin distinguir mayúsculas; Fase1-05)
- POST /api/equipos
- POST /api/equipos/{id}/imagen (multipart/form-data, campo `imagen`; sube el escudo del equipo y lo sirve luego en `/uploads/**`; Fase1-01)
- PUT /api/equipos
- DELETE /api/equipos/{id}

### Jugadores

- GET /api/jugadores
- GET /api/jugadores/{nombre}
- POST /api/jugadores
- POST /api/jugadores/batch (creación en lote, ver ejemplo más abajo; cada jugador del lote puede ir a un `idEquipo` distinto)
- POST /api/jugadores/{id}/imagen (multipart/form-data, campo `imagen`; sube la foto del jugador con el mismo mecanismo que el escudo del equipo; Fase2-07)
- PUT /api/jugadores
- DELETE /api/jugadores/{id}

### Partidos

- GET /api/partidos
- GET /api/partidos/{nombre} (coincidencia exacta)
- GET /api/partidos/buscar?nombre=... (coincidencia parcial por nombre de alguno de los dos equipos; Fase3-04)
- POST /api/partidos (los goles no se reciben en el payload: `golesLocal`/`golesVisitante` son de solo lectura y se calculan a partir de Estadística; Fase9-01)
- PUT /api/partidos (edición; no permite cambiar los equipos de un partido que ya tiene alineación registrada, Fase3-05; rechazada si el partido ya está FINALIZADO, Fase9-01)
- PUT /api/partidos/{id}/iniciar (pasa el partido de PROGRAMADO a EN_CURSO; exige al menos 7 titulares por equipo, Fase3-09/Fase9-01)
- PUT /api/partidos/{id}/finalizar (pasa el partido de EN_CURSO a FINALIZADO; Fase3-09)
- DELETE /api/partidos/{id} (rechazada si el partido ya está FINALIZADO; si se permite, borra en cascada su alineación, estadísticas y cambios de jugador; Fase9-01)

### Alineaciones

- GET /api/alineaciones/partido/{idPartido} (jugadores alineados en un partido, titulares y suplentes)
- POST /api/alineaciones (alinea a un jugador; valida que no esté repetido en el partido y que el equipo no supere 11 titulares)
- PUT /api/alineaciones/{idPartido}/{idJugador} (cambia a un jugador de titular a suplente o viceversa; solo mientras el partido está PROGRAMADO; revalida el cupo de 11 titulares únicamente si la transición es hacia titular; Fase9-01)
- DELETE /api/alineaciones/{idPartido}/{idJugador} (quita a un jugador de la alineación; solo mientras el partido está PROGRAMADO; Fase9-01)

### Cambios de jugador (Fase3-09/Fase3-10)

- GET /api/partidos/{idPartido}/cambios (historial de sustituciones del partido, ordenado por minuto)
- POST /api/partidos/{idPartido}/cambios (registra que un jugador titular sale y un suplente del mismo equipo entra, en un minuto dado; el partido debe estar EN_CURSO; al registrarse, voltea los flags `titular` en `Alineacion` para que el suplente que entró quede reconocido como alineado)

### Estadísticas

- GET /api/estadisticas
- GET /api/estadisticas/{id}
- GET /api/estadisticas/buscar?idJugador=&idPartido= (devuelve el registro existente de ese jugador en ese partido, o 204 si no hay uno; para que el cliente ofrezca editar en vez de duplicar; Fase9-01)
- POST /api/estadisticas (el campo `minuto` es opcional; permite anotar en qué minuto del partido ocurrió el gol/tarjeta/asistencia, Fase3-09/Fase3-10; rechazada si el partido está PROGRAMADO o si ya existe un registro para ese jugador+partido, Fase9-01; recalcula los goles del partido al guardar)
- PUT /api/estadisticas/{id} (edita un registro existente; valida que `idJugador`/`idPartido` del body coincidan con el registro; recalcula los goles del partido al guardar; Fase9-01)
- DELETE /api/estadisticas/{id} (recalcula los goles del partido tras eliminar; Fase9-01)

### Registros informativos (Fase 6 - Configuración)

- GET /api/registros-informativos (del más reciente al más antiguo)
- POST /api/registros-informativos (la fecha de publicación la asigna el servidor)
- PUT /api/registros-informativos/{id} (edita título y contenido; la fecha de publicación original no se toca; Fase9-01)
- DELETE /api/registros-informativos/{id}

### Galería de Inicio (Fase 9)

- GET /api/galeria (fotos del carrusel de la página de Inicio, sin orden garantizado más allá de `fechaSubida` descendente)
- POST /api/galeria (multipart/form-data, campo `imagen`; sube una foto nueva a la galería con el mismo mecanismo que el escudo de Equipo, sirviéndola luego en `/uploads/**`; a diferencia de la imagen de un equipo, cada subida crea un registro nuevo en vez de reemplazar uno existente, porque la galería es una lista)
- DELETE /api/galeria/{id} (borra el registro y, de forma best-effort, el archivo físico correspondiente)

## Ejemplo de payload para equipo

```json
{
  "nombre": "Nacional",
  "directorTecnico": "Rueda",
  "imagenUrl": "https://example.com/escudo.png",
  "titulos": 10,
  "tipoClasificacion": "ELIMINATORIA"
}
```

## Ejemplo de payload para jugador

```json
{
  "nombre": "Alex",
  "posicion": "DELANTERO",
  "edad": 26,
  "dorsal": 11,
  "idEquipo": 1
}
```

## Creación de jugadores en lote

`POST /api/jugadores/batch` permite registrar varios jugadores en una sola
petición (por ejemplo, cargar la plantilla completa de un equipo). El
procesamiento es **optimista, jugador por jugador**: si uno falla (datos
inválidos, equipo inexistente o dorsal duplicado), no afecta a los demás —
el lote sigue procesando el resto y la respuesta detalla el resultado
individual de cada jugador según su posición en el arreglo enviado. El
tamaño máximo del lote es 50 jugadores.

Ejemplo de request (el arreglo se envía directamente como body, sin
envoltorio):

```json
[
  { "nombre": "Alex", "posicion": "DELANTERO", "edad": 26, "dorsal": 11, "idEquipo": 1 },
  { "nombre": "Sin Dorsal", "posicion": "DEFENSA", "edad": 22, "dorsal": 0, "idEquipo": 1 },
  { "nombre": "Marino", "posicion": "MEDIOCAMPISTA", "edad": 24, "dorsal": 8, "idEquipo": 1 }
]
```

Ejemplo de response (200 OK; el segundo jugador falló por dorsal inválido,
pero el primero y el tercero sí se crearon):

```json
{
  "total": 3,
  "exitosos": 2,
  "fallidos": 1,
  "resultados": [
    { "indice": 0, "exito": true, "jugador": { "id": 1, "nombre": "Alex", "posicion": "DELANTERO", "edad": 26, "dorsal": 11, "idEquipo": 1, "equipo": { "id": 1, "nombre": "Nacional" } } },
    { "indice": 1, "exito": false, "error": "dorsal: El dorsal debe ser mayor a 0." },
    { "indice": 2, "exito": true, "jugador": { "id": 2, "nombre": "Marino", "posicion": "MEDIOCAMPISTA", "edad": 24, "dorsal": 8, "idEquipo": 1, "equipo": { "id": 1, "nombre": "Nacional" } } }
  ]
}
```

Solo se responde 400 (sin procesar nada) si el lote llega vacío o supera
los 50 jugadores; para todo lo demás, la petición responde 200 y el detalle
por jugador va en `resultados`.

## Subida de imágenes (escudo de equipo y foto de jugador)

`POST /api/equipos/{id}/imagen` recibe un archivo (`multipart/form-data`,
campo `imagen`, máximo 5MB) y lo guarda en disco bajo el directorio
configurado en `app.uploads.dir` (por defecto `uploads`, relativo a donde
corra el proceso). El archivo queda accesible públicamente en
`/uploads/<nombre-generado>` (ver `config/StaticResourceConfig.java`), y
esa URL absoluta (usando `app.base-url`) se guarda en `Equipo.imagenUrl`.
Antes de este hallazgo (Fase1-01) el backend no tenía ninguna capacidad de
archivos: ni multipart configurado ni servido de estáticos.

`POST /api/jugadores/{id}/imagen` (Fase2-07, bonus) funciona igual pero
guarda la foto del jugador en `uploads/jugadores/` y la URL resultante en
`Jugador.imagenUrl`; reutiliza `app.uploads.dir`/`app.base-url` y el mismo
`StaticResourceConfig` sin necesitar cambios ahí.

## Autenticación y roles (login con JWT)

El login real reemplazó a la clave temporal de Director Técnico de la Fase 7.
La API usa Spring Security con tokens JWT de corta vida enviados en el header
`Authorization: Bearer <token>`.

**Endpoints de sesión**

- `POST /api/auth/login`: usuario y contraseña (el usuario no distingue
  mayúsculas); devuelve `token`, `tokenType`, `expiresInSeconds` y los datos
  del usuario. Es público.
- `POST /api/auth/refresh`: con un token vigente entrega uno nuevo con la
  vigencia completa. Un token vencido no se puede renovar.
- `GET /api/auth/me`: devuelve quién es la persona del token.
- `POST /api/users`, `GET /api/users` y `PUT /api/users/{id}/team`: gestión
  de cuentas y del equipo de cada DT; solo para `ADMIN`.

**Roles**

| Rol | Qué puede hacer |
|---|---|
| Visitante (sin sesión) | Solo consultas (`GET`) |
| `DT` | Consultas y solicitar traspasos hacia su propio equipo |
| `ADMIN` | Todo, incluida la gestión de usuarios |

Todo endpoint de escritura nuevo exige `ADMIN` por defecto. Las respuestas de
acceso denegado mantienen el formato `{"indicadorRespuesta": "...",
"mensaje": "..."}`: `401` cuando falta, venció o es inválido el token, y `403`
cuando el rol no alcanza.

**Sesión**: el token dura 3 minutos por defecto. El front lo renueva mientras
haya actividad del usuario en pantalla; si deja de renovarlo, la sesión se
cierra sola.

**Configuración (variables de entorno)**

- `APP_SECURITY_JWT_SECRET`: secreto de firma de los tokens, de 32 caracteres
  o más. Sin valor por defecto: si falta o es corto, la aplicación no arranca.
- `APP_SECURITY_ADMIN_USERNAME` y `APP_SECURITY_ADMIN_PASSWORD`: cuenta de
  administrador que se crea al arrancar si no existe. Sin valor por defecto.
- `APP_SECURITY_JWT_TTL` (opcional): vigencia del token en formato ISO-8601,
  por defecto `PT3M`.

Los valores reales van solo en el `.env` (que no se sube al repositorio); en
`.env.example` quedan los marcadores. Las cuentas de DT las crea el
administrador y no hay auto-registro. El detalle del contrato está en
`mas-que-amigos/docs/api-spec.yml` y las reglas de seguridad en
`mas-que-amigos/docs/backend-standards.md`.

## Estado del proyecto

La aplicación está lista como API REST funcional para gestionar una liga deportiva con flujo básico de CRUD, validación de entrada y manejo de errores. Puede integrarse con un frontend para una experiencia completa de gestión deportiva.

## Frontend

El frontend web vive en un repositorio aparte, `MasQueAmigos-Torneo`
(carpeta hermana de este backend), y ya consume Equipos, Jugadores
(incluida la creación en lote), Partidos, Alineaciones, Estadísticas y,
desde la Fase 6, Registros informativos (Configuración) de extremo a
extremo. Es HTML/CSS/JS sin framework ni build — ver el `README.md` de ese
repo para el detalle de arquitectura, y `DEPLOYMENT.md` (en este backend)
para cómo levantar la API. En resumen, para probar todo junto:

1. Levanta este backend (`./mvnw spring-boot:run` o `docker compose up`, ver `DEPLOYMENT.md`).
2. Sirve la carpeta del frontend con Live Server (VS Code) u otro servidor
   estático — el backend ya tiene habilitado CORS para
   `http://127.0.0.1:5500` / `http://localhost:5500` / `http://localhost:5173`
   (ver `CorsConfig.java`).
3. Ajusta `js/config.js` del frontend si el backend no corre en
   `http://localhost:57075`.

## Mejoras aplicadas

- Corrección de la dependencia de validación incompatible con Spring Boot 2.7
- Activación de validaciones en DTOs y controladores
- Estandarización de rutas bajo /api
- Manejo más consistente de errores
- Documentación centralizada para uso y despliegue
- Auditoría completa de 32 hallazgos de `FRONTEND_VISION.md` (Fases 1-7),
  con los 32 en estado Completado: subida de imágenes (escudo y foto de
  jugador), búsqueda parcial, edición de partidos, registros
  informativos, el control de acceso de escrituras (hoy login con JWT y
  roles, ver "Autenticación y roles"), y el estado de
  partido (PROGRAMADO/EN_CURSO/FINALIZADO) con cambios de jugador y
  minuto en las estadísticas (ver secciones de arriba y
  `DIAGNOSTICO_HALLAZGOS.md` en el repo del frontend)
- Fase 9: reglas de negocio sobre Partidos (mínimo de titulares para
  iniciar, bloqueo de edición/eliminación al finalizar, goles derivados
  de Estadística, borrado en cascada), edición/eliminación de
  Alineaciones y Estadísticas (con recálculo automático del marcador),
  edición de Registros informativos, y una galería de fotos administrable
  para el carrusel de Inicio (ver secciones de arriba y la sección
  "Fase 9" de `DIAGNOSTICO_HALLAZGOS.md` en el repo del frontend)

