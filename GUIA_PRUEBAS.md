# Guía Completa de Pruebas - MAS-QUE-AMIGOS

Esta guía te ayudará a probar toda la API de forma completa y ordenada.

## 🚀 Requisitos previos

1. **Herramientas recomendadas:**
   - Postman (descargable desde: https://www.postman.com/downloads/)
   - O curl desde terminal
   - O Insomnia (https://insomnia.rest/)

2. **API en ejecución:**
   ```bash
   cd c:\FICOHSA\RutaAprendizaje\mas-que-amigos\mas-que-amigos
   ./mvnw spring-boot:run
   ```
   
   La API estará disponible en: `http://localhost:57075`

---

## 📊 Flujo de pruebas recomendado

Debes probar en este orden:

1. **Crear equipos** (sin equipos, no puedes crear jugadores ni partidos)
2. **Consultar equipos** (verificar que se guardaron)
3. **Crear jugadores** (asociar a los equipos creados)
4. **Consultar jugadores** (verificar datos)
5. **Crear partidos** (entre equipos existentes)
6. **Crear alineaciones** (jugadores en partidos)
7. **Crear estadísticas** (rendimiento de jugadores)

---

## 🔧 Colección de Requests con CURL

### 1️⃣ EQUIPOS

#### 1.1 Crear equipo #1
```bash
curl -X POST http://localhost:57075/api/equipos \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Deportivo Cali",
    "directorTecnico": "Rafael Dudamel",
    "imagenUrl": "https://example.com/cali.png",
    "titulos": 12,
    "tipoClasificacion": "Primer Division"
  }'
```

**Respuesta esperada:**
- Status: 200 OK
- Body: la entidad completa recién guardada (id, nombre, directorTecnico,
  imagenUrl, titulos, tipoClasificacion, jugadores: [], indicadorRespuesta,
  mensaje). Ya no se devuelven solo indicadorRespuesta/mensaje: los demás
  campos venían en null, así que ahora se retorna la representación completa
  del recurso (buena práctica REST para creación/edición).

#### 1.2 Crear equipo #2
```bash
curl -X POST http://localhost:57075/api/equipos \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Atletico Nacional",
    "directorTecnico": "Paulo Autuori",
    "imagenUrl": "https://example.com/nacional.png",
    "titulos": 16,
    "tipoClasificacion": "Primer Division"
  }'
```

#### 1.3 Listar todos los equipos
```bash
curl -X GET http://localhost:57075/api/equipos \
  -H "Content-Type: application/json"
```

**Respuesta esperada:**
- Status: 200 OK
- Body: Array con los 2 equipos creados

#### 1.4 Obtener equipo por nombre
```bash
curl -X GET http://localhost:57075/api/equipos/Deportivo%20Cali \
  -H "Content-Type: application/json"
```

#### 1.5 Actualizar equipo
```bash
curl -X PUT http://localhost:57075/api/equipos \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "nombre": "Deportivo Cali",
    "directorTecnico": "Rafael Dudamel",
    "imagenUrl": "https://example.com/cali-updated.png",
    "titulos": 13,
    "tipoClasificacion": "Primer Division"
  }'
```

**Respuesta esperada:**
- Status: 200 OK
- Body: la entidad completa con los datos actualizados (incluye jugadores del
  equipo, que nunca se pierden al editar).

---

### 2️⃣ JUGADORES

#### 2.1 Crear jugador en Deportivo Cali
```bash
curl -X POST http://localhost:57075/api/jugadores \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Cristian Benavides",
    "posicion": "DELANTERO",
    "edad": 24,
    "dorsal": 9,
    "idEquipo": 1
  }'
```

**Nota:** El idEquipo=1 debe corresponder al ID retornado al crear el equipo #1. Si es diferente, ajusta.

**Respuesta esperada:**
- Status: 200 OK
- Body: la entidad completa del jugador guardado, incluyendo el detalle del
  equipo real al que pertenece (`equipo`), no solo su id.

#### 2.2 Crear más jugadores en Deportivo Cali
```bash
curl -X POST http://localhost:57075/api/jugadores \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Jairo Torres",
    "posicion": "PORTERO",
    "edad": 29,
    "dorsal": 1,
    "idEquipo": 1
  }'
```

```bash
curl -X POST http://localhost:57075/api/jugadores \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Camilo Vargas",
    "posicion": "DEFENSA",
    "edad": 26,
    "dorsal": 5,
    "idEquipo": 1
  }'
```

```bash
curl -X POST http://localhost:57075/api/jugadores \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Jhon Arias",
    "posicion": "MEDIOCAMPISTA",
    "edad": 25,
    "dorsal": 7,
    "idEquipo": 1
  }'
```

#### 2.3 Crear jugadores en Atletico Nacional
```bash
curl -X POST http://localhost:57075/api/jugadores \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Luis Díaz",
    "posicion": "DELANTERO",
    "edad": 27,
    "dorsal": 19,
    "idEquipo": 2
  }'
```

```bash
curl -X POST http://localhost:57075/api/jugadores \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Franco Armani",
    "posicion": "PORTERO",
    "edad": 32,
    "dorsal": 1,
    "idEquipo": 2
  }'
```

```bash
curl -X POST http://localhost:57075/api/jugadores \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Andrés Andrade",
    "posicion": "DEFENSA",
    "edad": 28,
    "dorsal": 6,
    "idEquipo": 2
  }'
```

```bash
curl -X POST http://localhost:57075/api/jugadores \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Alexis Zapata",
    "posicion": "MEDIOCAMPISTA",
    "edad": 26,
    "dorsal": 4,
    "idEquipo": 2
  }'
```

#### 2.3.1 Crear varios jugadores en una sola petición (lote)

`POST /api/jugadores/batch` recibe un arreglo de jugadores y los procesa de
forma **optimista, uno por uno**: si alguno falla, los demás se guardan
igual (no se revierte el lote completo). Útil para cargar la plantilla
completa de un equipo de una sola vez. Tamaño máximo: 50 jugadores por
petición.

```bash
curl -X POST http://localhost:57075/api/jugadores/batch \
  -H "Content-Type: application/json" \
  -d '[
    { "nombre": "James Rodríguez", "posicion": "MEDIOCAMPISTA", "edad": 33, "dorsal": 10, "idEquipo": 1 },
    { "nombre": "Sin Dorsal Válido", "posicion": "DEFENSA", "edad": 24, "dorsal": 0, "idEquipo": 1 },
    { "nombre": "Davinson Sánchez", "posicion": "DEFENSA", "edad": 27, "dorsal": 5, "idEquipo": 1 }
  ]'
```

**Respuesta esperada:**
- Status: 200 OK (el status 200 no significa que TODOS se hayan creado — el
  detalle real está en `resultados`)
- Body: un resumen (`total`, `exitosos`, `fallidos`) más el detalle de cada
  jugador según su posición en el arreglo enviado. En este ejemplo el
  segundo jugador falla (dorsal 0 es inválido) y los otros dos sí se crean:

```json
{
  "total": 3,
  "exitosos": 2,
  "fallidos": 1,
  "resultados": [
    { "indice": 0, "exito": true, "jugador": { "id": 1, "nombre": "James Rodríguez", "...": "..." } },
    { "indice": 1, "exito": false, "error": "dorsal: El dorsal debe ser mayor a 0." },
    { "indice": 2, "exito": true, "jugador": { "id": 2, "nombre": "Davinson Sánchez", "...": "..." } }
  ]
}
```

Solo responde 400 (sin crear nada) si el arreglo llega vacío o si trae más
de 50 jugadores.

#### 2.4 Listar todos los jugadores
```bash
curl -X GET http://localhost:57075/api/jugadores \
  -H "Content-Type: application/json"
```

#### 2.5 Obtener jugador por nombre
```bash
curl -X GET http://localhost:57075/api/jugadores/Luis%20D%C3%ADaz \
  -H "Content-Type: application/json"
```

#### 2.6 Actualizar jugador
```bash
curl -X PUT http://localhost:57075/api/jugadores \
  -H "Content-Type: application/json" \
  -d '{
    "id": 1,
    "nombre": "Cristian Benavides",
    "posicion": "DELANTERO",
    "edad": 25,
    "dorsal": 9,
    "idEquipo": 1
  }'
```

**Respuesta esperada:**
- Status: 200 OK
- Body: la entidad completa del jugador con los datos actualizados.

---

### 3️⃣ PARTIDOS

#### 3.1 Crear partido
```bash
curl -X POST http://localhost:57075/api/partidos \
  -H "Content-Type: application/json" \
  -d '{
    "idEquipoLocal": 1,
    "idEquipoVisitante": 2,
    "fecha": "2026-08-25",
    "hora": "19:00:00",
    "golesLocal": 0,
    "golesVisitante": 0,
    "fase": "FASE_DE_GRUPOS"
  }'
```

**Nota:** Usa los IDs reales de los equipos. Ajusta si es necesario. El campo
`fase` debe ser uno de: `FASE_DE_GRUPOS`, `REPECHAJE`, `ELIMINACION_DIRECTA`,
`FINAL` (si se omite, se asume `FASE_DE_GRUPOS`).

**Respuesta esperada:**
- Status: 200 OK
- Body: la entidad completa del partido, incluyendo el detalle de
  `equipoLocal` y `equipoVisitante` (no solo sus ids).

#### 3.2 Crear otro partido
```bash
curl -X POST http://localhost:57075/api/partidos \
  -H "Content-Type: application/json" \
  -d '{
    "idEquipoLocal": 2,
    "idEquipoVisitante": 1,
    "fecha": "2026-09-01",
    "hora": "20:00:00",
    "golesLocal": 0,
    "golesVisitante": 0,
    "fase": "REPECHAJE"
  }'
```

#### 3.3 Listar todos los partidos
```bash
curl -X GET http://localhost:57075/api/partidos \
  -H "Content-Type: application/json"
```

#### 3.4 Buscar partidos por equipo
```bash
curl -X GET http://localhost:57075/api/partidos/Deportivo%20Cali \
  -H "Content-Type: application/json"
```

---

### 4️⃣ ESTADÍSTICAS

#### 4.1 Crear estadística
```bash
curl -X POST http://localhost:57075/api/estadisticas \
  -H "Content-Type: application/json" \
  -d '{
    "idJugador": 1,
    "idPartido": 1,
    "goles": 2,
    "tarjetasAmarillas": 0,
    "tarjetasRojas": 0,
    "asistencias": 1
  }'
```

**Nota:** Ajusta los IDs según los datos que tengas.

#### 4.2 Crear más estadísticas
```bash
curl -X POST http://localhost:57075/api/estadisticas \
  -H "Content-Type: application/json" \
  -d '{
    "idJugador": 5,
    "idPartido": 1,
    "goles": 1,
    "tarjetasAmarillas": 1,
    "tarjetasRojas": 0,
    "asistencias": 0
  }'
```

#### 4.3 Listar todas las estadísticas
```bash
curl -X GET http://localhost:57075/api/estadisticas \
  -H "Content-Type: application/json"
```

#### 4.4 Obtener estadística por ID
```bash
curl -X GET http://localhost:57075/api/estadisticas/1 \
  -H "Content-Type: application/json"
```

---

## 📦 Colección Postman (JSON)

Puedes importar este JSON directamente en Postman para tener todo organizado.

Guarda este contenido en un archivo llamado `MasQueAmigos-Collection.postman_collection.json`:

```json
{
  "info": {
    "name": "Mas Que Amigos API",
    "description": "Colección completa para probar la API de gestión deportiva",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Equipos",
      "item": [
        {
          "name": "Crear Equipo",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\"nombre\": \"Deportivo Cali\", \"directorTecnico\": \"Rafael Dudamel\", \"imagenUrl\": \"https://example.com/cali.png\", \"titulos\": 12, \"tipoClasificacion\": \"Primer Division\"}"
            },
            "url": {"raw": "http://localhost:57075/api/equipos", "protocol": "http", "host": ["localhost"], "port": ["8080"], "path": ["api", "equipos"]}
          }
        },
        {
          "name": "Listar Equipos",
          "request": {
            "method": "GET",
            "header": [],
            "url": {"raw": "http://localhost:57075/api/equipos", "protocol": "http", "host": ["localhost"], "port": ["8080"], "path": ["api", "equipos"]}
          }
        },
        {
          "name": "Obtener Equipo por Nombre",
          "request": {
            "method": "GET",
            "header": [],
            "url": {"raw": "http://localhost:57075/api/equipos/Deportivo Cali", "protocol": "http", "host": ["localhost"], "port": ["8080"], "path": ["api", "equipos", "Deportivo Cali"]}
          }
        }
      ]
    },
    {
      "name": "Jugadores",
      "item": [
        {
          "name": "Crear Jugador",
          "request": {
            "method": "POST",
            "header": [{"key": "Content-Type", "value": "application/json"}],
            "body": {
              "mode": "raw",
              "raw": "{\"nombre\": \"Cristian Benavides\", \"posicion\": \"DELANTERO\", \"edad\": 24, \"dorsal\": 9, \"idEquipo\": 1}"
            },
            "url": {"raw": "http://localhost:57075/api/jugadores", "protocol": "http", "host": ["localhost"], "port": ["8080"], "path": ["api", "jugadores"]}
          }
        },
        {
          "name": "Listar Jugadores",
          "request": {
            "method": "GET",
            "header": [],
            "url": {"raw": "http://localhost:57075/api/jugadores", "protocol": "http", "host": ["localhost"], "port": ["8080"], "path": ["api", "jugadores"]}
          }
        }
      ]
    }
  ]
}
```

---

## ⚠️ Casos de error esperados (Testing de validación)

Estos son intentos que **deberían fallar** con validaciones:

### ❌ Equipo sin nombre
```bash
curl -X POST http://localhost:57075/api/equipos \
  -H "Content-Type: application/json" \
  -d '{
    "directorTecnico": "Rafael Dudamel",
    "imagenUrl": "https://example.com/cali.png",
    "titulos": 12,
    "tipoClasificacion": "Primer Division"
  }'
```
**Esperado:** Status 400 Bad Request

### ❌ Jugador sin posición
```bash
curl -X POST http://localhost:57075/api/jugadores \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Cristian Benavides",
    "edad": 24,
    "dorsal": 9,
    "idEquipo": 1
  }'
```
**Esperado:** Status 400 Bad Request

### ❌ Jugador con edad inválida
```bash
curl -X POST http://localhost:57075/api/jugadores \
  -H "Content-Type: application/json" \
  -d '{
    "nombre": "Cristian Benavides",
    "posicion": "DELANTERO",
    "edad": 0,
    "dorsal": 9,
    "idEquipo": 1
  }'
```
**Esperado:** Status 400 Bad Request

### ❌ Eliminar equipo inexistente
```bash
curl -X DELETE http://localhost:57075/api/equipos/99999 \
  -H "Content-Type: application/json"
```
**Esperado:** Status 404 Not Found

---

## 🧪 Checklist de verificación

Marca ✅ cada punto conforme lo pruebas:

### Equipos
- [ ] Crear equipo (Status 200)
- [ ] Listar equipos (Status 200, array no vacío)
- [ ] Obtener equipo por nombre (Status 200)
- [ ] Actualizar equipo (Status 200)
- [ ] Eliminar equipo (Status 204)
- [ ] Eliminar equipo inexistente (Status 404)

### Jugadores
- [ ] Crear jugador (Status 200)
- [ ] Crear jugadores en lote, todos válidos (Status 200, todos exitosos)
- [ ] Crear jugadores en lote con uno inválido (Status 200, el resto se crea igual)
- [ ] Listar jugadores (Status 200, array no vacío)
- [ ] Obtener jugador por nombre (Status 200, array)
- [ ] Actualizar jugador (Status 200)
- [ ] Eliminar jugador (Status 204)
- [ ] Crear jugador sin equipo válido (Status 404)

### Partidos
- [ ] Crear partido (Status 200)
- [ ] Listar partidos (Status 200, array no vacío)
- [ ] Buscar partidos por equipo (Status 200)
- [ ] Eliminar partido (Status 204)

### Estadísticas
- [ ] Crear estadística (Status 200)
- [ ] Listar estadísticas (Status 200)
- [ ] Obtener estadística por ID (Status 200)
- [ ] Eliminar estadística (Status 204)

### Validaciones
- [ ] Equipo sin nombre (Status 400)
- [ ] Jugador con edad 0 (Status 400)
- [ ] Jugador sin posición (Status 400)
- [ ] Eliminar recurso inexistente (Status 404)

---

## 📝 Notas importantes

1. **IDs dinámicos:** Los IDs se generan automáticamente. Cuando crees un recurso, guarda su ID para usarlo en las siguientes operaciones.

2. **Orden de ejecución:** Siempre crea equipos primero, luego jugadores, luego partidos.

3. **Validación:** La API rechazará datos inválidos con Status 400 y un mensaje de error.

3.1. **Formato de respuesta en creación/edición:** los endpoints POST y PUT
   devuelven la entidad completa recién guardada/actualizada (no solo un
   mensaje de éxito), siguiendo la práctica REST estándar. Los campos
   `indicadorRespuesta`/`mensaje` se mantienen por compatibilidad con el
   frontend actual. Los DELETE devuelven `204 No Content` sin body.

4. **Base de datos:** desde la Fase 3, la aplicación corre contra **MySQL**
   (Flyway gestiona el esquema) — los datos ya persisten entre reinicios.
   H2 en memoria quedó exclusivamente para `mvn test`; ya no se usa al
   levantar la app con `spring-boot:run` ni con Docker Compose (ver
   `DEPLOYMENT.md`).

5. **Ver los datos:** con MySQL no hay consola web integrada como con H2;
   usa tu cliente de MySQL preferido (MySQL Workbench, DBeaver, la CLI
   `mysql`, etc.) apuntando a la base configurada por las variables
   `SPRING_DATASOURCE_*` (ver `DEPLOYMENT.md`).

---

## 🖥️ Pruebas end-to-end desde el frontend

Además de probar la API directamente (arriba), la Fase 5 agregó un frontend
completo (repositorio hermano `MasQueAmigos-Torneo`) que cubre Equipos,
Jugadores (incluida la carga en lote), Partidos y Estadísticas. Para un
flujo de negocio real de punta a punta:

1. Levanta este backend contra MySQL (`./mvnw spring-boot:run` con las 4
   variables `SPRING_DATASOURCE_*` exportadas, o `docker compose up`).
2. Sirve el frontend con Live Server (VS Code) u otro servidor estático en
   `http://127.0.0.1:5500` (ya permitido en `CorsConfig.java`).
3. Desde la interfaz: crea un equipo → agrégale jugadores (uno por uno o en
   lote desde "Cargar varios jugadores") → programa un partido entre dos
   equipos → en Alineaciones, alinea a algunos jugadores de ambos equipos en
   ese partido → registra una estadística de uno de esos jugadores alineados
   (el select de Estadísticas solo ofrece jugadores ya alineados en el
   partido elegido) → confirma que Inicio refleja los nuevos contadores.

Ver el `README.md` del frontend para el detalle de arquitectura y las
decisiones de alcance (por ejemplo, por qué no hay pantalla de Alineaciones).

## 🔗 URLs de referencia rápida

- API Base: `http://localhost:57075`
- Equipos: `http://localhost:57075/api/equipos`
- Jugadores: `http://localhost:57075/api/jugadores`
- Partidos: `http://localhost:57075/api/partidos`
- Estadísticas: `http://localhost:57075/api/estadisticas`
- Consola H2: `http://localhost:57075/h2-console`

