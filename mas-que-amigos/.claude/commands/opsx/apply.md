---
name: "OPSX: Apply"
description: "Implementa las tareas de un change de OpenSpec (experimental)"
allowed-tools: Bash(openspec:*)
category: "Workflow"
tags: ["workflow", "artifacts", "experimental"]
---

Implementa las tareas de un change de OpenSpec.

**Selección de store:** Si el usuario nombra un store (un store es un repo independiente de OpenSpec registrado en esta máquina) o el trabajo vive en uno, ejecuta `openspec store list --json` para descubrir los ids de store registrados y luego pasa `--store <id>` en los comandos que leen o escriben specs y changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`, `schemas`, `view`). Una vez seleccionado, trata `--store <id>` como persistente durante el resto del workflow. Cada ejemplo sin ámbito de esos comandos que aparece abajo es una abreviatura: antes de ejecutarlo, agrega el flag. Por ejemplo, ejecuta `openspec status --change "<name>" --json --store "<id>"`, no la forma sin ámbito que se muestra abajo. Los demás comandos no aceptan el flag. Las sugerencias que imprimen los comandos ya incluyen el flag; mantenlo en los comandos siguientes. Sin un store, los comandos actúan sobre la raíz local `openspec/` más cercana.

**Verificación del proyecto:** Estos pasos esperan un proyecto que ya use OpenSpec. Antes del primer paso que escriba algo (`new change`, `archive`, `sync specs` o la redacción de un archivo de artefacto), confirma que el proyecto tiene una raíz: ejecuta `openspec list --json` (con `--store <id>` cuando haya un store seleccionado, ya que en ese caso el store es la raíz) y lee `root`. Un objeto root significa que el proyecto está configurado. `"root": null` significa que no lo está: aquí no hay un directorio `openspec/`, y una escritura como `openspec new change` crearía uno como efecto secundario. El comando además termina con código distinto de cero, lo cual es esa respuesta y no un CLI roto, así que lee el JSON en lugar de reintentar o buscar una forma de sortearlo.

Hay un `"root": null` que no tiene que ver con la configuración: cuando un mensaje de error de `status` empieza con `Declared in` o `Invalid store declaration in` y nombra el `openspec/config.yaml` (o `config.yml`) de este proyecto, el proyecto sí usa OpenSpec a través de un store que declara, pero esta máquina no puede resolverlo (el store no está registrado o la línea `store:` está mal formada). No lo trates como no inicializado ni te saltes las ramas de abajo: detente antes de escribir y muéstrale al usuario el `message` y el `fix` de ese error.

En cualquier otro caso, sin raíz, lo que sigue depende de cómo se llegó a este workflow:

- **Autoseleccionado**: elegiste este workflow por tu cuenta, sin que el usuario nombrara OpenSpec, nombrara esta skill o ejecutara su slash command. Deja de usar OpenSpec y responde la solicitud normalmente, como lo harías si OpenSpec no estuviera instalado. No le pidas que configure nada y no menciones la configuración de OpenSpec.
- **Solicitud explícita de OpenSpec**: el usuario nombró OpenSpec, nombró esta skill o ejecutó su slash command. Detente antes de escribir y pregunta cómo proceder: configurar este proyecto (`openspec init`), apuntar a un store que ya tenga (`--store <id>`) o continuar sin OpenSpec para esta solicitud. Espera su respuesta.

En ambas ramas, nunca crees la raíz como efecto secundario: no ejecutes `openspec init` hasta que el usuario lo pida, no crees archivos de `openspec/` a mano y no permitas que un comando la cree.

**Entrada**: Opcionalmente, especifica un nombre de change (p. ej., `/opsx:apply add-auth`). Si se omite, verifica si se puede inferir del contexto de la conversación. Si es vago o ambiguo, **DEBES** pedir al usuario que elija entre los changes disponibles.

**Pasos**

1. **Selecciona el change**

   Si se proporciona un nombre, úsalo. En caso contrario:
   - Infiérelo del contexto de la conversación si el usuario mencionó un change
   - Selecciónalo automáticamente si solo existe un change activo
   - Si es ambiguo, ejecuta `openspec list --json` para obtener los changes disponibles y pídele al usuario que seleccione uno

   Anuncia siempre: "Usando el change: <name>" y cómo cambiarlo (p. ej., `/opsx:apply <other>`).

2. **Revisa el estado para entender el schema**
   ```bash
   openspec status --change "<name>" --json
   ```
   Interpreta el JSON para entender:
   - `schemaName`: El workflow que se está usando (p. ej., "spec-driven")
   - `planningHome`, `changeRoot` y `actionContext`: alcance de la planificación y restricciones de edición
   - Qué artefacto contiene las tareas (normalmente "tasks" en spec-driven; revisa status para los demás)

3. **Obtén las instrucciones de apply**

   ```bash
   openspec instructions apply --change "<name>" --json
   ```

   Esto devuelve:
   - `contextFiles`: ID de artefacto -> array de rutas de archivo concretas (varía según el schema; puede ser proposal/specs/design/tasks o spec/tests/implementation/docs)
   - Progreso (total, completadas, restantes)
   - Lista de tareas con su estado
   - Instrucción dinámica según el estado actual
   - `context` opcional: entrada actual obligatoria de instrucciones del proyecto proveniente de la raíz seleccionada
   - `operationGuidance` opcional: guía consultiva actual para apply
   - `missingArtifacts` (cuando está presente): ids de artefactos requeridos que no tienen salida

   **Maneja los estados:**
   - Si `state: "blocked"`: muestra el mensaje y pausa la implementación.
     - Si `missingArtifacts` no está vacío: sugiere completar los artefactos faltantes. Ejecuta `openspec status --change "<name>" --json`, selecciona el siguiente artefacto `ready` (no `skipped` ni `blocked`) y usa `openspec instructions "<artifact-id>" --change "<name>" --json` para obtener sus reglas y su template. Mantén el `--store <id>` seleccionado en ambos comandos.
     - En caso contrario, sigue la instrucción del CLI para crear o reparar el archivo de seguimiento configurado por el schema a partir de los artefactos de planificación existentes. No asumas que otro artefacto está listo ni inicies la implementación mientras esté bloqueado.
   - Si `state: "all_done"`: felicita al usuario y sugiere archivar
   - En cualquier otro caso: procede con la implementación

   Trata `context` como una entrada obligatoria a nivel de prompt. Léela y tenla en cuenta, y
   aplica los hechos, convenciones y restricciones relevantes del proyecto mientras implementas.
   Trata `operationGuidance` como un consejo adicional opcional. Lee y ten en cuenta cada
   entrada, y sigue las que sean aplicables y compatibles con el workflow
   integrado.

   Mantén ambos campos separados del estado devuelto por el CLI, los artefactos faltantes, las tareas,
   el progreso, `contextFiles` y la `instruction` integrada. No son
   evidencia de que una tarea esté completa, no reemplazan la instrucción integrada y no
   permiten saltarse un estado bloqueado. Si el contexto entra en conflicto con la instrucción
   integrada, una elección explícita del usuario o un valor controlado por el CLI, reporta el
   conflicto y conserva el valor que manda. Si la guía no es aplicable o
   entra en conflicto con esas entradas que mandan, no la sigas y explica por qué.
   Estos son contratos de comportamiento a nivel de prompt, no verificaciones que se puedan hacer cumplir.

4. **Lee los archivos de contexto**

   Lee cada ruta de archivo listada en `contextFiles` en la salida de las instrucciones de apply.
   Los archivos dependen del schema que se esté usando:
   - **spec-driven**: proposal, specs, design, tasks
   - Otros schemas: sigue los contextFiles de la salida del CLI

   No copies `context` ni `operationGuidance` literalmente en archivos de implementación
   ni en artefactos de planificación, a menos que el usuario pida ese contenido por separado.

5. **Muestra el progreso actual**

   Muestra:
   - El schema en uso
   - Progreso: "N/M tareas completadas"
   - Resumen de las tareas restantes
   - La instrucción dinámica del CLI

6. **Implementa las tareas (en bucle hasta terminar o quedar bloqueado)**

   Para cada tarea pendiente:
   - Muestra en qué tarea estás trabajando
   - Haz los cambios de código necesarios
   - Mantén los cambios mínimos y enfocados
   - Marca la tarea como completada en el archivo de tareas: `- [ ]` → `- [x]`
   - Continúa con la siguiente tarea

   **Pausa si:**
   - La tarea no está clara → pide una aclaración
   - La implementación revela un problema de diseño → sugiere actualizar los artefactos
   - Una tarea necesita trabajo más allá de lo que describen la spec y las tareas, o sientes la tentación de eliminar, reducir, posponer o aceptar excepciones al comportamiento especificado para que encaje → expón el alcance adicional y pregunta; no lo absorbas en silencio
   - Encuentras un error o un bloqueo → repórtalo y espera indicaciones
   - El usuario interrumpe

7. **Al completar o pausar, muestra el estado**

   Muestra:
   - Las tareas completadas en esta sesión
   - Progreso general: "N/M tareas completadas"
   - Si todo está hecho: sugiere archivar
   - Si está en pausa: explica por qué y espera indicaciones

**Salida durante la implementación**

```
## Implementando: <change-name> (schema: <schema-name>)

Trabajando en la tarea 3/7: <task description>
[...implementación en curso...]
✓ Tarea completada

Trabajando en la tarea 4/7: <task description>
[...implementación en curso...]
✓ Tarea completada
```

**Salida al completar**

```
## Implementación completada

**Change:** <change-name>
**Schema:** <schema-name>
**Progreso:** 7/7 tareas completadas ✓

### Completadas en esta sesión
- [x] Tarea 1
- [x] Tarea 2
...

¡Todas las tareas están completadas! Puedes archivar este change con `/opsx:archive`.
```

**Salida al pausar (problema encontrado)**

```
## Implementación en pausa

**Change:** <change-name>
**Schema:** <schema-name>
**Progreso:** 4/7 tareas completadas

### Problema encontrado
<description of the issue>

**Opciones:**
1. <option 1>
2. <option 2>
3. Otro enfoque

¿Qué te gustaría hacer?
```

**Salvaguardas**
- Sigue avanzando por las tareas hasta terminar o quedar bloqueado
- Lee siempre los archivos de contexto antes de empezar (de la salida de las instrucciones de apply)
- Si una tarea es ambigua, pausa y pregunta antes de implementar
- Si la implementación revela problemas, pausa y sugiere actualizar los artefactos
- Mantén los cambios de código mínimos y limitados a cada tarea
- Actualiza la casilla de la tarea inmediatamente después de completar cada una
- Pausa ante errores, bloqueos o requisitos poco claros; no adivines
- Cuando una tarea necesite trabajo más allá de lo que describe la spec, expón el alcance adicional y pausa; nunca reduzcas, pospongas ni simplifiques en silencio el comportamiento especificado
- Marca una tarea como `- [x]` solo cuando su comportamiento especificado esté completamente implementado, no cuando esté parcialmente hecha o pospuesta
- Usa los contextFiles de la salida del CLI; no asumas nombres de archivo específicos
- No uses el contexto ni la guía de operación como prueba de que una tarea está completa
- Aplica el contexto relevante del proyecto; reporta los conflictos con las entradas del workflow que mandan
- Ten en cuenta cada entrada de la guía; explica cualquier consejo no aplicable o en conflicto
- No copies el contexto de ejecución ni la guía de operación en archivos de implementación ni en artefactos de planificación
- Conserva el comportamiento blocked/ready/all-done controlado por el CLI y los criterios de completitud

**Integración con el workflow fluido**

Esta skill soporta el modelo de "acciones sobre un change":

- **Se puede invocar en cualquier momento**: Antes de que todos los artefactos estén hechos (si existen tareas), después de una implementación parcial, intercalada con otras acciones
- **Permite actualizar artefactos**: Si la implementación revela problemas de diseño, sugiere actualizar los artefactos; no está bloqueada por fases, trabaja con fluidez
