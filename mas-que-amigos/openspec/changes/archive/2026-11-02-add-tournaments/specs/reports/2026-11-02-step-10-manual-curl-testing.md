# Reporte del Paso 10 - Testing manual de endpoints con curl

- Fecha: 2026-11-02
- Change: add-tournaments
- Agente: Claude (Sonnet 5.5)

## Entorno
- Backend local (`./mvnw spring-boot:run`) en el puerto **57077**, con el código del branch `feature/add-tournaments-backend`.
- Base de datos: **copia** `masqueamigos_t9` del MySQL del contenedor `TorneoContainer` (volcado de `masqueamigos`), no la base real: el contenedor Docker del backend sigue con el código anterior y fallaría al insertar partidos con `id_torneo` obligatorio. El arranque aplicó `V9__tournaments.sql` sobre la copia (Flyway 8 → 9).
- Sesión: login real como `ADMIN` con las credenciales de `.env` (no se muestran) y una cuenta `DT` temporal sin equipo (`dt-t9`). Los tokens duran 3 minutos, así que se pidieron nuevos antes de cada tanda; en este reporte los tokens aparecen como `<token>` y las contraseñas como `***`.
- Equipos de la copia: 1 Macalister, 2 Socios.com, 3 Los de la T, 4 MRL, 5 Real. Las respuestas largas de `equipos`/`partidos` se acortaron a 300 caracteres al registrarlas.

## Estado de la base de datos
- Baseline (antes de las pruebas, con `V9` aplicada): partido 2, equipos 5, jugador 29, alineacion 21, estadistica 2, player_transfer 7, app_user 3, tournament 1 (Histórico), tournament_team 0, Flyway 9.
- Después de la limpieza: idéntico al baseline (ver 10.10).

## Resumen de resultados
| Tarea | Resultado |
|---|---|
| 10.1 Servidor, sesión y baseline (Histórico presente, 0 partidos sin torneo) | OK |
| 10.2 GET públicos y 404 de torneo inexistente | OK |
| 10.3 Crear torneo: válido 201; formato ausente/inválido/`LEGACY`, nombre corto y repetido 400; sin token 401; `DT` 403 | OK |
| 10.4 Editar/eliminar: formato cambiado en borrador, borrado de borrador 204, Histórico protegido 400, 404 inexistente | OK |
| 10.5 Inscripción: válida, duplicada 400, equipo inexistente 400, mismo equipo en 2 torneos 201, retiro 204, retiro de no inscrito 404 | OK |
| 10.6 Iniciar: con 0 equipos 400, con 3 equipos 200, de nuevo 400, y luego inscribir/retirar/editar/eliminar 400 | OK |
| 10.7 Partidos: A vs B y reemplazo A vs C (200), equipo no inscrito 400, torneo en borrador 400, inexistente 400, reglas generales intactas, sin `idTorneo` cae en el Histórico, `DT` 403, edición ignora `idTorneo` y valida inscripción | OK |
| 10.8 Finalizar: con 2 partidos pendientes 400 (con el conteo), al eliminarlos 200, de nuevo 400, partido nuevo en torneo `FINISHED` 400 | OK |
| 10.10 Restauración | OK |

## Observaciones
- **Formato del 404**: el 404 de `NotFoundException` es `{"indicadorRespuesta":"Sin Registros","mensaje":"..."}`, no `{"error": ...}` como decían las specs. Se corrigieron specs, design y tasks al formato real (el que ya usan partidos y equipos) y se revalidó con `openspec validate --strict`; no hubo cambio de código. Las excepciones de jugador, traspaso y usuario sí responden `{"error": ...}`.
- **Orden de equipos**: `GET /api/tournaments/{id}/teams` devolvió `['Los de la T', 'Macalister', 'Socios.com']` (por nombre) y `GET /api/tournaments/{id}/matches` los partidos por fecha y hora.
- **Torneo `FINISHED` no se elimina por API** (el spec lo exige así): el torneo de prueba finalizado se borró con SQL en la limpieza, junto con sus inscripciones y la cuenta `dt-t9`.
- **E2E con Playwright: no aplica** en este change. El front vive en el repositorio aparte `MasQueAmigos-Torneo` y no se modifica aquí; el contrato queda cubierto por el prompt `docs/frontend-prompts/add-tournaments.md` (tarea 12.3).
- **Guías de pruebas (tarea 12.4)**: `GUIA_PRUEBAS.md` y `HERRAMIENTAS_TESTING.md` (carpeta padre) crean partidos sin `idTorneo`, lo que sigue funcionando (caen en el torneo Histórico), así que no se modificaron. Siguen mostrando escrituras sin token (ahora 401), pendiente ya conocido del change `add-authentication-roles` y fuera del alcance de este change.
- Algunas líneas de este reporte con `| python` imprimen solo fragmentos por el recorte a 300 caracteres de la respuesta; el código HTTP y el cuerpo completo de cada llamada están en las entradas de abajo.

## Comandos y respuestas

### Setup: crear cuenta DT temporal
```
curl -X POST http://localhost:57077/api/users -H 'Authorization: Bearer <token>' -d '{"username":"dt-t9","password":"***","role":"DT"}'
{"id":8,"username":"dt-t9","role":"DT","teamId":null,"teamName":null,"createdAt":"2026-11-02T18:45:40.727"}
HTTP 201
```

### 10.2 GET /api/tournaments (sin token)
```
curl -X GET http://localhost:57077/api/tournaments
[{"id":1,"name":"Histórico","format":"LEGACY","status":"IN_PROGRESS","teamCount":0,"createdAt":"2026-11-02T17:44:15","startedAt":null}]
HTTP 200
```

### 10.2 GET /api/tournaments/1 (Histórico)
```
curl -X GET http://localhost:57077/api/tournaments/1
{"id":1,"name":"Histórico","format":"LEGACY","status":"IN_PROGRESS","teamCount":0,"createdAt":"2026-11-02T17:44:15","startedAt":null}
HTTP 200
```

### 10.2 GET /api/tournaments/1/teams
```
curl -X GET http://localhost:57077/api/tournaments/1/teams
[]
HTTP 200
```

### 10.2 GET /api/tournaments/1/matches
```
curl -X GET http://localhost:57077/api/tournaments/1/matches
[{"id":1,"equipoLocal":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2026-09-10","hora":"09:00:00","golesLocal":2,"golesVisitante":0,"fase":"ELIMINACION_DIRECTA","idTorneo":1,"estado":"FINALIZADO","indicadorRespuesta":"Success"},{"id":7,"equipoLocal":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"},"equipoVisitante":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2026-09-29","hora":"19:48:00","golesLocal":1,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":1,"estado":"EN_CURSO","indicadorRespuesta":"Success"}]
HTTP 200
```

### 10.2 GET torneo inexistente
```
curl -X GET http://localhost:57077/api/tournaments/999
{"indicadorRespuesta":"Sin Registros","mensaje":"Fuera de juego! El torneo con Id 999 no existe."}
HTTP 404
```

### 10.2 GET equipos de torneo inexistente
```
curl -X GET http://localhost:57077/api/tournaments/999/teams
{"indicadorRespuesta":"Sin Registros","mensaje":"Fuera de juego! El torneo con Id 999 no existe."}
HTTP 404
```

### 10.2 GET partidos de torneo inexistente
```
curl -X GET http://localhost:57077/api/tournaments/999/matches
{"indicadorRespuesta":"Sin Registros","mensaje":"Fuera de juego! El torneo con Id 999 no existe."}
HTTP 404
```

### 10.3 POST crear torneo válido (ADMIN)
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Liga Prueba","format":"LEAGUE"}'
{"id":2,"name":"Liga Prueba","format":"LEAGUE","status":"DRAFT","teamCount":0,"createdAt":"2026-11-02T18:46:02.051","startedAt":null}
HTTP 201
```

### 10.3 POST formato ausente
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Sin Formato"}'
{"format":"El formato del torneo es obligatorio."}
HTTP 400
```

### 10.3 POST formato inválido
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Formato Malo","format":"ROUND_ROBIN"}'
{"indicadorRespuesta":"Error","mensaje":"Formato de torneo inválido: ROUND_ROBIN. Los formatos permitidos son LEAGUE, GROUPS_KNOCKOUT y KNOCKOUT."}
HTTP 400
```

### 10.3 POST formato LEGACY
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Legacy Malo","format":"LEGACY"}'
{"indicadorRespuesta":"Error","mensaje":"Formato de torneo inválido: LEGACY. Los formatos permitidos son LEAGUE, GROUPS_KNOCKOUT y KNOCKOUT."}
HTTP 400
```

### 10.3 POST nombre corto
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"ab","format":"LEAGUE"}'
{"name":"El nombre del torneo debe tener entre 3 y 100 caracteres."}
HTTP 400
```

### 10.3 POST nombre repetido con otras mayúsculas
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"LIGA prueba","format":"KNOCKOUT"}'
{"indicadorRespuesta":"Error","mensaje":"Ya existe un torneo con el nombre \"LIGA prueba\"."}
HTTP 400
```

### 10.3 POST sin token
```
curl -X POST http://localhost:57077/api/tournaments -d '{"name":"Sin Token","format":"LEAGUE"}'
{"indicadorRespuesta":"No autorizado","mensaje":"Debes iniciar sesión para realizar esta acción."}
HTTP 401
```

### 10.3 POST con token DT
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Con DT","format":"LEAGUE"}'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```

### 10.4 PUT cambiar formato en borrador
```
curl -X PUT http://localhost:57077/api/tournaments/2 -H 'Authorization: Bearer <token>' -d '{"name":"Liga Prueba","format":"KNOCKOUT"}'
{"id":2,"name":"Liga Prueba","format":"KNOCKOUT","status":"DRAFT","teamCount":0,"createdAt":"2026-11-02T18:46:02","startedAt":null}
HTTP 200
```

### 10.4 PUT con DT
```
curl -X PUT http://localhost:57077/api/tournaments/2 -H 'Authorization: Bearer <token>' -d '{"name":"Liga Prueba","format":"KNOCKOUT"}'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```

### 10.4 POST crear torneo para borrar
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Para Borrar","format":"GROUPS_KNOCKOUT"}'
{"id":3,"name":"Para Borrar","format":"GROUPS_KNOCKOUT","status":"DRAFT","teamCount":0,"createdAt":"2026-11-02T18:47:04.16","startedAt":null}
HTTP 201
```

### 10.4 DELETE borrador
```
curl -X DELETE http://localhost:57077/api/tournaments/3 -H 'Authorization: Bearer <token>'

HTTP 204
```

### 10.4 GET torneo borrado
```
curl -X GET http://localhost:57077/api/tournaments/3
{"indicadorRespuesta":"Sin Registros","mensaje":"Fuera de juego! El torneo con Id 3 no existe."}
HTTP 404
```

### 10.4 PUT Histórico
```
curl -X PUT http://localhost:57077/api/tournaments/1 -H 'Authorization: Bearer <token>' -d '{"name":"Otro","format":"LEAGUE"}'
{"indicadorRespuesta":"Error","mensaje":"El torneo Histórico es un torneo protegido: no se puede editar, eliminar, iniciar ni finalizar."}
HTTP 400
```

### 10.4 DELETE Histórico
```
curl -X DELETE http://localhost:57077/api/tournaments/1 -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"El torneo Histórico es un torneo protegido: no se puede editar, eliminar, iniciar ni finalizar."}
HTTP 400
```

### 10.4 POST start Histórico
```
curl -X POST http://localhost:57077/api/tournaments/1/start -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"El torneo Histórico es un torneo protegido: no se puede editar, eliminar, iniciar ni finalizar."}
HTTP 400
```

### 10.4 POST finish Histórico
```
curl -X POST http://localhost:57077/api/tournaments/1/finish -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"El torneo Histórico es un torneo protegido: no se puede editar, eliminar, iniciar ni finalizar."}
HTTP 400
```

### 10.5 POST inscribir en Histórico
```
curl -X POST http://localhost:57077/api/tournaments/1/teams -H 'Authorization: Bearer <token>' -d '{"teamId":1}'
{"indicadorRespuesta":"Error","mensaje":"El torneo Histórico no usa inscripción de equipos."}
HTTP 400
```

### 10.4 PUT torneo inexistente
```
curl -X PUT http://localhost:57077/api/tournaments/999 -H 'Authorization: Bearer <token>' -d '{"name":"Nada","format":"LEAGUE"}'
{"indicadorRespuesta":"Sin Registros","mensaje":"Fuera de juego! El torneo con Id 999 no existe."}
HTTP 404
```

### 10.4 DELETE torneo inexistente
```
curl -X DELETE http://localhost:57077/api/tournaments/999 -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Sin Registros","mensaje":"Fuera de juego! El torneo con Id 999 no existe."}
HTTP 404
```

### 10.5 inscribir equipo 1 en torneo 2
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":1}'
{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":13,"nombre":"Daniel","posicion":"DEFENSA","edad":35,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":14,"nombre":"Brayan","posicion":"DEFENSA","edad":24,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":16,"nombre":"Alex","posicion":"MEDIOCAMPISTA","edad":31,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":15,"nombre":"Duvan","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":11,"nombre":"James","posicion":"MEDIOCAMPISTA","edad":28,"dorsal":11,"imagenUrl":"http://localhost:57075/uploads/jugadores/c26d17af-6da2-4ba6-848a-f5fd39dd0620.png","costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":7,"nombre":"Jose","posicion":"PORTERO","edad":22,"dorsal":22,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":29,"nombre":"dilan","posicion":"DEFENSA","edad":23,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":35,"nombre":"ZZTest SinCosto","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### 10.5 inscribir equipo 2 en torneo 2
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":2}'
{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":6,"nombre":"Didier","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":5,"nombre":"Yeiler","posicion":"DEFENSA","edad":21,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":2,"nombre":"James Rodríguez","posicion":"MEDIOCAMPISTA","edad":33,"dorsal":10,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":10,"nombre":"Erick","posicion":"DELANTERO","edad":34,"dorsal":15,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":4,"nombre":"Jhonatan","posicion":"DEFENSA","edad":33,"dorsal":21,"imagenUrl":null,"costo":100000.00,"indicadorRespuesta":null,"mensaje":null},{"id":34,"nombre":"ZZTest Costo","posicion":"DELANTERO","edad":22,"dorsal":97,"imagenUrl":null,"costo":1500.50,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### 10.5 inscribir equipo 3 en torneo 2
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":3}'
{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":18,"nombre":"Yair","posicion":"DEFENSA","edad":21,"dorsal":3,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":19,"nombre":"Alan","posicion":"MEDIOCAMPISTA","edad":25,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":20,"nombre":"Brandon","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":22,"nombre":"Raul","posicion":"DELANTERO","edad":27,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":21,"nombre":"Cristian","posicion":"MEDIOCAMPISTA","edad":36,"dorsal":14,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":23,"nombre":"Jhonatan","posicion":"DELANTERO","edad":21,"dorsal":21,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":17,"nombre":"Alex","posicion":"PORTERO","edad":25,"dorsal":31,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### 10.5 inscripción duplicada
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":1}'
{"indicadorRespuesta":"Error","mensaje":"El equipo Macalister ya está inscrito en este torneo."}
HTTP 400
```

### 10.5 equipo inexistente
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":999}'
{"indicadorRespuesta":"Error","mensaje":"Fuera de juego! No se ha seleccionado un o unos equipos existentes."}
HTTP 400
```

### 10.5 sin teamId
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{}'
{"teamId":"El equipo a inscribir es obligatorio."}
HTTP 400
```

### 10.5 inscribir con DT
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":4}'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```

### 10.5 crear segundo torneo (Copa Prueba)
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Copa Prueba","format":"LEAGUE"}'
{"id":4,"name":"Copa Prueba","format":"LEAGUE","status":"DRAFT","teamCount":0,"createdAt":"2026-11-02T18:47:43.793","startedAt":null}
HTTP 201
```

### 10.5 mismo equipo 1 en otro torneo
```
curl -X POST http://localhost:57077/api/tournaments/4/teams -H 'Authorization: Bearer <token>' -d '{"teamId":1}'
{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":13,"nombre":"Daniel","posicion":"DEFENSA","edad":35,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":14,"nombre":"Brayan","posicion":"DEFENSA","edad":24,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":16,"nombre":"Alex","posicion":"MEDIOCAMPISTA","edad":31,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":15,"nombre":"Duvan","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":11,"nombre":"James","posicion":"MEDIOCAMPISTA","edad":28,"dorsal":11,"imagenUrl":"http://localhost:57075/uploads/jugadores/c26d17af-6da2-4ba6-848a-f5fd39dd0620.png","costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":7,"nombre":"Jose","posicion":"PORTERO","edad":22,"dorsal":22,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":29,"nombre":"dilan","posicion":"DEFENSA","edad":23,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":35,"nombre":"ZZTest SinCosto","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### 10.5 GET equipos torneo 2 (ordenados por nombre)
```
curl -X GET http://localhost:57077/api/tournaments/2/teams
[{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":18,"nombre":"Yair","posicion":"DEFENSA","edad":21,"dorsal":3,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":19,"nombre":"Alan","posicion":"MEDIOCAMPISTA","edad":25,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":20,"nombre":"Brandon","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":22,"nombre":"Raul","posicion":"DELANTERO","edad":27,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":21,"nombre":"Cristian","posicion":"MEDIOCAMPISTA","edad":36,"dorsal":14,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":23,"nombre":"Jhonatan","posicion":"DELANTERO","edad":21,"dorsal":21,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":17,"nombre":"Alex","posicion":"PORTERO","edad":25,"dorsal":31,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]},{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":13,"nombre":"Daniel","posicion":"DEFENSA","edad":35,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":14,"nombre":"Brayan","posicion":"DEFENSA","edad":24,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":16,"nombre":"Alex","posicion":"MEDIOCAMPISTA","edad":31,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":15,"nombre":"Duvan","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":11,"nombre":"James","posicion":"MEDIOCAMPISTA","edad":28,"dorsal":11,"imagenUrl":"http://localhost:57075/uploads/jugadores/c26d17af-6da2-4ba6-848a-f5fd39dd0620.png","costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":7,"nombre":"Jose","posicion":"PORTERO","edad":22,"dorsal":22,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":29,"nombre":"dilan","posicion":"DEFENSA","edad":23,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":35,"nombre":"ZZTest SinCosto","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]},{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":6,"nombre":"Didier","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":5,"nombre":"Yeiler","posicion":"DEFENSA","edad":21,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":2,"nombre":"James Rodríguez","posicion":"MEDIOCAMPISTA","edad":33,"dorsal":10,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":10,"nombre":"Erick","posicion":"DELANTERO","edad":34,"dorsal":15,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":4,"nombre":"Jhonatan","posicion":"DEFENSA","edad":33,"dorsal":21,"imagenUrl":null,"costo":100000.00,"indicadorRespuesta":null,"mensaje":null},{"id":34,"nombre":"ZZTest Costo","posicion":"DELANTERO","edad":22,"dorsal":97,"imagenUrl":null,"costo":1500.50,"indicadorRespuesta":null,"mensaje":null}]}]
HTTP 200
```

### 10.5 retirar equipo 3 de torneo 2
```
curl -X DELETE http://localhost:57077/api/tournaments/2/teams/3 -H 'Authorization: Bearer <token>'

HTTP 204
```

### 10.5 retirar equipo no inscrito (5)
```
curl -X DELETE http://localhost:57077/api/tournaments/2/teams/5 -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Sin Registros","mensaje":"Fuera de juego! El equipo no está inscrito en este torneo."}
HTTP 404
```

### 10.5 retirar equipo 1 del torneo 4
```
curl -X DELETE http://localhost:57077/api/tournaments/4/teams/1 -H 'Authorization: Bearer <token>'

HTTP 204
```

### 10.5 equipo 1 sigue en torneo 2
```
curl -X GET http://localhost:57077/api/tournaments/2/teams
[{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":13,"nombre":"Daniel","posicion":"DEFENSA","edad":35,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":14,"nombre":"Brayan","posicion":"DEFENSA","edad":24,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":16,"nombre":"Alex","posicion":"MEDIOCAMPISTA","edad":31,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":15,"nombre":"Duvan","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":11,"nombre":"James","posicion":"MEDIOCAMPISTA","edad":28,"dorsal":11,"imagenUrl":"http://localhost:57075/uploads/jugadores/c26d17af-6da2-4ba6-848a-f5fd39dd0620.png","costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":7,"nombre":"Jose","posicion":"PORTERO","edad":22,"dorsal":22,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":29,"nombre":"dilan","posicion":"DEFENSA","edad":23,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":35,"nombre":"ZZTest SinCosto","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]},{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":6,"nombre":"Didier","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":5,"nombre":"Yeiler","posicion":"DEFENSA","edad":21,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":2,"nombre":"James Rodríguez","posicion":"MEDIOCAMPISTA","edad":33,"dorsal":10,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":10,"nombre":"Erick","posicion":"DELANTERO","edad":34,"dorsal":15,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":4,"nombre":"Jhonatan","posicion":"DEFENSA","edad":33,"dorsal":21,"imagenUrl":null,"costo":100000.00,"indicadorRespuesta":null,"mensaje":null},{"id":34,"nombre":"ZZTest Costo","posicion":"DELANTERO","edad":22,"dorsal":97,"imagenUrl":null,"costo":1500.50,"indicadorRespuesta":null,"mensaje":null}]}]
HTTP 200
```

### 10.5 re-inscribir equipo 3
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":3}'
{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":18,"nombre":"Yair","posicion":"DEFENSA","edad":21,"dorsal":3,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":19,"nombre":"Alan","posicion":"MEDIOCAMPISTA","edad":25,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":20,"nombre":"Brandon","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":22,"nombre":"Raul","posicion":"DELANTERO","edad":27,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":21,"nombre":"Cristian","posicion":"MEDIOCAMPISTA","edad":36,"dorsal":14,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":23,"nombre":"Jhonatan","posicion":"DELANTERO","edad":21,"dorsal":21,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":17,"nombre":"Alex","posicion":"PORTERO","edad":25,"dorsal":31,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### 10.6 start con 0 equipos (torneo 4)
```
curl -X POST http://localhost:57077/api/tournaments/4/start -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"Para iniciar el torneo se necesitan al menos 2 equipos inscritos (hay 0)."}
HTTP 400
```

### 10.6 start con 3 equipos (torneo 2)
```
curl -X POST http://localhost:57077/api/tournaments/2/start -H 'Authorization: Bearer <token>'
{"id":2,"name":"Liga Prueba","format":"KNOCKOUT","status":"IN_PROGRESS","teamCount":3,"createdAt":"2026-11-02T18:46:02","startedAt":"2026-11-02T18:47:55.534"}
HTTP 200
```

### 10.6 start de nuevo
```
curl -X POST http://localhost:57077/api/tournaments/2/start -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"Solo se puede iniciar un torneo en borrador (estado actual: IN_PROGRESS)."}
HTTP 400
```

### 10.6 inscribir tras iniciar
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":4}'
{"indicadorRespuesta":"Error","mensaje":"El torneo ya inició: solo se puede hacer esto mientras está en borrador."}
HTTP 400
```

### 10.6 retirar tras iniciar
```
curl -X DELETE http://localhost:57077/api/tournaments/2/teams/3 -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"El torneo ya inició: solo se puede hacer esto mientras está en borrador."}
HTTP 400
```

### 10.6 PUT tras iniciar
```
curl -X PUT http://localhost:57077/api/tournaments/2 -H 'Authorization: Bearer <token>' -d '{"name":"Otro Nombre","format":"LEAGUE"}'
{"indicadorRespuesta":"Error","mensaje":"El torneo ya inició: solo se puede hacer esto mientras está en borrador."}
HTTP 400
```

### 10.6 DELETE tras iniciar
```
curl -X DELETE http://localhost:57077/api/tournaments/2 -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"El torneo ya inició: solo se puede hacer esto mientras está en borrador."}
HTTP 400
```

### 10.7 partido A vs B en torneo 2
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":1,"idEquipoVisitante":2,"fecha":"2027-03-01","hora":"15:00:00","fase":"FASE_DE_GRUPOS","idTorneo":2}'
{"id":8,"equipoLocal":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"},"equipoVisitante":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-03-01","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":2,"estado":"PROGRAMADO","indicadorRespuesta":"Success","mensaje":"Equipos, el partido ya fue programado"}
HTTP 200
```

### 10.7 partido de reemplazo A vs C (manual)
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":1,"idEquipoVisitante":3,"fecha":"2027-03-08","hora":"15:00:00","fase":"FASE_DE_GRUPOS","idTorneo":2}'
{"id":9,"equipoLocal":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"},"equipoVisitante":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"},"fecha":"2027-03-08","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":2,"estado":"PROGRAMADO","indicadorRespuesta":"Success","mensaje":"Equipos, el partido ya fue programado"}
HTTP 200
```

### 10.7 equipo no inscrito (4 vs 1) en torneo 2
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":4,"idEquipoVisitante":1,"fecha":"2027-03-15","hora":"15:00:00","fase":"FASE_DE_GRUPOS","idTorneo":2}'
{"indicadorRespuesta":"Error","mensaje":"El equipo MRL no está inscrito en el torneo Liga Prueba."}
HTTP 400
```

### 10.7 torneo en borrador (id 4)
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":1,"idEquipoVisitante":2,"fecha":"2027-03-15","hora":"15:00:00","fase":"FASE_DE_GRUPOS","idTorneo":4}'
{"indicadorRespuesta":"Error","mensaje":"El torneo no está en curso: solo se pueden crear partidos en un torneo en curso (estado actual: DRAFT)."}
HTTP 400
```

### 10.7 torneo inexistente
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":1,"idEquipoVisitante":2,"fecha":"2027-03-15","hora":"15:00:00","fase":"FASE_DE_GRUPOS","idTorneo":999}'
{"indicadorRespuesta":"Error","mensaje":"Fuera de juego! El torneo indicado no existe."}
HTTP 400
```

### 10.7 mismo equipo local y visitante
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":1,"idEquipoVisitante":1,"fecha":"2027-03-15","hora":"15:00:00","fase":"FASE_DE_GRUPOS","idTorneo":2}'
{"indicadorRespuesta":"Error","mensaje":"Los equipos seleccionados son los mismos. Un equipo no puede jugar contra sí mismo."}
HTTP 400
```

### 10.7 fecha ocupada para el equipo 1
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":1,"idEquipoVisitante":3,"fecha":"2027-03-01","hora":"15:00:00","fase":"FASE_DE_GRUPOS","idTorneo":2}'
{"indicadorRespuesta":"Error","mensaje":"Ya existe un partido programado el dia 2027-03-01 para uno de estos equipos"}
HTTP 400
```

### 10.7 sin idTorneo (cae en Histórico)
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":4,"idEquipoVisitante":5,"fecha":"2027-03-15","hora":"15:00:00","fase":"FASE_DE_GRUPOS"}'
{"id":10,"equipoLocal":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-03-15","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":1,"estado":"PROGRAMADO","indicadorRespuesta":"Success","mensaje":"Equipos, el partido ya fue programado"}
HTTP 200
```

### 10.7 partido con token DT
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":1,"idEquipoVisitante":2,"fecha":"2027-04-01","hora":"15:00:00","fase":"FASE_DE_GRUPOS","idTorneo":2}'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```

### 10.7 editar hora enviando idTorneo=1 (se ignora)
```
curl -X PUT http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"id":8,"idEquipoLocal":1,"idEquipoVisitante":2,"fecha":"2027-03-01","hora":"16:00:00","fase":"FASE_DE_GRUPOS","idTorneo":1}'
{"id":8,"equipoLocal":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"},"equipoVisitante":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-03-01","hora":"16:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":2,"estado":"PROGRAMADO","indicadorRespuesta":"Success","mensaje":"Partido actualizado correctamente."}
HTTP 200
```

### 10.7 editar reemplazando equipo por uno no inscrito (4)
```
curl -X PUT http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"id":8,"idEquipoLocal":1,"idEquipoVisitante":4,"fecha":"2027-03-01","hora":"16:00:00","fase":"FASE_DE_GRUPOS"}'
{"indicadorRespuesta":"Error","mensaje":"El equipo MRL no está inscrito en el torneo Liga Prueba."}
HTTP 400
```

### 10.7 editar reemplazando equipo 2 por inscrito 3 (fecha libre)
```
curl -X PUT http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"id":8,"idEquipoLocal":1,"idEquipoVisitante":3,"fecha":"2027-03-15","hora":"16:00:00","fase":"FASE_DE_GRUPOS"}'
{"id":8,"equipoLocal":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"},"equipoVisitante":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"},"fecha":"2027-03-15","hora":"16:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":2,"estado":"PROGRAMADO","indicadorRespuesta":"Success","mensaje":"Partido actualizado correctamente."}
HTTP 200
```

### 10.8 finalizar con partidos pendientes
```
curl -X POST http://localhost:57077/api/tournaments/2/finish -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"No se puede finalizar el torneo: faltan por finalizar 2 partido(s). Juégalos o elimínalos primero."}
HTTP 400
```

### 10.8 finalizar torneo en borrador (4)
```
curl -X POST http://localhost:57077/api/tournaments/4/finish -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"El torneo no está en curso: solo se puede finalizar un torneo en curso (estado actual: DRAFT)."}
HTTP 400
```

### 10.8 eliminar partido 8
```
curl -X DELETE http://localhost:57077/api/partidos/8 -H 'Authorization: Bearer <token>'

HTTP 204
```

### 10.8 eliminar partido 9
```
curl -X DELETE http://localhost:57077/api/partidos/9 -H 'Authorization: Bearer <token>'

HTTP 204
```

### 10.8 finalizar sin partidos pendientes
```
curl -X POST http://localhost:57077/api/tournaments/2/finish -H 'Authorization: Bearer <token>'
{"id":2,"name":"Liga Prueba","format":"KNOCKOUT","status":"FINISHED","teamCount":3,"createdAt":"2026-11-02T18:46:02","startedAt":"2026-11-02T18:47:56"}
HTTP 200
```

### 10.8 finalizar de nuevo
```
curl -X POST http://localhost:57077/api/tournaments/2/finish -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"El torneo no está en curso: solo se puede finalizar un torneo en curso (estado actual: FINISHED)."}
HTTP 400
```

### 10.8 crear partido en torneo FINISHED
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":1,"idEquipoVisitante":2,"fecha":"2027-05-01","hora":"15:00:00","fase":"FASE_DE_GRUPOS","idTorneo":2}'
{"indicadorRespuesta":"Error","mensaje":"El torneo no está en curso: solo se pueden crear partidos en un torneo en curso (estado actual: FINISHED)."}
HTTP 400
```

### 10.10 limpieza: eliminar partido 10 (Histórico)
```
curl -X DELETE http://localhost:57077/api/partidos/10 -H 'Authorization: Bearer <token>'

HTTP 204
```

### 10.10 limpieza: eliminar torneo 4 (borrador, con 0 inscritos)
```
curl -X DELETE http://localhost:57077/api/tournaments/4 -H 'Authorization: Bearer <token>'

HTTP 204
```

