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
> la clave de Director Técnico, y el estado de partido con cambios de
> jugador) están documentados en las secciones de abajo.

## ¿Qué puede hacer este proyecto?

El proyecto es capaz de:

- Registrar y consultar equipos (búsqueda parcial por nombre, subida del
  escudo del equipo como imagen)
- Registrar y consultar jugadores asociados a un equipo (incluida carga
  en lote)
- Crear, editar y consultar partidos entre dos equipos (búsqueda parcial
  por equipo)
- Registrar alineaciones de jugadores en partidos
- Consultar estadísticas por jugador o partido
- Crear y eliminar registros informativos (tipo blog) para la página de
  Inicio del frontend
- Manejar errores con respuestas consistentes en JSON
- Validar datos de entrada antes de persistirlos
- Exponer la API para consumo desde frontend web o móvil
- Exigir una clave de "Director Técnico" en toda operación de escritura
  (ver sección "Autenticación temporal" más abajo)

## Arquitectura

El proyecto sigue una arquitectura en capas con Spring Boot:

- Capa Controllers: expone endpoints REST
- Capa Services: contiene la lógica de negocio
- Capa Repositories: acceso a datos con Spring Data JPA
- Capa Models / Entities: mapeo de tablas de base de datos
- Capa DTOs: transporte de datos hacia y desde la API
- Capa Mapper: conversión entre entidades y DTOs
- Capa Exceptions: manejo centralizado de errores
- Capa Security: interceptor de la clave de Director Técnico (Fase 7)
- Capa Config: configuración de CORS, archivos estáticos e interceptores

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
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/security](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/security): interceptor de la clave de Director Técnico (Fase 7)
- [mas-que-amigos/src/main/java/com/marin/mas_que_amigos/config](mas-que-amigos/src/main/java/com/marin/mas_que_amigos/config): CORS, archivos estáticos (`/uploads/**`) y registro de interceptores
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
- POST /api/partidos
- PUT /api/partidos (edición; no permite cambiar los equipos de un partido que ya tiene alineación registrada; Fase3-05)
- PUT /api/partidos/{id}/iniciar (pasa el partido de PROGRAMADO a EN_CURSO; Fase3-09)
- PUT /api/partidos/{id}/finalizar (pasa el partido de EN_CURSO a FINALIZADO; Fase3-09)
- DELETE /api/partidos/{id}

### Cambios de jugador (Fase3-09/Fase3-10)

- GET /api/partidos/{idPartido}/cambios (historial de sustituciones del partido, ordenado por minuto)
- POST /api/partidos/{idPartido}/cambios (registra que un jugador titular sale y un suplente del mismo equipo entra, en un minuto dado; el partido debe estar EN_CURSO; al registrarse, voltea los flags `titular` en `Alineacion` para que el suplente que entró quede reconocido como alineado)

### Estadísticas

- GET /api/estadisticas
- GET /api/estadisticas/{id}
- POST /api/estadisticas (el campo `minuto` es opcional; permite anotar en qué minuto del partido ocurrió el gol/tarjeta/asistencia; Fase3-09/Fase3-10)
- DELETE /api/estadisticas/{id}

### Registros informativos (Fase 6 - Configuración)

- GET /api/registros-informativos (del más reciente al más antiguo)
- POST /api/registros-informativos (la fecha de publicación la asigna el servidor)
- DELETE /api/registros-informativos/{id}

No hay PUT: el hallazgo de Fase 6 solo pide crear y eliminar, no editar.

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

## Autenticación temporal: clave de Director Técnico (Fase 7)

Mientras no exista un login real que distinga roles, `DirectorTecnicoWebConfig`
registra un interceptor (`DirectorTecnicoInterceptor`) sobre todo `/api/**`:

- Los métodos de solo consulta (`GET`, `HEAD`, `OPTIONS`) pasan libres.
- Cualquier otro método (`POST`, `PUT`, `DELETE`) exige el header
  `X-Director-Tecnico-Key` con el valor configurado en
  `app.director-tecnico.clave`. Si falta o no coincide, responde `401`
  con el mismo formato de error que el resto de la API
  (`{"indicadorRespuesta": "...", "mensaje": "..."}`).
- El valor por defecto en `application.properties` (`director2025`) es
  solo para desarrollo local. En un despliegue real hay que
  sobreescribirlo con la variable de entorno `APP_DIRECTOR_TECNICO_CLAVE`,
  igual que las credenciales de MySQL (`SPRING_DATASOURCE_*`).

Es una solución intencionalmente temporal (así lo pide el hallazgo de
Fase 7, "constante intencional"): el reemplazo natural a futuro es un
login real con roles, dejando este interceptor como base para agregar
ahí la verificación de sesión/JWT sin tener que tocar cada controlador.

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
  informativos, la clave temporal de Director Técnico, y el estado de
  partido (PROGRAMADO/EN_CURSO/FINALIZADO) con cambios de jugador y
  minuto en las estadísticas (ver secciones de arriba y
  `DIAGNOSTICO_HALLAZGOS.md` en el repo del frontend)

