# Buenas prácticas para crear Skills

> Aprende a escribir Skills efectivas que Claude pueda descubrir y usar con éxito.

Las buenas Skills son concisas, están bien estructuradas y se prueban con uso real. Esta guía ofrece decisiones prácticas de autoría para ayudarte a escribir Skills que Claude pueda descubrir y usar de forma efectiva.

Para contexto conceptual sobre cómo funcionan las Skills, consulta la [descripción general de Skills](/en/docs/agents-and-tools/agent-skills/overview).

## Principios fundamentales

### La concisión es clave

La [ventana de contexto](https://platform.claude.com/docs/en/build-with-claude/context-windows) es un bien público. Tu Skill comparte la ventana de contexto con todo lo demás que Claude necesita saber, incluyendo:

* El system prompt
* El historial de la conversación
* Los metadatos de otras Skills
* Tu solicitud real

No todo token de tu Skill tiene un costo inmediato. Al inicio, solo se precargan los metadatos (nombre y descripción) de todas las Skills. Claude lee SKILL.md solo cuando la Skill se vuelve relevante, y lee archivos adicionales solo según se necesiten. Sin embargo, ser conciso en SKILL.md sigue importando: una vez que Claude lo carga, cada token compite con el historial de la conversación y el resto del contexto.

**Suposición por defecto**: Claude ya es muy inteligente

Agrega solo el contexto que Claude todavía no tiene. Cuestiona cada pieza de información:

* "¿Claude realmente necesita esta explicación?"
* "¿Puedo asumir que Claude sabe esto?"
* "¿Este párrafo justifica su costo en tokens?"

**Buen ejemplo: Conciso** (aproximadamente 50 tokens):

````markdown  theme={null}
## Extraer texto de PDF

Usa pdfplumber para la extracción de texto:

```python
import pdfplumber

with pdfplumber.open("file.pdf") as pdf:
    text = pdf.pages[0].extract_text()
```
````

**Mal ejemplo: Demasiado verboso** (aproximadamente 150 tokens):

```markdown  theme={null}
## Extraer texto de PDF

Los archivos PDF (Portable Document Format) son un formato de archivo común que contiene
texto, imágenes y otro contenido. Para extraer texto de un PDF, necesitarás
usar una librería. Hay muchas librerías disponibles para procesar PDF, pero
recomendamos pdfplumber porque es fácil de usar y maneja bien la mayoría de los casos.
Primero, tendrás que instalarla usando pip. Luego puedes usar el código de abajo...
```

La versión concisa asume que Claude sabe qué son los PDF y cómo funcionan las librerías.

### Define grados de libertad apropiados

Ajusta el nivel de especificidad a la fragilidad y variabilidad de la tarea.

**Libertad alta** (instrucciones basadas en texto):

Úsala cuando:

* Varios enfoques son válidos
* Las decisiones dependen del contexto
* Las heurísticas guían el enfoque

Ejemplo:

```markdown  theme={null}
## Proceso de revisión de código

1. Analiza la estructura y organización del código
2. Busca posibles bugs o casos borde
3. Sugiere mejoras de legibilidad y mantenibilidad
4. Verifica el cumplimiento de las convenciones del proyecto
```

**Libertad media** (pseudocódigo o scripts con parámetros):

Úsala cuando:

* Existe un patrón preferido
* Se acepta cierta variación
* La configuración afecta el comportamiento

Ejemplo:

````markdown  theme={null}
## Generar reporte

Usa esta plantilla y personalízala según sea necesario:

```python
def generate_report(data, format="markdown", include_charts=True):
    # Process data
    # Generate output in specified format
    # Optionally include visualizations
```
````

**Libertad baja** (scripts específicos, pocos o ningún parámetro):

Úsala cuando:

* Las operaciones son frágiles y propensas a errores
* La consistencia es crítica
* Se debe seguir una secuencia específica

Ejemplo:

````markdown  theme={null}
## Migración de base de datos

Ejecuta exactamente este script:

```bash
python scripts/migrate.py --verify --backup
```

No modifiques el comando ni agregues flags adicionales.
````

**Analogía**: Piensa en Claude como un robot que explora un camino:

* **Puente angosto con precipicios a ambos lados**: Solo hay una forma segura de avanzar. Proporciona barandas específicas e instrucciones exactas (libertad baja). Ejemplo: migraciones de base de datos que deben ejecutarse en una secuencia exacta.
* **Campo abierto sin peligros**: Muchos caminos llevan al éxito. Da una dirección general y confía en que Claude encontrará la mejor ruta (libertad alta). Ejemplo: revisiones de código donde el contexto determina el mejor enfoque.

### Prueba con todos los modelos que planeas usar

Las Skills actúan como complementos de los modelos, así que su efectividad depende del modelo subyacente. Prueba tu Skill con todos los modelos con los que planeas usarla.

**Consideraciones de prueba por modelo**:

* **Claude Haiku** (rápido, económico): ¿La Skill ofrece suficiente orientación?
* **Claude Sonnet** (equilibrado): ¿La Skill es clara y eficiente?
* **Claude Opus** (razonamiento potente): ¿La Skill evita explicar de más?

Lo que funciona perfectamente para Opus podría necesitar más detalle para Haiku. Si planeas usar tu Skill en varios modelos, apunta a instrucciones que funcionen bien con todos ellos.

## Estructura de una Skill

<Note>
  **Frontmatter YAML**: El frontmatter de SKILL.md requiere dos campos:

  * `name` - Nombre legible de la Skill (máximo 64 caracteres)
  * `description` - Descripción de una línea de lo que hace la Skill y cuándo usarla (máximo 1024 caracteres)

  Para detalles completos sobre la estructura de una Skill, consulta la [descripción general de Skills](/en/docs/agents-and-tools/agent-skills/overview#skill-structure).
</Note>

### Convenciones de nombres

Usa patrones de nombres consistentes para que sea más fácil referenciar y discutir las Skills. Recomendamos usar la **forma de gerundio** (verbo + -ing) para los nombres de Skills, ya que describe claramente la actividad o capacidad que ofrece la Skill.

**Buenos ejemplos de nombres (forma de gerundio)**:

* "Processing PDFs"
* "Analyzing spreadsheets"
* "Managing databases"
* "Testing code"
* "Writing documentation"

**Alternativas aceptables**:

* Frases nominales: "PDF Processing", "Spreadsheet Analysis"
* Orientados a la acción: "Process PDFs", "Analyze Spreadsheets"

**Evita**:

* Nombres vagos: "Helper", "Utils", "Tools"
* Nombres demasiado genéricos: "Documents", "Data", "Files"
* Patrones inconsistentes dentro de tu colección de skills

Los nombres consistentes facilitan:

* Referenciar Skills en la documentación y las conversaciones
* Entender de un vistazo qué hace una Skill
* Organizar y buscar entre múltiples Skills
* Mantener una biblioteca de skills profesional y cohesiva

### Escribir descripciones efectivas

El campo `description` permite descubrir la Skill y debe incluir tanto lo que hace la Skill como cuándo usarla.

<Warning>
  **Escribe siempre en tercera persona**. La descripción se inyecta en el system prompt, y un punto de vista inconsistente puede causar problemas de descubrimiento.

  * **Bien:** "Procesa archivos de Excel y genera reportes"
  * **Evita:** "Puedo ayudarte a procesar archivos de Excel"
  * **Evita:** "Puedes usar esto para procesar archivos de Excel"
</Warning>

**Sé específico e incluye términos clave**. Incluye tanto lo que hace la Skill como disparadores/contextos específicos de cuándo usarla.

Cada Skill tiene exactamente un campo de descripción. La descripción es crítica para la selección de skills: Claude la usa para elegir la Skill correcta entre potencialmente más de 100 Skills disponibles. Tu descripción debe dar suficiente detalle para que Claude sepa cuándo seleccionar esta Skill, mientras que el resto de SKILL.md aporta los detalles de implementación.

Ejemplos efectivos:

**Skill de procesamiento de PDF:**

```yaml  theme={null}
description: Extract text and tables from PDF files, fill forms, merge documents. Use when working with PDF files or when the user mentions PDFs, forms, or document extraction.
```

**Skill de análisis de Excel:**

```yaml  theme={null}
description: Analyze Excel spreadsheets, create pivot tables, generate charts. Use when analyzing Excel files, spreadsheets, tabular data, or .xlsx files.
```

**Skill asistente de commits de Git:**

```yaml  theme={null}
description: Generate descriptive commit messages by analyzing git diffs. Use when the user asks for help writing commit messages or reviewing staged changes.
```

Evita descripciones vagas como estas:

```yaml  theme={null}
description: Helps with documents
```

```yaml  theme={null}
description: Processes data
```

```yaml  theme={null}
description: Does stuff with files
```

### Patrones de divulgación progresiva

SKILL.md sirve como una visión general que dirige a Claude hacia materiales detallados según se necesiten, como una tabla de contenido en una guía de onboarding. Para una explicación de cómo funciona la divulgación progresiva, consulta [Cómo funcionan las Skills](/en/docs/agents-and-tools/agent-skills/overview#how-skills-work) en la descripción general.

**Orientación práctica:**

* Mantén el cuerpo de SKILL.md por debajo de 500 líneas para un rendimiento óptimo
* Divide el contenido en archivos separados cuando te acerques a este límite
* Usa los patrones de abajo para organizar instrucciones, código y recursos de forma efectiva

#### Visión general visual: De lo simple a lo complejo

Una Skill básica comienza solo con un archivo SKILL.md que contiene metadatos e instrucciones:

<img src="https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-simple-file.png?fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=87782ff239b297d9a9e8e1b72ed72db9" alt="Archivo SKILL.md simple que muestra el frontmatter YAML y el cuerpo en markdown" data-og-width="2048" width="2048" data-og-height="1153" height="1153" data-path="images/agent-skills-simple-file.png" data-optimize="true" data-opv="3" srcset="https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-simple-file.png?w=280&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=c61cc33b6f5855809907f7fda94cd80e 280w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-simple-file.png?w=560&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=90d2c0c1c76b36e8d485f49e0810dbfd 560w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-simple-file.png?w=840&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=ad17d231ac7b0bea7e5b4d58fb4aeabb 840w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-simple-file.png?w=1100&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=f5d0a7a3c668435bb0aee9a3a8f8c329 1100w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-simple-file.png?w=1650&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=0e927c1af9de5799cfe557d12249f6e6 1650w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-simple-file.png?w=2500&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=46bbb1a51dd4c8202a470ac8c80a893d 2500w" />

A medida que tu Skill crece, puedes empaquetar contenido adicional que Claude carga solo cuando lo necesita:

<img src="https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-bundling-content.png?fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=a5e0aa41e3d53985a7e3e43668a33ea3" alt="Empaquetado de archivos de referencia adicionales como reference.md y forms.md." data-og-width="2048" width="2048" data-og-height="1327" height="1327" data-path="images/agent-skills-bundling-content.png" data-optimize="true" data-opv="3" srcset="https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-bundling-content.png?w=280&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=f8a0e73783e99b4a643d79eac86b70a2 280w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-bundling-content.png?w=560&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=dc510a2a9d3f14359416b706f067904a 560w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-bundling-content.png?w=840&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=82cd6286c966303f7dd914c28170e385 840w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-bundling-content.png?w=1100&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=56f3be36c77e4fe4b523df209a6824c6 1100w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-bundling-content.png?w=1650&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=d22b5161b2075656417d56f41a74f3dd 1650w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-bundling-content.png?w=2500&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=3dd4bdd6850ffcc96c6c45fcb0acd6eb 2500w" />

La estructura completa del directorio de una Skill podría verse así:

```
pdf/
├── SKILL.md              # Main instructions (loaded when triggered)
├── FORMS.md              # Form-filling guide (loaded as needed)
├── reference.md          # API reference (loaded as needed)
├── examples.md           # Usage examples (loaded as needed)
└── scripts/
    ├── analyze_form.py   # Utility script (executed, not loaded)
    ├── fill_form.py      # Form filling script
    └── validate.py       # Validation script
```

#### Patrón 1: Guía de alto nivel con referencias

````markdown  theme={null}
---
name: PDF Processing
description: Extrae texto y tablas de archivos PDF, llena formularios y combina documentos. Úsala cuando trabajes con archivos PDF o cuando el usuario mencione PDFs, formularios o extracción de documentos.
---

# Procesamiento de PDF

## Inicio rápido

Extrae texto con pdfplumber:
```python
import pdfplumber
with pdfplumber.open("file.pdf") as pdf:
    text = pdf.pages[0].extract_text()
```

## Funciones avanzadas

**Llenado de formularios**: Consulta [FORMS.md](FORMS.md) para la guía completa
**Referencia de API**: Consulta [REFERENCE.md](REFERENCE.md) para todos los métodos
**Ejemplos**: Consulta [EXAMPLES.md](EXAMPLES.md) para patrones comunes
````

Claude carga FORMS.md, REFERENCE.md o EXAMPLES.md solo cuando los necesita.

#### Patrón 2: Organización por dominio

Para Skills con múltiples dominios, organiza el contenido por dominio para evitar cargar contexto irrelevante. Cuando un usuario pregunta por métricas de ventas, Claude solo necesita leer los esquemas relacionados con ventas, no los datos de finanzas o marketing. Esto mantiene bajo el uso de tokens y el contexto enfocado.

```
bigquery-skill/
├── SKILL.md (overview and navigation)
└── reference/
    ├── finance.md (revenue, billing metrics)
    ├── sales.md (opportunities, pipeline)
    ├── product.md (API usage, features)
    └── marketing.md (campaigns, attribution)
```

````markdown SKILL.md theme={null}
# Análisis de datos en BigQuery

## Datasets disponibles

**Finanzas**: Ingresos, ARR, facturación → Consulta [reference/finance.md](reference/finance.md)
**Ventas**: Oportunidades, pipeline, cuentas → Consulta [reference/sales.md](reference/sales.md)
**Producto**: Uso de API, funciones, adopción → Consulta [reference/product.md](reference/product.md)
**Marketing**: Campañas, atribución, email → Consulta [reference/marketing.md](reference/marketing.md)

## Búsqueda rápida

Encuentra métricas específicas usando grep:

```bash
grep -i "revenue" reference/finance.md
grep -i "pipeline" reference/sales.md
grep -i "api usage" reference/product.md
```
````

#### Patrón 3: Detalles condicionales

Muestra el contenido básico y enlaza al contenido avanzado:

```markdown  theme={null}
# Procesamiento de DOCX

## Crear documentos

Usa docx-js para documentos nuevos. Consulta [DOCX-JS.md](DOCX-JS.md).

## Editar documentos

Para ediciones simples, modifica el XML directamente.

**Para control de cambios**: Consulta [REDLINING.md](REDLINING.md)
**Para detalles de OOXML**: Consulta [OOXML.md](OOXML.md)
```

Claude lee REDLINING.md u OOXML.md solo cuando el usuario necesita esas funciones.

### Evita referencias profundamente anidadas

Claude puede leer parcialmente archivos cuando están referenciados desde otros archivos referenciados. Al encontrar referencias anidadas, Claude podría usar comandos como `head -100` para previsualizar el contenido en lugar de leer archivos completos, lo que resulta en información incompleta.

**Mantén las referencias a un solo nivel de profundidad desde SKILL.md**. Todos los archivos de referencia deben enlazarse directamente desde SKILL.md para asegurar que Claude lea archivos completos cuando los necesite.

**Mal ejemplo: Demasiado profundo**:

```markdown  theme={null}
# SKILL.md
Consulta [advanced.md](advanced.md)...

# advanced.md
Consulta [details.md](details.md)...

# details.md
Aquí está la información real...
```

**Buen ejemplo: Un solo nivel de profundidad**:

```markdown  theme={null}
# SKILL.md

**Uso básico**: [instrucciones en SKILL.md]
**Funciones avanzadas**: Consulta [advanced.md](advanced.md)
**Referencia de API**: Consulta [reference.md](reference.md)
**Ejemplos**: Consulta [examples.md](examples.md)
```

### Estructura los archivos de referencia largos con una tabla de contenido

Para archivos de referencia de más de 100 líneas, incluye una tabla de contenido al inicio. Esto asegura que Claude pueda ver el alcance completo de la información disponible incluso cuando previsualiza con lecturas parciales.

**Ejemplo**:

```markdown  theme={null}
# Referencia de API

## Contenido
- Autenticación y configuración
- Métodos principales (crear, leer, actualizar, eliminar)
- Funciones avanzadas (operaciones por lotes, webhooks)
- Patrones de manejo de errores
- Ejemplos de código

## Autenticación y configuración
...

## Métodos principales
...
```

Claude puede entonces leer el archivo completo o saltar a secciones específicas según lo necesite.

Para detalles sobre cómo esta arquitectura basada en el sistema de archivos habilita la divulgación progresiva, consulta la sección [Entorno de ejecución](#entorno-de-ejecución) en la sección Avanzado más abajo.

## Flujos de trabajo y ciclos de retroalimentación

### Usa flujos de trabajo para tareas complejas

Divide las operaciones complejas en pasos claros y secuenciales. Para flujos de trabajo particularmente complejos, proporciona un checklist que Claude pueda copiar en su respuesta e ir marcando a medida que avanza.

**Ejemplo 1: Flujo de trabajo de síntesis de investigación** (para Skills sin código):

````markdown  theme={null}
## Flujo de trabajo de síntesis de investigación

Copia este checklist y registra tu progreso:

```
Progreso de la investigación:
- [ ] Paso 1: Leer todos los documentos fuente
- [ ] Paso 2: Identificar temas clave
- [ ] Paso 3: Contrastar afirmaciones
- [ ] Paso 4: Crear un resumen estructurado
- [ ] Paso 5: Verificar citas
```

**Paso 1: Leer todos los documentos fuente**

Revisa cada documento en el directorio `sources/`. Anota los argumentos principales y la evidencia que los respalda.

**Paso 2: Identificar temas clave**

Busca patrones entre las fuentes. ¿Qué temas aparecen repetidamente? ¿En qué coinciden o difieren las fuentes?

**Paso 3: Contrastar afirmaciones**

Para cada afirmación importante, verifica que aparezca en el material fuente. Anota qué fuente respalda cada punto.

**Paso 4: Crear un resumen estructurado**

Organiza los hallazgos por tema. Incluye:
- Afirmación principal
- Evidencia de respaldo de las fuentes
- Puntos de vista en conflicto (si los hay)

**Paso 5: Verificar citas**

Comprueba que cada afirmación referencie el documento fuente correcto. Si las citas están incompletas, regresa al Paso 3.
````

Este ejemplo muestra cómo los flujos de trabajo se aplican a tareas de análisis que no requieren código. El patrón de checklist funciona para cualquier proceso complejo de varios pasos.

**Ejemplo 2: Flujo de trabajo de llenado de formularios PDF** (para Skills con código):

````markdown  theme={null}
## Flujo de trabajo de llenado de formularios PDF

Copia este checklist y marca los elementos a medida que los completes:

```
Progreso de la tarea:
- [ ] Paso 1: Analizar el formulario (ejecutar analyze_form.py)
- [ ] Paso 2: Crear el mapeo de campos (editar fields.json)
- [ ] Paso 3: Validar el mapeo (ejecutar validate_fields.py)
- [ ] Paso 4: Llenar el formulario (ejecutar fill_form.py)
- [ ] Paso 5: Verificar la salida (ejecutar verify_output.py)
```

**Paso 1: Analizar el formulario**

Ejecuta: `python scripts/analyze_form.py input.pdf`

Esto extrae los campos del formulario y sus ubicaciones, y los guarda en `fields.json`.

**Paso 2: Crear el mapeo de campos**

Edita `fields.json` para agregar valores a cada campo.

**Paso 3: Validar el mapeo**

Ejecuta: `python scripts/validate_fields.py fields.json`

Corrige cualquier error de validación antes de continuar.

**Paso 4: Llenar el formulario**

Ejecuta: `python scripts/fill_form.py input.pdf fields.json output.pdf`

**Paso 5: Verificar la salida**

Ejecuta: `python scripts/verify_output.py output.pdf`

Si la verificación falla, regresa al Paso 2.
````

Los pasos claros evitan que Claude se salte validaciones críticas. El checklist les ayuda tanto a Claude como a ti a seguir el progreso en flujos de trabajo de varios pasos.

### Implementa ciclos de retroalimentación

**Patrón común**: Ejecutar el validador → corregir errores → repetir

Este patrón mejora enormemente la calidad de la salida.

**Ejemplo 1: Cumplimiento de la guía de estilo** (para Skills sin código):

```markdown  theme={null}
## Proceso de revisión de contenido

1. Redacta tu contenido siguiendo las pautas de STYLE_GUIDE.md
2. Revísalo contra el checklist:
   - Comprueba la consistencia de la terminología
   - Verifica que los ejemplos sigan el formato estándar
   - Confirma que estén presentes todas las secciones requeridas
3. Si encuentras problemas:
   - Anota cada problema con una referencia a la sección específica
   - Revisa el contenido
   - Revisa el checklist de nuevo
4. Continúa solo cuando se cumplan todos los requisitos
5. Finaliza y guarda el documento
```

Esto muestra el patrón de ciclo de validación usando documentos de referencia en lugar de scripts. El "validador" es STYLE\_GUIDE.md, y Claude realiza la comprobación leyendo y comparando.

**Ejemplo 2: Proceso de edición de documentos** (para Skills con código):

```markdown  theme={null}
## Proceso de edición de documentos

1. Haz tus ediciones en `word/document.xml`
2. **Valida de inmediato**: `python ooxml/scripts/validate.py unpacked_dir/`
3. Si la validación falla:
   - Revisa el mensaje de error con cuidado
   - Corrige los problemas en el XML
   - Ejecuta la validación de nuevo
4. **Continúa solo cuando la validación pase**
5. Reconstruye: `python ooxml/scripts/pack.py unpacked_dir/ output.docx`
6. Prueba el documento de salida
```

El ciclo de validación detecta errores temprano.

## Pautas de contenido

### Evita información sensible al tiempo

No incluyas información que quedará desactualizada:

**Mal ejemplo: Sensible al tiempo** (se volverá incorrecto):

```markdown  theme={null}
Si estás haciendo esto antes de agosto de 2025, usa la API antigua.
Después de agosto de 2025, usa la API nueva.
```

**Buen ejemplo** (usa una sección de "patrones antiguos"):

```markdown  theme={null}
## Método actual

Usa el endpoint de la API v2: `api.example.com/v2/messages`

## Patrones antiguos

<details>
<summary>API v1 heredada (obsoleta desde 2025-08)</summary>

La API v1 usaba: `api.example.com/v1/messages`

Este endpoint ya no tiene soporte.
</details>
```

La sección de patrones antiguos aporta contexto histórico sin saturar el contenido principal.

### Usa terminología consistente

Elige un término y úsalo en toda la Skill:

**Bien - Consistente**:

* Siempre "endpoint de API"
* Siempre "campo"
* Siempre "extraer"

**Mal - Inconsistente**:

* Mezclar "endpoint de API", "URL", "ruta de API", "path"
* Mezclar "campo", "casilla", "elemento", "control"
* Mezclar "extraer", "sacar", "obtener", "recuperar"

La consistencia ayuda a Claude a entender y seguir las instrucciones.

## Patrones comunes

### Patrón de plantilla

Proporciona plantillas para el formato de salida. Ajusta el nivel de rigidez a tus necesidades.

**Para requisitos estrictos** (como respuestas de API o formatos de datos):

````markdown  theme={null}
## Estructura del reporte

SIEMPRE usa exactamente esta estructura de plantilla:

```markdown
# [Título del análisis]

## Resumen ejecutivo
[Visión general de un párrafo de los hallazgos clave]

## Hallazgos clave
- Hallazgo 1 con datos de respaldo
- Hallazgo 2 con datos de respaldo
- Hallazgo 3 con datos de respaldo

## Recomendaciones
1. Recomendación específica y accionable
2. Recomendación específica y accionable
```
````

**Para orientación flexible** (cuando la adaptación es útil):

````markdown  theme={null}
## Estructura del reporte

Este es un formato por defecto razonable, pero usa tu mejor criterio según el análisis:

```markdown
# [Título del análisis]

## Resumen ejecutivo
[Visión general]

## Hallazgos clave
[Adapta las secciones según lo que descubras]

## Recomendaciones
[Ajústalas al contexto específico]
```

Ajusta las secciones según sea necesario para el tipo específico de análisis.
````

### Patrón de ejemplos

Para Skills donde la calidad de la salida depende de ver ejemplos, proporciona pares de entrada/salida igual que en el prompting normal:

````markdown  theme={null}
## Formato de mensajes de commit

Genera mensajes de commit siguiendo estos ejemplos:

**Ejemplo 1:**
Entrada: Se agregó autenticación de usuarios con tokens JWT
Salida:
```
feat(auth): implement JWT-based authentication

Add login endpoint and token validation middleware
```

**Ejemplo 2:**
Entrada: Se corrigió un bug donde las fechas se mostraban incorrectamente en los reportes
Salida:
```
fix(reports): correct date formatting in timezone conversion

Use UTC timestamps consistently across report generation
```

**Ejemplo 3:**
Entrada: Se actualizaron dependencias y se refactorizó el manejo de errores
Salida:
```
chore: update dependencies and refactor error handling

- Upgrade lodash to 4.17.21
- Standardize error response format across endpoints
```

Sigue este estilo: tipo(alcance): descripción breve, luego una explicación detallada.
````

Los ejemplos ayudan a Claude a entender el estilo y el nivel de detalle deseados con más claridad que las descripciones por sí solas.

### Patrón de flujo de trabajo condicional

Guía a Claude a través de los puntos de decisión:

```markdown  theme={null}
## Flujo de trabajo de modificación de documentos

1. Determina el tipo de modificación:

   **¿Creando contenido nuevo?** → Sigue el "Flujo de trabajo de creación" de abajo
   **¿Editando contenido existente?** → Sigue el "Flujo de trabajo de edición" de abajo

2. Flujo de trabajo de creación:
   - Usa la librería docx-js
   - Construye el documento desde cero
   - Exporta a formato .docx

3. Flujo de trabajo de edición:
   - Desempaqueta el documento existente
   - Modifica el XML directamente
   - Valida después de cada cambio
   - Vuelve a empaquetar al terminar
```

<Tip>
  Si los flujos de trabajo se vuelven grandes o complicados con muchos pasos, considera moverlos a archivos separados e indicarle a Claude que lea el archivo apropiado según la tarea en cuestión.
</Tip>

## Evaluación e iteración

### Construye las evaluaciones primero

**Crea evaluaciones ANTES de escribir documentación extensa.** Esto asegura que tu Skill resuelva problemas reales en lugar de documentar problemas imaginarios.

**Desarrollo guiado por evaluaciones:**

1. **Identifica brechas**: Ejecuta Claude en tareas representativas sin una Skill. Documenta fallas específicas o contexto faltante
2. **Crea evaluaciones**: Construye tres escenarios que prueben estas brechas
3. **Establece un baseline**: Mide el desempeño de Claude sin la Skill
4. **Escribe instrucciones mínimas**: Crea solo el contenido suficiente para cubrir las brechas y pasar las evaluaciones
5. **Itera**: Ejecuta las evaluaciones, compara contra el baseline y refina

Este enfoque asegura que estés resolviendo problemas reales en lugar de anticipar requisitos que quizás nunca se materialicen.

**Estructura de una evaluación**:

```json  theme={null}
{
  "skills": ["pdf-processing"],
  "query": "Extract all text from this PDF file and save it to output.txt",
  "files": ["test-files/document.pdf"],
  "expected_behavior": [
    "Successfully reads the PDF file using an appropriate PDF processing library or command-line tool",
    "Extracts text content from all pages in the document without missing any pages",
    "Saves the extracted text to a file named output.txt in a clear, readable format"
  ]
}
```

<Note>
  Este ejemplo demuestra una evaluación basada en datos con una rúbrica de prueba simple. Actualmente no ofrecemos una forma integrada de ejecutar estas evaluaciones. Los usuarios pueden crear su propio sistema de evaluación. Las evaluaciones son tu fuente de verdad para medir la efectividad de una Skill.
</Note>

### Desarrolla Skills de forma iterativa con Claude

El proceso de desarrollo de Skills más efectivo involucra al propio Claude. Trabaja con una instancia de Claude ("Claude A") para crear una Skill que usarán otras instancias ("Claude B"). Claude A te ayuda a diseñar y refinar las instrucciones, mientras Claude B las prueba en tareas reales. Esto funciona porque los modelos Claude entienden tanto cómo escribir instrucciones efectivas para agentes como qué información necesitan los agentes.

**Crear una Skill nueva:**

1. **Completa una tarea sin una Skill**: Trabaja un problema con Claude A usando prompting normal. Mientras trabajas, naturalmente aportarás contexto, explicarás preferencias y compartirás conocimiento procedimental. Fíjate en qué información proporcionas repetidamente.

2. **Identifica el patrón reutilizable**: Después de completar la tarea, identifica qué contexto proporcionaste que sería útil para tareas similares en el futuro.

   **Ejemplo**: Si trabajaste en un análisis de BigQuery, podrías haber proporcionado nombres de tablas, definiciones de campos, reglas de filtrado (como "siempre excluye las cuentas de prueba") y patrones de consulta comunes.

3. **Pídele a Claude A que cree una Skill**: "Crea una Skill que capture este patrón de análisis de BigQuery que acabamos de usar. Incluye los esquemas de las tablas, las convenciones de nombres y la regla sobre filtrar las cuentas de prueba."

   <Tip>
     Los modelos Claude entienden el formato y la estructura de las Skills de forma nativa. No necesitas system prompts especiales ni una skill de "escribir skills" para que Claude te ayude a crear Skills. Simplemente pídele a Claude que cree una Skill y generará contenido de SKILL.md correctamente estructurado, con el frontmatter y el cuerpo apropiados.
   </Tip>

4. **Revisa la concisión**: Comprueba que Claude A no haya agregado explicaciones innecesarias. Pide: "Elimina la explicación sobre qué significa la tasa de éxito; Claude ya lo sabe."

5. **Mejora la arquitectura de la información**: Pídele a Claude A que organice el contenido de forma más efectiva. Por ejemplo: "Organiza esto para que el esquema de la tabla esté en un archivo de referencia separado. Quizás agreguemos más tablas después."

6. **Prueba en tareas similares**: Usa la Skill con Claude B (una instancia nueva con la Skill cargada) en casos de uso relacionados. Observa si Claude B encuentra la información correcta, aplica las reglas correctamente y maneja la tarea con éxito.

7. **Itera según lo observado**: Si Claude B tiene dificultades o se le escapa algo, regresa a Claude A con detalles concretos: "Cuando Claude usó esta Skill, olvidó filtrar por fecha para el Q4. ¿Deberíamos agregar una sección sobre patrones de filtrado por fecha?"

**Iterar sobre Skills existentes:**

El mismo patrón jerárquico continúa al mejorar las Skills. Alternas entre:

* **Trabajar con Claude A** (el experto que ayuda a refinar la Skill)
* **Probar con Claude B** (el agente que usa la Skill para realizar trabajo real)
* **Observar el comportamiento de Claude B** y llevar los hallazgos de vuelta a Claude A

1. **Usa la Skill en flujos de trabajo reales**: Dale a Claude B (con la Skill cargada) tareas reales, no escenarios de prueba

2. **Observa el comportamiento de Claude B**: Anota dónde tiene dificultades, dónde tiene éxito o dónde toma decisiones inesperadas

   **Ejemplo de observación**: "Cuando le pedí a Claude B un reporte de ventas regional, escribió la consulta pero olvidó filtrar las cuentas de prueba, aunque la Skill menciona esta regla."

3. **Regresa a Claude A para mejoras**: Comparte el SKILL.md actual y describe lo que observaste. Pregunta: "Noté que Claude B olvidó filtrar las cuentas de prueba cuando le pedí un reporte regional. La Skill menciona el filtrado, pero ¿quizás no es lo suficientemente visible?"

4. **Revisa las sugerencias de Claude A**: Claude A podría sugerir reorganizar para que las reglas sean más visibles, usar un lenguaje más fuerte como "DEBES filtrar" en lugar de "siempre filtra", o reestructurar la sección del flujo de trabajo.

5. **Aplica y prueba los cambios**: Actualiza la Skill con los refinamientos de Claude A y luego prueba de nuevo con Claude B en solicitudes similares

6. **Repite según el uso**: Continúa este ciclo de observar-refinar-probar a medida que encuentres nuevos escenarios. Cada iteración mejora la Skill con base en el comportamiento real del agente, no en suposiciones.

**Recopilar retroalimentación del equipo:**

1. Comparte las Skills con tus compañeros de equipo y observa cómo las usan
2. Pregunta: ¿La Skill se activa cuando se espera? ¿Las instrucciones son claras? ¿Qué falta?
3. Incorpora la retroalimentación para cubrir los puntos ciegos de tus propios patrones de uso

**Por qué funciona este enfoque**: Claude A entiende las necesidades del agente, tú aportas la experiencia del dominio, Claude B revela brechas a través del uso real, y el refinamiento iterativo mejora las Skills con base en el comportamiento observado en lugar de suposiciones.

### Observa cómo Claude navega las Skills

A medida que iteras sobre las Skills, presta atención a cómo Claude realmente las usa en la práctica. Fíjate en:

* **Rutas de exploración inesperadas**: ¿Claude lee los archivos en un orden que no anticipaste? Esto podría indicar que tu estructura no es tan intuitiva como pensabas
* **Conexiones perdidas**: ¿Claude no sigue las referencias a archivos importantes? Tus enlaces podrían necesitar ser más explícitos o visibles
* **Dependencia excesiva de ciertas secciones**: Si Claude lee repetidamente el mismo archivo, considera si ese contenido debería estar en el SKILL.md principal
* **Contenido ignorado**: Si Claude nunca accede a un archivo empaquetado, podría ser innecesario o estar mal señalado en las instrucciones principales

Itera con base en estas observaciones en lugar de suposiciones. El 'name' y la 'description' en los metadatos de tu Skill son particularmente críticos. Claude los usa al decidir si activar la Skill en respuesta a la tarea actual. Asegúrate de que describan claramente qué hace la Skill y cuándo se debe usar.

## Antipatrones a evitar

### Evita rutas estilo Windows

Usa siempre barras diagonales (/) en las rutas de archivos, incluso en Windows:

* ✓ **Bien**: `scripts/helper.py`, `reference/guide.md`
* ✗ **Evita**: `scripts\helper.py`, `reference\guide.md`

Las rutas estilo Unix funcionan en todas las plataformas, mientras que las rutas estilo Windows causan errores en sistemas Unix.

### Evita ofrecer demasiadas opciones

No presentes múltiples enfoques a menos que sea necesario:

````markdown  theme={null}
**Mal ejemplo: Demasiadas opciones** (confuso):
"Puedes usar pypdf, o pdfplumber, o PyMuPDF, o pdf2image, o..."

**Buen ejemplo: Proporciona una opción por defecto** (con una salida alternativa):
"Usa pdfplumber para la extracción de texto:
```python
import pdfplumber
```

Para PDFs escaneados que requieren OCR, usa en su lugar pdf2image con pytesseract."
````

## Avanzado: Skills con código ejecutable

Las secciones de abajo se enfocan en Skills que incluyen scripts ejecutables. Si tu Skill usa solo instrucciones en markdown, salta a [Checklist para Skills efectivas](#checklist-para-skills-efectivas).

### Resuelve, no delegues

Al escribir scripts para Skills, maneja las condiciones de error en lugar de delegárselas a Claude.

**Buen ejemplo: Maneja los errores explícitamente**:

```python  theme={null}
def process_file(path):
    """Process a file, creating it if it doesn't exist."""
    try:
        with open(path) as f:
            return f.read()
    except FileNotFoundError:
        # Create file with default content instead of failing
        print(f"File {path} not found, creating default")
        with open(path, 'w') as f:
            f.write('')
        return ''
    except PermissionError:
        # Provide alternative instead of failing
        print(f"Cannot access {path}, using default")
        return ''
```

**Mal ejemplo: Delegarle el problema a Claude**:

```python  theme={null}
def process_file(path):
    # Just fail and let Claude figure it out
    return open(path).read()
```

Los parámetros de configuración también deben estar justificados y documentados para evitar "constantes vudú" (ley de Ousterhout). Si tú no sabes el valor correcto, ¿cómo lo determinará Claude?

**Buen ejemplo: Autodocumentado**:

```python  theme={null}
# HTTP requests typically complete within 30 seconds
# Longer timeout accounts for slow connections
REQUEST_TIMEOUT = 30

# Three retries balances reliability vs speed
# Most intermittent failures resolve by the second retry
MAX_RETRIES = 3
```

**Mal ejemplo: Números mágicos**:

```python  theme={null}
TIMEOUT = 47  # Why 47?
RETRIES = 5   # Why 5?
```

### Proporciona scripts utilitarios

Aunque Claude podría escribir un script, los scripts prefabricados ofrecen ventajas:

**Beneficios de los scripts utilitarios**:

* Más confiables que el código generado
* Ahorran tokens (no hace falta incluir el código en el contexto)
* Ahorran tiempo (no se requiere generar código)
* Aseguran consistencia entre usos

<img src="https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-executable-scripts.png?fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=4bbc45f2c2e0bee9f2f0d5da669bad00" alt="Empaquetado de scripts ejecutables junto con los archivos de instrucciones" data-og-width="2048" width="2048" data-og-height="1154" height="1154" data-path="images/agent-skills-executable-scripts.png" data-optimize="true" data-opv="3" srcset="https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-executable-scripts.png?w=280&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=9a04e6535a8467bfeea492e517de389f 280w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-executable-scripts.png?w=560&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=e49333ad90141af17c0d7651cca7216b 560w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-executable-scripts.png?w=840&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=954265a5df52223d6572b6214168c428 840w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-executable-scripts.png?w=1100&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=2ff7a2d8f2a83ee8af132b29f10150fd 1100w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-executable-scripts.png?w=1650&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=48ab96245e04077f4d15e9170e081cfb 1650w, https://mintcdn.com/anthropic-claude-docs/4Bny2bjzuGBK7o00/images/agent-skills-executable-scripts.png?w=2500&fit=max&auto=format&n=4Bny2bjzuGBK7o00&q=85&s=0301a6c8b3ee879497cc5b5483177c90 2500w" />

El diagrama de arriba muestra cómo funcionan los scripts ejecutables junto con los archivos de instrucciones. El archivo de instrucciones (forms.md) referencia el script, y Claude puede ejecutarlo sin cargar su contenido en el contexto.

**Distinción importante**: Deja claro en tus instrucciones si Claude debe:

* **Ejecutar el script** (lo más común): "Ejecuta `analyze_form.py` para extraer los campos"
* **Leerlo como referencia** (para lógica compleja): "Consulta `analyze_form.py` para ver el algoritmo de extracción de campos"

Para la mayoría de los scripts utilitarios, es preferible la ejecución porque es más confiable y eficiente. Consulta la sección [Entorno de ejecución](#entorno-de-ejecución) más abajo para detalles sobre cómo funciona la ejecución de scripts.

**Ejemplo**:

````markdown  theme={null}
## Scripts utilitarios

**analyze_form.py**: Extrae todos los campos de formulario del PDF

```bash
python scripts/analyze_form.py input.pdf > fields.json
```

Formato de salida:
```json
{
  "field_name": {"type": "text", "x": 100, "y": 200},
  "signature": {"type": "sig", "x": 150, "y": 500}
}
```

**validate_boxes.py**: Busca bounding boxes superpuestos

```bash
python scripts/validate_boxes.py fields.json
# Returns: "OK" or lists conflicts
```

**fill_form.py**: Aplica los valores de los campos al PDF

```bash
python scripts/fill_form.py input.pdf fields.json output.pdf
```
````

### Usa análisis visual

Cuando las entradas se pueden renderizar como imágenes, haz que Claude las analice:

````markdown  theme={null}
## Análisis del diseño del formulario

1. Convierte el PDF a imágenes:
   ```bash
   python scripts/pdf_to_images.py form.pdf
   ```

2. Analiza la imagen de cada página para identificar los campos del formulario
3. Claude puede ver visualmente las ubicaciones y los tipos de los campos
````

<Note>
  En este ejemplo, necesitarías escribir el script `pdf_to_images.py`.
</Note>

Las capacidades de visión de Claude ayudan a entender diseños y estructuras.

### Crea salidas intermedias verificables

Cuando Claude realiza tareas complejas y abiertas, puede cometer errores. El patrón "planificar-validar-ejecutar" detecta errores temprano haciendo que Claude primero cree un plan en un formato estructurado y luego valide ese plan con un script antes de ejecutarlo.

**Ejemplo**: Imagina pedirle a Claude que actualice 50 campos de formulario en un PDF con base en una hoja de cálculo. Sin validación, Claude podría referenciar campos inexistentes, crear valores en conflicto, omitir campos requeridos o aplicar actualizaciones incorrectamente.

**Solución**: Usa el patrón de flujo de trabajo mostrado arriba (llenado de formularios PDF), pero agrega un archivo intermedio `changes.json` que se valide antes de aplicar los cambios. El flujo de trabajo queda así: analizar → **crear archivo de plan** → **validar el plan** → ejecutar → verificar.

**Por qué funciona este patrón:**

* **Detecta errores temprano**: La validación encuentra problemas antes de aplicar los cambios
* **Verificable por máquina**: Los scripts proporcionan una verificación objetiva
* **Planificación reversible**: Claude puede iterar sobre el plan sin tocar los originales
* **Depuración clara**: Los mensajes de error apuntan a problemas específicos

**Cuándo usarlo**: Operaciones por lotes, cambios destructivos, reglas de validación complejas, operaciones de alto riesgo.

**Consejo de implementación**: Haz que los scripts de validación sean detallados, con mensajes de error específicos como "Field 'signature\_date' not found. Available fields: customer\_name, order\_total, signature\_date\_signed" para ayudar a Claude a corregir los problemas.

### Dependencias de paquetes

Las Skills se ejecutan en el entorno de ejecución de código con limitaciones específicas de cada plataforma:

* **claude.ai**: Puede instalar paquetes desde npm y PyPI y descargar desde repositorios de GitHub
* **Anthropic API**: No tiene acceso a la red ni instalación de paquetes en tiempo de ejecución

Lista los paquetes requeridos en tu SKILL.md y verifica que estén disponibles en la [documentación de la herramienta de ejecución de código](/en/docs/agents-and-tools/tool-use/code-execution-tool).

### Entorno de ejecución

Las Skills se ejecutan en un entorno de ejecución de código con acceso al sistema de archivos, comandos bash y capacidades de ejecución de código. Para la explicación conceptual de esta arquitectura, consulta [La arquitectura de las Skills](/en/docs/agents-and-tools/agent-skills/overview#the-skills-architecture) en la descripción general.

**Cómo afecta esto tu autoría:**

**Cómo accede Claude a las Skills:**

1. **Metadatos precargados**: Al inicio, el nombre y la descripción del frontmatter YAML de todas las Skills se cargan en el system prompt
2. **Archivos leídos bajo demanda**: Claude usa herramientas bash Read para acceder a SKILL.md y otros archivos del sistema de archivos cuando los necesita
3. **Scripts ejecutados de forma eficiente**: Los scripts utilitarios se pueden ejecutar vía bash sin cargar su contenido completo en el contexto. Solo la salida del script consume tokens
4. **Sin penalización de contexto para archivos grandes**: Los archivos de referencia, datos o documentación no consumen tokens de contexto hasta que realmente se leen

* **Las rutas de archivos importan**: Claude navega el directorio de tu skill como un sistema de archivos. Usa barras diagonales (`reference/guide.md`), no barras invertidas
* **Nombra los archivos de forma descriptiva**: Usa nombres que indiquen el contenido: `form_validation_rules.md`, no `doc2.md`
* **Organiza para facilitar el descubrimiento**: Estructura los directorios por dominio o funcionalidad
  * Bien: `reference/finance.md`, `reference/sales.md`
  * Mal: `docs/file1.md`, `docs/file2.md`
* **Empaqueta recursos completos**: Incluye documentación completa de API, ejemplos extensos, datasets grandes; no hay penalización de contexto hasta que se accede a ellos
* **Prefiere scripts para operaciones deterministas**: Escribe `validate_form.py` en lugar de pedirle a Claude que genere código de validación
* **Deja clara la intención de ejecución**:
  * "Ejecuta `analyze_form.py` para extraer los campos" (ejecutar)
  * "Consulta `analyze_form.py` para ver el algoritmo de extracción" (leer como referencia)
* **Prueba los patrones de acceso a archivos**: Verifica que Claude pueda navegar tu estructura de directorios probando con solicitudes reales

**Ejemplo:**

```
bigquery-skill/
├── SKILL.md (overview, points to reference files)
└── reference/
    ├── finance.md (revenue metrics)
    ├── sales.md (pipeline data)
    └── product.md (usage analytics)
```

Cuando el usuario pregunta por ingresos, Claude lee SKILL.md, ve la referencia a `reference/finance.md` e invoca bash para leer solo ese archivo. Los archivos sales.md y product.md permanecen en el sistema de archivos, consumiendo cero tokens de contexto hasta que se necesiten. Este modelo basado en el sistema de archivos es lo que habilita la divulgación progresiva. Claude puede navegar y cargar selectivamente exactamente lo que requiere cada tarea.

Para detalles completos sobre la arquitectura técnica, consulta [Cómo funcionan las Skills](/en/docs/agents-and-tools/agent-skills/overview#how-skills-work) en la descripción general de Skills.

### Referencias a herramientas MCP

Si tu Skill usa herramientas MCP (Model Context Protocol), usa siempre nombres de herramienta completamente calificados para evitar errores de "tool not found".

**Formato**: `ServerName:tool_name`

**Ejemplo**:

```markdown  theme={null}
Usa la herramienta BigQuery:bigquery_schema para obtener los esquemas de las tablas.
Usa la herramienta GitHub:create_issue para crear issues.
```

Donde:

* `BigQuery` y `GitHub` son nombres de servidores MCP
* `bigquery_schema` y `create_issue` son los nombres de las herramientas dentro de esos servidores

Sin el prefijo del servidor, Claude podría no encontrar la herramienta, especialmente cuando hay varios servidores MCP disponibles.

### Evita asumir que las herramientas están instaladas

No asumas que los paquetes están disponibles:

````markdown  theme={null}
**Mal ejemplo: Asume la instalación**:
"Usa la librería de pdf para procesar el archivo."

**Buen ejemplo: Explícito sobre las dependencias**:
"Instala el paquete requerido: `pip install pypdf`

Luego úsalo:
```python
from pypdf import PdfReader
reader = PdfReader("file.pdf")
```"
````

## Notas técnicas

### Requisitos del frontmatter YAML

El frontmatter de SKILL.md requiere los campos `name` (máximo 64 caracteres) y `description` (máximo 1024 caracteres). Consulta la [descripción general de Skills](/en/docs/agents-and-tools/agent-skills/overview#skill-structure) para detalles completos de la estructura.

### Presupuestos de tokens

Mantén el cuerpo de SKILL.md por debajo de 500 líneas para un rendimiento óptimo. Si tu contenido supera esto, divídelo en archivos separados usando los patrones de divulgación progresiva descritos antes. Para detalles de arquitectura, consulta la [descripción general de Skills](/en/docs/agents-and-tools/agent-skills/overview#how-skills-work).

## Checklist para Skills efectivas

Antes de compartir una Skill, verifica:

### Calidad fundamental

* [ ] La descripción es específica e incluye términos clave
* [ ] La descripción incluye tanto lo que hace la Skill como cuándo usarla
* [ ] El cuerpo de SKILL.md tiene menos de 500 líneas
* [ ] Los detalles adicionales están en archivos separados (si hace falta)
* [ ] No hay información sensible al tiempo (o está en la sección de "patrones antiguos")
* [ ] Terminología consistente en todo el documento
* [ ] Los ejemplos son concretos, no abstractos
* [ ] Las referencias a archivos tienen un solo nivel de profundidad
* [ ] La divulgación progresiva se usa de forma apropiada
* [ ] Los flujos de trabajo tienen pasos claros

### Código y scripts

* [ ] Los scripts resuelven problemas en lugar de delegárselos a Claude
* [ ] El manejo de errores es explícito y útil
* [ ] No hay "constantes vudú" (todos los valores están justificados)
* [ ] Los paquetes requeridos están listados en las instrucciones y verificados como disponibles
* [ ] Los scripts tienen documentación clara
* [ ] No hay rutas estilo Windows (todas con barras diagonales)
* [ ] Pasos de validación/verificación para operaciones críticas
* [ ] Ciclos de retroalimentación incluidos para tareas donde la calidad es crítica

### Pruebas

* [ ] Al menos tres evaluaciones creadas
* [ ] Probada con Haiku, Sonnet y Opus
* [ ] Probada con escenarios de uso reales
* [ ] Retroalimentación del equipo incorporada (si aplica)

## Próximos pasos

<CardGroup cols={2}>
  <Card title="Comienza con Agent Skills" icon="rocket" href="/en/docs/agents-and-tools/agent-skills/quickstart">
    Crea tu primera Skill
  </Card>

  <Card title="Usa Skills en Claude Code" icon="terminal" href="/en/docs/claude-code/skills">
    Crea y administra Skills en Claude Code
  </Card>

  <Card title="Usa Skills con la API" icon="code" href="/en/api/skills-guide">
    Sube y usa Skills de forma programática
  </Card>
</CardGroup>
