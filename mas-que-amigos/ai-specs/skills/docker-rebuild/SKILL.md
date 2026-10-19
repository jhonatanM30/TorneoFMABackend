---
name: docker-rebuild
description: Reconstruye y reinicia la imagen Docker del backend con el código actual, una vez el flujo de desarrollo (tests, verificación de BD, pruebas manuales con curl) ya fue confirmado por el usuario. Se activa solo cuando el usuario pide explícitamente actualizar/reconstruir el contenedor Docker con una funcionalidad ya validada — nunca de forma automática al terminar `/opsx:apply`.
---

# Reconstruir la imagen Docker del backend

## Precondición
Este skill asume que ya se completaron los pasos obligatorios de
`docs/openspec-tasks-mandatory-steps.md` (tests unitarios en verde,
verificación de BD, pruebas manuales con curl) **y que el usuario dio
confirmación explícita** de que quiere llevar ese código probado al
contenedor Docker. No se activa solo porque `/opsx:apply` haya terminado.

## Estructura de carpetas (importante)
El `docker-compose.yml` y el `.env` NO viven en este repo — viven un nivel
arriba, en la carpeta padre (`C:\FICOHSA\RutaAprendizaje\mas-que-amigos`),
que es un repositorio git aparte. Este repo del backend es un subdirectorio
de esa carpeta, y el compose lo referencia como `context: ./mas-que-amigos`.
Por eso, los comandos `docker compose` deben ejecutarse desde la carpeta
padre, no desde la raíz de este repo.

## Pasos
1. Confirmar con el usuario en qué branch/commit está parado el código que
   se va a reconstruir (`git log -1 --oneline`), para que sepa exactamente
   qué va a quedar corriendo en el contenedor.
2. Verificar si hay un proceso local (`mvnw spring-boot:run` u otro) usando
   el mismo puerto que expone el compose (`SERVER_PORT`, default 57075 en
   `.env.example`) — si lo hay, avisar al usuario para detenerlo antes de
   continuar (evita conflicto de puerto).
3. Ir a la carpeta padre y reconstruir solo el servicio `backend`:
   ```bash
   cd "C:\FICOHSA\RutaAprendizaje\mas-que-amigos"
   docker compose build backend
   docker compose up -d backend
   ```
4. NO tocar el volumen `uploads_data` ni el contenedor externo de MySQL —
   este compose no los gestiona, y no hay razón para recrearlos.
5. Verificar que el contenedor quedó arriba y con el endpoint nuevo:
   ```bash
   docker compose ps
   curl.exe http://localhost:<SERVER_PORT>/v3/api-docs | Select-String "<término del endpoint>"
   ```
6. Reportar al usuario: qué commit quedó corriendo, en qué puerto, y si el
   endpoint nuevo aparece confirmado en el OpenAPI del contenedor.

## Qué NO hacer
- No reconstruir automáticamente solo porque terminó `/opsx:apply` — solo
  cuando el usuario lo pida en estos términos.
- No agregar un servicio `mysql` al compose salvo que el usuario indique
  explícitamente que ya no tiene un MySQL externo corriendo.
- No modificar `.env` sin mostrarle antes el diff al usuario.
