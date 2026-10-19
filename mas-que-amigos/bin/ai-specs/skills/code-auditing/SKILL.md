---
name: code-auditing
description: Skill de proyecto enfocada en tareas.
version: 1.0.0
---
# Skill de auditoría de código

Metodología integral para auditorías sistemáticas de calidad de código.

## Cuándo usarla

- Auditorías integrales de calidad de código
- Evaluaciones de vulnerabilidades de seguridad
- Identificación de deuda técnica
- Revisiones de código previas a un release
- Verificación de buenas prácticas
- Auditorías de librerías y dependencias

## Fases de la auditoría

### Fase 0: Preparación previa al análisis
1. Revisa los archivos de configuración del proyecto (package.json, tsconfig.json, etc.)
2. Identifica el stack tecnológico y las librerías principales
3. Revisa las configuraciones de linting/formateo
4. Ejecuta los comandos de linting/testing existentes como baseline
5. Carga la documentación de las librerías principales identificadas

### Fase 1: Descubrimiento
1. Encuentra todos los archivos de código por tipo
2. Crea una lista de seguimiento para cada archivo
3. Agrupa los archivos por módulo/funcionalidad para un análisis contextual

### Fase 2: Análisis archivo por archivo
Para cada archivo, analiza:
- Código muerto (funciones, variables, imports sin usar)
- Code smells y anti-patrones
- Implementaciones personalizadas que podrían usar librerías establecidas
- Vulnerabilidades de seguridad
- Problemas de rendimiento
- Patrones obsoletos o APIs deprecadas
- Manejo de errores faltante
- Funciones excesivamente complejas
- Código duplicado

### Fase 3: Verificación de buenas prácticas
Para cada librería y framework:
1. Obtén la documentación oficial
2. Compara la implementación con los patrones oficiales
3. Identifica desviaciones de las recomendaciones
4. Anota patrones de uso obsoletos
5. Marca los anti-patrones desaconsejados

### Fase 4: Detección de patrones
Busca problemas recurrentes:
- Anti-patrones comunes en varios archivos
- Lógica duplicada que podría abstraerse
- Estilos de código inconsistentes
- Patrones de manejo de errores faltantes

### Fase 5: Recomendaciones de librerías
Para implementaciones personalizadas:
1. Verifica si las librerías actuales ya proveen la funcionalidad
2. Busca paquetes maduros del ecosistema
3. Verifica la salud de la librería (commits, issues, actividad)
4. Verifica la compatibilidad con la configuración del proyecto

### Fase 6: Reporte integral
Genera un reporte detallado con:
- Resumen ejecutivo
- Problemas críticos que requieren atención inmediata
- Hallazgos archivo por archivo
- Plan de acción priorizado
- Estimaciones de esfuerzo
- Recomendaciones de librerías

## Niveles de prioridad de los problemas

- **Crítico** - Vulnerabilidades de seguridad, funcionalidad rota
- **Prioridad alta** - Cuellos de botella de rendimiento, código inmantenible
- **Prioridad media** - Calidad del código, desviaciones de buenas prácticas
- **Prioridad baja** - Estilo, mejoras menores
- **Quick Wins** - Menos de 30 minutos para corregir

## Categorías de análisis

### Seguridad
- Secretos hardcodeados
- Riesgos de inyección SQL
- Vulnerabilidades XSS
- Validación de entradas faltante
- Datos sensibles expuestos

### Rendimiento
- Algoritmos ineficientes
- Operaciones bloqueantes
- Fugas de memoria
- Oportunidades de caché desaprovechadas
- Patrones de consultas N+1

### TypeScript/Seguridad de tipos
- Anotaciones de tipo faltantes
- Uso del tipo `any`
- Tipos personalizados que duplican tipos oficiales
- Paquetes @types faltantes

### Problemas de Async/Promise
- Palabras clave await faltantes
- Rechazos de promesas no manejados
- Callback hell

### Código muerto
- Imports y exports sin usar
- Funciones, clases y métodos sin usar
- Variables y tipos sin usar
- Bloques de código inalcanzables
- Archivos sin usar (no importados en ninguna parte)
- Dependencias sin usar

**Herramientas:**
- JavaScript/TypeScript: `npx knip --reporter json`
- Python: `deadcode . --dry`

**Importante:** Verifica siempre los hallazgos de las herramientas antes de reportarlos. Revisa:
- Imports dinámicos (`import(variable)`)
- Patrones de frameworks (componentes de React, decoradores)
- Re-exports para la API pública
- Puntos de entrada (scripts de CLI, handlers serverless)

## Recursos

Consulta los documentos de referencia para las metodologías completas:

- `references/audit-methodology.md` - Proceso de auditoría completo de 6 fases con checklists detallados
- `references/dead-code-methodology.md` - Herramientas de detección de código muerto, verificación y flujos de limpieza

## Referencia rápida

### Antes de empezar
- [ ] Lee los archivos de configuración del proyecto
- [ ] Identifica el stack tecnológico y las librerías
- [ ] Ejecuta los linters existentes como baseline
- [ ] Crea la lista de seguimiento de archivos

### Durante la auditoría
- [ ] Marca los archivos como en progreso
- [ ] Analiza cada categoría sistemáticamente
- [ ] Anota los números de línea específicos
- [ ] Documenta ejemplos de antes/después
- [ ] Marca los archivos como completados

### Después de la auditoría
- [ ] Categoriza todos los hallazgos por prioridad
- [ ] Genera el reporte integral
- [ ] Guarda el reporte en la raíz del proyecto
- [ ] Proporciona un breve resumen en consola
