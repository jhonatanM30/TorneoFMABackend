---
name: enrich-us
description: Analiza y mejora historias de usuario con detalle técnico completo y listo para implementar, a partir del ticket ingresado directamente o desde Jira.
author: LIDR.co
version: 1.0.0
---
# Skill enrich-us

Úsala cuando este flujo de trabajo sea necesario en el proyecto.

## Instrucciones

Analiza y enriquece el ticket: $ARGUMENTS.

Sigue estos pasos:

1. Determina la fuente de entrada del ticket:
   - **Modo de entrada directa (por defecto cuando se proporciona el texto del ticket):** Usa el contenido del ticket que el usuario compartió en el prompt/chat.
   - **Modo Jira (opcional):** Si el usuario proporciona un id/clave de Jira, o pide usar Jira (incluidas referencias como "el que está en progreso"), usa el MCP de Jira para obtener los detalles del ticket.
2. Actúa como un experto de producto con conocimientos técnicos.
3. Comprende el problema descrito en el ticket.
4. Decide si la Historia de Usuario está completamente detallada o no, según las buenas prácticas de producto. Valida que incluya:
   - Descripción completa de la funcionalidad
   - Lista exhaustiva de los campos a actualizar
   - Estructura y URLs de los endpoints requeridos
   - Archivos/módulos a modificar según la arquitectura y las buenas prácticas
   - Definición de terminado (pasos de implementación y entrega)
   - Actualizaciones de documentación y de pruebas unitarias
   - Requisitos no funcionales (seguridad, rendimiento, observabilidad, etc.)
5. Si a la historia le falta suficiente detalle técnico para una implementación autónoma, proporciona una versión mejorada que sea más clara, más específica y concisa, alineada con el paso 4. Usa el contexto técnico del proyecto de `@documentation`. Devuelve el resultado en markdown.
6. El formato de salida siempre debe incluir:
   - `## Original`
   - `## Enhanced`
7. La escritura de vuelta en Jira es opcional y solo aplica en modo Jira:
   - Actualiza el ticket de Jira agregando el contenido mejorado después del contenido original, con secciones `h2` claras `[original]` y `[enhanced]` y un formato legible (listas/fragmentos de código cuando sea útil).
   - Si el estado del ticket es `To refine`, muévelo a `Pending refinement validation`.

## Notas

- No exijas Jira cuando el usuario ya proporcionó directamente el contenido completo del ticket.
- Si la entrada es ambigua (por ejemplo, el usuario da una referencia corta sin contenido), pregunta si resolverla vía Jira o solicita el texto completo del ticket.
