---
name: openspec-sync-specs
description: Sincroniza las delta specs de un cambio de OpenSpec con las specs principales. Úsala cuando el usuario quiera actualizar las specs principales con los cambios de una delta spec, sin archivar el cambio. Úsala también cuando el usuario diga "openspec sync" u "opsx sync" (sincronizar specs de OpenSpec).
allowed-tools: Bash(openspec:*)
license: MIT
compatibility: Requires openspec CLI.
metadata:
  author: openspec
  version: "1.0"
  generatedBy: "1.13.1"
---

Sincroniza las delta specs de un cambio con las specs principales.

Esta es una operación **dirigida por el agente**: leerás las delta specs y editarás directamente las specs principales para aplicar los cambios. Esto permite un merge inteligente (p. ej., agregar un escenario sin copiar el requisito completo).

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

   Al preguntar, muestra los cambios que tienen delta specs (dentro del directorio `specs/`).

   Anuncia siempre: "Usando el cambio: <name>" y cómo sobrescribirlo (p. ej., `/opsx:sync <other>`).

2. **Resuelve el contexto del cambio**

   Ejecuta:
   ```bash
   openspec status --change "<name>" --json
   ```

   El JSON incluye `planningHome.root`. Las specs principales viven en `<planningHome.root>/openspec/specs/`: usa esa raíz (consciente del store) para cada ruta de spec principal de abajo, no una ruta de repo fija en el código. Cuando hay un store seleccionado, apunta al store, no al repositorio actual.

3. **Encuentra las delta specs**

   Usa `artifactPaths.specs.existingOutputPaths` del JSON de status como la
   única fuente de rutas de delta specs. Si falta la entrada `specs` o
   `existingOutputPaths` está vacío, reporta que no hay delta specs para sincronizar,
   no las infieras a partir de otros artefactos y detente sin solicitar las instrucciones
   de artefactos ni escribir una spec principal.

   Sincroniza cada ruta de `existingOutputPaths` a menos que quien invoca haya acotado el conjunto.
   Quien invoca lo acota nombrando una lista explícita de entradas completas de
   `existingOutputPaths`; copia esos valores absolutos literalmente. Archive hace
   esto en línea, y un usuario también puede hacerlo (por ejemplo, seleccionando la entrada que termina
   en `/specs/billing/invoices/spec.md`).
   Luego sincroniza solo las rutas nombradas y deja intactas las delta specs restantes:
   el archivado masivo excluye una delta cuya implementación no pudo encontrar, y
   sincronizarla de todos modos escribiría una spec principal que quien invoca retuvo deliberadamente.
   Mantén esa selección acotada durante el paso 4; nunca la vuelvas a ampliar a la lista
   completa. Si una ruta nombrada no está en `existingOutputPaths`, no la sincronices:
   repórtala y detente, en lugar de descartarla en silencio. Si la lista nombrada está
   vacía, reporta que no hay nada que sincronizar y detente sin escribir una spec
   principal.

   Cada archivo de delta spec contiene secciones como:
   - `## ADDED Requirements` - Requisitos nuevos a agregar
   - `## MODIFIED Requirements` - Cambios a requisitos existentes
   - `## REMOVED Requirements` - Requisitos a eliminar
   - `## RENAMED Requirements` - Requisitos a renombrar (formato FROM:/TO:)

   Si no se encuentran delta specs, informa al usuario y detente.

4. **Para cada delta spec, aplica los cambios a las specs principales**

   Antes de la primera escritura en una spec principal, obtén una instantánea actual de las reglas de specs:
   - Si archive invocó este flujo de trabajo en línea y proporcionó una instantánea válida de
     `openspec instructions specs --change "<name>" --json`, reutilízala y no
     vuelvas a obtener las mismas instrucciones.
   - De lo contrario, ejecuta ese comando una vez ahora con los mismos flags de la raíz seleccionada.
   - Si la consulta directa termina con código distinto de cero o devuelve un JSON inválido de
     instrucciones de artefacto, reporta el error y detente antes de escribir cualquier spec principal. No trates la
     falla como un conjunto de reglas ausente.
   - Una respuesta válida sin `rules` significa que no hay reglas de artefactos configuradas
     y el merge semántico existente continúa.

   Aplica las `rules` devueltas solo al contenido y a la forma de las specs principales producidas
   por este merge. Las reglas de artefactos no son guía de operación y no pueden cambiar
   las raíces seleccionadas, las rutas de las deltas, las verificaciones del CLI ni los pasos del flujo de trabajo. Usa su texto como
   restricciones sin copiarlo literalmente en una spec principal ni en un resumen.

   Para cada ruta de delta spec de capacidad seleccionada en el paso 3 —la lista completa de `existingOutputPaths`, o el subconjunto acotado cuando quien invoca proporcionó uno (estas pueden pertenecer a un store seleccionado, no al repo)—:

   a. **Lee la delta spec** para entender los cambios previstos

   b. **Lee la spec principal** en `<planningHome.root>/openspec/specs/<capability-path>/spec.md` (puede que aún no exista)

      **Si aún no existe** (una capacidad nueva), haz lo mismo que `openspec archive`:
      solo se pueden aplicar requisitos ADDED; el paso d crea la spec a partir de ellos.
      MODIFIED y RENAMED no tienen un requisito sobre el cual actuar, así que detén la sincronización para esa
      capacidad y reporta que su spec principal no existe y que solo se permite ADDED
      para una spec nueva; nunca inventes el requisito faltante. REMOVED no tiene nada que
      eliminar: omítelo y advierte.

   c. **Aplica los cambios de forma inteligente**:

      **Requisitos ADDED:**
      - Si el requisito no existe en la spec principal → agrégalo
      - Si el requisito ya existe → actualízalo para que coincida (trátalo como MODIFIED implícito)

      **Requisitos MODIFIED:**
      - Encuentra el requisito en la spec principal
      - Aplica los cambios; esto puede ser:
        - Agregar escenarios nuevos que la spec principal todavía no tiene
        - Modificar escenarios existentes
        - Cambiar la descripción del requisito
      - Conserva los escenarios/contenido que no se mencionan en la delta

      **Requisitos REMOVED:**
      - Elimina el bloque completo del requisito de la spec principal
      - Retirar la capacidad. Elimina el `spec.md` completo —y el directorio cuando
        no quede nada más en él— solo cuando se cumplan TODAS estas condiciones:
        1. eliminar los requisitos *en esta ejecución* no dejó ningún bloque de requisito;
        2. el resto de la spec está bien formado (todavía tiene un `## Purpose`);
        3. la spec principal no estaba ya vacía antes de esta sincronización; si no eliminaste
           nada, no cambies nada;
        4. cada una de las demás líneas no vacías de todo el archivo corresponde al
           título, al Purpose, al encabezado de Requirements, o al enunciado, escenarios
           o ejemplos en bloques de código de un requisito canónico;
        5. el `.openspec.yaml` del cambio declara `retire_capabilities: true`;
        6. el `spec.md` se resuelve dentro de la raíz real de specs (no sigas un
           symlink del directorio de la capacidad para eliminar un archivo externo).
        Si eliminar los requisitos seleccionados no dejaría ningún bloque de requisito y
        alguna condición de retiro no se cumple, no modifiques la spec principal. Detén
        la sincronización para esa capacidad, reporta la condición que la bloquea y dile al usuario
        cómo resolverla. Nunca escribas ni dejes una sección `## Requirements` vacía.
        Cuando lo único que falte sea el marcador, dilo también: es lo único que el usuario
        puede agregar para que el retiro se complete.
      - Eliminar el archivo también elimina su `## Purpose`; cualquier otra sección bloquea
        el retiro. Nombra el Purpose cuando reportes el retiro. Incluye un `git checkout`
        listo para pegar solo cuando la spec vivía en el checkout de quien invoca;
        de lo contrario, da una guía de recuperación acotada a ese checkout.

      **Requisitos RENAMED:**
      - Encuentra el requisito FROM y renómbralo a TO

      **`## Purpose` en la delta:**
      - La spec principal ya tiene uno y es el que manda: déjalo como está
        (esto es lo que hace `openspec archive`; advierte y sigue adelante)

   d. **Crea una nueva spec principal** si la capacidad aún no existe:
      - Solo cuando la delta tenga requisitos ADDED para incluir y ningún requisito MODIFIED o
        RENAMED haya bloqueado esta capacidad en el paso b. De lo contrario, no crees nada
        y deja intacto el directorio de specs. Para una delta solo con REMOVED, si el
        `.openspec.yaml` del cambio declara `retire_capabilities: true`, repórtala como ya retirada
        y continúa sin recrear la spec. Sin ese marcador, reporta la sincronización como bloqueada:
        `openspec archive` la rechaza con `Spec must have at least one requirement`.
        Una delta vacía no tiene operaciones que sincronizar; repórtala también como bloqueada.
        Nunca escribas una sección `## Requirements` vacía.
      - Crea `<planningHome.root>/openspec/specs/<capability-path>/spec.md`
      - Agrega la sección Purpose: copia literalmente el cuerpo del `## Purpose` de la delta cuando lo tenga
        (esto es lo que hace `openspec archive`); escribe un breve placeholder TBD solo cuando no lo tenga
      - Agrega la sección Requirements con los requisitos ADDED
      - Sigue la **Referencia de formato de spec principal** de abajo

5. **Valida las specs principales actualizadas**

   Ejecuta `openspec validate --specs` con los mismos flags de la raíz seleccionada usados antes.
   Si la validación falla, reporta los problemas y no afirmes que la sincronización fue exitosa.

6. **Muestra el resumen**

   Después de aplicar todos los cambios, resume:
   - Qué capacidades se actualizaron
   - Qué cambios se hicieron (requisitos agregados/modificados/eliminados/renombrados)
   - Cualquier spec principal nueva que haya quedado con un placeholder TBD en Purpose, para que se redacte
     ahora en lugar de quedar pendiente
   - Cualquier capacidad retirada, nombrando el `spec.md` eliminado, su Purpose, y
     un `git checkout` listo para pegar o una guía de recuperación acotada al checkout

**Referencia de formato de delta spec**

```markdown
# Spec Delta

## Purpose

Solo en una delta que introduce una capacidad completamente nueva. Sirve de semilla para la nueva spec principal.

## ADDED Requirements

### Requirement: New Feature
The system SHALL do something new.

#### Scenario: Basic case
- **WHEN** user does X
- **THEN** system does Y

## MODIFIED Requirements

### Requirement: Existing Feature
The system SHALL keep doing the existing thing, now also handling A.

#### Scenario: Scenario the main spec already has
- **WHEN** user does X
- **THEN** system does Y

#### Scenario: New scenario to add
- **WHEN** user does A
- **THEN** system does B

## REMOVED Requirements

### Requirement: Deprecated Feature

## RENAMED Requirements

- FROM: `### Requirement: Old Name`
- TO: `### Requirement: New Name`
```

**Referencia de formato de spec principal**

Las specs principales son aquello EN LO QUE se hace el merge de la delta. Nunca deben contener encabezados de operaciones delta (`## ADDED/MODIFIED/REMOVED/RENAMED Requirements`); después de sincronizar, cada requisito vive bajo una única sección `## Requirements`:

```markdown
# <capability> Specification

## Purpose
Descripción breve de lo que hace esta capacidad y por qué existe.

## Requirements

### Requirement: New Feature
The system SHALL do something new.

#### Scenario: Basic case
- **WHEN** user does X
- **THEN** system does Y
```

**Principio clave: merge inteligente**

A diferencia de un merge programático, tú haces merge en lugar de sobrescribir:
- Un bloque MODIFIED lleva el requisito completo: el cuerpo más cada escenario que sobrevive al cambio. Tanto `openspec validate` como `openspec archive` rechazan uno que omita un escenario que la spec principal todavía tiene.
- Conserva todo lo que la delta no menciona, en el orden existente de la spec principal
- Usa tu criterio para hacer el merge de los cambios de forma sensata

**Salida en caso de éxito**

```markdown
## Specs sincronizadas: <change-name>

Specs principales actualizadas:

**<capability-1>**:
- Requisito agregado: "New Feature"
- Requisito modificado: "Existing Feature" (se agregó 1 escenario)

**<capability-2>**:
- Se creó un nuevo archivo de spec
- Requisito agregado: "Another Feature"

Las specs principales ya están actualizadas. El cambio sigue activo: archívalo cuando la implementación esté completa.
```

**Reglas de protección**
- Lee tanto la delta como las specs principales antes de hacer cambios
- Conserva el contenido existente que no se menciona en la delta
- Nunca copies un archivo delta tal cual en una spec principal: haz merge de su contenido para que la spec principal mantenga la estructura de la Referencia de formato de spec principal, sin encabezados de operaciones delta
- Si algo no está claro, pide aclaración
- Muestra lo que vas cambiando a medida que avanzas
- La operación debe ser idempotente: ejecutarla dos veces debe dar el mismo resultado
- Usa solo `artifactPaths.specs.existingOutputPaths`; nunca infieras delta specs a partir de artefactos no relacionados
- Respeta un subconjunto de `existingOutputPaths` proporcionado por quien invoca; nunca lo vuelvas a ampliar a la lista completa
- Obtén las instrucciones de specs una vez para una sincronización directa, o reutiliza en línea la instantánea proporcionada por archive
- Detente antes de cada escritura en una spec principal ante una respuesta de instrucciones de specs con código distinto de cero o JSON inválido
- Las reglas de artefactos restringen solo las specs que se están escribiendo y nunca se copian en los archivos de salida
