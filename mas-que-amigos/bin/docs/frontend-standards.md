---
description: Referencia rápida del frontend de Más que Amigos — ubicación del repositorio, stack y cómo se conecta con este backend.
globs: []
alwaysApply: false
---

## Descripción general

El frontend de **Más que Amigos** no vive en este repositorio. Está en un
repositorio aparte, **`MasQueAmigos-Torneo`**, y consume esta API vía
HTTP/JSON. Este archivo es solo un puntero: los estándares detallados de
frontend (estructura de carpetas, convenciones de JS, etc.) deben vivir en
ese otro repositorio, no aquí.

## Stack tecnológico

- HTML, CSS y JavaScript planos, **sin framework ni build tool** (no hay
  React, Vue, webpack, Vite como bundler, etc.).
- Servido en desarrollo con Live Server o Vite en modo servidor estático
  (de ahí los orígenes `http://127.0.0.1:5500`, `http://localhost:5500` y
  `http://localhost:5173` permitidos en `CorsConfig` — ver
  `docs/backend-standards.md`, sección CORS).

## Relación con este backend

- El frontend consume los endpoints documentados en `docs/api-spec.yml` y
  expuestos en `/swagger-ui.html`.
- Las imágenes subidas (escudos, fotos de jugadores, galería) se sirven
  desde `/uploads/**`, con la URL pública armada a partir de
  `app.base-url` (ver `docs/backend-standards.md`, sección Subida de
  archivos).
- Los endpoints que requieren rol de Director Técnico esperan el header
  `X-Director-Tecnico-Key` (ver `docs/data-model.md` y
  `docs/api-spec.yml` para el detalle de qué operaciones lo exigen).

## Si necesitas trabajar en el frontend

Clona o abre el repositorio `MasQueAmigos-Torneo` por separado. Este
harness (Specboot/OpenSpec) está configurado sobre el repositorio del
backend; si en el futuro decides instalar un harness similar en el
repositorio del frontend, sus propios `docs/base-standards.md` y
`docs/frontend-standards.md` deberían vivir allá.
