---
description: Hace cumplir los pasos obligatorios de openspec/config.yaml al crear artefactos tasks.md y asegura que el agente ejecute todos los tests manuales
alwaysApply: true
---

# Tareas de OpenSpec: cumplimiento de los pasos obligatorios

Al crear o actualizar artefactos `tasks.md` en changes de OpenSpec, DEBES:

## 1. Lee primero openspec/config.yaml

**ANTES** de crear o actualizar cualquier archivo `tasks.md`, DEBES leer `openspec/config.yaml` para entender:
- Los pasos obligatorios específicos de backend y frontend
- Las convenciones de nombres de branches
- Los requisitos de estructura de las tareas
- Los requisitos de testing y documentación

## 2. Pasos obligatorios

Todas las tareas de implementación DEBEN incluir estos pasos en el orden correcto:

### Paso 0: Crear el feature branch (DEBE SER EL PRIMERO)
- **Ubicación**: Debe ser el primerísimo paso (Paso 0)
- **Nombre del branch**: `feature/[ticket-id]` o `feature/[change-name]`
- **Acción**: Crea el feature branch y cámbiate a él antes de cualquier cambio de código

### Pasos obligatorios (deben incluirse):
- **Paso N**: Revisar y actualizar los tests unitarios existentes (OBLIGATORIO)
- **Paso N+1**: Ejecutar los tests unitarios y verificar el estado de la base de datos (OBLIGATORIO)
- **Paso N+2**: Testing manual de endpoints con curl (OBLIGATORIO) - **EL AGENTE DEBE EJECUTAR**
- **Paso N+3**: Testing E2E con Playwright MCP (OBLIGATORIO si aplica) - **EL AGENTE DEBE EJECUTAR**
- **Paso N+4**: Actualizar la documentación técnica (OBLIGATORIO)

## 3. Requisitos de testing manual - CRÍTICO: el agente debe ejecutarlos

**IMPORTANTE**: El agente de código (IA) DEBE realizar por sí mismo todos los pasos de testing manual. **NUNCA delegues el testing al usuario**. Estos tests deben ser ejecutados por el agente para poder marcar las tareas como completadas en `tasks.md`.

### Paso N+1: Ejecutar los tests unitarios y verificar el estado de la base de datos (OBLIGATORIO)

**Responsabilidad del agente**: El agente de código DEBE ejecutar los tests unitarios, validar la integridad de la base de datos antes y después de la ejecución, y producir un artefacto de reporte de tests en la carpeta de specs del change. Esto NO es opcional y no se puede delegar al usuario.

**Pasos de implementación** (el agente debe realizarlos):
1. **Prepara el entorno de pruebas**:
   - Asegúrate de que los servicios requeridos estén disponibles (base de datos, caché, dependencias)
   - Captura el estado de la base de datos previo a los tests que sea relevante para el change (conteos, registros clave, checksums o snapshots)
   - Documenta el o los comandos exactos de test que se ejecutarán

2. **Ejecuta primero los tests unitarios focalizados**:
   - Ejecuta tests focalizados para el o los módulos modificados y el comportamiento relacionado
   - Confirma que las fallas se resolvieron y que no aparecen nuevas regresiones en el alcance focalizado
   - Captura un resumen de la salida del comando (passed/failed/skipped)

3. **Ejecuta la suite de tests unitarios más amplia**:
   - Ejecuta la suite del proyecto/unitaria que exige `openspec/config.yaml` (o un subconjunto justificado si así está configurado)
   - Registra el total de tests, las fallas, el tiempo de ejecución y cualquier comportamiento inestable (flaky) observado

4. **Verifica el estado de la base de datos después de los tests**:
   - Vuelve a revisar los mismos indicadores de la base de datos capturados antes de los tests
   - Confirma que no quedan mutaciones no deseadas después de que terminan los tests
   - Si ocurrió alguna mutación, restaura el estado y documenta la restauración

5. **Crea el reporte de verificación de tests unitarios en la carpeta de specs**:
   - Guarda el reporte dentro de la carpeta del change actual en `specs/<change-name>/reports/`
   - Usa este patrón de nombre de archivo: `YYYY-MM-DD-step-N+1-unit-test-and-db-verification.md`
   - Incluye los comandos ejecutados, el resumen de resultados, la comparación pre/post de la base de datos y las acciones de limpieza

6. **Marca la tarea como completada**: Solo después de que los tests unitarios pasen (o de que se documenten las excepciones aprobadas), de que el estado de la base de datos esté verificado/restaurado y de que se haya creado el archivo de reporte, marca el Paso N+1 como completado en `tasks.md`.

**Plantilla de reporte** (guárdala en `specs/<change-name>/reports/`):
```markdown
# Reporte del Paso N+1 - Tests unitarios y verificación de la base de datos

- Fecha: YYYY-MM-DD
- Change: <change-name>
- Agente: <agent-name>

## Comandos ejecutados
- `<command 1>`
- `<command 2>`

## Resultados de los tests unitarios
- Tests focalizados: X pasaron, Y fallaron, Z omitidos
- Suite completa/requerida: X pasaron, Y fallaron, Z omitidos
- Tiempo de ejecución: <duration>
- Notas: <flaky tests, retries, exceptions>

## Verificación del estado de la base de datos
- Baseline previo a los tests:
  - <metric/table/check>: <value>
- Validación posterior a los tests:
  - <metric/table/check>: <value>
- Estado restaurado: Sí/No
- Acciones de restauración (si las hubo): <actions>

## Resultado
- Estado del Paso N+1: PASS/FAIL
- Problemas bloqueantes: <none or list>
```

**Dependencias**:
- Test runner y dependencias de test del proyecto instalados
- Acceso a la base de datos para verificar/restaurar el estado
- Permiso para crear archivos de reporte en `specs/<change-name>/reports/`

**Notas**:
- **El agente DEBE ejecutar los tests por sí mismo**: nunca le pidas al usuario que ejecute los tests
- Este paso es obligatorio incluso cuando los cambios de código parezcan pequeños
- El nombre del reporte debe seguir el patrón requerido para garantizar la trazabilidad
- **La tarea solo puede marcarse como completada en tasks.md después de crear el reporte**

### Paso N+2: Testing manual de endpoints con curl (OBLIGATORIO)

**Responsabilidad del agente**: El agente de código DEBE ejecutar todos los comandos curl y verificar las respuestas. Esto NO es opcional y no se puede delegar al usuario.

**Pasos de implementación** (el agente debe realizarlos):
1. **Prepara el entorno de pruebas**:
   - Asegúrate de que el servidor backend esté corriendo (inícialo si es necesario)
   - Verifica que la conexión a la base de datos esté activa
   - Anota el estado actual de la base de datos (si pruebas endpoints de CREATE/UPDATE/DELETE)

2. **Prueba los endpoints GET** (si los hay):
   - Crea el comando curl para probar el endpoint GET
   - Ejecuta el comando curl: `curl -X GET [endpoint-url] [headers]`
   - Verifica el código de estado de la respuesta (200, 404, etc.)
   - Verifica la estructura y el contenido del cuerpo de la respuesta
   - Documenta el comando curl y la respuesta al completar la tarea

3. **Prueba los endpoints POST** (operaciones CREATE):
   - Crea el comando curl con el cuerpo de la petición: `curl -X POST [endpoint-url] -H "Content-Type: application/json" -d '[json-body]'`
   - Ejecuta el comando curl y captura la respuesta
   - Verifica el código de estado de la respuesta (201, 400, 422, etc.)
   - Verifica que el cuerpo de la respuesta contenga el recurso creado
   - **Restaura el estado de la base de datos**: Después de probar, elimina el registro creado para devolver la base de datos a su estado original
   - Documenta el comando curl, la respuesta y la acción de limpieza

4. **Prueba los endpoints PUT/PATCH** (operaciones UPDATE):
   - Crea el comando curl con los datos actualizados: `curl -X PUT [endpoint-url] -H "Content-Type: application/json" -d '[json-body]'`
   - Ejecuta el comando curl y captura la respuesta
   - Verifica el código de estado de la respuesta (200, 404, 400, etc.)
   - Verifica que el cuerpo de la respuesta contenga el recurso actualizado
   - **Restaura el estado de la base de datos**: Después de probar, revierte el registro actualizado a sus valores originales para restaurar el estado de la base de datos
   - Documenta el comando curl, la respuesta y la acción de limpieza

5. **Prueba los endpoints DELETE**:
   - Crea el comando curl: `curl -X DELETE [endpoint-url]`
   - Ejecuta el comando curl y captura la respuesta
   - Verifica el código de estado de la respuesta (200, 204, 404, etc.)
   - Verifica que la eliminación haya sido exitosa
   - **Restaura el estado de la base de datos**: Después de probar, vuelve a crear el registro eliminado con sus valores originales para restaurar el estado de la base de datos
   - Documenta el comando curl, la respuesta y la acción de limpieza

6. **Prueba los casos de error**:
   - Prueba con datos inválidos (errores de validación)
   - Prueba con recursos inexistentes (errores 404)
   - Prueba con acceso no autorizado (si aplica)
   - Verifica que el formato de la respuesta de error coincida con la especificación de la API

7. **Marca la tarea como completada**: Solo después de que todos los tests con curl pasen y el estado de la base de datos esté restaurado, marca la tarea como completada en `tasks.md`

**Dependencias**:
- Servidor backend corriendo (el agente debe iniciarlo si es necesario)
- Acceso a la base de datos para restaurar el estado
- Herramienta de línea de comandos curl

**Notas**:
- Este paso es OBLIGATORIO para todos los endpoints nuevos
- **El agente DEBE ejecutar todos los comandos curl por sí mismo**: nunca le pidas al usuario que ejecute los tests
- Todas las operaciones CREATE/UPDATE/DELETE deben restaurar la base de datos a su estado original después de las pruebas
- Documenta todos los comandos curl y las respuestas para referencia futura en un reporte en la carpeta de specs, con el nombre adecuado
- Verifica que el estado de la base de datos coincida con el estado previo a los tests después de la limpieza
- No omitas el testing manual aunque los tests unitarios pasen
- **La tarea solo puede marcarse como completada en tasks.md después de ejecutar con éxito todos los tests con curl**

### Paso N+3: Testing E2E con Playwright MCP (OBLIGATORIO si aplica)

**Responsabilidad del agente**: El agente de código DEBE ejecutar todos los tests E2E usando las herramientas de Playwright MCP. Esto NO es opcional y no se puede delegar al usuario.

**Cuándo aplica**:
- Cambios de frontend que afectan los flujos del usuario
- Integración entre el frontend y los endpoints del backend
- Funcionalidades visibles para el usuario que requieren interacción con el navegador

**Pasos de implementación** (el agente debe realizarlos):
1. **Prepara el entorno de pruebas**:
   - Asegúrate de que los servidores de frontend y backend estén corriendo (inícialos si es necesario)
   - Verifica que la base de datos esté en un estado conocido
   - Revisa las herramientas de Playwright MCP disponibles usando el sistema de archivos de MCP

2. **Navega a la aplicación**:
   - Usa `browser_navigate` de Playwright MCP para abrir la URL de la aplicación
   - Espera a que la página cargue por completo
   - Toma un snapshot para verificar el estado inicial

3. **Ejecuta los flujos del usuario**:
   - Usa las herramientas de Playwright MCP para interactuar con la UI:
     - `browser_click` para clics en botones y navegación
     - `browser_type` o `browser_fill` para los campos de formulario
     - `browser_snapshot` para verificar cambios de estado
     - `browser_wait` para operaciones asíncronas
   - Prueba el flujo completo del usuario de principio a fin
   - Verifica los resultados esperados en cada paso

4. **Prueba los escenarios de error**:
   - Prueba los errores de validación de formularios
   - Prueba que los mensajes de error se muestren correctamente
   - Prueba los flujos de recuperación ante errores

5. **Verifica la persistencia de los datos**:
   - Después de crear/actualizar datos a través de la UI, verifica que persistan correctamente
   - Comprueba que el estado de la base de datos coincida con el estado de la UI
   - Verifica que los datos aparezcan correctamente en las vistas de listas/detalles

6. **Restaura el entorno de pruebas**:
   - Limpia cualquier dato de prueba creado durante los tests E2E
   - Restaura la base de datos a su estado original
   - Cierra las sesiones del navegador

7. **Marca la tarea como completada**: Solo después de que todos los tests E2E pasen y el entorno esté restaurado, marca la tarea como completada en `tasks.md`

**Dependencias**:
- Servidor frontend corriendo (el agente debe iniciarlo si es necesario)
- Servidor backend corriendo (el agente debe iniciarlo si es necesario)
- Herramientas de Playwright MCP disponibles
- Acceso a la base de datos para verificación y limpieza

**Notas**:
- **El agente DEBE ejecutar todos los tests E2E por sí mismo**: nunca le pidas al usuario que ejecute los tests
- Usa esperas incrementales (1-3 segundos) con verificaciones mediante snapshots en lugar de esperas largas
- Restaura siempre el estado de la base de datos después de los tests que modifican datos
- Documenta los escenarios de prueba y sus resultados en un reporte en la carpeta de specs, con el nombre adecuado
- **La tarea solo puede marcarse como completada en tasks.md después de ejecutar con éxito todos los tests E2E**

## 4. Checklist de verificación

Antes de finalizar cualquier archivo `tasks.md`, verifica que:
- [ ] El Paso 0 (Crear el feature branch) es el PRIMER paso
- [ ] Se incluyen todos los pasos obligatorios de config.yaml
- [ ] Los pasos están numerados de forma secuencial
- [ ] Los pasos obligatorios están claramente marcados con la etiqueta "(OBLIGATORIO)"
- [ ] El nombre del branch sigue la convención: `feature/[name]-backend`
- [ ] El Paso N+1 incluye la ruta del reporte y la convención de nombres en `specs/<change-name>/reports/`
- [ ] Los pasos de testing manual indican explícitamente "EL AGENTE DEBE EJECUTAR"
- [ ] Las tareas incluyen pasos de restauración del estado de la base de datos
- [ ] Se incluye el paso de testing E2E si hay cambios de frontend involucrados

## 5. Cuándo aplica

Esta regla aplica cuando:
- Se crea `tasks.md` mediante `/opsx:ff` (fast-forward) o la skill `openspec-ff-change`
- Se crea `tasks.md` mediante `/opsx:continue` (continuar change) o la skill `openspec-continue-change`
- Se actualizan archivos `tasks.md` existentes
- Cualquier creación de tareas que involucre cambios de backend
- Se implementan tareas de `tasks.md` mediante `/opsx:apply` o la skill `openspec-apply-change`: el agente debe ejecutar los tests manuales

## 6. Estructura de ejemplo

```markdown
## 0. Preparación: Crear el feature branch (OBLIGATORIO - PRIMER PASO)

- [ ] 0.1 Crear el feature branch `feature/update-position-backend` a partir del branch main/master
- [ ] 0.2 Verificar la creación del branch y el estado del branch actual

## 1. Backend: Tests del validador (TDD)
...

## 8. Backend: Revisar y actualizar los tests unitarios existentes (OBLIGATORIO)
...

## 9. Backend: Ejecutar los tests unitarios y verificar el estado de la base de datos (OBLIGATORIO)
- [ ] 9.1 Capturar el baseline de la base de datos previo a los tests para las entidades impactadas
- [ ] 9.2 Ejecutar los tests unitarios focalizados para los módulos modificados
- [ ] 9.3 Ejecutar la suite de tests unitarios más amplia requerida por la configuración
- [ ] 9.4 Verificar el estado de la base de datos después de los tests y restaurarlo si es necesario
- [ ] 9.5 Crear el reporte `specs/<change-name>/reports/YYYY-MM-DD-step-N+1-unit-test-and-db-verification.md`
- [ ] 9.6 Marcar el paso como completado solo después de que los tests pasen y el reporte exista

## 10. Backend: Testing manual de endpoints con curl (OBLIGATORIO - EL AGENTE DEBE EJECUTAR)
- [ ] 10.1 Asegurar que el servidor backend esté corriendo
- [ ] 10.2 Probar los endpoints GET con curl y verificar las respuestas
- [ ] 10.3 Probar los endpoints POST con curl, verificar la creación y luego restaurar el estado de la base de datos
- [ ] 10.4 Probar los endpoints PUT/PATCH con curl, verificar las actualizaciones y luego restaurar el estado de la base de datos
- [ ] 10.5 Probar los endpoints DELETE con curl, verificar la eliminación y luego restaurar el estado de la base de datos
- [ ] 10.6 Probar los casos de error (errores de validación, 404, etc.)
- [ ] 10.7 Documentar todos los comandos curl y las respuestas
- [ ] 10.8 Verificar que el estado de la base de datos coincida con el estado previo a los tests

## 11. Frontend: Testing E2E con Playwright MCP (OBLIGATORIO si aplica - EL AGENTE DEBE EJECUTAR)
- [ ] 11.1 Asegurar que los servidores de frontend y backend estén corriendo
- [ ] 11.2 Navegar a la aplicación usando browser_navigate de Playwright MCP
- [ ] 11.3 Ejecutar el flujo completo del usuario usando las herramientas de Playwright MCP
- [ ] 11.4 Probar los escenarios de error y la validación
- [ ] 11.5 Verificar la persistencia de los datos y el estado de la UI
- [ ] 11.6 Restaurar el entorno de pruebas y el estado de la base de datos
- [ ] 11.7 Documentar los escenarios de prueba y sus resultados

## 16. Actualizar la documentación técnica (OBLIGATORIO)
...
```

## 7. Requisitos de ejecución del agente

**CRÍTICO**: Al implementar tareas de `tasks.md` (mediante la skill `openspec-apply-change` o el comando `/opsx:apply`), el agente de código DEBE:

1. **Ejecutar todos los tests manuales**: Nunca le pidas al usuario que ejecute comandos curl o tests E2E. El agente debe:
   - Iniciar los servidores si es necesario (backend, frontend)
   - Ejecutar todos los comandos curl para el testing de endpoints
   - Ejecutar todos los tests E2E usando las herramientas de Playwright MCP
   - Verificar todas las respuestas y resultados
   - Restaurar el estado de la base de datos después de los tests

2. **Marcar las tareas como completadas**: Las tareas SOLO pueden marcarse como completadas (`[x]`) en `tasks.md` DESPUÉS de que:
   - El agente haya ejecutado con éxito todos los tests requeridos
   - Se hayan verificado todos los resultados de los tests
   - Se haya restaurado el estado de la base de datos (para operaciones CREATE/UPDATE/DELETE)
   - Se hayan documentado todos los resultados de los tests

3. **Nunca delegar el testing**: El agente nunca debe:
   - Pedirle al usuario que ejecute comandos curl
   - Pedirle al usuario que pruebe los endpoints manualmente
   - Pedirle al usuario que ejecute tests E2E
   - Marcar tareas como completadas sin ejecutar los tests
   - Omitir los pasos de testing manual

4. **Documentar la ejecución de los tests**: El agente debe documentar:
   - Todos los comandos curl ejecutados
   - Todas las respuestas recibidas
   - Todos los escenarios de tests E2E ejecutados
   - Las acciones de restauración del estado de la base de datos
   - Cualquier problema encontrado y su resolución

## Consecuencias de no cumplirlo

Si creas tareas sin seguir estos pasos obligatorios, el usuario tendrá que corregir manualmente el archivo tasks.md. Lee siempre primero `openspec/config.yaml` y asegúrate de incluir todos los pasos obligatorios.

**Si implementas tareas sin ejecutar tú mismo los tests manuales, estás violando esta regla. El agente debe ejecutar todos los tests para marcar las tareas como completadas.**
