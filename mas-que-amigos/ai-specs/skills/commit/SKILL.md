---
name: commit
description: Crea commits y pull requests enfocados siguiendo los estándares del repositorio.
author: LIDR.co
version: 1.0.0
---
# Skill commit

Úsala cuando este flujo de trabajo sea requerido en el proyecto.

## Instrucciones

# Rol

Eres un experto en control de versiones y flujos de release. Creas commits y Pull Requests claros y completos que se alinean con los estándares del proyecto y hacen que la revisión y la trazabilidad sean sencillas.

# Argumentos

**Opcional.** `$ARGUMENTS` puede contener:

- **Nada (vacío)**: Haz stage y commit de todos los cambios relevantes del working tree y luego abre un único PR.
- **Identificadores de funcionalidad/ticket**: p. ej. IDs de ticket (p. ej. `SCRUM-123`), nombres de branch o etiquetas cortas de funcionalidad. Cuando se proporcionen, haz stage y PR **solo** de los cambios que pertenecen a esas funcionalidades; deja todos los demás cambios sin stage y sin commit.
- **Modo solo descripción / sin git**: Si el usuario dice **explícitamente** algo como "no PR", "only commit" (es decir, solo producir el texto del commit), "only description", "don't touch git", "just the message" o "dry run" (o equivalentes en español como "sin PR", "solo el commit", "solo la descripción", "no toques git", "solo el mensaje"), entonces **no** ejecutes ningún comando de git ni crees un PR. Solo determina el alcance, lista lo que se incluiría en el stage y muestra el mensaje de commit propuesto (asunto + cuerpo). El usuario puede copiar y ejecutar los comandos de git por su cuenta.

# Objetivo

1. Producir un **único commit completo** que describa con precisión los cambios relevantes.
2. Hacer **push** de la branch y **crear (o actualizar) un Pull Request** para revisión.
3. Si se dieron argumentos: hacer **stage y commit solo** de los cambios ligados a esas funcionalidades; no toques otros archivos modificados.

# Proceso y reglas

## 0. Modo solo descripción / sin git (revísalo primero)

Si el usuario solicitó **explícitamente** que no haya operaciones de git (p. ej. "no PR", "only commit", "only description", "don't touch git", "just the message", "dry run"):

- Realiza **solo** los pasos 1–3: inspecciona el estado, resuelve el alcance (qué archivos/hunks se incluirían en el stage) y escribe el mensaje de commit completo (asunto + cuerpo).
- **No** ejecutes `git add`, `git commit`, `git push` ni `gh pr create`. No modifiques el repositorio de ninguna forma.
- Salida para el usuario:
  1. Lista de archivos (y hunks, si es parcial) que se incluirían en el stage.
  2. El mensaje de commit propuesto en un bloque fácil de copiar y pegar.
- Luego detente; omite los pasos 4, 5 y 6.

## 1. Inspecciona el estado actual

- Ejecuta `git status` y `git diff` (y `git diff --staged` si es necesario) para listar todos los archivos modificados, agregados y eliminados.
- Identifica la branch actual. Si no estás en una feature branch, decide si crear una a partir de la branch base (p. ej. `main` o `develop`) antes de hacer commit.

## 2. Resuelve el alcance: commit completo vs commit acotado a funcionalidades

- **Si `$ARGUMENTS` está vacío o no se proporcionó**  
  - Trata todos los cambios relevantes (excluyendo archivos que no deberían incluirse en el commit, p. ej. `.env`, artefactos de build, configuración local) como el alcance de este commit.  
  - Haz stage de todos ellos y continúa con el paso 3.

- **Si se proporcionó `$ARGUMENTS` (p. ej. IDs de ticket o nombres de funcionalidades)**  
  - Mapea cada argumento a los cambios que claramente le pertenecen (por ruta, id de ticket en el nombre de la branch o contexto en los diffs).  
  - Haz stage **solo** de los archivos/hunks que pertenecen a esas funcionalidades.  
  - Deja cualquier otro archivo modificado **sin stage** y no lo incluyas en el commit.  
  - Si un archivo contiene tanto cambios relacionados con la funcionalidad como no relacionados, usa `git add -p` (o equivalente) para hacer stage solo de los hunks que pertenecen a las funcionalidades solicitadas.  
  - Si ningún cambio coincide claramente con los argumentos dados, repórtalo y no hagas commit.

## 3. Mensaje de commit

- Escribe el mensaje de commit **en inglés** (según `docs/base-standards.md`).
- Hazlo **descriptivo** (según el Git Workflow en `backend-standards.md` y `frontend-standards.md`).
- Estructúralo de modo que:
  - **Línea de asunto**: Resumen corto en imperativo (p. ej. "Add candidate filters to position list", "Fix validation for application deadline"). Opcionalmente antepón un scope o id de ticket (p. ej. `SCRUM-123: Add candidate filters`).
  - **Cuerpo** (si es necesario): Viñetas o párrafos cortos que describan qué cambió y por qué (áreas tocadas, nuevo comportamiento, correcciones). Referencia aquí los IDs de ticket si aplican.
- No hagas commit de secretos, `.env` ni otros artefactos sensibles o generados.

## 4. Commit y push

- Crea el commit con el mensaje del paso 3.
- Haz push de la branch actual al remoto (`git push origin <branch>`). Si la branch no existe en el remoto, haz push con `-u` para configurar el upstream.

## 5. Pull Request

- Usa el **GitHub CLI (`gh`)** para todas las operaciones de GitHub (según los estándares del repositorio).
- Crea o actualiza el PR de la branch actual:
  - **Título**: Claro, alineado con el commit (p. ej. incluye el ID del ticket si aplica: `[SCRUM-123] Add candidate filters to position list`).
  - **Descripción**: Resume el conjunto de cambios, enlaza el ticket si es relevante y anota cualquier prueba o seguimiento pendiente.
- Si el repo usa protección de branches o checks obligatorios, menciona que el PR estará listo para revisión una vez que pasen los checks.

## 6. Resumen para el usuario

- Reporta lo que se incluyó en el commit (archivos y alcance).
- Si se proporcionaron argumentos: confirma qué funcionalidades/tickets se incluyeron y que los demás cambios se dejaron sin stage.
- Proporciona la URL del PR (de la salida de `gh`).

# Referencias

- `docs/base-standards.md` (sección 2.2): mensajes de commit y código en inglés.
- `docs/backend-standards.md` y `docs/frontend-standards.md`: Git Workflow (feature branches, commits descriptivos, branches pequeñas y enfocadas).
- Convenciones del flujo de git del repositorio: Usa `gh` para GitHub y la creación de PR; vinculación opcional de branch y PR basada en tickets.

# Notas

- **Solo descripción**: Cuando el usuario pida que no haya PR o solo el texto del commit, muestra únicamente el plan de stage y el mensaje; no ejecutes ningún comando de git ni de `gh`.
- No ejecutes comandos destructivos de git (p. ej. `git push --force` sin una solicitud explícita del usuario).
- Si hay conflictos o el push es rechazado, reporta la situación y sugiere próximos pasos (p. ej. pull/rebase y luego push), pero no hagas force-push a menos que el usuario lo pida.
- Cuando se proporcionen argumentos, **solo** se hace stage y commit de los cambios ligados a esas funcionalidades; todo lo demás permanece en el working tree para un commit o PR separado.
