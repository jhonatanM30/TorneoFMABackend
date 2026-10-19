# Principios de persuasión para el diseño de skills

## Resumen

Los LLMs responden a los mismos principios de persuasión que los humanos. Entender esta psicología te ayuda a diseñar skills más efectivas: no para manipular, sino para asegurar que las prácticas críticas se sigan incluso bajo presión.

**Base de investigación:** Meincke et al. (2025) probaron 7 principios de persuasión con N=28,000 conversaciones con IA. Las técnicas de persuasión duplicaron con creces las tasas de cumplimiento (33% → 72%, p < .001).

## Los siete principios

### 1. Autoridad
**Qué es:** Deferencia hacia la experiencia, las credenciales o las fuentes oficiales.

**Cómo funciona en las skills:**
- Lenguaje imperativo: "DEBES", "Nunca", "Siempre"
- Enfoque no negociable: "Sin excepciones"
- Elimina la fatiga de decisión y la racionalización

**Cuándo usarlo:**
- Skills que imponen disciplina (TDD, requisitos de verificación)
- Prácticas críticas para la seguridad
- Mejores prácticas establecidas

**Ejemplo:**
```markdown
✅ ¿Escribiste código antes de la prueba? Bórralo. Empieza de nuevo. Sin excepciones.
❌ Considera escribir las pruebas primero cuando sea factible.
```

### 2. Compromiso
**Qué es:** Coherencia con acciones, afirmaciones o declaraciones públicas previas.

**Cómo funciona en las skills:**
- Exige anuncios: "Anuncia el uso de la skill"
- Fuerza elecciones explícitas: "Elige A, B o C"
- Usa seguimiento: TodoWrite para checklists

**Cuándo usarlo:**
- Para asegurar que las skills realmente se sigan
- Procesos de varios pasos
- Mecanismos de rendición de cuentas

**Ejemplo:**
```markdown
✅ Cuando encuentres una skill, DEBES anunciar: "Estoy usando [Skill Name]"
❌ Considera avisarle a tu compañero qué skill estás usando.
```

### 3. Escasez
**Qué es:** Urgencia por límites de tiempo o disponibilidad limitada.

**Cómo funciona en las skills:**
- Requisitos con límite de tiempo: "Antes de continuar"
- Dependencias secuenciales: "Inmediatamente después de X"
- Previene la procrastinación

**Cuándo usarlo:**
- Requisitos de verificación inmediata
- Flujos de trabajo sensibles al tiempo
- Para prevenir el "lo haré después"

**Ejemplo:**
```markdown
✅ Después de completar una tarea, solicita INMEDIATAMENTE una revisión de código antes de continuar.
❌ Puedes revisar el código cuando te convenga.
```

### 4. Prueba social
**Qué es:** Conformidad con lo que hacen los demás o con lo que se considera normal.

**Cómo funciona en las skills:**
- Patrones universales: "Cada vez", "Siempre"
- Modos de falla: "X sin Y = falla"
- Establece normas

**Cuándo usarlo:**
- Documentar prácticas universales
- Advertir sobre fallas comunes
- Reforzar estándares

**Ejemplo:**
```markdown
✅ Checklists sin seguimiento con TodoWrite = se saltan pasos. Cada vez.
❌ A algunas personas les resulta útil TodoWrite para los checklists.
```

### 5. Unidad
**Qué es:** Identidad compartida, sentido de "nosotros", pertenencia al grupo.

**Cómo funciona en las skills:**
- Lenguaje colaborativo: "nuestro codebase", "somos colegas"
- Objetivos compartidos: "ambos queremos calidad"

**Cuándo usarlo:**
- Flujos de trabajo colaborativos
- Establecer cultura de equipo
- Prácticas no jerárquicas

**Ejemplo:**
```markdown
✅ Somos colegas trabajando juntos. Necesito tu criterio técnico honesto.
❌ Probablemente deberías decirme si me equivoco.
```

### 6. Reciprocidad
**Qué es:** Obligación de devolver los beneficios recibidos.

**Cómo funciona:**
- Úsalo con moderación: puede sentirse manipulador
- Rara vez se necesita en las skills

**Cuándo evitarlo:**
- Casi siempre (otros principios son más efectivos)

### 7. Simpatía
**Qué es:** Preferencia por cooperar con quienes nos agradan.

**Cómo funciona:**
- **NO LO USES para lograr cumplimiento**
- Entra en conflicto con una cultura de retroalimentación honesta
- Genera adulación (sycophancy)

**Cuándo evitarlo:**
- Siempre, para imponer disciplina

## Combinaciones de principios por tipo de skill

| Tipo de skill | Usar | Evitar |
|------------|-----|-------|
| Impone disciplina | Autoridad + Compromiso + Prueba social | Simpatía, Reciprocidad |
| Guía/técnica | Autoridad moderada + Unidad | Autoridad fuerte |
| Colaborativa | Unidad + Compromiso | Autoridad, Simpatía |
| Referencia | Solo claridad | Toda persuasión |

## Por qué funciona: la psicología

**Las reglas de línea clara reducen la racionalización:**
- "DEBES" elimina la fatiga de decisión
- El lenguaje absoluto elimina las preguntas de "¿es esto una excepción?"
- Los contraargumentos explícitos contra la racionalización cierran resquicios específicos

**Las intenciones de implementación crean comportamiento automático:**
- Disparadores claros + acciones requeridas = ejecución automática
- "Cuando X, haz Y" es más efectivo que "en general, haz Y"
- Reduce la carga cognitiva del cumplimiento

**Los LLMs son parahumanos:**
- Entrenados con texto humano que contiene estos patrones
- El lenguaje de autoridad precede al cumplimiento en los datos de entrenamiento
- Las secuencias de compromiso (afirmación → acción) se modelan con frecuencia
- Los patrones de prueba social (todos hacen X) establecen normas

## Uso ético

**Legítimo:**
- Asegurar que se sigan las prácticas críticas
- Crear documentación efectiva
- Prevenir fallas predecibles

**Ilegítimo:**
- Manipular para beneficio personal
- Crear falsa urgencia
- Cumplimiento basado en la culpa

**La prueba:** ¿Serviría esta técnica a los intereses genuinos del usuario si la entendiera por completo?

## Citas de investigación

**Cialdini, R. B. (2021).** *Influence: The Psychology of Persuasion (New and Expanded).* Harper Business.
- Siete principios de persuasión
- Base empírica para la investigación sobre la influencia

**Meincke, L., Shapiro, D., Duckworth, A. L., Mollick, E., Mollick, L., & Cialdini, R. (2025).** Call Me A Jerk: Persuading AI to Comply with Objectionable Requests. University of Pennsylvania.
- Probaron 7 principios con N=28,000 conversaciones con LLMs
- El cumplimiento aumentó de 33% → 72% con técnicas de persuasión
- Autoridad, compromiso y escasez fueron los más efectivos
- Valida el modelo parahumano del comportamiento de los LLMs

## Referencia rápida

Al diseñar una skill, pregúntate:

1. **¿De qué tipo es?** (Disciplina vs. guía vs. referencia)
2. **¿Qué comportamiento estoy intentando cambiar?**
3. **¿Qué principio(s) aplican?** (Normalmente autoridad + compromiso para disciplina)
4. **¿Estoy combinando demasiados?** (No uses los siete)
5. **¿Es esto ético?** (¿Sirve a los intereses genuinos del usuario?)
