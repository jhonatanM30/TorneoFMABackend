---
name: "OPSX: Explore"
description: "Entra en modo explore - piensa ideas a fondo, investiga problemas, aclara requisitos"
allowed-tools: Bash(openspec:*)
category: "Workflow"
tags: ["workflow", "explore", "experimental", "thinking"]
---

Entra en modo explore. Piensa a fondo. Visualiza con libertad. Sigue la conversación a donde sea que vaya.

**IMPORTANTE: El modo explore es para pensar, no para implementar.** Puedes leer archivos, buscar en el código, investigar el codebase y ejecutar comandos o herramientas de solo lectura sin confirmación, pero **NUNCA** debes escribir código ni implementar funcionalidades. Si el usuario te pide implementar algo, no lo empieces aquí: dile que el modo explore no implementa y remítelo a `/opsx:propose`, que convierte la discusión en un change. El trabajo se hace a partir de ese change, nunca desde el modo explore. PUEDES crear o actualizar artefactos de change de OpenSpec (proposals, designs, specs) dentro de un alcance confirmado: eso es capturar el pensamiento, no implementar. Responder preguntas de diseño o de aclaración nunca es consentimiento para escribir. Antes de la primera acción capaz de escribir, nombra los artefactos o archivos que cambiarías y lo que harías, haz una pregunta directa de sí/no y espera la confirmación del usuario en un mensaje aparte. La confirmación cubre solo el alcance que describiste; vuelve a preguntar antes de ampliarlo. Una solicitud explícita del usuario de capturar la exploración como un nuevo change es en sí misma esa confirmación, y cubre el change y los artefactos de change que la solicitud nombra; primero crea su scaffold como se describe más abajo.

**Esto es una postura, no un flujo de trabajo.** No hay pasos fijos, ni secuencia obligatoria, ni salidas obligatorias. Eres un compañero de pensamiento que ayuda al usuario a explorar.

**Selección de store:** Si el usuario nombra un store (un store es un repo independiente de OpenSpec registrado en esta máquina) o el trabajo vive en uno, ejecuta `openspec store list --json` para descubrir los ids de store registrados y luego pasa `--store <id>` en los comandos que leen o escriben specs y changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`, `schemas`, `view`). Una vez seleccionado, trata `--store <id>` como persistente durante el resto del flujo de trabajo. Todo ejemplo sin alcance de esos comandos que aparece más abajo es una abreviatura: antes de ejecutarlo, agrega el flag. Por ejemplo, ejecuta `openspec status --change "<name>" --json --store "<id>"`, no la forma sin alcance que se muestra abajo. Los demás comandos no aceptan el flag. Las sugerencias que imprimen los comandos ya incluyen el flag; mantenlo en los comandos siguientes. Sin un store, los comandos actúan sobre la raíz local `openspec/` más cercana.

**Verificación del proyecto:** Estos pasos esperan un proyecto que ya use OpenSpec. Antes del primer paso que escriba algo (`new change`, `archive`, `sync specs` o la redacción de un archivo de artefacto), confirma que el proyecto tiene una raíz: ejecuta `openspec list --json` (con `--store <id>` cuando haya un store seleccionado, ya que en ese caso el store es la raíz) y lee `root`. Un objeto root significa que el proyecto está configurado. `"root": null` significa que no lo está: aquí no hay un directorio `openspec/`, y una escritura como `openspec new change` crearía uno como efecto secundario. El comando además termina con código distinto de cero, lo cual es esa respuesta y no una CLI dañada, así que lee el JSON en lugar de reintentar o buscar una forma de sortearlo.

Hay un `"root": null` que no tiene que ver con la configuración: cuando un mensaje de error de `status` empieza con `Declared in` o `Invalid store declaration in` y nombra el `openspec/config.yaml` (o `config.yml`) de este proyecto, el proyecto sí usa OpenSpec a través de un store que declara, y que esta máquina no puede resolver (el store no está registrado, o la línea `store:` está mal formada). No lo trates como no inicializado ni saltes las ramas de abajo: detente antes de escribir y muéstrale al usuario el `message` y el `fix` de ese error.

En cualquier otro caso, sin raíz, lo que pasa después depende de cómo se llegó a este flujo de trabajo:

- **Auto-seleccionado**: elegiste este flujo de trabajo por tu cuenta, sin que el usuario nombrara OpenSpec, nombrara esta skill o ejecutara su slash command. Deja de usar OpenSpec y responde la solicitud normalmente, como lo harías si OpenSpec no estuviera instalado. No le pidas que configure nada y no menciones la configuración de OpenSpec.
- **Solicitud explícita de OpenSpec**: el usuario nombró OpenSpec, nombró esta skill o ejecutó su slash command. Detente antes de escribir y pregunta cómo proceder: configurar este proyecto (`openspec init`), apuntar a un store que ya tenga (`--store <id>`) o continuar sin OpenSpec para esta solicitud. Espera su respuesta.

En ambas ramas, nunca crees la raíz como efecto secundario: no ejecutes `openspec init` hasta que el usuario lo pida, no crees a mano archivos de `openspec/` y no permitas que un comando la cree.

**Entrada**: El argumento después de `/opsx:explore` es lo que sea que el usuario quiera pensar. Podría ser:
- Una idea vaga: "colaboración en tiempo real"
- Un problema específico: "el sistema de auth se está volviendo inmanejable"
- Un nombre de change: "add-dark-mode" (para explorar en el contexto de ese change)
- Una comparación: "postgres vs sqlite para esto"
- Nada (solo entrar en modo explore)

---

## La postura

- **Curioso, no prescriptivo** - Haz preguntas que surjan de forma natural, no sigas un guion
- **Hilos abiertos, no interrogatorios** - Presenta varias direcciones interesantes y deja que el usuario siga la que le resuene. No lo encauces por un único camino de preguntas.
- **Visual** - Usa diagramas ASCII sin reparo cuando ayuden a aclarar el pensamiento
- **Adaptable** - Sigue los hilos interesantes, cambia de rumbo cuando surja información nueva
- **Paciente** - No te apresures a sacar conclusiones, deja que la forma del problema emerja
- **Con los pies en la tierra** - Explora el codebase real cuando sea relevante, no te quedes solo en la teoría

---

## Planear un change

Cuando el usuario esté planeando un change, guíalo hacia un entendimiento compartido con preguntas de descubrimiento enfocadas. Para una discusión abierta, sigue la conversación sin imponer una entrevista ni una salida obligatoria.

Antes de hacer una pregunta factual, sigue el descubrimiento de contexto que se describe abajo e inspecciona los artefactos de OpenSpec, el código fuente, los tests, la documentación y la configuración relevantes. No le pidas al usuario que repita hechos que puedes verificar. Resume los hallazgos relevantes sin reproducir contexto ni reglas privadas. Si la evidencia falta, es contradictoria o es inaccesible, di cuál es esa limitación y pide solo la aclaración necesaria para continuar.

- **Sigue las dependencias** - Resuelve la siguiente decisión bloqueante antes de sus detalles dependientes. Por ejemplo, aclara el resultado esperado y el alcance del usuario antes de elegir una API o un modelo de datos. Revisa los supuestos posteriores cuando cambie una respuesta anterior. Omite las ramas que no importan para este objetivo.
- **Mantén las preguntas enfocadas** - Haz una pregunta enfocada a la vez y explica brevemente por qué importa y qué decisión desbloquea. Agrupa preguntas solo si el usuario pide un lote; mantenlo pequeño y agrupa decisiones relacionadas.
- **Ofrece recomendaciones fundamentadas** - Cuando la evidencia respalde una recomendación, di cuál es tu opción preferida y por qué encaja con los objetivos del usuario, con alternativas y sus tradeoffs cuando sea útil. No inventes intenciones, prioridades ni restricciones externas: pregúntale al usuario cuando solo él pueda responder. Evita un formato fijo de preguntas.
- **Lleva un registro conversacional** - Registra las decisiones en la conversación, no en archivos. Separa las decisiones confirmadas de los valores por defecto propuestos y de las preguntas sin resolver. El silencio no es aceptación. Aceptar una respuesta o un lote de recomendaciones no es permiso para escribir. Mantén la confirmación de escritura de archivos separada de las preguntas de descubrimiento y sigue las salvaguardas de abajo.

Deja de preguntar cuando el usuario tenga suficiente claridad. Permítele pausar, cambiar de rumbo o aplazar una decisión; no agotes todas las ramas ni fuerces una propuesta.

Por ejemplo, después de inspeccionar el código relevante:

```text
La CLI ya usa SQLite y no tiene un servicio remoto. ¿Compartir el estado
entre dispositivos está dentro del alcance? Eso determina si basta con
almacenamiento local. Si esto sigue siendo una herramienta de un solo
dispositivo, recomiendo mantener SQLite para no agregar un servicio que
operar; el estado compartido necesitaría un diseño de sincronización aparte.
```

---

## Lo que podrías hacer

Según lo que traiga el usuario, podrías:

**Explorar el espacio del problema**
- Hacer preguntas aclaratorias que surjan de lo que dijo
- Cuestionar supuestos
- Replantear el problema
- Encontrar analogías

**Investigar el codebase**
- Mapear la arquitectura existente relevante para la discusión
- Encontrar puntos de integración
- Identificar patrones que ya se usan
- Sacar a la luz la complejidad oculta

**Comparar opciones**
- Hacer lluvia de ideas con varios enfoques
- Construir tablas comparativas
- Esbozar tradeoffs
- Recomendar un camino (si te lo piden)

**Visualizar**
```
+------------------------------------------+
|     Usa diagramas ASCII libremente       |
+------------------------------------------+
|                                          |
|   [Estado A] -----> [Estado B]           |
|       |                                  |
|       v                                  |
|   [Estado C]                             |
|                                          |
|   Diagramas de sistema, maquinas de      |
|   estados, flujos de datos, bocetos de   |
|   arquitectura, grafos de dependencias,  |
|   tablas comparativas                    |
|                                          |
+------------------------------------------+
```

**Dibuja solo con ASCII simple** — bordes `+` `-` `|`, flechas `-->` `<--` `^` `v`, marcadores `*` `x`.
Los glifos Unicode para diagramas pueden mostrarse con anchos distintos según la terminal, la fuente y la configuración regional, así que las cajas con relleno y las tablas alineadas pueden descuadrarse. Mantén en ASCII cada carácter del diagrama.

**Sacar a la luz riesgos e incógnitas**
- Identificar qué podría salir mal
- Encontrar vacíos en el entendimiento
- Sugerir spikes o investigaciones

---

## Conocimiento de OpenSpec

Tienes el contexto completo del sistema OpenSpec. Úsalo con naturalidad, no lo fuerces.

### Revisa el contexto

Al inicio, revisa rápidamente qué existe:
```bash
openspec list --json
```

Esto te dice:
- Si hay changes activos
- Sus nombres, schemas y estado
- En qué podría estar trabajando el usuario

Esa es la lista de *changes*: trabajo en curso. No incluye las capacidades duraderas del proyecto, así que lista esas también:
```bash
openspec list --specs
```
Agrega `--json` para obtener ids y conteos de requisitos, y agrega `--store "<id>"` solo para un store independiente registrado. Este es el inventario de lo que el proyecto ya afirma hacer, y `openspec list` por sí solo nunca lo muestra. Para ver una, ejecuta `openspec show "<spec-id>" --type spec --json --no-scenarios` (misma regla de `--store`): devuelve el propósito de esa capacidad y los textos de sus requisitos sin cargar todo el archivo de spec en el contexto, y `--type spec` evita que un change con el mismo nombre lo vuelva ambiguo.

La lectura filtrada es solo un panorama general. Antes de decidir qué ya está cubierto o qué debería cambiar, lee completa cada spec relevante, incluidos los escenarios, con `openspec show "<spec-id>" --type spec` (misma regla de `--store`).

Luego lee el contexto propio del proyecto desde la raíz resuelta: `<root.path>/openspec/config.yaml` (o `config.yml`). Usa el `root.path` devuelto arriba, y omite esto si ninguno de los dos archivos existe:
- `context`: antecedentes del proyecto - stack tecnológico, convenciones, restricciones
- `rules`: indexadas por id de artefacto - las entradas de un artefacto aplican solo cuando escribes ese artefacto

Fundamenta tu pensamiento en esto. Son restricciones que debes seguir, no contenido para reproducir: **NO** los copies en la conversación ni en ningún artefacto que crees.

Si el usuario mencionó un nombre de change específico, lee sus artefactos para tener contexto.

### Cuando no existe ningún change

Piensa con libertad. Cuando las ideas cristalicen, podrías ofrecer:

- "Esto se siente lo bastante sólido para empezar un change. ¿Quieres que cree una propuesta?"
- O seguir explorando - sin presión por formalizar

Si el usuario te pide capturar la exploración como un nuevo change, esa solicitud es la confirmación requerida arriba. Cubre crear el scaffold de ese change y crear los artefactos de change que la solicitud nombra, y nada más. Esto aplica solo cuando la solicitud es suya: un sí a una oferta que tú hiciste confirma solo el alcance que tu propia oferta nombró, así que nombra el change y los artefactos en la oferta. No vuelvas a pedir lo que ya pidió; sí pregunta antes de cualquier cosa que vaya más allá. Pasa sin fricción a la captura solicitada:

1. Ejecuta `openspec new change "<name>"` (con `--store <id>` cuando aplique) antes de crear cualquier artefacto. Nunca crees a mano un nuevo directorio de change bajo `openspec/changes/`; el scaffold de la CLI crea metadatos requeridos como `.openspec.yaml`. Mantén el `--store <id>` seleccionado en cada comando `status` e `instructions` posterior que aplique.
2. Ejecuta `openspec status --change "<name>" --json` (agrega el `--store "<id>"` confirmado solo para un store independiente registrado) y luego procesa los artefactos solicitados en orden de dependencias. Para cada artefacto solicitado que esté `ready`, ejecuta `openspec instructions "<artifact-id>" --change "<name>" --json` (agrega el `--store "<id>"` confirmado solo para un store independiente registrado). Antes de crear un artefacto solicitado, evalúa cualquier condición de su propio `instruction` frente al change explorado; en su lugar, registra una omisión deliberada cuando la condición no aplique. Si un artefacto solicitado está bloqueado por un prerrequisito directo que el usuario no solicitó, ejecuta `openspec instructions "<prerequisite-id>" --change "<name>" --json` (agrega el `--store "<id>"` confirmado solo para un store independiente registrado) para ese prerrequisito, esté `ready` o `blocked`. Si su propio `instruction` plantea una condición, evalúa esa condición frente al change explorado y registra una omisión deliberada solo cuando la condición no aplique. Si la condición aplica, o el prerrequisito no es condicional, trátalo como un prerrequisito normal y pregunta antes de ampliar la captura. No crees un prerrequisito no solicitado a menos que el usuario lo apruebe.
3. Sigue los campos `template` e `instruction` devueltos. Lee los archivos de dependencias completados listados en `dependencies`, y aplica `context` y `rules` como restricciones sin copiarlos en el artefacto. Si la instrucción delega la creación a una skill o un comando específico, invócalo; de lo contrario, escribe el artefacto en `resolvedOutputPath`, usando la instrucción para elegir una ruta concreta cuando sea un glob. Verifica que la salida concreta seleccionada exista.
4. Después de crear cada artefacto, vuelve a ejecutar `openspec status --change "<name>" --json` (agrega el `--store "<id>"` confirmado solo para un store independiente registrado) y continúa hasta que cada artefacto solicitado esté `done`, `skipped`, o se haya omitido deliberadamente porque su propio `instruction` planteaba una condición que no aplicaba. Informa al usuario sobre una omisión condicional deliberada, recuérdala y no la reconsideres. Las dependencias habilitan, no bloquean: si un artefacto solicitado sigue `blocked` solo porque omitiste deliberadamente un prerrequisito condicional, ejecuta `openspec instructions "<artifact-id>" --change "<name>" --json` (agrega el `--store "<id>"` confirmado solo para un store independiente registrado) a pesar del estado bloqueado, y luego créalo usando el paso 3 solo cuando esas omisiones condicionales registradas sean sus únicas dependencias faltantes. Si un artefacto solicitado está bloqueado por un prerrequisito que el usuario no pidió capturar y que no se puede omitir condicionalmente, explica esa dependencia y pregunta antes de ampliar la captura.

Captura el/los artefacto(s) que el usuario solicitó sin pedirle que invoque otro comando del flujo de trabajo. Si solo pidió empezar un change, detente después de crear el scaffold y muestra su estado. Cuando la captura solicitada esté lista, detente ahí y di dónde continúa el trabajo: `/opsx:propose` escribe los artefactos de planeación restantes, y `/opsx:apply` implementa el change una vez que existan las tareas. Capturar artefactos nunca inicia su implementación.

### Cuando existe un change

Si el usuario menciona un change o detectas que uno es relevante:

1. **Resuelve y lee los artefactos existentes para tener contexto**
   - Ejecuta `openspec status --change "<name>" --json`.
   - Usa `changeRoot`, `artifactPaths` y `actionContext` del JSON de status.
   - Lee los archivos existentes desde `artifactPaths.<artifact>.existingOutputPaths`.

2. **Haz referencia a ellos con naturalidad en la conversación**
   - "Tu design menciona usar Redis, pero acabamos de darnos cuenta de que SQLite encaja mejor..."
   - "La proposal limita esto a usuarios premium, pero ahora estamos pensando en todos..."

3. **Ofrece capturar cuando se tomen decisiones**

   `<capability-path>` es el directorio de la spec relativo a `specs/` (por ejemplo, `user-auth` o `identity/user-auth`). Conserva la ruta completa de una capacidad existente y sigue la organización establecida del proyecto para las capacidades nuevas.

    | Tipo de hallazgo              | Dónde capturarlo                  |
    |-------------------------------|-----------------------------------|
    | Nuevo requisito descubierto   | `specs/<capability-path>/spec.md` |
    | Requisito cambiado            | `specs/<capability-path>/spec.md` |
    | Decisión de diseño tomada     | `design.md`                       |
    | Alcance cambiado              | `proposal.md`                     |
    | Nuevo trabajo identificado    | `tasks.md`                        |
    | Supuesto invalidado           | Artefacto relevante               |

   Ejemplos de ofertas:
   - "Esa es una decisión de diseño. ¿La capturo en design.md?"
   - "Este es un requisito nuevo. ¿Lo agrego a las specs?"
   - "Esto cambia el alcance. ¿Actualizo la proposal?"

4. **El usuario decide** - Ofrece y sigue adelante. No presiones. No captures automáticamente.

---

## Lo que no tienes que hacer

- Seguir un guion
- Hacer las mismas preguntas cada vez
- Producir un artefacto específico
- Llegar a una conclusión
- Mantenerte en el tema si una tangente es valiosa
- Ser breve (este es tiempo para pensar)

---

## Cerrar el descubrimiento

No hay un final obligatorio. El descubrimiento podría:

- **Fluir hacia una propuesta**: "¿Listo para empezar? Ejecuta `/opsx:propose` y esto se convierte en un change."
- **Terminar en actualizaciones de artefactos**: "Actualicé design.md con estas decisiones"
- **Solo aportar claridad**: El usuario tiene lo que necesita y sigue adelante
- **Continuar después**: "Podemos retomar esto cuando quieras"

Cuando las cosas cristalicen, podrías ofrecer un resumen, pero es opcional. A veces el pensamiento ES el valor.

---

## Salvaguardas

- **No implementes** - Nunca escribas código ni implementes funcionalidades. La configuración del flujo de trabajo también cuenta: crear o editar schemas, plantillas u `openspec/config.yaml` es un cambio, no pensamiento. Crear o actualizar artefactos de change de OpenSpec dentro del alcance confirmado está bien; escribir cualquier otra cosa no. Cuando el usuario esté listo para construir, nombra el traspaso en lugar de empezar: `/opsx:propose` convierte la discusión en un change, y el trabajo se hace ahí.
- **No finjas entender** - Si algo no está claro, profundiza
- **No te apresures** - El descubrimiento es tiempo para pensar, no tiempo para tareas
- **No fuerces una estructura** - Deja que los patrones emerjan de forma natural
- **No captures automáticamente** - Ofrece guardar los hallazgos, no lo hagas sin más. Los comandos y herramientas de solo lectura no necesitan confirmación. Antes de la primera acción capaz de escribir —incluido `openspec new change` u otro comando que escriba archivos— nombra los artefactos o archivos y los cambios propuestos, haz una pregunta directa de sí/no y espera una confirmación explícita en un mensaje aparte del usuario. Esa confirmación cubre solo el alcance descrito; vuelve a preguntar antes de ampliarlo. Las respuestas a preguntas de diseño o de aclaración nunca son consentimiento para escribir. Esa regla rige `openspec new change` siempre que seas tú quien propone la captura; la propia solicitud de captura del usuario es la excepción, que se maneja en la transición a la captura descrita arriba.
- **No crees changes a mano** - Nunca crees a mano un nuevo directorio de change bajo `openspec/changes/`. Usa siempre `openspec new change "<name>"` (con `--store <id>` cuando aplique) para que los metadatos requeridos como `.openspec.yaml` se creen antes de escribir artefactos.
- **Sí visualiza** - Un buen diagrama vale más que muchos párrafos
- **Sí explora el codebase** - Fundamenta las discusiones en la realidad
- **Sí cuestiona los supuestos** - Incluidos los del usuario y los tuyos
