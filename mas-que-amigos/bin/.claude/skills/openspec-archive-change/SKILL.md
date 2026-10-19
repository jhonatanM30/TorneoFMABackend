---
name: openspec-archive-change
description: Archiva un cambio de OpenSpec completado en el flujo de trabajo experimental. Úsala cuando el usuario quiera finalizar y archivar un cambio después de completar la implementación. Úsala también cuando el usuario diga "openspec archive" u "opsx archive" (archivar cambio de OpenSpec).
allowed-tools: Bash(openspec:*)
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.13.1"
---

Archiva un cambio completado en el flujo de trabajo experimental.

**Selección de store:** Si el usuario nombra un store (un store es un repo de OpenSpec independiente registrado en esta máquina) o el trabajo vive en uno, ejecuta `openspec store list --json` para descubrir los ids de store registrados y luego pasa `--store <id>` en los comandos que leen o escriben specs y cambios (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`, `schemas`, `view`). Una vez seleccionado, trata `--store <id>` como persistente durante el resto del flujo de trabajo. Cada ejemplo sin alcance de esos comandos que aparece abajo es una abreviatura: antes de ejecutarlo, agrega el flag. Por ejemplo, ejecuta `openspec status --change "<name>" --json --store "<id>"`, no la forma sin alcance que se muestra abajo. Los demás comandos no aceptan el flag. Las sugerencias que imprimen los comandos ya incluyen el flag; mantenlo en los comandos siguientes. Sin un store, los comandos actúan sobre la raíz local `openspec/` más cercana.

**Verificación del proyecto:** Estos pasos esperan un proyecto que ya use OpenSpec. Antes del primer paso que escriba algo (`new change`, `archive`, `sync specs`, o la redacción de un archivo de artefacto), confirma que el proyecto tiene una raíz: ejecuta `openspec list --json` (con `--store <id>` cuando haya un store seleccionado, ya que en ese caso el store es la raíz) y lee `root`. Un objeto root significa que el proyecto está configurado. `"root": null` significa que no lo está: aquí no hay un directorio `openspec/`, y una escritura como `openspec new change` crearía uno como efecto secundario. El comando además termina con código distinto de cero, lo cual es esa respuesta y no un CLI roto, así que lee el JSON en lugar de reintentar o buscar una forma de sortearlo.

Hay un caso de `"root": null` que no tiene que ver con la configuración: cuando un mensaje de error de `status` empieza con `Declared in` o `Invalid store declaration in` y nombra el `openspec/config.yaml` (o `config.yml`) de este proyecto, el proyecto sí usa OpenSpec a través de un store que declara, el cual esta máquina no puede resolver (el store no está registrado, o la línea `store:` está mal formada). No lo trates como no inicializado ni omitas las ramas de abajo: detente antes de escribir y muéstrale al usuario el `message` y el `fix` de ese error.

De lo contrario, sin raíz, lo que sigue depende de cómo se llegó a este flujo de trabajo:

- **Autoseleccionado**: tú mismo elegiste este flujo de trabajo, sin que el usuario nombrara OpenSpec, nombrara esta skill o ejecutara su slash command. Deja de usar OpenSpec y responde la solicitud normalmente, como lo harías si OpenSpec no estuviera instalado. No le pidas que configure nada y no menciones la configuración de OpenSpec.
- **Solicitud explícita de OpenSpec**: el usuario nombró OpenSpec, nombró esta skill o ejecutó su slash command. Detente antes de escribir y pregunta cómo proceder: configurar este proyecto (`openspec init`), apuntar a un store que ya tenga (`--store <id>`), o continuar sin OpenSpec para esta solicitud. Espera su respuesta.

En ambas ramas, nunca crees la raíz como efecto secundario: no ejecutes `openspec init` hasta que el usuario lo pida, no crees a mano archivos de `openspec/` y no permitas que un comando la cree.

`<capability-path>` es el directorio de la spec relativo a `specs/` (por ejemplo, `user-auth` o `identity/user-auth`). Conserva la ruta completa de cada delta spec al resolver su spec principal.

**Entrada**: Opcionalmente especifica un nombre de cambio. Si se omite, revisa si se puede inferir del contexto de la conversación. Si es vago o ambiguo, **DEBES** pedir al usuario que elija entre los cambios disponibles.

**Pasos**

1. **Selecciona el cambio**

   Si se proporciona un nombre, úsalo. De lo contrario:
   - Infiérelo del contexto de la conversación si el usuario mencionó un cambio
   - Selecciónalo automáticamente si solo existe un cambio activo
   - Si es ambiguo, ejecuta `openspec list --json` para obtener los cambios disponibles y pide al usuario que seleccione uno

   Al preguntar, muestra solo los cambios activos (no los ya archivados).
   Incluye el schema usado por cada cambio si está disponible.

   Anuncia siempre: "Usando el cambio: <name>" y cómo sobrescribirlo (p. ej., `/opsx:archive <other>`).

   **Carga las entradas actuales de archivado antes de las verificaciones de archivado existentes:**

   Después de resolver el cambio seleccionado y la raíz de planificación, ejecuta:
   ```bash
   openspec instructions archive --change "<name>" --json
   ```
   Mantén en este comando los mismos flags de la raíz seleccionada. Esta consulta es orientativa y
   opcional: solo aporta entradas adicionales al prompt, así que nunca debe bloquear el archivado.
   Si termina con código distinto de cero o devuelve JSON inválido —por ejemplo, en un CLI más antiguo que
   todavía no soporta este comando—, continúa el flujo de archivado sin
   contexto y sin guía de operación. No reportes un error y no te detengas.

   Una respuesta exitosa puede omitir ambos campos opcionales. Trata `context` como una
   entrada obligatoria a nivel de prompt: léela y tenla en cuenta, y aplica los hechos,
   convenciones y restricciones relevantes del proyecto. Trata `operationGuidance` como un consejo
   opcional y adicional: lee y considera cada entrada, y sigue las entradas que sean
   aplicables y compatibles con el flujo de archivado integrado.

   Mantén ambos campos separados de los pasos integrados, las elecciones explícitas del usuario, las rutas
   resueltas, las verificaciones del CLI y los contratos de los comandos. Si el contexto entra en conflicto con una de esas
   entradas de control, reporta el conflicto y conserva el valor de control. Si la
   guía no es aplicable o entra en conflicto con una entrada de control, no la sigas
   y explica por qué. No infieras rutas de reemplazo, preguntas omitidas ni flags a partir de
   ninguno de los dos campos, y no copies su texto literalmente en specs, artefactos del cambio
   o resúmenes de archivado, a menos que el usuario lo pida por separado. Estos son
   contratos de comportamiento a nivel de prompt, no verificaciones obligatorias.

2. **Verifica el estado de completitud de los artefactos**

   Ejecuta `openspec status --change "<name>" --json` para verificar la completitud de los artefactos.

   Analiza el JSON para entender:
   - `schemaName`: El flujo de trabajo que se está usando
   - `planningHome`, `changeRoot`, `artifactPaths` y `actionContext`: contexto de rutas y alcance
   - `artifacts`: Lista de artefactos con su estado (`done`, `skipped` u otro)

   **Si algún artefacto no está ni en `done` ni en `skipped`** (los artefactos omitidos satisfacen el requisito: el cambio declara skip_specs):
   - Muestra una advertencia que liste los artefactos incompletos
   - Pide al usuario que confirme que quiere continuar
   - Continúa si el usuario confirma

3. **Verifica el estado de completitud de las tareas**

   Lee el archivo de tareas (normalmente `tasks.md`) para buscar tareas incompletas.

   Una casilla está completa cuando su único contenido es `x` o `X`; los espacios dentro
   de los corchetes no importan, así que `- [ x]` también cuenta como completa. Cualquier
   otro marcador está incompleto: `- [ ]`, un `- []` vacío y marcadores a los que OpenSpec
   no asigna ningún significado, como `- [~]` o `- [-]`. Nunca interpretes un marcador
   desconocido como completo.

   **Si se encuentran tareas incompletas:**
   - Muestra una advertencia con la cantidad de tareas incompletas
   - Pide al usuario que confirme que quiere continuar
   - Continúa si el usuario confirma

   **Si no existe archivo de tareas:** Continúa sin advertencia relacionada con tareas.

4. **Evalúa el estado de sincronización de las delta specs**

   Usa `artifactPaths.specs.existingOutputPaths` del JSON de status como la única
   fuente de delta specs. Si falta la entrada `specs` o
   `existingOutputPaths` está vacío, continúa sin preguntar por la sincronización y no infieras
   delta specs a partir de otros artefactos.

   **Si existen delta specs:**
   - Compara cada delta spec con su spec principal correspondiente en `<planningHome.root>/openspec/specs/<capability-path>/spec.md` (usa el `planningHome.root` consciente del store del paso 2, no una ruta de repo fija en el código)
   - Una spec principal inexistente **no es automáticamente** "ya sincronizada". Para una capacidad nueva, la spec principal es una *salida* de la sincronización, no una entrada:
     - Si la delta tiene requisitos MODIFIED o RENAMED, reporta que solo los requisitos ADDED pueden crear una nueva spec principal y marca esa capacidad como bloqueada para sincronizar. Nunca inventes un requisito que no tenga una versión actual.
     - De lo contrario, si la delta solo tiene requisitos REMOVED y el `.openspec.yaml` del cambio declara `retire_capabilities: true`, la capacidad ya está retirada: cuéntala como ya sincronizada, advierte que no queda nada por eliminar y no recrees la spec principal. Aplica esta regla tanto ahora como al verificar una sincronización completada.
     - De lo contrario, si la delta no tiene requisitos ADDED, reporta que no es posible sincronizar y marca esa capacidad como bloqueada para sincronizar. Para una delta solo con REMOVED, advierte que no hay spec principal de la cual eliminar y deja sin cambios el árbol de specs principales. `openspec archive` rechaza el caso solo con REMOVED sin marcar con `Spec must have at least one requirement`.
     - De lo contrario, cuenta la capacidad como pendiente de sincronizar y nómbrala en el resumen (`<capability-path>: new main spec will be created`). Si la delta también tiene requisitos REMOVED, advierte que se ignorarán porque no hay spec principal de la cual eliminar. La sincronización crea la spec principal únicamente a partir de los requisitos ADDED de la delta, exactamente como lo hace `openspec archive`.
   - Determina qué cambios se aplicarían (adiciones, modificaciones, eliminaciones, renombres)
   - Continúa evaluando las capacidades restantes aunque una esté bloqueada para sincronizar. Muestra un resumen combinado antes de preguntar.

   **Opciones a preguntar:**
   - Si alguna capacidad está bloqueada para sincronizar: explica por qué y ofrece solo "Archivar sin sincronizar", "Cancelar"
   - De lo contrario, si se necesitan cambios: "Sincronizar ahora (recomendado)", "Archivar sin sincronizar"
   - De lo contrario, si ya está sincronizado: "Archivar ahora", "Sincronizar de todos modos", "Cancelar"

   Actúa según la respuesta:
   - "Cancelar": detente, no archives
   - "Archivar sin sincronizar" o "Archivar ahora": continúa con el archivado
   - "Sincronizar ahora" o "Sincronizar de todos modos": sincroniza y luego verifica (ver abajo). No inicies ninguna sincronización mientras una capacidad esté bloqueada para sincronizar; explica el bloqueo y repite las opciones disponibles.
   - Cualquier otra cosa: vuelve a preguntar en lugar de archivar

   Antes de que una sincronización seleccionada escriba cualquier spec principal, ejecuta
   `openspec instructions specs --change "<name>" --json` una vez con los mismos
   flags de la raíz seleccionada. Exige un código de salida cero y un JSON válido de
   instrucciones de artefacto. Si la consulta falla o devuelve JSON inválido, reporta el error y detente
   antes de escribir cualquier spec principal o de mover el cambio. Una respuesta válida sin
   `rules` es el caso sin reglas. Aplica las `rules` devueltas solo al contenido y a la
   forma de las specs principales producidas por este merge; no las uses como guía de archivado,
   no cambies el comportamiento del CLI ni copies el texto de las reglas en ningún archivo de salida.

   Luego ejecuta el flujo de trabajo `openspec-sync-specs` en línea (merge inteligente dirigido por el agente) para el cambio '<name>', pasándole el análisis de las delta specs y la instantánea de reglas de specs obtenida arriba, y espera a que termine. La sincronización en línea debe reutilizar esa instantánea sin volver a obtener las instrucciones de `specs`. No la delegues a una tarea en segundo plano: el paso 5 movería `changeRoot` mientras una sincronización todavía lo está leyendo, dejando el cambio archivado y las specs principales sin actualizar nunca. Si tu agente solo puede ejecutarla mediante delegación, delega de forma síncrona y espera el resultado.

   Luego vuelve a ejecutar la comparación desde el inicio de este paso, incluido el caso explícitamente retirado sin spec principal, contra cada capacidad que tenga una delta spec en `artifactPaths.specs.existingOutputPaths`, no solo las que la sincronización reporta haber tocado. Una sincronización exitosa no deja nada pendiente por aplicar, así que cada capacidad debe leerse ahora como ya sincronizada:
   - Requisitos ADDED presentes
   - Requisitos MODIFIED con los cambios de escenarios y descripción indicados en la delta, con sus demás escenarios intactos
   - Requisitos REMOVED eliminados; y donde esta sincronización retiró una capacidad (eliminó su último requisito, dejando `## Requirements` vacío), su spec principal eliminada en lugar de dejarla vacía; una spec que la sincronización conservó deliberadamente y reportó también cuenta como coincidencia
   - Requisitos RENAMED presentes con el nombre nuevo y ausentes con el nombre anterior

   Si la sincronización falló, o alguna capacidad no coincide, reporta qué difiere y detente: no archives. Nada se ha movido y `changeRoot` está intacto, así que el usuario puede corregir la discrepancia o volver a ejecutar la sincronización e iniciar el archivado de nuevo.

5. **Realiza el archivado**

   Crea un directorio `archive` dentro de `planningHome.changesDir` si no existe:
   ```bash
   mkdir -p "<planningHome.changesDir>/archive"
   ```

   Genera el nombre de destino: usa el nombre del cambio tal cual cuando ya empiece con un prefijo `YYYY-MM-DD-`; de lo contrario, antepón la fecha actual como `YYYY-MM-DD-<change-name>`. Nunca apiles una segunda fecha (la misma regla que `openspec archive`).

   **Verifica si el destino ya existe:**
   - Si sí: Falla con un error y sugiere renombrar el archivo existente o usar una fecha diferente
   - Si no: Mueve `changeRoot` al directorio de archivo

   ```bash
   mv "<changeRoot>" "<planningHome.changesDir>/archive/<target-name>"
   ```

6. **Muestra el resumen**

   Muestra un resumen de la finalización del archivado que incluya:
   - Nombre del cambio
   - Schema que se usó
   - Ubicación del archivo
   - Si las specs se sincronizaron (si aplica)
   - Nota sobre cualquier advertencia (artefactos/tareas incompletos)

**Salida en caso de éxito**

```markdown
## Archivado completado

**Cambio:** <change-name>
**Schema:** <schema-name>
**Archivado en:** la ruta de archivo derivada de `planningHome.changesDir`/<target-name>/
**Specs:** <"✓ Sincronizadas con las specs principales" solo si la verificación del paso 4 pasó; de lo contrario, "Sin delta specs" o "Sincronización omitida">

<"Todos los artefactos completos. Todas las tareas completas." — o, si se archivó con advertencias, lístalas en su lugar (p. ej. "Archivado con 2 tareas incompletas")>
```

**Reglas de protección**
- Anuncia el cambio seleccionado; pide que se seleccione cuando sea ambiguo
- Usa el grafo de artefactos (openspec status --json) para verificar la completitud
- No bloquees el archivado por advertencias: solo informa y confirma
- Conserva .openspec.yaml al mover al archivo (se mueve junto con el directorio)
- Muestra un resumen claro de lo que ocurrió
- Si se solicita sincronizar, ejecuta el flujo de trabajo `openspec-sync-specs` en línea (dirigido por el agente)
- Nunca archives mientras una sincronización de specs siga en curso: ejecuta la sincronización en línea y verifica las specs principales antes de mover `changeRoot`
- Si existen delta specs, ejecuta siempre la evaluación de sincronización y muestra el resumen combinado antes de preguntar
- Aplica el contexto de ejecución relevante y reporta los conflictos; la guía de operación sigue siendo orientativa
- Considera cada entrada de la guía y explica cualquier consejo no aplicable o en conflicto
- Las verificaciones existentes del CLI, las rutas resueltas, las preguntas y los contratos de los comandos no cambian
- Las reglas de artefactos restringen solo las specs que se están escribiendo y nunca son guía de operación
- Nunca copies literalmente el texto del contexto de ejecución, de la guía de operación o de las reglas de artefactos en los archivos de salida
