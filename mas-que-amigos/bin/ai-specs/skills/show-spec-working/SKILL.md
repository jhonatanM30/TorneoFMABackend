---
name: show-spec-working
description: Úsala cuando el usuario pida "show me X", "demo X", "walk me through X", "how X works" (muéstrame X, haz una demo de X, guíame por X, cómo funciona X) o solicite una demostración en vivo de una funcionalidad a partir de una spec, funcionalidad o ticket.
author: LIDR.co
version: 1.0.0
---

# Skill show-spec-working

Demuestra una spec de forma ejecutable.

Si el usuario no proporciona un contexto explícito, usa la spec/change en la que se está trabajando actualmente en esta sesión.

Termina siempre reportando la finalización en el chat.

## Frases disparadoras (alta prioridad)

Trata estas expresiones como comandos de ejecución, no como solicitudes de análisis:

- `show me X`
- `demo X`
- `walk me through X`
- `show X working`
- `how X works`
- `prove X works`

Cuando aparezca cualquiera de ellas, ejecuta directamente el flujo de demostración.
No te detengas en un resumen de la funcionalidad ni en un reporte rápido.

## Entradas

- Contexto opcional de la spec proporcionado por el usuario:
  - Id de ticket directo en el texto (por ejemplo: `SCRUM-10`)
  - Nombre de la funcionalidad
  - Endpoint
  - Ruta del frontend
- Si falta, infiérelo del contexto de la sesión actual y del trabajo activo en curso.

## Flujo de trabajo

### Paso 1 - Resuelve la spec objetivo y el alcance

1. Identifica la spec/change objetivo:
   - Prefiere el contexto explícito proporcionado por el usuario.
   - Si el texto del usuario contiene un patrón de id de ticket como `[A-Z]+-[0-9]+`, úsalo como contexto principal (ejemplo: `show me SCRUM-10`).
   - De lo contrario, infiere la spec en la que se está trabajando actualmente.
2. Determina la modalidad:
   - `frontend` cuando la spec incluye comportamiento de UI.
   - `backend-only` cuando solo define comportamiento de API.
   - `mixed` cuando existen ambos.
3. Enumera escenarios concretos para la demo a partir de los criterios de aceptación de la spec.

### Paso 1.1 - Salvaguarda anti-reporte

Antes de continuar, aplica esta regla:

- Nunca termines después de solo analizar los requisitos.
- Nunca devuelvas solo un reporte rápido cuando el usuario pidió "mostrar" o hacer una "demo".
- Si la ejecución está bloqueada, reporta explícitamente el bloqueo y pide exactamente lo que se necesita para continuar la demo en vivo.

### Paso 2 - Ruta de demostración del frontend

Ejecuta esta ruta cuando la modalidad sea `frontend` o `mixed`.

1. Inicia los servicios locales requeridos si es necesario.
2. Usa automatización del navegador para abrir la app y navegar hasta la funcionalidad objetivo.
3. Demuestra el comportamiento de la funcionalidad según la spec, una interacción a la vez.
   - Secuencia de ejemplo para funcionalidades de lista/tabla:
     - Abre la página del listado
     - Verifica que aparezcan los datos de la tabla
     - Usa la caja de búsqueda
     - Aplica filtros
     - Cambia el ordenamiento
     - Abre la vista de detalles
4. Después de cada acción significativa:
   - Verifica que el resultado visible coincida con lo esperado en la spec.
5. Detente en un estado final estable y deja que el usuario continúe la exploración manual o cierre la ventana.
6. Mantén el navegador abierto a menos que el usuario pida cerrarlo.

### Paso 3 - Ruta de demostración de la API del backend

Ejecuta esta ruta cuando la modalidad sea `backend-only` o `mixed`.

1. Identifica el o los endpoints y el o los payloads de ejemplo definidos por la spec.
2. Ejecuta el o los comandos curl que muestren el comportamiento real de la respuesta.
3. Si alguna llamada cambia el estado de los datos (CREATE/UPDATE/DELETE):
   - Ejecuta el comando curl de restauración/reinicio correspondiente (o una acción de restauración equivalente) inmediatamente después de demostrar el comportamiento.
4. Confirma el estado restaurado para que las demos repetidas sigan siendo deterministas.
5. Incluye en el chat el comando y la evidencia clave de la respuesta (de forma concisa).

## Requisitos del MCP de navegador

Antes de llamar a cualquier herramienta MCP de navegador:

1. Lee primero el JSON descriptor de la herramienta MCP.
2. Sigue las instrucciones del servidor para el flujo de lock/unlock y de actualización de snapshot.
3. Evita reintentos a ciegas repetidos; si estás bloqueado, reporta el bloqueo y la mejor acción siguiente.

## Requisitos de la demo de API

- Usa comandos `curl` explícitos (no pseudocódigo) siempre que los datos del entorno estén disponibles.
- Enmascara los valores sensibles en la salida del chat.
- Mantén los comandos idempotentes cuando sea posible.
- Incluye comandos de restauración para cualquier operación que cambie el estado.

## Contrato de finalización

Envía siempre un mensaje final en el chat que contenga:

1. La spec/change objetivo demostrada.
2. Lo que se ejecutó:
   - Flujos de frontend mostrados.
   - Comandos curl de backend ejecutados.
3. Resultado de la verificación por cada escenario demostrado (pass/fail con una nota corta).
4. Estado de la restauración de datos (si aplica).
5. Entrega final:
   - "Demo completa. Puedes seguir revisando en la ventana abierta del navegador o pedirme que la cierre."

## Formato de salida

Usa esta estructura concisa en la respuesta final del chat:

```markdown
Demo de la spec completada para: <spec/change>

Recorrido del frontend:
- <paso/resultado>

Recorrido de la API del backend:
- <curl + nota clave de la respuesta>

Restauración de datos:
- <restaurados / no necesario / falló + motivo>

Siguiente:
- Puedes continuar en la ventana abierta del navegador, o pedirme que la cierre.
```
