---
description: Este documento contiene todas las reglas y lineamientos de desarrollo del proyecto, aplicables a todos los agentes de IA (Claude, Cursor, Codex, Gemini, etc.).
alwaysApply: true
---

## 1. Principios fundamentales

- **Tareas pequeñas, una a la vez**: trabaja siempre en pasos pequeños, uno a la vez. Nunca avances más de un paso.
- **Desarrollo guiado por pruebas (TDD)**: empieza con pruebas que fallen para toda funcionalidad nueva, según el detalle de la tarea.
- **Nombres claros**: usa nombres claros y descriptivos para todas las variables y funciones.
- **Cambios incrementales**: prefiere cambios incrementales y enfocados sobre modificaciones grandes y complejas.
- **Cuestionar supuestos**: cuestiona siempre los supuestos y las inferencias.
- **Detección de patrones**: detecta y señala los patrones de código repetidos.

## 2. Estándares de idioma

### 2.1 Harness y documentación: español
- Todo lo que pertenece al harness se escribe en español: `docs/`, `ai-specs/` (agentes, skills, scripts), `AGENTS.md`, `CLAUDE.md` y los demás archivos de entrada de agentes.
- También los artefactos de OpenSpec (`proposal.md`, `design.md`, `specs/`, `tasks.md`), el README y las guías.

### 2.2 Código: inglés
- El código nuevo se escribe en inglés: clases, métodos, variables, comentarios, mensajes de log y mensajes de error técnicos.
- Los nombres de pruebas y sus descripciones van en inglés.
- Los mensajes de commit van en inglés, con Conventional Commits: `feat(matches): add lineup validation`.

### 2.3 Mensajes al usuario final: español
- Todo texto que ve el usuario final (mensajes de error del API que se muestran en pantalla y textos de la interfaz) se escribe en español.
- Ejemplo: `log.warn("Player not found: {}", id)` en el log, y `"El jugador no existe"` en la respuesta al usuario.

### 2.4 Código existente en español (transición)
- El código existente con dominio en español (`Jugador`, `Partido`, `Alineacion`, etc.) **no se renombra** de forma oportunista.
- Al modificar un archivo existente, se respeta el idioma de ese archivo. No se mezclan idiomas dentro de un mismo archivo o clase.
- Los archivos y clases nuevos van en inglés.
- La migración a inglés de una entidad existente se hace solo mediante un cambio de OpenSpec dedicado, que cubra código, base de datos, API y pruebas.

### 2.5 Base de datos y API
- Las tablas, columnas y rutas del API nuevas van en inglés (`/api/matches`, tabla `match_event`).
- Las tablas, columnas y rutas existentes no se renombran fuera de un cambio de OpenSpec dedicado, porque romperían la base de datos, el contrato del API y el frontend.
- Una columna nueva en una tabla existente sigue el idioma de esa tabla, para no mezclar idiomas dentro de ella.

## 3. Estándares específicos

Para estándares y lineamientos detallados de cada área del proyecto, consulta:

- [Estándares de backend](./backend-standards.md): desarrollo de APIs, patrones de base de datos, pruebas, seguridad y buenas prácticas de backend
- [Estándares de frontend](./frontend-standards.md): frontend web en HTML, CSS y JavaScript sin framework ni build, en el repositorio aparte `MasQueAmigos-Torneo`; lineamientos de UI/UX y consumo de esta API
- [Estándares de documentación](./documentation-standards.md): estructura, formato y mantenimiento de la documentación técnica, incluidos los estándares para IA como este documento
- [Pasos obligatorios de tareas OpenSpec](./openspec-tasks-mandatory-steps.md): checklist obligatorio y reglas de ejecución al crear o actualizar archivos `tasks.md` de OpenSpec

## 4. Skills del proyecto

- Las skills viven en `ai-specs/skills`.
- Cuando una solicitud coincida con una skill, carga y sigue automáticamente su `SKILL.md` antes de continuar.
- Carga también los archivos referenciados dentro de la carpeta de la skill (por ejemplo, `references/*.md`) cuando la skill los requiera.

## 5. Requisito de modelo para planificación

Los flujos de planificación deben ejecutarse con Opus en razonamiento alto.

Este requisito aplica a:
- `enrich-us`
- `openspec-ff-change`
- `openspec-continue-change`

Antes de iniciar cualquiera de estos flujos, verifica que la sesión esté usando Opus en razonamiento alto. Si no es así, **autocorrígelo** agregando `"model": "claude-opus-4-7"` a `.claude/settings.json` (usa la skill `update-config` o edítalo directamente) y continúa, sin detenerte a preguntarle al usuario. Haz lo mismo para volver a Sonnet en razonamiento medio en cualquier otro paso.

## 6. Integridad de symlinks y portabilidad multiagente

- **Fuente canónica**: mantén los artefactos reutilizables en `ai-specs` como fuente canónica. Las rutas específicas de cada agente (como `.claude` y `.cursor`) deben referenciarlos mediante symlinks cuando sea posible.
- **Seguridad en actualizaciones**: cada vez que un archivo se renombre, se mueva o cambie de extensión, verifica y actualiza todos los symlinks que apuntan a él antes de dar el cambio por terminado.
- **Enlazado de artefactos nuevos**: al crear un artefacto nuevo que deba estar disponible para varios agentes (por ejemplo, agentes o skills nuevos en `ai-specs`), crea los symlinks correspondientes desde las rutas esperadas por cada agente.
- **Revisión de personalizaciones externas**: cada vez que se introduzca una personalización fuera de `ai-specs`, evalúa si debe moverse a `ai-specs` y reemplazarse por symlinks desde su ubicación original.
- **Criterio de finalización**: un cambio está incompleto si deja symlinks rotos, destinos desactualizados o artefactos canónicos duplicados entre las carpetas de los agentes.

## 7. Actualización obligatoria de artefactos OpenSpec en cambios posteriores al apply

Cuando aparezca una nueva solicitud de corrección o cambio después de `opsx:apply` (o `/apply`) y antes de `opsx:archive` (o `/archive`), los agentes deben tratarla primero como una actualización de la especificación, no como un "arréglalo rápido" informal. Es el principio central de OpenSpec: la documentación es la fuente de verdad.

Orden obligatorio:

1. Actualiza los artefactos del cambio OpenSpec actual que se vean afectados (por ejemplo, escenarios, requisitos/specs y `tasks.md`). No agregues las tareas como "bugfixes", sino como parte del diseño inicial, en la sección que les corresponda.
2. Si hace falta regenerar artefactos, ejecuta el paso de OpenSpec correspondiente (`opsx:continue`, `opsx:ff` o equivalente) antes de escribir código.
3. Implementa el código solo cuando los artefactos ya reflejen la nueva solicitud.
4. Vuelve a verificar contra los artefactos actualizados antes de archivar.

No apliques en esta ventana correcciones directas solo en el código sin actualizar los artefactos de OpenSpec.
