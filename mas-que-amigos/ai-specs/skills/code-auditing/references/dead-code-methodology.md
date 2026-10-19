# Metodología de detección de código muerto

Este documento proporciona una guía para detectar y eliminar código muerto usando herramientas automatizadas y verificación manual.

## Descripción general

El código muerto es código que existe en el codebase pero nunca se ejecuta. Aumenta la carga de mantenimiento, el tamaño del bundle y la carga cognitiva. Esta metodología usa herramientas especializadas combinadas con verificación por parte del agente para filtrar los falsos positivos.

## Tipos de código muerto

### 1. Imports sin usar
Sentencias import de módulos/símbolos que nunca se usan:
```javascript
import { unused } from './utils';  // Never referenced
```

### 2. Exports sin usar
Funciones, clases o variables exportadas pero nunca importadas en otro lugar:
```javascript
export function formatDate() { ... }  // Not imported anywhere
```

### 3. Variables sin usar
Variables declaradas pero nunca leídas:
```python
result = compute()  # Never used after assignment
```

### 4. Funciones/métodos sin usar
Funciones definidas pero nunca llamadas:
```typescript
function legacyHelper() { ... }  // Never invoked
```

### 5. Archivos sin usar
Archivos completos que no se importan en ninguna parte del codebase.

### 6. Dependencias sin usar
Paquetes en package.json/requirements.txt que no se usan en el código.

### 7. Código inalcanzable
Código después de sentencias return/throw o en ramas muertas:
```javascript
function foo() {
  return 42;
  console.log('never runs');  // Unreachable
}
```

## Herramientas de detección

### Knip (JavaScript/TypeScript)

**Instalación:**
```bash
npm install -g knip
# or use via npx (no install required)
```

**Comandos:**
```bash
# Detection (default output)
npx knip

# JSON output for parsing
npx knip --reporter json

# Fix automatically (use with caution)
npx knip --fix

# Specific workspace
npx knip --workspace packages/core

# Include/exclude patterns
npx knip --include files --exclude dependencies
```

**Configuración (`knip.json`):**
```json
{
  "$schema": "https://unpkg.com/knip@latest/schema.json",
  "entry": ["src/index.ts"],
  "project": ["src/**/*.ts"],
  "ignore": ["**/*.test.ts"],
  "ignoreDependencies": ["@types/*"]
}
```

**Categorías detectadas:**
- files, dependencies, devDependencies
- exports, nsExports, classMembers
- types, nsTypes, enumMembers
- unlisted, binaries, unresolved, duplicates

### Deadcode (Python)

**Instalación:**
```bash
pip install deadcode
```

**Comandos:**
```bash
# Detection
deadcode .

# With verbose output
deadcode . --verbose

# Fix automatically
deadcode . --fix

# Dry run (show what would be fixed)
deadcode . --dry

# Specific path
deadcode src/

# Exclude patterns
deadcode . --exclude tests/
```

**Capacidades de detección:**
- Imports sin usar (DC01)
- Variables sin usar (DC02)
- Funciones sin usar (DC03)
- Clases sin usar (DC04)
- Métodos sin usar (DC05)

## Detección de falsos positivos

**CRÍTICO: Verifica siempre los hallazgos antes de reportarlos al usuario.**

### Falsos positivos comunes

#### 1. Imports dinámicos
```javascript
// These imports appear unused but are loaded at runtime
const module = await import(modulePath);
require(variableName);
```

#### 2. Magia de los frameworks
```typescript
// React components used in JSX
export const Button = () => <button />;  // Used as <Button />

// Decorators in TypeScript/Python
@decorator
export class Controller {}

// Vue setup functions
export function useCounter() { ... }  // Used in <script setup>
```

#### 3. Re-exports para la API pública
```typescript
// index.ts barrel file - exports ARE the purpose
export { Helper } from './helper';  // Re-exported for external use
```

#### 4. Puntos de entrada
```javascript
// CLI scripts, serverless handlers, etc.
export const handler = async (event) => { ... };  // AWS Lambda entry
```

#### 5. Utilidades de prueba
```typescript
// Only used in test files
export class TestHelper {}  // Referenced in *.test.ts
```

#### 6. Referencias basadas en strings
```javascript
// Accessed via strings or reflection
const fn = functions['dynamicName'];
getattr(obj, 'method_name')()
```

### Checklist de verificación

Para cada elemento marcado, el agente DEBE:

1. **Leer el código marcado** para entender el contexto
2. **Buscar referencias dinámicas**:
   - `import(` o `require(` con variables
   - Interpolación de strings con el nombre del elemento
   - `eval`, `getattr` o patrones de reflexión
3. **Revisar los patrones del framework**:
   - React: ¿Es un componente usado en JSX?
   - Angular: ¿Está decorado con @Component, @Injectable, etc.?
   - Express: ¿Es un middleware o un route handler?
4. **Revisar los re-exports**:
   - ¿Está en un barrel file index.ts/index.js?
   - ¿Es parte de la API pública?
5. **Revisar los puntos de entrada**:
   - ¿Es un script de CLI, un handler serverless o un worker?
   - ¿Se referencia en los scripts de package.json o en archivos de configuración?

## Flujo de trabajo

### 1. Ejecuta la herramienta de detección
```bash
# Use the helper script
${CLAUDE_PLUGIN_ROOT}/scripts/dead-code-detect.sh --format json
```

### 2. Parsea y categoriza
Agrupa los hallazgos por tipo:
- Exports sin usar
- Archivos sin usar
- Dependencias sin usar
- Imports sin usar
- Miembros de clase sin usar

### 3. Verifica cada hallazgo
Para cada elemento:
1. Lee el código
2. Revisa si coincide con patrones de falsos positivos
3. Márcalo como "verified" (verificado) o "likely false positive" (probable falso positivo)

### 4. Presenta el reporte verificado
Muéstrale al usuario:
- Conteos resumidos (solo elementos verificados)
- Lista detallada con referencias archivo:línea
- Elementos filtrados con sus razones

### 5. Aplica las correcciones (tras la aprobación)
```bash
# Only after user confirms
npx knip --fix
# or
deadcode . --fix
```

## Integración con las auditorías

### Integración con la verificación rápida
- Ejecuta la detección como parte de las verificaciones automatizadas
- Reporta los hallazgos en la categoría "Dead Code"
- No corrijas automáticamente; informa al usuario de los hallazgos

### Integración con la auditoría profunda
- Ejecútala como la Fase 2.5: Detección de código muerto
- Inclúyela en el reporte integral
- Proporciona los comandos de corrección para que el usuario los ejecute

### Comando independiente `/dead-code`
- Flujo de trabajo enfocado solo en código muerto
- Limpieza interactiva con aprobación del usuario
- Muestra el proceso de verificación

## Buenas prácticas

1. **Ejecútalo con regularidad** - Inclúyelo en el pipeline de CI/CD
2. **Configúralo correctamente** - Define reglas de ignore para los patrones del framework
3. **Prueba después de eliminar** - Asegúrate de que nada se rompa
4. **Revisa antes del commit** - Se recomienda la verificación manual
5. **Documenta las excepciones** - Comenta por qué cierto código "muerto" es intencional
6. **Empieza de forma conservadora** - Es mejor que se escape algo a romper cosas
