# Metodología de auditoría de código

Este documento proporciona un enfoque integral y sistemático para la auditoría de calidad de código. Sigue estas fases para un análisis exhaustivo.

## Fase 0: Preparación previa al análisis

Antes de analizar el código, establece el contexto:

### 1. Configuración del proyecto
- **Archivos de paquetes**: package.json, requirements.txt, go.mod, pom.xml, etc.
- **Stack tecnológico**: Identifica lenguajes, frameworks y librerías principales
- **Configuraciones de linting**: eslint, prettier, black, golint, etc.
- **Documentación del proyecto**: CLAUDE.md, README.md para lineamientos específicos del proyecto

### 2. Verificaciones de baseline
Ejecuta el linting y el testing existentes:
```bash
# JavaScript/TypeScript
npm run lint
npm run typecheck
npm test

# Python
black --check .
flake8
pytest

# Go
go vet ./...
golint ./...
```

Documenta los errores/advertencias existentes como baseline.

### 3. Carga de documentación
Usa Context7 para precargar la documentación de las librerías principales identificadas:
```
mcp__context7__resolve-library-id  → Get library ID
mcp__context7__query-docs    → Load current best practices
```

## Fase 1: Descubrimiento

### Identificación de archivos
Encuentra todos los archivos de código por tipo:
```
*.js, *.ts, *.jsx, *.tsx  (JavaScript/TypeScript)
*.py                       (Python)
*.java                     (Java)
*.go                       (Go)
*.rs                       (Rust)
*.rb                       (Ruby)
```

### Organización
- Agrupa los archivos por módulo/funcionalidad para un análisis contextual
- Crea una lista de seguimiento para un avance sistemático
- Prioriza la lógica de negocio central sobre las utilidades

## Fase 2: Análisis archivo por archivo

Para cada archivo, analiza las siguientes categorías:

### Código muerto
- Funciones y métodos sin usar
- Variables e imports sin usar
- Bloques de código inalcanzables
- Código comentado
- Funcionalidades deprecadas que aún están presentes

### Code smells y anti-patrones
- Funciones de más de 50 líneas
- Alta complejidad ciclomática (> 10)
- Condicionales profundamente anidados (> 3 niveles)
- Números mágicos sin constantes
- Duplicación de código por copiar y pegar
- God objects/funciones que hacen demasiado
- Listas de parámetros largas (> 5 parámetros)

### Vulnerabilidades de seguridad
- Secretos, API keys y contraseñas hardcodeados
- Vulnerabilidades de inyección SQL
- Riesgos de XSS (Cross-Site Scripting)
- Riesgos de inyección de comandos
- Deserialización insegura
- Validación de entradas faltante
- Divulgación de información en los errores

### Problemas de rendimiento
- Algoritmos O(n²) o peores en rutas críticas (hot paths)
- Índices de base de datos faltantes
- Patrones de consultas N+1
- Operaciones síncronas innecesarias
- Caché faltante para operaciones costosas
- Asignaciones de memoria grandes dentro de ciclos
- I/O bloqueante en contextos async

### Problemas de TypeScript/seguridad de tipos
- Anotaciones de tipo faltantes
- Uso excesivo del tipo `any`
- Aserciones de tipo que podrían evitarse
- Tipos personalizados que duplican paquetes oficiales @types/*
- Verificaciones de null/undefined faltantes

### Problemas de Async/Promise
- Palabras clave `await` faltantes
- Rechazos de promesas no manejados
- Callback hell que debería usar async/await
- Promesas fire-and-forget sin manejo de errores

### Fugas de memoria
- Event listeners que no se eliminan en el cleanup
- Timers (setInterval, setTimeout) que no se limpian
- Objetos grandes retenidos innecesariamente
- Closures que mantienen referencias por demasiado tiempo
- Referencias al DOM que se conservan después de eliminar el elemento

### Manejo de errores
- Bloques catch vacíos
- Patrones de capturar e ignorar
- try/catch faltante en código async
- Tipos de error inconsistentes
- Mensajes de error genéricos que ocultan la causa raíz

## Fase 3: Verificación de buenas prácticas

### Revisión de documentación con Context7
Para cada librería importante identificada:

1. **Resuelve el ID de la librería**:
   ```
   mcp__context7__resolve-library-id: "react"
   ```

2. **Obtén las buenas prácticas actuales**:
   ```
   mcp__context7__query-docs: {
     "context7CompatibleLibraryID": "/facebook/react",
     "topic": "hooks best practices"
   }
   ```

3. **Áreas de enfoque**:
   - Guías de migración entre versiones
   - Funcionalidades deprecadas y sus reemplazos
   - Buenas prácticas de rendimiento
   - Consideraciones de seguridad
   - Errores comunes y anti-patrones

### Investigación en GitHub
Usa el CLI `gh` para investigar el uso en el mundo real:

```bash
# Search for patterns
gh search code "useEffect cleanup" --language=typescript

# Check repository health
gh repo view [library] --json stargazersCount,updatedAt,openIssues

# Look for security advisories
gh api /repos/{owner}/{repo}/security-advisories
```

### Contraste de hallazgos
- Compara la implementación real con la documentación oficial
- Identifica desviaciones de los patrones recomendados
- Anota los patrones obsoletos que necesitan modernización
- Marca los anti-patrones desaconsejados explícitamente en la documentación

## Fase 3.5: Verificación de tipos de TypeScript

Para proyectos TypeScript, realiza un análisis de tipos adicional:

### Busca tipos duplicados
Busca interfaces personalizadas que replican tipos oficiales:
- Tipos de React (React.FC, React.Component, tipos de eventos)
- Tipos de Node.js (Buffer, Process, Global)
- Tipos del DOM (HTMLElement, tipos de Event)
- Tipos de Express (Request, Response)
- Tipos de librerías populares (lodash, axios, etc.)

### Verifica los paquetes @types
```bash
# Check if official types exist
npm view @types/[library] types

# Verify installed @types versions
npm ls @types/*
```

### Problemas comunes
- `IRequest` personalizado cuando existe `express.Request`
- Tipos de eventos personalizados cuando React ya los provee
- Duplicar tipos integrados de `@types/node`

## Fase 4: Detección de patrones

Busca problemas recurrentes en todo el codebase:

### Patrones entre archivos
- El mismo anti-patrón repetido en varios archivos
- Funciones utilitarias duplicadas
- Enfoques inconsistentes de manejo de errores
- Estilos de código distintos en distintos módulos

### Oportunidades de abstracción
- Código repetido que podría ser una función utilitaria
- Patrones comunes que podrían ser hooks (React)
- Responsabilidades transversales (cross-cutting concerns) que requieren middleware

### Inconsistencias
- Estilos async mezclados (callbacks, promesas, async/await)
- Convenciones de nombres inconsistentes
- Distintas estrategias de manejo de errores
- Patrones de organización de código variables

## Fase 5: Recomendaciones de librerías

Para implementaciones personalizadas, encuentra reemplazos maduros:

### Proceso de descubrimiento
1. **Revisa primero las librerías existentes** - Usa Context7 para ver si las librerías actuales ya proveen la funcionalidad necesaria
2. **Busca en los registros de paquetes** - npm, PyPI, crates.io, etc.
3. **Verifica la salud de la librería**:
   - Commits recientes (desarrollo activo)
   - Issues abiertos (capacidad de respuesta)
   - Estadísticas de descargas (adopción de la comunidad)
   - Security advisories (historial de vulnerabilidades)

### Criterios de evaluación
- **Mantenimiento**: Último commit < 6 meses
- **Adopción**: Cantidad significativa de descargas/estrellas
- **Seguridad**: Sin vulnerabilidades sin atender
- **Tamaño del bundle**: Importante para código frontend
- **Estabilidad de la API**: Versionado semántico, guías de migración
- **Documentación**: Ejemplos claros y documentación de la API

### Reemplazos comunes
| Implementación personalizada | Librería recomendada |
|----------------------|---------------------|
| Manipulación de fechas | date-fns, dayjs |
| Cliente HTTP | axios, ky |
| Validación de formularios | zod, yup |
| Gestión del estado | zustand, jotai |
| Clonado profundo | lodash/cloneDeep, structuredClone |
| Generación de UUID | uuid, nanoid |
| Lógica de reintentos | p-retry, async-retry |

## Fase 6: Generación del reporte

### Estructura del reporte

#### Resumen ejecutivo (2-3 párrafos)
- Total de archivos analizados
- Panorama general de los hallazgos
- Riesgos y recomendaciones clave

#### Problemas críticos (acción inmediata)
Para cada uno:
- Ruta del archivo y número de línea
- Descripción del problema
- Impacto en seguridad/estabilidad
- Ejemplo de corrección
- Estimación de esfuerzo

#### Problemas de prioridad alta
- Cuellos de botella de rendimiento
- Problemas de mantenibilidad
- Manejo de errores faltante

#### Problemas de prioridad media
- Violaciones de buenas prácticas
- Inquietudes de calidad de código
- Mejoras de seguridad de tipos

#### Problemas de prioridad baja
- Inconsistencias de estilo
- Mejoras menores
- Vacíos en la documentación

#### Recomendaciones de librerías
Para cada reemplazo sugerido:
- Ubicación actual del código personalizado
- Librería recomendada
- Esfuerzo de migración
- Impacto en el tamaño del bundle

#### Quick Wins
Correcciones de bajo esfuerzo y alto valor:
- < 30 minutos de implementación
- Alto impacto en calidad/seguridad

#### Plan de acción
Pasos priorizados con:
- Estimaciones de esfuerzo (S/M/L/XL)
- Dependencias entre tareas
- Asignación sugerida por sprint

### Requisitos de formato del reporte

Cada problema debe incluir:
```markdown
### [PRIORITY] Título del problema
**Ubicación:** `src/auth/login.js:42`

**Problema:**
Descripción del problema y por qué importa.

**Antes:**
```javascript
// problematic code
```

**Después:**
```javascript
// fixed code
```

**Esfuerzo:** S (< 30 min) | M (1-4 horas) | L (4-8 horas) | XL (> 8 horas)
```

## Referencia de uso de herramientas

### Context7
```
# Resolve library ID first
mcp__context7__resolve-library-id: "express"

# Then get documentation
mcp__context7__query-docs: {
  "context7CompatibleLibraryID": "/expressjs/express",
  "topic": "middleware"
}
```

### GitHub CLI
```bash
# Repository health
gh repo view owner/repo --json stargazersCount,updatedAt

# Code search
gh search code "pattern" --language=javascript

# Issues search
gh search issues "memory leak" --repo=owner/repo
```

### Investigación de paquetes
Usa `mcp__fetch__fetch` para las páginas de los registros de paquetes:
- npmjs.com/package/[name]
- pypi.org/project/[name]

## Errores comunes que debes evitar

1. **No te bases en suposiciones** - Verifica siempre con la documentación
2. **No sugieras patrones obsoletos** - Revisa las buenas prácticas actuales
3. **No recomiendes librerías sin mantenimiento** - Verifica la actividad
4. **No ignores las convenciones del proyecto** - Respeta los lineamientos de CLAUDE.md
5. **No rompas la funcionalidad** - Asegúrate de que las correcciones sean seguras
6. **No sobrediseñes (over-engineering)** - Considera la relación costo/beneficio
7. **No omitas las verificaciones de tipos de TypeScript** - Los tipos son documentación
8. **No ignores el tamaño del bundle** - El rendimiento del frontend importa

## Optimización del rendimiento

Para codebases grandes:
- **Procesamiento en paralelo**: Analiza varios archivos simultáneamente
- **Operaciones por lotes**: Agrupa verificaciones similares
- **Escaneo selectivo**: Enfócate primero en los archivos modificados
- **Caché de documentación**: Reutiliza las consultas a Context7
- **Reportes progresivos**: Proporciona resultados intermedios
