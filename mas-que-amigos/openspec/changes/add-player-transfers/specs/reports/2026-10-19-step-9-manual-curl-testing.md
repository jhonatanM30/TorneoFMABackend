# Reporte del Paso 9 - Testing manual de endpoints con curl

- Fecha: 2026-10-19
- Change: add-player-transfers
- Agente: Claude (Sonnet 5.5)
- Entorno: backend local (`./mvnw spring-boot:run`) en `http://localhost:57076` contra el MySQL de `TorneoContainer` (puerto 3308, esquema `masqueamigos`). Flyway aplicó `V7__player_cost_and_transfers.sql` al arrancar (versión 7, `success=1`; columna `jugador.costo` y tabla `player_transfer` creadas).
- Cabecera de autorización en las escrituras: `X-Director-Tecnico-Key` (la clave de desarrollo; no se imprime el script de arranque).

## Resumen
- Los 41 casos devolvieron el código HTTP esperado por las specs `player-cost` y `player-transfers`.
- Costo: crear con costo, sin costo (0), negativo (400), más de 2 decimales (400), editar sin costo conserva el valor (300.00).
- Solicitudes: 3 pendientes del mismo jugador a la vez (201); mismo equipo (400); jugador inexistente (404); equipo inexistente (400); cuerpo vacío (400); sin clave (401); dorsal 0 (400); dorsal ocupado (400) y dorsal nuevo libre (201).
- Snapshot: tras editar el costo del jugador a 2000, las solicitudes siguen con `cost` 1500.50.
- Rechazo manual: 200, repetir → 400, inexistente → 404.
- Confirmación: el jugador pasó al equipo 2; las demás solicitudes pendientes del jugador quedaron `REJECTED` (la bandeja `PENDING` del jugador quedó vacía); repetir/confirmar rechazada → 400; inexistente → 404; sin clave → 401.
- Dorsal ocupado al confirmar: 400, la solicitud sigue `PENDING` y el jugador no se movió.
- Consultas: `status` inválido → 400; jugador sin traspasos → `[]`; `GET` sin clave → 200; `POST` sin clave → 401.
- Observación: los textos con tildes de mensajes del backend se ven como `�` en esta salida porque el backend responde con el encoding que ya usaba antes de este change (p. ej. "Gooool! El jugador ... se guardó"); no es una regresión de este change.

## Restauración de la base de datos
- Baseline previo: jugador 27 · equipos 5 · alineacion 21 · estadistica 2 · player_transfer (tabla inexistente, V6).
- Se crearon 4 jugadores de prueba (ids 30-33) y 4 traspasos (ids 1-4). Se eliminaron con `DELETE FROM player_transfer WHERE player_id IN (30,31,32,33)` y `DELETE FROM jugador WHERE id_jugador IN (30,31,32,33)`.
- Estado posterior: jugador 27 · equipos 5 · alineacion 21 · estadistica 2 · player_transfer 0 · 0 jugadores con costo distinto de 0. Coincide con el baseline (la migración V7 queda aplicada, que es el cambio deseado). Los contadores AUTO_INCREMENT no se revirtieron.

## Comandos y respuestas
Salida literal del script (`curl -s -w` con el código HTTP en la línea `HTTP nnn`):

```text
=== A. Costo del jugador ===

### A1 crear jugador P con costo 1500.50 (equipo 5 vacio, dorsal 99)
$ curl -s -X POST http://localhost:57076/api/jugadores -H 'Content-Type: application/json' -d '{"nombre":"ZZ Test Transfer","posicion":"DELANTERO","edad":25,"dorsal":99,"idEquipo":5,"costo":1500.50}'
HTTP 200
{"id":30,"nombre":"ZZ Test Transfer","posicion":"DELANTERO","edad":25,"dorsal":99,"imagenUrl":null,"costo":1500.50,"indicadorRespuesta":"Success","mensaje":"Gooool! El jugador ZZ Test Transfer se guardó en la base de datos.","idEquipo":5,"equipo":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"REPECHAJE"}}

### A2 crear jugador sin costo (queda 0)
$ curl -s -X POST http://localhost:57076/api/jugadores -H 'Content-Type: application/json' -d '{"nombre":"ZZ Sin Costo","posicion":"DEFENSA","edad":22,"dorsal":98,"idEquipo":5}'
HTTP 200
{"id":31,"nombre":"ZZ Sin Costo","posicion":"DEFENSA","edad":22,"dorsal":98,"imagenUrl":null,"costo":0,"indicadorRespuesta":"Success","mensaje":"Gooool! El jugador ZZ Sin Costo se guardó en la base de datos.","idEquipo":5,"equipo":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"REPECHAJE"}}

### A3 costo negativo -> 400
$ curl -s -X POST http://localhost:57076/api/jugadores -H 'Content-Type: application/json' -d '{"nombre":"ZZ Neg","posicion":"DEFENSA","edad":22,"dorsal":97,"idEquipo":5,"costo":-5}'
HTTP 400
{"costo":"El costo no puede ser negativo."}

### A4 costo con 3 decimales -> 400
$ curl -s -X POST http://localhost:57076/api/jugadores -H 'Content-Type: application/json' -d '{"nombre":"ZZ Dec","posicion":"DEFENSA","edad":22,"dorsal":96,"idEquipo":5,"costo":10.123}'
HTTP 400
{"costo":"El costo admite hasta 10 enteros y 2 decimales."}

### A5 editar S sin costo conserva el costo (primero se le pone 300)
$ curl -s -X PUT http://localhost:57076/api/jugadores -H 'Content-Type: application/json' -d '{"id":31,"nombre":"ZZ Sin Costo","posicion":"DEFENSA","edad":22,"dorsal":98,"idEquipo":5,"costo":300}'
HTTP 200
{"id":31,"nombre":"ZZ Sin Costo","posicion":"DEFENSA","edad":22,"dorsal":98,"imagenUrl":null,"costo":300,"indicadorRespuesta":"Success","mensaje":"Gooool! El jugador ZZ Sin Costo se actualizó en la base de datos.","idEquipo":5,"equipo":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"REPECHAJE"}}

### A6 editar S sin enviar costo -> sigue en 300
$ curl -s -X PUT http://localhost:57076/api/jugadores -H 'Content-Type: application/json' -d '{"id":31,"nombre":"ZZ Sin Costo","posicion":"DEFENSA","edad":23,"dorsal":98,"idEquipo":5}'
HTTP 200
{"id":31,"nombre":"ZZ Sin Costo","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":300.00,"indicadorRespuesta":"Success","mensaje":"Gooool! El jugador ZZ Sin Costo se actualizó en la base de datos.","idEquipo":5,"equipo":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"REPECHAJE"}}

=== B. Solicitar traspasos ===

### B1 P -> equipo 2 (T1)
$ curl -s -X POST http://localhost:57076/api/transfers -H 'Content-Type: application/json' -d '{"playerId":30,"destinationTeamId":2}'
HTTP 201
{"id":1,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":2,"destinationTeamName":"Socios.com","cost":1500.50,"status":"PENDING","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:30.436","resolvedAt":null}

### B2 P -> equipo 3 (T2), segunda pendiente del mismo jugador
$ curl -s -X POST http://localhost:57076/api/transfers -H 'Content-Type: application/json' -d '{"playerId":30,"destinationTeamId":3}'
HTTP 201
{"id":2,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":3,"destinationTeamName":"Los de la T","cost":1500.50,"status":"PENDING","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:31.642","resolvedAt":null}

### B3 P -> equipo 4 (T3), tercera pendiente
$ curl -s -X POST http://localhost:57076/api/transfers -H 'Content-Type: application/json' -d '{"playerId":30,"destinationTeamId":4}'
HTTP 201
{"id":3,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":4,"destinationTeamName":"MRL","cost":1500.50,"status":"PENDING","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:33.025","resolvedAt":null}

### B4 mismo equipo actual -> 400
$ curl -s -X POST http://localhost:57076/api/transfers -H 'Content-Type: application/json' -d '{"playerId":30,"destinationTeamId":5}'
HTTP 400
{"indicadorRespuesta":"Error","mensaje":"El jugador ya pertenece al equipo destino."}

### B5 jugador inexistente -> 404
$ curl -s -X POST http://localhost:57076/api/transfers -H 'Content-Type: application/json' -d '{"playerId":999999,"destinationTeamId":2}'
HTTP 404
{"error":"Fuera de juego! No se encontró registros de jugador con Id 999999."}

### B6 equipo destino inexistente -> 400
$ curl -s -X POST http://localhost:57076/api/transfers -H 'Content-Type: application/json' -d '{"playerId":30,"destinationTeamId":999999}'
HTTP 400
{"indicadorRespuesta":"Error","mensaje":"Fuera de juego! No se ha seleccionado un o unos equipos existentes."}

### B7 cuerpo sin campos obligatorios -> 400
$ curl -s -X POST http://localhost:57076/api/transfers -H 'Content-Type: application/json' -d '{}'
HTTP 400
{"destinationTeamId":"El equipo destino del traspaso es obligatorio.","playerId":"El jugador del traspaso es obligatorio."}

### B8 sin clave de Director Tecnico -> 401
$ curl -s -X POST http://localhost:57076/api/transfers -H 'Content-Type: application/json' -d '{"playerId":30,"destinationTeamId":2}'
HTTP 401
{"indicadorRespuesta":"No autorizado","mensaje":"Esta accion requiere la clave de Director Tecnico. Solo puedes consultar informacion."}

### B9 dorsal 0 invalido -> 400
$ curl -s -X POST http://localhost:57076/api/transfers -H 'Content-Type: application/json' -d '{"playerId":30,"destinationTeamId":2,"destinationDorsal":0}'
HTTP 400
{"destinationDorsal":"El dorsal debe ser mayor a 0."}

=== C. Snapshot del costo ===

### C1 editar costo de P a 2000
$ curl -s -X PUT http://localhost:57076/api/jugadores -H 'Content-Type: application/json' -d '{"id":30,"nombre":"ZZ Test Transfer","posicion":"DELANTERO","edad":25,"dorsal":99,"idEquipo":5,"costo":2000}'
HTTP 200
{"id":30,"nombre":"ZZ Test Transfer","posicion":"DELANTERO","edad":25,"dorsal":99,"imagenUrl":null,"costo":2000,"indicadorRespuesta":"Success","mensaje":"Gooool! El jugador ZZ Test Transfer se actualizó en la base de datos.","idEquipo":5,"equipo":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"REPECHAJE"}}

### C2 el traspaso T1 conserva costo 1500.50 (bandeja pendientes)
$ curl -s -X GET http://localhost:57076/api/transfers?status=PENDING&playerId=30
HTTP 200
[{"id":3,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":4,"destinationTeamName":"MRL","cost":1500.50,"status":"PENDING","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:33","resolvedAt":null},{"id":2,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":3,"destinationTeamName":"Los de la T","cost":1500.50,"status":"PENDING","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:32","resolvedAt":null},{"id":1,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":2,"destinationTeamName":"Socios.com","cost":1500.50,"status":"PENDING","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:30","resolvedAt":null}]

=== D. Rechazo manual ===

### D1 rechazar T3
$ curl -s -X POST http://localhost:57076/api/transfers/3/reject
HTTP 200
{"id":3,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":4,"destinationTeamName":"MRL","cost":1500.50,"status":"REJECTED","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:33","resolvedAt":"2026-10-19T11:00:42"}

### D2 rechazar T3 otra vez -> 400
$ curl -s -X POST http://localhost:57076/api/transfers/3/reject
HTTP 400
{"indicadorRespuesta":"Error","mensaje":"La solicitud de traspaso ya no está pendiente."}

### D3 rechazar inexistente -> 404
$ curl -s -X POST http://localhost:57076/api/transfers/999999/reject
HTTP 404
{"error":"Fuera de juego! No se encontró la solicitud de traspaso con Id 999999."}

### D4 P sigue en equipo 5 tras rechazo
$ curl -s -X GET http://localhost:57076/api/jugadores/ZZ%20Test
HTTP 200
[{"id":30,"nombre":"ZZ Test Transfer","posicion":"DELANTERO","edad":25,"dorsal":99,"imagenUrl":null,"costo":2000.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":5,"equipo":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"REPECHAJE"}}]

=== E. Confirmacion + rechazo automatico ===

### E1 confirmar T1 (P pasa al equipo 2)
$ curl -s -X POST http://localhost:57076/api/transfers/1/confirm
HTTP 200
{"id":1,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":2,"destinationTeamName":"Socios.com","cost":1500.50,"status":"CONFIRMED","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:30","resolvedAt":"2026-10-19T11:00:45.437"}

### E2 P ahora esta en equipo 2 con dorsal 99
$ curl -s -X GET http://localhost:57076/api/jugadores/ZZ%20Test
HTTP 200
[{"id":30,"nombre":"ZZ Test Transfer","posicion":"DELANTERO","edad":25,"dorsal":99,"imagenUrl":null,"costo":2000.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":2,"equipo":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"}}]

### E3 bandeja de pendientes de P: vacia (T2 auto-rechazada)
$ curl -s -X GET http://localhost:57076/api/transfers?status=PENDING&playerId=30
HTTP 200
[]

### E4 todos los traspasos de P (T1 CONFIRMED, T2 y T3 REJECTED)
$ curl -s -X GET http://localhost:57076/api/transfers?playerId=30
HTTP 200
[{"id":3,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":4,"destinationTeamName":"MRL","cost":1500.50,"status":"REJECTED","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:33","resolvedAt":"2026-10-19T11:00:42"},{"id":2,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":3,"destinationTeamName":"Los de la T","cost":1500.50,"status":"REJECTED","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:32","resolvedAt":"2026-10-19T11:00:45"},{"id":1,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":2,"destinationTeamName":"Socios.com","cost":1500.50,"status":"CONFIRMED","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:30","resolvedAt":"2026-10-19T11:00:45"}]

### E5 historial de equipos de P (solo CONFIRMED)
$ curl -s -X GET http://localhost:57076/api/transfers?status=CONFIRMED&playerId=30
HTTP 200
[{"id":1,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":2,"destinationTeamName":"Socios.com","cost":1500.50,"status":"CONFIRMED","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:30","resolvedAt":"2026-10-19T11:00:45"}]

### E6 confirmar T1 otra vez -> 400
$ curl -s -X POST http://localhost:57076/api/transfers/1/confirm
HTTP 400
{"indicadorRespuesta":"Error","mensaje":"La solicitud de traspaso ya no está pendiente."}

### E7 confirmar T2 ya auto-rechazada -> 400
$ curl -s -X POST http://localhost:57076/api/transfers/2/confirm
HTTP 400
{"indicadorRespuesta":"Error","mensaje":"La solicitud de traspaso ya no está pendiente."}

### E8 confirmar inexistente -> 404
$ curl -s -X POST http://localhost:57076/api/transfers/999999/confirm
HTTP 404
{"error":"Fuera de juego! No se encontró la solicitud de traspaso con Id 999999."}

### E9 confirmar sin clave -> 401
$ curl -s -X POST http://localhost:57076/api/transfers/2/confirm
HTTP 401
{"indicadorRespuesta":"No autorizado","mensaje":"Esta accion requiere la clave de Director Tecnico. Solo puedes consultar informacion."}

=== F. Dorsal ocupado ===

### F1 crear Q (equipo 5, dorsal 7 = dorsal de Yeison en equipo 4)
$ curl -s -X POST http://localhost:57076/api/jugadores -H 'Content-Type: application/json' -d '{"nombre":"ZZ Dorsal Q","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"idEquipo":5,"costo":10}'
HTTP 200
{"id":32,"nombre":"ZZ Dorsal Q","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"imagenUrl":null,"costo":10,"indicadorRespuesta":"Success","mensaje":"Gooool! El jugador ZZ Dorsal Q se guardó en la base de datos.","idEquipo":5,"equipo":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"REPECHAJE"}}

### F2 Q -> equipo 4 sin dorsal nuevo (ocupado) -> 400
$ curl -s -X POST http://localhost:57076/api/transfers -H 'Content-Type: application/json' -d '{"playerId":32,"destinationTeamId":4}'
HTTP 400
{"indicadorRespuesta":"Error","mensaje":"El dorsal 7 ya está asignado en el equipo destino. Indica un dorsal nuevo para el traspaso."}

### F3 Q -> equipo 4 con dorsal nuevo 95 -> 201
$ curl -s -X POST http://localhost:57076/api/transfers -H 'Content-Type: application/json' -d '{"playerId":32,"destinationTeamId":4,"destinationDorsal":95}'
HTTP 201
{"id":4,"playerId":32,"playerName":"ZZ Dorsal Q","originTeamId":5,"originTeamName":"Real","destinationTeamId":4,"destinationTeamName":"MRL","cost":10.00,"status":"PENDING","destinationDorsal":95,"requestedAt":"2026-10-19T11:00:56.032","resolvedAt":null}

### F4 R en equipo 4 con dorsal 95 (ocupa el dorsal antes de confirmar)
$ curl -s -X POST http://localhost:57076/api/jugadores -H 'Content-Type: application/json' -d '{"nombre":"ZZ Dorsal R","posicion":"DEFENSA","edad":24,"dorsal":95,"idEquipo":4}'
HTTP 200
{"id":33,"nombre":"ZZ Dorsal R","posicion":"DEFENSA","edad":24,"dorsal":95,"imagenUrl":null,"costo":0,"indicadorRespuesta":"Success","mensaje":"Gooool! El jugador ZZ Dorsal R se guardó en la base de datos.","idEquipo":4,"equipo":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"}}

### F5 confirmar T4 -> 400 por dorsal ocupado al confirmar
$ curl -s -X POST http://localhost:57076/api/transfers/4/confirm
HTTP 400
{"indicadorRespuesta":"Error","mensaje":"El dorsal 95 ya está asignado en el equipo destino. Indica un dorsal nuevo para el traspaso."}

### F6 T4 sigue PENDING y Q sigue en equipo 5
$ curl -s -X GET http://localhost:57076/api/transfers?status=PENDING&playerId=32
HTTP 200
[{"id":4,"playerId":32,"playerName":"ZZ Dorsal Q","originTeamId":5,"originTeamName":"Real","destinationTeamId":4,"destinationTeamName":"MRL","cost":10.00,"status":"PENDING","destinationDorsal":95,"requestedAt":"2026-10-19T11:00:56","resolvedAt":null}]

### F7 Q sigue en equipo 5
$ curl -s -X GET http://localhost:57076/api/jugadores/ZZ%20Dorsal%20Q
HTTP 200
[{"id":32,"nombre":"ZZ Dorsal Q","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"imagenUrl":null,"costo":10.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":5,"equipo":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"REPECHAJE"}}]

=== G. Consultas y errores de filtro ===

### G1 estado invalido -> 400
$ curl -s -X GET http://localhost:57076/api/transfers?status=DONE
HTTP 400
{"indicadorRespuesta":"Error","mensaje":"El estado 'DONE' no es válido. Valores permitidos: PENDING, CONFIRMED, REJECTED."}

### G2 jugador sin traspasos -> []
$ curl -s -X GET http://localhost:57076/api/transfers?playerId=1
HTTP 200
[]

### G3 sin filtros (todos)
$ curl -s -X GET http://localhost:57076/api/transfers
HTTP 200
[{"id":4,"playerId":32,"playerName":"ZZ Dorsal Q","originTeamId":5,"originTeamName":"Real","destinationTeamId":4,"destinationTeamName":"MRL","cost":10.00,"status":"PENDING","destinationDorsal":95,"requestedAt":"2026-10-19T11:00:56","resolvedAt":null},{"id":3,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":4,"destinationTeamName":"MRL","cost":1500.50,"status":"REJECTED","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:33","resolvedAt":"2026-10-19T11:00:42"},{"id":2,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":3,"destinationTeamName":"Los de la T","cost":1500.50,"status":"REJECTED","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:32","resolvedAt":"2026-10-19T11:00:45"},{"id":1,"playerId":30,"playerName":"ZZ Test Transfer","originTeamId":5,"originTeamName":"Real","destinationTeamId":2,"destinationTeamName":"Socios.com","cost":1500.50,"status":"CONFIRMED","destinationDorsal":null,"requestedAt":"2026-10-19T11:00:30","resolvedAt":"2026-10-19T11:00:45"}]

### G4 costo y equipo en GET /api/jugadores de jugador existente (costo 0)
$ curl -s -X GET http://localhost:57076/api/jugadores/Andres
HTTP 200
[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":1,"equipo":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"}},{"id":26,"nombre":"Andres","posicion":"MEDIOCAMPISTA","edad":22,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":4,"equipo":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"}}]

IDS: P=30 S=31 Q=32 R=33 T1=1 T2=2 T3=3 T4=4
```
