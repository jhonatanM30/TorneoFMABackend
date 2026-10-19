---
name: sync-agent-symlinks
description: Analiza y sincroniza la exposición de skills a los agentes después de cambios en las skills de ai-specs (adiciones, eliminaciones, renombres). Úsala cuando se agreguen/eliminen skills en ai-specs y .claude/skills y .cursor/skills deban mantenerse alineadas mediante symlinks.
author: LIDR.co
version: 1.0.0
---

# Skill sync-agent-symlinks

Mantén las estructuras de skills expuestas a los agentes sincronizadas con `ai-specs/skills` como fuente canónica.

Usa esta skill después de cualquier cambio en `ai-specs/skills` (skill nueva, skill eliminada, skill renombrada, skill movida), especialmente cuando necesites evitar symlinks obsoletos o rotos.

## Alcance y reglas de seguridad

- La fuente canónica es `ai-specs/skills`.
- Los destinos espejo son:
  - `.claude/skills`
  - `.cursor/skills`
- Gestiona solo las entradas que sean symlinks a `../../ai-specs/skills/<skill-name>`.
- No elimines directorios que no sean symlinks en los destinos espejo a menos que el usuario lo pida explícitamente.
- Nunca sobrescribas automáticamente un directorio real; repórtalo como conflicto.

## Flujo de trabajo

### Paso 1 - Construye los inventarios

Recopila tres inventarios:

1. Skills canónicas de `ai-specs/skills/*/SKILL.md`
2. Entradas espejo en `.claude/skills`
3. Entradas espejo en `.cursor/skills`

A partir de las entradas espejo, clasifica:
- `linked`: symlink válido que apunta a una skill canónica existente
- `broken`: falta el destino del symlink
- `orphan`: el symlink apunta al namespace canónico pero la skill ya no existe
- `conflict`: entrada que no es symlink con el mismo nombre que una skill canónica
- `external`: entrada no gestionada por la política de symlinks canónicos (déjala sin cambios)

### Paso 2 - Calcula el plan de sincronización

Para cada destino espejo:

- `to_add`: skills canónicas que faltan en el destino espejo
- `to_fix`: symlinks canónicos rotos que deben recrearse
- `to_remove`: symlinks canónicos huérfanos sin fuente canónica
- `to_skip`: conflictos y entradas externas (solo reportar)

### Paso 3 - Aplica la sincronización de forma segura

Aplica los cambios en este orden:

1. Agrega los symlinks faltantes:
   - `<mirror>/<skill-name> -> ../../ai-specs/skills/<skill-name>`
2. Corrige los symlinks canónicos rotos:
   - Elimina el enlace roto y recrea el mismo enlace canónico
3. Elimina los symlinks canónicos huérfanos:
   - Elimina el symlink solo si apunta al namespace canónico y la skill ya no existe

Nunca elimines:
- directorios que no sean symlinks
- archivos que no estén bajo la política de symlinks canónicos

### Paso 4 - Verifica la integridad

Después de los cambios:

- Confirma que cada skill canónica exista en ambos espejos como un symlink válido, o que esté listada explícitamente como conflicto.
- Confirma que no queden symlinks canónicos rotos.
- Confirma que las entradas externas permanezcan intactas.

### Paso 5 - Reporta los resultados

Devuelve un reporte de sincronización conciso:

- Cantidad de skills canónicas
- Por destino espejo:
  - agregados
  - corregidos
  - eliminados
  - conflictos
  - entradas externas omitidas
- Bloqueos restantes (si los hay)

## Escenarios de agregar/eliminar

### Escenario A - Se agregó una skill nueva en ai-specs

Comportamiento esperado:
- Agrega el symlink faltante en `.claude/skills`
- Agrega el symlink faltante en `.cursor/skills`
- Verifica que ambos enlaces resuelvan a la carpeta canónica

### Escenario B - Se eliminó una skill de ai-specs

Comportamiento esperado:
- Elimina el symlink canónico huérfano de `.claude/skills`
- Elimina el symlink canónico huérfano de `.cursor/skills`
- Mantén intactos los directorios no canónicos y repórtalos

## Patrones de comandos (referencia)

Usa comandos equivalentes para tu entorno:

```bash
# list canonical skill directories (names with SKILL.md)
ls ai-specs/skills

# inspect mirror entries with link metadata
ls -la .claude/skills
ls -la .cursor/skills

# add canonical link
ln -s ../../ai-specs/skills/<skill-name> .claude/skills/<skill-name>
ln -s ../../ai-specs/skills/<skill-name> .cursor/skills/<skill-name>

# remove orphan canonical link
rm .claude/skills/<skill-name>
rm .cursor/skills/<skill-name>
```

## Señales de alerta

Nunca:
- trates `ai-specs` como no canónica
- elimines automáticamente directorios reales en los destinos espejo
- dejes symlinks canónicos rotos después de sincronizar
- omitas conflictos en silencio sin reportarlos

Siempre:
- analiza antes de cambiar
- aplica cambios mínimos y seguros
- conserva las entradas no canónicas
- entrega un reporte final de sincronización con los bloqueos
