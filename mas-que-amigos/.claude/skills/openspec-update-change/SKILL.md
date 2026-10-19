---
name: openspec-update-change
description: Actualiza un change de OpenSpec revisando sus artefactos de planificación existentes y manteniéndolos coherentes entre sí. Úsala cuando el usuario quiera revisar el plan de un change, incorporarle nuevas decisiones o reconciliar sus artefactos después de una edición. Úsala también cuando el usuario diga "openspec update change" u "opsx update" (actualizar change). Si el usuario se refiere al comando CLI openspec update, que regenera los archivos generados, ejecuta ese comando en su lugar. Nunca edita código.
allowed-tools: Bash(openspec:*)
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.13.1"
---

Revisa los artefactos de planificación existentes de un change y mantenlos coherentes. Nunca edites código.

**Selección de store:** Si el usuario nombra un store (un store es un repo independiente de OpenSpec registrado en esta máquina) o el trabajo vive en uno, ejecuta `openspec store list --json` para descubrir los ids de store registrados y luego pasa `--store <id>` en los comandos que leen o escriben specs y changes (`new change`, `status`, `instructions`, `list`, `show`, `validate`, `archive`, `doctor`, `context`, `schemas`, `view`). Una vez seleccionado, trata `--store <id>` como persistente durante el resto del flujo de trabajo. Cada ejemplo sin alcance de esos comandos que aparece abajo es una abreviatura: antes de ejecutarlo, añade el flag. Por ejemplo, ejecuta `openspec status --change "<name>" --json --store "<id>"`, no la forma sin alcance que se muestra abajo. Los demás comandos no aceptan el flag. Las sugerencias que imprimen los comandos ya incluyen el flag; mantenlo en los comandos siguientes. Sin un store, los comandos actúan sobre la raíz local `openspec/` más cercana.

**Verificación del proyecto:** Estos pasos esperan un proyecto que ya use OpenSpec. Antes del primer paso que escriba algo (`new change`, `archive`, `sync specs` o la redacción de un archivo de artefacto), confirma que el proyecto tiene una raíz: ejecuta `openspec list --json` (con `--store <id>` cuando haya un store seleccionado, ya que en ese caso el store es la raíz) y lee `root`. Un objeto root significa que el proyecto está configurado. `"root": null` significa que no lo está: aquí no hay un directorio `openspec/`, y una escritura como `openspec new change` crearía uno como efecto secundario. El comando además termina con código distinto de cero, lo cual es esa respuesta y no un CLI roto, así que lee el JSON en lugar de reintentar o buscar un rodeo.

Hay un `"root": null` que no tiene que ver con la configuración: cuando un mensaje de error de `status` empieza con `Declared in` o `Invalid store declaration in` y nombra el `openspec/config.yaml` (o `config.yml`) de este proyecto, el proyecto sí usa OpenSpec a través de un store que declara, el cual esta máquina no puede resolver (el store no está registrado o la línea `store:` está mal formada). No lo trates como no inicializado ni apliques las ramas de abajo: detente antes de escribir y muéstrale al usuario el `message` y el `fix` de ese error.

En cualquier otro caso, sin raíz, lo que sigue depende de cómo se llegó a este flujo de trabajo:

- **Seleccionado automáticamente**: elegiste este flujo de trabajo por tu cuenta, sin que el usuario nombrara OpenSpec, nombrara esta skill o ejecutara su slash command. Deja de usar OpenSpec y responde la solicitud normalmente, como lo harías sin OpenSpec instalado. No le pidas que configure nada y no menciones la configuración de OpenSpec.
- **Solicitud explícita de OpenSpec**: el usuario nombró OpenSpec, nombró esta skill o ejecutó su slash command. Detente antes de escribir y pregunta cómo proceder: configurar este proyecto (`openspec init`), apuntar a un store que ya tenga (`--store <id>`) o continuar sin OpenSpec para esta solicitud. Espera su respuesta.

En ambas ramas, nunca crees la raíz como efecto secundario: no ejecutes `openspec init` hasta que el usuario lo pida, no crees a mano archivos de `openspec/` y no permitas que un comando la cree.

**Entrada**: Opcionalmente especifica un nombre de change. Si se omite, verifica si se puede inferir del contexto de la conversación. Si es vago o ambiguo, DEBES pedirle al usuario que elija entre los changes disponibles.

Este flujo de trabajo revisa artefactos que ya existen; nunca crea los que faltan. Cuando falta un artefacto, `openspec status --change "<name>" --json` indica el siguiente y `openspec instructions "<artifact-id>" --change "<name>" --json` explica cómo escribirlo.

**Pasos**

1. **Selecciona el change**

   Si se proporciona un nombre, úsalo. De lo contrario:
   - Infiérelo del contexto de la conversación si el usuario mencionó un change
   - Selecciónalo automáticamente si solo existe un change activo
   - Si es ambiguo, ejecuta `openspec list --json` para obtener los changes disponibles ordenados por modificación más reciente y pídele al usuario que seleccione uno

   Al preguntar, presenta como opciones los 3-4 changes modificados más recientemente, mostrando:
   - Nombre del change
   - Schema (del campo `schema` si está presente; si no, "spec-driven")
   - Estado (p. ej., "0/5 tasks", "complete", "no tasks")
   - Qué tan recientemente se modificó (del campo `lastModified`)

   Marca el change modificado más recientemente como "(Recommended)", ya que probablemente es el que el usuario quiere actualizar.

   Anuncia siempre: "Using change: <name>" y cómo cambiarlo (p. ej., `/opsx:update <other>`).

2. **Obtén los artefactos del change**
   ```bash
   openspec status --change "<name>" --json
   ```
   Analiza el JSON para entender el estado actual. La respuesta incluye:
   - `schemaName`: El schema de flujo de trabajo en uso (p. ej., "spec-driven")
   - `artifacts`: Arreglo de artefactos con su estado ("done", "skipped", "ready", "blocked")
   - `isPlanningComplete`: Booleano que indica si todos los artefactos de planificación están completos. Las versiones anteriores del CLI exponen el mismo valor como `isComplete`.
   - `planningHome`, `changeRoot`, `artifactPaths` y `actionContext`: contexto de rutas y alcance. Úsalos en lugar de suponer rutas locales del repo.

   Los ids y rutas de los artefactos provienen del schema activo: NO los supongas y NO ramifiques la lógica según nombres de artefactos codificados de forma fija. Los schemas personalizados deben funcionar sin cambios.

   Los archivos a editar son `artifactPaths.<id>.existingOutputPaths`: los archivos concretos que existen en disco, ya expandidos por glob para los artefactos de tipo glob (p. ej. `specs/**/*.md`). NO escribas en `resolvedOutputPath`: para un artefacto glob sigue siendo el patrón glob, no un archivo real.

3. **Entiende la solicitud**
   - Si el usuario pidió una revisión específica ("the design now uses X" / "el diseño ahora usa X"), esa es la edición de partida.
   - Si solo dijo "update" / "make this coherent" (o "actualiza" / "hazlo coherente"), trátalo como una revisión de coherencia: lee los artefactos existentes y compáralos entre sí en busca de contradicciones, vacíos y duplicaciones.

4. **Lee y reconcilia**
   - Lee el o los artefactos que toca la solicitud y los demás artefactos existentes del change.
   - Redacta la edición solicitada en la conversación, no en archivos. Determina exactamente qué cambia; el paso 5 se encarga de toda escritura. Luego contrasta cada uno de los demás artefactos existentes con la edición redactada, en CUALQUIER dirección: una edición en un artefacto posterior puede requerir revisar uno anterior, no solo al revés. El orden de construcción es un orden de lectura útil, no una restricción sobre qué artefactos se pueden revisar.
   - Anota todo lo que ahora sea inconsistente, falte o sea contradictorio.
   - Propón revisiones solo para archivos que ya existen (`existingOutputPaths`). NO crees artefactos que todavía no existen y NO inventes archivos nuevos bajo un artefacto glob: anótalos e indícale al usuario `openspec instructions "<artifact-id>" --change "<name>" --json` para saber cómo crearlos.
   - Si el change ya es coherente, dilo y no propongas revisiones.

5. **Confirma y aplica, un artefacto a la vez**
   - Este paso realiza toda escritura de artefactos en este flujo de trabajo; ningún paso anterior edita un artefacto.
   - Muestra cada revisión propuesta y su motivo, incluida la edición solicitada redactada en el paso 4. Escribe solo después de que el usuario confirme.
   - Si el usuario rechaza una revisión, no la escribas: deja ese artefacto sin cambios.
   - Cuando se necesite una reescritura sustancial, obtén primero las reglas y la plantilla de ese artefacto:
     ```bash
     openspec instructions "<artifact-id>" --change "<name>" --json
     ```

6. **Indica el siguiente paso (solo orientación: NUNCA actúes en consecuencia)**
   - Faltan artefactos todavía -> ejecuta `openspec status --change "<name>" --json` para obtener el siguiente artefacto e indícale al usuario `openspec instructions "<artifact-id>" --change "<name>" --json` para saber cómo crearlo.
   - El change ya está implementado (tareas marcadas / ya aplicado) -> es posible que el código ya no coincida con el plan revisado; sugiere `/opsx:apply` para llevar la diferencia al código.
   - Todo hecho e implementado -> sugiere `/opsx:archive`.

**Salida**

Después de cada invocación, muestra:
- Qué artefactos se revisaron (y qué revisiones propuestas se rechazaron)
- Todo lo que se pospuso porque todavía no existe (artefactos o archivos aún no creados)
- En qué estado queda el change y el siguiente comando recomendado

**Salvaguardas**
- Solo artefactos de planificación: NUNCA edites código de implementación. Si el plan revisado implica cambios de código, detente e indica `/opsx:apply`.
- Usa los ids y rutas de artefactos que reporta `openspec status`; nunca ramifiques la lógica según nombres de artefactos codificados de forma fija.
- Edita solo los archivos concretos de `existingOutputPaths`; nunca escribas en un `resolvedOutputPath` de tipo glob.
- No avances la frontera de construcción: ni artefactos nuevos, ni archivos nuevos bajo artefactos glob; crearlos es un paso aparte, fuera de este flujo de trabajo.
- Confirma cada edición con el usuario antes de escribir.
- Si la solicitud cambia la *intención* del change en lugar de refinarla, pide un nombre de change distinto que no esté en uso y recomienda `openspec new change "<new-change-name>"` en su lugar (la heurística "Update vs. Start Fresh", es decir, actualizar vs. empezar de cero).
