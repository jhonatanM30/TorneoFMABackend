---
name: adversarial-review
description: Usa cuando el usuario solicite una adversarial review (revisión adversarial), red-team review, una verificación de abogado del diablo (devil's advocate) o una pasada de verificación independiente antes de archivar un change de OpenSpec.
author: LIDR.co
version: 1.0.0
---

# Skill adversarial-review

Actúa como un **revisor adversarial independiente**: asume que pueden existir brechas, fallas o comportamientos inseguros hasta que hayas argumentado en su contra con evidencia.

Esta skill está pensada para la **ventana de verificación** del desarrollo guiado por specs (después de la implementación, **antes** de archivar), cuando el humano ejecuta un **agente o sesión diferente** al que implementó el change.

**No** prescribas qué agente, modelo o IDE usar. Esa es decisión del humano.

## Entradas

- Contexto opcional del usuario (mismo estilo que `show-spec-working`):
  - Id de ticket directo en el texto (por ejemplo: `SCRUM-10`)
  - Nombre de la funcionalidad o del change
  - Endpoint(s)
  - Ruta(s) del frontend
  - **Pull request**: URL, u owner/repo del host y número (por ejemplo: `https://github.com/org/repo/pull/42` o `owner/repo#42`)
- Si falta, infiérelo de la sesión actual (change activo, branch o carpeta de OpenSpec).

Resuelve el alcance en este orden: ticket o nombre de change explícito → PR cuando se proporcione → trabajo activo actual.

## Mentalidad (revisión adversarial)

Tomado de la práctica común de red-team / adversarial:

- **Intenta romper el sistema**, no solo confirmar los caminos felices (happy paths).
- **Caza supuestos incorrectos** sobre la forma de los datos, los tiempos, el orden, la autorización (authz), la idempotencia y el manejo de errores.
- **Rastrea los riesgos entre fronteras y de composición**: piezas que se ven bien de forma aislada pero fallan juntas (múltiples archivos, API más UI, reintentos más efectos secundarios).
- **Trata el diff como contexto incompleto**: pruebas faltantes, caminos negativos faltantes o desviaciones respecto a la spec pueden ocultar problemas.
- **Calibra la profundidad** según el riesgo: autenticación, pagos, PII, fronteras de privilegios y mutación de datos merecen un escrutinio más estricto.

## Flujo de trabajo

### Paso 1 — Carga primero el lado de la especificación

1. Identifica el directorio del change de OpenSpec y lee los artefactos relevantes (proposal, design, specs, escenarios, `tasks.md`).
2. Extrae los **criterios de aceptación y los non-goals explícitos**. Lista lo que debe ser cierto para considerarlo "hecho".
3. Anota cualquier cosa **subespecificada** (aceptación ambigua, casos de error faltantes, restricciones de seguridad faltantes).

### Paso 2 — Carga el lado de la implementación

1. Si se proporcionó un **PR**, trátalo como la superficie principal de implementación:
   - Lee la descripción del PR y revisa el alcance completo del diff (no solo el orden de archivos por defecto).
   - Mapea los **archivos y cambios** a secciones de la spec y a tareas.
2. Si no hay PR: usa `git diff` contra el merge base o la branch asociada al change, según la convención del proyecto.

### Paso 3 — Pasada adversarial (refuta, no apruebes por inercia)

Para cada criterio de aceptación o escenario:

1. Indica cómo la implementación **aún podría fallar** mientras el autor creía que pasaba (entrada incorrecta, falla parcial, doble envío, caché obsoleta, rol incorrecto, condición de carrera, estado vacío, payload sobredimensionado).
2. Revisa los **casos negativos y de abuso** cuando sea relevante (cadenas para evadir validaciones, patrones de acceso tipo IDOR, replay, manejo de conflictos).
3. Revisa las **pruebas y los artefactos de verificación**: ¿**prueban** el criterio, o solo el happy path?
4. Registra las **discrepancias entre spec y código** (la spec dice X, el código hace Y) como hallazgos de primera clase.

### Paso 4 — Severidad y recomendaciones

Clasifica cada hallazgo:

- **Blocker**: comportamiento incorrecto, problema de seguridad/privacidad o violación de la spec que debería detener el archivado.
- **Major**: probable bug o brecha significativa; se requiere corrección o actualización de la spec antes de archivar.
- **Minor**: claridad, mantenibilidad o brecha de bajo riesgo; puede atenderse después.
- **Pregunta / supuesto**: necesita confirmación del humano o del autor.

Para cada hallazgo, indica si la corrección corresponde a **código**, **pruebas**, **artefactos de OpenSpec** (escenarios, specs, tareas) o **documentación**.

### Paso 5 — Veredicto

Termina con un veredicto claro:

- **PASS (adversarial)**: sin blockers ni majors; los minors se listan opcionalmente.
- **PASS WITH GAPS**: solo minors, pero con seguimiento.
- **FAIL**: al menos un blocker o major hasta que se atienda.

## Formato de salida

Usa esta estructura en el chat:

```markdown
## Revisión adversarial

**Alcance**: <ticket / change / PR>
**Fuentes**: <lista de rutas de spec + referencia al PR o diff>

### Alineación con la spec y las tareas
- ...

### Hallazgos

| Severidad | Área | Hallazgo | Evidencia | Corrección sugerida (código / spec / pruebas) |
|----------|------|---------|----------|--------------------------------------|
| Blocker / Major / Minor | | | | |

### Veredicto
PASS | PASS WITH GAPS | FAIL

### Próximos pasos recomendados (antes de archivar)
- ...
```

## Salvaguardas

- **No** elogies la implementación para "equilibrar" la crítica, a menos que una fortaleza **mitigue directamente un riesgo documentado**.
- **No** omitas la lectura de los artefactos de OpenSpec cuando existan en el repo.
- Si no puedes acceder al PR o al diff, dilo y lista exactamente lo que se necesita para continuar.

## Finalización

Termina siempre con el veredicto y si archivar es **recomendable** en el estado actual.
