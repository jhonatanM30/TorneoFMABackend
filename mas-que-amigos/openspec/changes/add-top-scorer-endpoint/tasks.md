# Tasks

## 0. Preparación: Crear el feature branch (OBLIGATORIO - PRIMER PASO)

- [x] 0.1 Crear el feature branch `feature/add-top-scorer-endpoint-backend` a partir de `master` y verificar con `git branch --show-current` que quedó activo
- [x] 0.2 Verificar que el working tree está limpio antes de empezar (`git status`) — se confirmó que solo hay archivos sin trackear preexistentes del harness de agentes (`.claude/`, `.cursor/`, `docs/`, `openspec/`, etc.), ninguno relacionado con este change

## 1. Backend: Tests unitarios del cálculo del goleador (TDD)

- [x] 1.1 Crear `EstadisticaServiceTest.java` (no existe todavía) siguiendo el patrón Mockito+AssertJ de `PartidoServiceTest.java`, con un test para "un único jugador tiene más goles que todos los demás" y verificar que compila pero falla (rojo) porque `obtenerGoleadores()` no existe aún
- [x] 1.2 Agregar test para "empate en el máximo de goles" (dos jugadores con el mismo total, ambos deben devolverse) y verificar que también falla en rojo
- [x] 1.3 Agregar test para "sin goles registrados en el torneo" (debe devolver lista vacía) y verificar que falla en rojo

Nota: el escenario "los goles se suman sin importar el equipo ni el partido" (`specs/top-scorer/spec.md`) se cubre mediante la propia consulta JPQL `SUM(goles) GROUP BY id_jugador` (tarea 2.1), no con un test de servicio: con el patrón de testing de este proyecto (Mockito puro sobre el repositorio mockeado, sin ningún `@DataJpaTest` existente), un test de `EstadisticaServiceTest` no puede verificar la agregación real entre partidos sin duplicar el test de la tarea 1.1.

## 2. Backend: Implementación de la consulta de agregación y el servicio

- [x] 2.1 Agregar a `EstadisticaRepository` la consulta JPQL de agregación por jugador (`SUM(goles) GROUP BY id_jugador`, ordenada descendente) y verificar que el proyecto compila (`mvnw compile`)
- [x] 2.2 Crear `GoleadorDTO` (id de jugador, nombre, equipo actual, total de goles) en `dto/`
- [x] 2.3 Implementar `EstadisticaService.obtenerGoleadores()`: toma el máximo total devuelto por la consulta y filtra todos los jugadores empatados en ese máximo, mapeando a `GoleadorDTO`, y verificar que los tests del grupo 1 pasan (verde)

## 3. Backend: Endpoint

- [x] 3.1 Agregar `GET /api/estadisticas/goleador` en `EstadisticaController`, documentado con las anotaciones Swagger/OpenAPI que ya usa el controller, delegando a `EstadisticaService.obtenerGoleadores()`

## 4. Backend: Revisar y actualizar los tests unitarios existentes (OBLIGATORIO)

- [x] 4.1 Revisar `EstadisticaServiceTest.java` (grupo 1) y confirmar que cubre los escenarios de `specs/top-scorer/spec.md`; ejecutar `mvnw test -Dtest=EstadisticaServiceTest` y verificar que todos pasan (3/3 pasaron)
- [x] 4.2 Revisar los tests existentes de `PartidoServiceTest`, `EquipoServiceTest`, `JugadorServiceTest`, `AlineacionServiceTest` y confirmar que no se rompieron por los cambios en `EstadisticaRepository`/`EstadisticaService` (19/19 pasaron, sin regresiones)

## 5. Backend: Ejecutar los tests unitarios y verificar el estado de la base de datos (OBLIGATORIO)

- [x] 5.1 Capturar el baseline de la base de datos previo a los tests (conteo de filas en `estadistica`, `jugador`, `partido`) — este endpoint es de solo lectura, por lo que no debería producir mutaciones (baseline: estadistica=0, jugador=21, partido=1, equipos=4)
- [x] 5.2 Ejecutar los tests unitarios focalizados de `EstadisticaServiceTest` y confirmar 0 fallas (3/3 pasaron)
- [x] 5.3 Ejecutar la suite completa de tests unitarios del proyecto (`mvnw test`) y registrar el total de tests, fallas y tiempo de ejecución (31 tests, 30 pasaron, 1 falla preexistente no relacionada — ver reporte)
- [x] 5.4 Verificar el estado de la base de datos después de los tests (mismos conteos del baseline) y confirmar que no hay mutaciones no deseadas (sin cambios: estadistica=0, jugador=21, partido=1, equipos=4)
- [x] 5.5 Crear el reporte `openspec/changes/add-top-scorer-endpoint/specs/reports/2026-09-25-step-5-unit-test-and-db-verification.md` con los comandos ejecutados, el resumen de resultados y la comparación pre/post de la base de datos
- [x] 5.6 Marcar este paso como completado solo después de que los tests pasen y el reporte exista

## 6. Backend: Testing manual del endpoint con curl (OBLIGATORIO - EL AGENTE DEBE EJECUTAR)

- [x] 6.1 Asegurar que el servidor backend esté corriendo (iniciarlo si es necesario) y que la base de datos tenga al menos un partido con estadísticas de goles registradas (se levantó el backend localmente en el puerto 57076 contra el MySQL de `TorneoContainer`, y se crearon estadísticas de prueba vía POST)
- [x] 6.2 Ejecutar `curl -X GET http://localhost:<puerto>/api/estadisticas/goleador` y verificar código 200 y que el cuerpo contiene el/los jugador(es) con más goles, cada uno con id, nombre y total de goles
- [x] 6.3 Ajustar temporalmente los datos (o usar datos existentes) para provocar un empate en el máximo de goles, repetir la petición y verificar que el cuerpo devuelve todos los jugadores empatados; si se modificaron datos, restaurarlos a su estado original al terminar
- [x] 6.4 Probar el caso sin goles registrados (base de datos de prueba sin filas en `estadistica`, o filtrando temporalmente) y verificar código 200 con lista vacía; restaurar el estado original de la base de datos después de la prueba
- [x] 6.5 Documentar todos los comandos curl ejecutados y las respuestas obtenidas en el reporte del paso 5, o en un reporte adicional `openspec/changes/add-top-scorer-endpoint/specs/reports/2026-09-25-step-6-manual-curl-testing.md`

## 7. Frontend: Testing E2E con Playwright MCP (No aplica)

- [x] 7.1 Confirmar y documentar que este change no incluye cambios de frontend (el frontend vive en el repositorio aparte `MasQueAmigos-Torneo`, fuera de alcance de este change) y que por lo tanto el testing E2E con Playwright MCP no aplica aquí

## 8. Documentación técnica (OBLIGATORIO)

- [x] 8.1 Documentar el nuevo endpoint `GET /api/estadisticas/goleador` (request, respuesta, casos de empate y de lista vacía) en la documentación técnica del backend donde ya se documentan los demás endpoints de `estadisticas` (`docs/api-spec.yml`: nueva ruta y schema `GoleadorDTO`)
