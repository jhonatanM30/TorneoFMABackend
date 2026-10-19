---
name: using-git-worktrees
description: Úsala al iniciar trabajo en una funcionalidad que necesite aislamiento del espacio de trabajo actual o antes de ejecutar planes de implementación; garantiza que exista un espacio de trabajo aislado mediante herramientas nativas o, como alternativa, git worktree
author: LIDR.co
version: 1.0.0
---

# Uso de Git Worktrees

## Descripción general

Asegúrate de que el trabajo ocurra en un espacio de trabajo aislado. Prefiere las herramientas nativas de worktree de tu plataforma. Recurre a git worktrees manuales solo cuando no haya ninguna herramienta nativa disponible.

**Principio central:** Primero detecta el aislamiento existente. Luego usa las herramientas nativas. Luego recurre a git. Nunca pelees con el harness.

**Anuncia al inicio:** "Estoy usando la skill using-git-worktrees para configurar un espacio de trabajo aislado."

## Paso 0: Detecta el aislamiento existente

**Antes de crear cualquier cosa, verifica si ya estás en un espacio de trabajo aislado.**

```bash
GIT_DIR=$(cd "$(git rev-parse --git-dir)" 2>/dev/null && pwd -P)
GIT_COMMON=$(cd "$(git rev-parse --git-common-dir)" 2>/dev/null && pwd -P)
BRANCH=$(git branch --show-current)
```

**Protección para submódulos:** `GIT_DIR != GIT_COMMON` también es verdadero dentro de submódulos de git. Antes de concluir "ya estoy en un worktree", verifica que no estés en un submódulo:

```bash
# If this returns a path, you're in a submodule, not a worktree — treat as normal repo
git rev-parse --show-superproject-working-tree 2>/dev/null
```

**Si `GIT_DIR != GIT_COMMON` (y no es un submódulo):** Ya estás en un worktree vinculado. Salta al Paso 3 (Configuración del proyecto). NO crees otro worktree.

Reporta con el estado del branch:
- En un branch: "Ya estoy en un espacio de trabajo aislado en `<path>` en el branch `<name>`."
- HEAD desacoplado (detached HEAD): "Ya estoy en un espacio de trabajo aislado en `<path>` (detached HEAD, gestionado externamente). Se necesitará crear un branch al finalizar."

**Si `GIT_DIR == GIT_COMMON` (o estás en un submódulo):** Estás en un checkout normal del repo.

¿El usuario ya indicó su preferencia sobre worktrees en tus instrucciones? Si no, pide su consentimiento antes de crear un worktree:

> "¿Quieres que configure un worktree aislado? Protege tu branch actual de los cambios."

Respeta cualquier preferencia ya declarada sin preguntar. Si el usuario no da su consentimiento, trabaja en el lugar actual y salta al Paso 3.

## Paso 1: Crea el espacio de trabajo aislado

**Tienes dos mecanismos. Pruébalos en este orden.**

### 1a. Herramientas nativas de worktree (preferido)

El usuario pidió un espacio de trabajo aislado (consentimiento del Paso 0). ¿Ya tienes una forma de crear un worktree? Puede ser una herramienta con un nombre como `EnterWorktree`, `WorktreeCreate`, un comando `/worktree` o un flag `--worktree`. Si la tienes, úsala y salta al Paso 3.

Las herramientas nativas manejan automáticamente la ubicación del directorio, la creación del branch y la limpieza. Usar `git worktree add` cuando tienes una herramienta nativa crea un estado fantasma que tu harness no puede ver ni gestionar.

Si el flujo nativo no propaga la configuración de Claude/Cursor y el usuario espera paridad con el checkout principal, copia `.claude/settings.json` y `.claude/settings.local.json` desde el espacio de trabajo principal usando el mismo loop del Paso 1b **después** de que termine la herramienta nativa, solo cuando los archivos existan en disco.

Continúa al Paso 1b solo si no tienes ninguna herramienta nativa de worktree disponible.

### 1b. Alternativa con Git Worktree

**Usa esto solo si el Paso 1a no aplica**: no tienes ninguna herramienta nativa de worktree disponible. Crea un worktree manualmente usando git.

#### Selección del directorio

Usa una única ubicación: `.worktrees/` dentro de la raíz del repositorio.

1. Define la raíz del repositorio y el directorio base de los worktrees:
   ```bash
   SOURCE_ROOT=$(git rev-parse --show-toplevel)
   LOCATION="$SOURCE_ROOT/.worktrees"
   ```

2. Asegúrate de que el directorio exista:
   ```bash
   mkdir -p "$LOCATION"
   ```

3. Crea siempre el worktree en:
   ```bash
   path="$LOCATION/$BRANCH_NAME"
   ```

No se deben usar alternativas globales/hermanas a menos que el usuario anule explícitamente esta regla para una tarea puntual.

#### Verificación de seguridad

**DEBES verificar que `.worktrees/` esté ignorado antes de crear el worktree:**

```bash
git check-ignore -q .worktrees 2>/dev/null
```

**Si NO está ignorado:** Agrega `.worktrees/` a `.gitignore`, haz commit del cambio y luego continúa.

**Por qué es crítico:** Evita hacer commit accidentalmente del contenido del worktree en el repositorio.

#### Crea el worktree

**Captura la raíz del checkout principal antes de `git worktree add`.** Después de hacer `cd` al nuevo worktree, `git rev-parse --show-toplevel` apunta al directorio del worktree, no al checkout donde normalmente está `.claude/settings*.json`.

```bash
project=$(basename "$(git rev-parse --show-toplevel)")
SOURCE_ROOT=$(git rev-parse --show-toplevel)

# Path is always inside repo-local .worktrees/
path="$LOCATION/$BRANCH_NAME"

git worktree add "$path" -b "$BRANCH_NAME"
cd "$path"
```

**Alternativa por sandbox:** Si `git worktree add` falla con un error de permisos (denegación del sandbox), dile al usuario que el sandbox bloqueó la creación del worktree y que, en su lugar, estás trabajando en el directorio actual. Luego ejecuta la configuración y las pruebas de baseline en el lugar actual.

#### Copia la configuración de Claude (solo Paso 1b)

Después de crear el worktree con git (Paso 1b), copia la configuración local de Claude desde el **checkout principal** para que el comportamiento de Cursor/Claude CLI coincida con el espacio de trabajo principal. Estos archivos a menudo no están versionados (untracked).

**No** los copies si el usuario se niega o si una herramienta nativa (Paso 1a) ya propaga la configuración; respeta el harness.

Ejecuta **después** de `cd "$path"` (usando todavía el `SOURCE_ROOT` capturado arriba):

```bash
copied_claude_settings=false
for claude_settings in ".claude/settings.json" ".claude/settings.local.json"; do
    if [ -f "$SOURCE_ROOT/$claude_settings" ]; then
        mkdir -p ".claude"
        cp -p "$SOURCE_ROOT/$claude_settings" "./$claude_settings"
        echo "Copied $claude_settings to worktree"
        copied_claude_settings=true
    fi
done

if [ "$copied_claude_settings" = false ]; then
    echo "No local Claude settings found (.claude/settings.json or .claude/settings.local.json)"
fi
```

**Symlinks:** Si el árbol de origen usa symlinks para `.claude` (p. ej. `.claude/skills` → `../../ai-specs/skills`), copiar solo estos archivos JSON no recrea los destinos de los symlinks. Repite la misma estructura de symlinks en el worktree o apóyate en rutas relativas al proyecto dentro de `settings.json`.

## Paso 3: Configuración del proyecto

Detecta automáticamente y ejecuta la configuración apropiada:

```bash
# Node.js
if [ -f package.json ]; then npm install; fi

# Rust
if [ -f Cargo.toml ]; then cargo build; fi

# Python
if [ -f requirements.txt ]; then pip install -r requirements.txt; fi
if [ -f pyproject.toml ]; then poetry install; fi

# Go
if [ -f go.mod ]; then go mod download; fi
```

## Paso 4: Verifica un baseline limpio

Ejecuta las pruebas para asegurarte de que el espacio de trabajo inicie limpio:

```bash
# Use project-appropriate command
npm test / cargo test / pytest / go test ./...
```

**Si las pruebas fallan:** Reporta los fallos y pregunta si continuar o investigar.

**Si las pruebas pasan:** Reporta que está listo.

### Reporte

```
Worktree listo en <full-path>
Configuración de Claude copiada (o no se encontró / se omitió según el harness)
Pruebas pasando (<N> pruebas, 0 fallos)
Listo para implementar <feature-name>
```

## Paso 5: Limpieza — Elimina el worktree al terminar

**Ejecuta la limpieza una vez que el trabajo esté completo.** "Terminado" significa: branch con merge hecho, PR cerrado, el experimento se descartó o el usuario confirmó explícitamente que ya no necesita el espacio de trabajo aislado. Nunca elimines un worktree que todavía contenga cambios sin commit, sin push o sin merge que el usuario pueda querer.

### 5.0 Detecta el modo de limpieza

```bash
GIT_DIR=$(cd "$(git rev-parse --git-dir)" 2>/dev/null && pwd -P)
GIT_COMMON=$(cd "$(git rev-parse --git-common-dir)" 2>/dev/null && pwd -P)
```

- **Si `GIT_DIR == GIT_COMMON`:** Nunca estuviste en un worktree vinculado (el Paso 0 te envió directamente al Paso 3). No hay nada que limpiar; omite el Paso 5 por completo.
- **Si `GIT_DIR != GIT_COMMON`:** Estás dentro de un worktree vinculado. Continúa con la limpieza.

### 5.1 Verifica que el trabajo esté guardado

Antes de eliminar cualquier cosa, confirma que no hay trabajo que se pueda perder:

```bash
git status --porcelain                 # Must be empty (no uncommitted changes)
git log @{u}.. 2>/dev/null             # Must be empty (no unpushed commits)
```

**Si alguno de los comandos devuelve salida:** Detente. Reporta al usuario el trabajo sin guardar y pregunta cómo proceder (commit/push, stash o descartar forzosamente). Nunca elimines un worktree con trabajo sin guardar sin la confirmación explícita del usuario.

Captura la ruta del worktree y el nombre del branch antes de salir del directorio:

```bash
WORKTREE_PATH=$(git rev-parse --show-toplevel)
BRANCH_NAME=$(git branch --show-current)
```

### 5.2 Herramientas nativas de worktree (preferido)

Si usaste una herramienta nativa de worktree en el Paso 1a (`EnterWorktree`, `WorktreeCreate`, `/worktree`, etc.), usa el comando nativo correspondiente para eliminar el worktree (p. ej. `LeaveWorktree`, `WorktreeRemove`, `/worktree remove`). Las herramientas nativas limpian directorios, branches y el estado del harness de forma consistente.

Continúa al 5.3 solo si no hay ninguna herramienta nativa de limpieza disponible.

### 5.3 Limpieza con la alternativa de Git Worktree

**Usa esto solo si el Paso 5.2 no aplica**: creaste el worktree manualmente en el Paso 1b.

```bash
# 1. Move out of the worktree before deleting it
cd "$GIT_COMMON/.."   # or any path inside the main checkout

# 2. Remove the worktree
git worktree remove "$WORKTREE_PATH"

# 3. If files are still present (e.g. ignored artifacts blocking removal),
#    confirm with the user, then force-remove
git worktree remove --force "$WORKTREE_PATH"

# 4. Delete the local branch only if it was created for this worktree
#    AND has been merged or is no longer needed
git branch -d "$BRANCH_NAME"            # safe delete (refuses if unmerged)
git branch -D "$BRANCH_NAME"            # force delete (only with user confirmation)

# 5. Prune stale worktree metadata if the directory was deleted manually
git worktree prune
```

**Alternativa por sandbox:** Si la eliminación falla por permisos, reporta el fallo y la ruta que necesita limpieza manual. No reintentes de forma destructiva.

### 5.4 Verifica la limpieza

```bash
git worktree list                      # WORKTREE_PATH must no longer appear
ls -d "$WORKTREE_PATH" 2>/dev/null     # Must return nothing
```

### Reporte

```
Worktree eliminado: <full-path>
Branch <name> eliminado (o conservado, si aún se necesita)
Checkout principal sin modificar
```

## Referencia rápida

| Situación | Acción |
|-----------|--------|
| Ya estás en un worktree vinculado | Omite la creación (Paso 0) |
| En un submódulo | Trátalo como un repo normal (protección del Paso 0) |
| Herramienta nativa de worktree disponible | Úsala (Paso 1a) |
| Sin herramienta nativa | Alternativa con git worktree (Paso 1b) |
| Necesitas una ubicación para el worktree | Usa `<repo>/.worktrees/<branch>` |
| Directorio no ignorado | Agrégalo a .gitignore + commit |
| Error de permisos al crear | Alternativa por sandbox, trabaja en el lugar actual |
| Las pruebas fallan durante el baseline | Reporta los fallos + pregunta |
| Sin package.json/Cargo.toml | Omite la instalación de dependencias |
| Trabajo completo, en un worktree vinculado | Ejecuta la limpieza del Paso 5 |
| Nunca se creó un worktree | Omite el Paso 5 |
| Cambios sin commit/sin push al limpiar | Detente y pregunta al usuario |
| Herramienta nativa de limpieza disponible | Úsala (Paso 5.2) |
| Sin herramienta nativa de limpieza | `git worktree remove` (Paso 5.3) |
| Directorio del worktree eliminado manualmente | `git worktree prune` |
| Alternativa con git: replicar el comportamiento de Claude en el worktree | Copia `.claude/settings*.json` desde `SOURCE_ROOT` (Paso 1b) |
| El repo usa symlinks en `.claude` | Copiar solo los JSON no basta: recrea los symlinks o las rutas |

## Errores comunes

### Pelear con el harness

- **Problema:** Usar `git worktree add` cuando la plataforma ya proporciona aislamiento
- **Solución:** El Paso 0 detecta el aislamiento existente. El Paso 1a delega en las herramientas nativas.

### Omitir la detección

- **Problema:** Crear un worktree anidado dentro de uno existente
- **Solución:** Ejecuta siempre el Paso 0 antes de crear cualquier cosa

### Omitir la verificación de ignorado

- **Problema:** El contenido del worktree queda versionado y contamina el git status
- **Solución:** Usa siempre `git check-ignore` antes de crear un worktree local al proyecto

### Suponer la ubicación del directorio

- **Problema:** Genera inconsistencia y viola las convenciones del proyecto
- **Solución:** Usa siempre `<repo>/.worktrees/<branch>` a menos que el usuario lo anule explícitamente para una tarea

### Continuar con pruebas fallando

- **Problema:** No se pueden distinguir los bugs nuevos de los problemas preexistentes
- **Solución:** Reporta los fallos y obtén permiso explícito para continuar

### Eliminar un worktree con trabajo sin guardar

- **Problema:** `git worktree remove --force` descarta en silencio los cambios sin commit/sin push
- **Solución:** Ejecuta siempre `git status --porcelain` y `git log @{u}..` antes de eliminar; detente y pregunta si alguno no está vacío

### Eliminar el branch antes de que tenga merge

- **Problema:** `git branch -D` destruye commits que nunca tuvieron merge ni push
- **Solución:** Prefiere `git branch -d` (eliminación segura); fuerza la eliminación solo después de la confirmación explícita del usuario

### Mezclar limpieza nativa y manual

- **Problema:** Ejecutar `git worktree remove` sobre un worktree creado por una herramienta nativa deja un estado fantasma en el harness
- **Solución:** Lo que haya creado el worktree debe eliminarlo (herramienta nativa ↔ herramienta nativa, git ↔ git)

### Copiar la configuración de Claude desde el directorio equivocado

- **Problema:** Usar `git rev-parse --show-toplevel` después de hacer `cd` al nuevo worktree resuelve a la ruta del worktree, así que la copia no encuentra configuración o copia desde el lugar equivocado
- **Solución:** Define `SOURCE_ROOT=$(git rev-parse --show-toplevel)` en el checkout principal **antes** de `git worktree add` y luego usa `$SOURCE_ROOT` en el loop de copia

## Señales de alerta

**Nunca:**
- Crees un worktree cuando el Paso 0 detecte aislamiento existente
- Uses `git worktree add` cuando tengas una herramienta nativa de worktree (p. ej., `EnterWorktree`). Este es el error n.º 1: si la tienes, úsala.
- Omitas el Paso 1a saltando directamente a los comandos git del Paso 1b
- Crees un worktree sin verificar que esté ignorado (local al proyecto)
- Omitas la verificación de las pruebas de baseline
- Continúes con pruebas fallando sin preguntar
- Elimines un worktree que todavía tenga trabajo sin commit, sin push o sin merge sin la confirmación explícita del usuario
- Fuerces la eliminación de un branch (`git branch -D`) sin confirmar que tiene merge o que ya no se necesita
- Ejecutes `git worktree remove` sobre un worktree creado por una herramienta nativa

**Siempre:**
- Ejecuta primero la detección del Paso 0
- Prefiere las herramientas nativas sobre la alternativa con git
- Usa `<repo>/.worktrees/<branch>` como ubicación estándar
- Verifica que el directorio esté ignorado cuando sea local al proyecto
- Detecta automáticamente y ejecuta la configuración del proyecto
- Verifica un baseline de pruebas limpio
- Ejecuta la limpieza del Paso 5 una vez terminado el trabajo, usando el mismo mecanismo que creó el worktree
- Verifica que no haya nada que perder antes de eliminar un worktree (`git status --porcelain`, `git log @{u}..`)
- Confirma con `git worktree list` y una verificación del directorio que la limpieza realmente se completó
- Después del Paso 1b (alternativa con git), copia `.claude/settings.json` y `.claude/settings.local.json` desde el `SOURCE_ROOT` capturado antes de `git worktree add`
