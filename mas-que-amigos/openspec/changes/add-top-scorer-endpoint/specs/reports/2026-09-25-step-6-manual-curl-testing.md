# Reporte del Paso 6 - Testing manual del endpoint con curl

- Fecha: 2026-09-25
- Change: add-top-scorer-endpoint
- Agente: Claude Sonnet 5 (Claude Code)

## Entorno de pruebas

- Backend local levantado con `./mvnw -o spring-boot:run -Dspring-boot.run.jvmArguments="-Dserver.port=57076"`, apuntando (vía `SPRING_DATASOURCE_*`) al MySQL del contenedor `TorneoContainer` (`localhost:3308`, base `masqueamigos`). Se usó el puerto 57076 en vez del 57075 porque ese ya estaba ocupado por el contenedor `mas-que-amigos-backend-1` (jar antiguo, sin este endpoint).
- Baseline de la base de datos antes de las pruebas: `estadistica`=0, `jugador`=21, `partido`=1 (`FINALIZADO`), `equipos`=4.
- Jugadores usados: id 1 "Andres" (equipo 1, Macalister), id 5 "Yeiler" (equipo 2, Socios.com) — de equipos distintos, para cubrir el escenario "sin importar el equipo".

## Comandos ejecutados y respuestas

1. **Sin goles registrados** (antes de crear datos de prueba):
   - `curl -s -w "\nHTTP_STATUS:%{http_code}\n" http://localhost:57076/api/estadisticas/goleador`
   - Respuesta: `[]`, `HTTP_STATUS:200`

2. **Crear estadística del jugador 1** (5 goles, partido 1):
   - `curl -X POST http://localhost:57076/api/estadisticas -H "Content-Type: application/json" -H "X-Director-Tecnico-Key: director2025" -d '{"idJugador":1,"idPartido":1,"goles":5,"tarjetasAmarillas":0,"tarjetasRojas":0,"asistencias":0}'`
   - Respuesta: `{"id":6,"idJugador":1,"idPartido":1,"goles":5,...}`, `HTTP_STATUS:200`

3. **Crear estadística del jugador 5** (3 goles, partido 1, equipo distinto):
   - Mismo comando con `idJugador=5, goles=3`
   - Respuesta: `{"id":7,"idJugador":5,"idPartido":1,"goles":3,...}`, `HTTP_STATUS:200`

4. **Un único goleador**:
   - `curl -s -w "..." http://localhost:57076/api/estadisticas/goleador`
   - Respuesta: `[{"idJugador":1,"nombreJugador":"Andres","idEquipo":1,"nombreEquipo":"Macalister","totalGoles":5}]`, `HTTP_STATUS:200` — coincide con el escenario "un jugador tiene más goles que todos los demás" de `specs/top-scorer/spec.md`.

5. **Provocar empate** (editar la estadística del jugador 5 a 5 goles):
   - `curl -X PUT http://localhost:57076/api/estadisticas/7 -H "Content-Type: application/json" -H "X-Director-Tecnico-Key: director2025" -d '{"idJugador":5,"idPartido":1,"goles":5,"tarjetasAmarillas":0,"tarjetasRojas":0,"asistencias":0}'`
   - Respuesta: `{"id":7,"idJugador":5,"idPartido":1,"goles":5,...}`, `HTTP_STATUS:200`

6. **Empate en el máximo de goles**:
   - `curl -s -w "..." http://localhost:57076/api/estadisticas/goleador`
   - Respuesta: `[{"idJugador":1,"nombreJugador":"Andres","idEquipo":1,"nombreEquipo":"Macalister","totalGoles":5},{"idJugador":5,"nombreJugador":"Yeiler","idEquipo":2,"nombreEquipo":"Socios.com","totalGoles":5}]`, `HTTP_STATUS:200` — coincide con el escenario "dos o más jugadores empatan en el máximo de goles" (ambos jugadores, de equipos distintos, devueltos).

7. **Limpieza** (eliminar los registros de prueba):
   - `curl -X DELETE http://localhost:57076/api/estadisticas/6 -H "X-Director-Tecnico-Key: director2025"` → `HTTP_STATUS:204`
   - `curl -X DELETE http://localhost:57076/api/estadisticas/7 -H "X-Director-Tecnico-Key: director2025"` → `HTTP_STATUS:204`

8. **Verificación final (sin goles registrados, tras limpieza)**:
   - `curl -s -w "..." http://localhost:57076/api/estadisticas/goleador`
   - Respuesta: `[]`, `HTTP_STATUS:200`

## Casos de error

- No se probaron casos de error específicos del nuevo endpoint porque `GET /api/estadisticas/goleador` no recibe parámetros de entrada (no hay validación posible de payload/path variable); el único "caso de error" observable es la ausencia de goles, cubierto en los pasos 1 y 8 (lista vacía, no un error HTTP).
- Se confirmó de paso que el endpoint no colisiona con el mapeo existente `GET /api/estadisticas/{id}` (patrón ya usado por `/buscar` en este mismo controller).

## Verificación del estado de la base de datos

- Antes de las pruebas: `estadistica`=0, `jugador`=21, `partido`=1, `equipos`=4.
- Después de la limpieza: `estadistica`=0, `jugador`=21, `partido`=1, `equipos`=4 (idéntico; solo avanzó el autoincremental interno de `estadistica`, sin filas remanentes).
- Estado restaurado: Sí, mediante los propios `DELETE /api/estadisticas/{id}` de los registros creados.

## Resultado

- Estado del Paso N+2: PASS
- Problemas bloqueantes: ninguno.
