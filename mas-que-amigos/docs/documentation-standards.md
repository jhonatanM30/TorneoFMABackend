---
description: Estándares y buenas prácticas para la documentación técnica de este proyecto, incluida la estructura de la documentación, los procesos de actualización y las reglas de idioma.
globs:
alwaysApply: true
---
# Reglas y patrones para la documentación y las AI specs

## Introducción
La documentación técnica aplica a toda la documentación relativa al proyecto, como el modelo de datos, el README, las especificaciones de la API y otros documentos MD que describen cómo está estructurado, cómo se ejecuta y cómo opera el proyecto.
Las AI specs se refieren a los documentos que les explican a los agentes de IA cómo comportarse, documentar, planificar, programar, etc., lo que incluye acuerdos del equipo, estándares y convenciones.

## Reglas generales
- IDIOMA según la sección 2 de `docs/base-standards.md`: la documentación (archivos de `docs/`, `ai-specs/`, README, guías y artefactos de OpenSpec) se escribe SIEMPRE EN ESPAÑOL, tanto al crearla como al actualizarla. La documentación dentro del código (comentarios, explicaciones de funciones o campos, etc.) se escribe EN INGLÉS.



## Documentación técnica
Antes de hacer cualquier commit o git push, o si se te pide documentar un commit, SIEMPRE debes revisar qué documentación técnica se debe actualizar.

Al actualizar la documentación, haré lo siguiente:
1. Revisar todos los cambios recientes en el código
2. Identificar qué archivos de documentación necesitan actualizaciones según los cambios. Algunos ejemplos claros:
   - Para cambios en el modelo de datos: Actualizar la sección de definición del modelo de datos en data-model.md
   - Para cambios en la API: Actualizar api-spec.yml
   - Para cambios en librerías, migraciones de base de datos o cualquier cosa que cambie el proceso de instalación, actualizar *-standards.md
3. Actualizar cada archivo de documentación afectado en español, manteniendo la coherencia con la documentación existente
4. Asegurar que toda la documentación tenga el formato adecuado y siga la estructura establecida
5. Verificar que todos los cambios queden reflejados con precisión en la documentación
6. Reportar qué archivos se actualizaron y qué cambios se hicieron

## AI specs

Esta regla establece un proceso obligatorio para que la IA:
*   Aprenda de la retroalimentación, la orientación y las sugerencias del usuario durante las interacciones.
*   Identifique de forma proactiva oportunidades para mejorar las Reglas de Desarrollo existentes con base en estos aprendizajes.
*   Mantenga la asistencia de la IA alineada con las necesidades cambiantes del proyecto y las expectativas del usuario.
*   Incorpore la retroalimentación del usuario en el marco operativo de la IA para maximizar su valor.

Esta regla aplica después de cualquier interacción en la que el usuario proporcione retroalimentación explícita o implícita, sugerencias, correcciones, información nueva o exprese preferencias. **La IA DEBE analizar activamente todas las interacciones con el usuario en busca de estas oportunidades de aprendizaje, no solo esperar pasivamente retroalimentación directa, para refinar de forma proactiva su comprensión y las buenas prácticas del proyecto.**

### Errores comunes y antipatrones que la IA debe evitar

*   **Omitir el proceso de aprobación:** Aplicar modificaciones a las reglas sin obtener primero la revisión y aprobación explícitas del usuario.
*   **Propuestas desconectadas:** Proponer cambios a las reglas sin conectarlos claramente con la retroalimentación específica del usuario o con los aprendizajes obtenidos de la interacción.
*   **Modificaciones imprecisas:** Sugerir modificaciones sin identificar con precisión qué regla o qué secciones específicas dentro de una regla deben cambiarse, lo que dificulta una revisión efectiva por parte del usuario.
*   **Retroalimentación sin atender:** No iniciar el proceso de aprendizaje y revisión cuando el usuario proporciona retroalimentación relevante que podría mejorar las reglas.
*   **Crecimiento descontrolado del alcance (scope creep):** Actualizar simultáneamente varias reglas no relacionadas o hacer cambios que excedan el alcance de la retroalimentación recibida.
*   **Cambios de reglas no solicitados:** Modificar reglas de forma proactiva cuando no hay una conexión directa con la retroalimentación del usuario o con una oportunidad de aprendizaje. Las actualizaciones de las reglas deben ser reactivas y guiadas por la retroalimentación.
*   **Falta de confirmación de la actualización:** No notificar al usuario después de que una modificación de una regla se haya implementado con éxito tras su aprobación.