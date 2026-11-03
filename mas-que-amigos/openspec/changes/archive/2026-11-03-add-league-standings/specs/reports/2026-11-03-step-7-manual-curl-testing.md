# Reporte del Paso 7 - Testing manual de endpoints con curl

- Fecha: 2026-11-03
- Change: add-league-standings
- Agente: Claude (Sonnet 5.5)

## Entorno
- Backend local (`./mvnw spring-boot:run`) en el puerto **57077**, con el código del branch `feature/add-league-standings-backend`.
- Base de datos: **copia** `masqueamigos_t9` del MySQL del contenedor `TorneoContainer` (con Flyway en V9). La base real no se tocó.
- Sesión: login real como `ADMIN` con las credenciales de `.env` (no se muestran) y una cuenta `DT` temporal sin equipo (`dt-t9`). Los tokens duran 3 minutos, así que se pidieron nuevos antes de cada tanda; aquí aparecen como `<token>` y las contraseñas como `***`.
- Equipos: 1 Macalister, 2 Socios.com, 3 Los de la T, 4 MRL, 5 Real. Torneos creados para la prueba: 5 Liga Calendario, 6 Copa K (KNOCKOUT), 7 Liga Borrador, 8 Liga Choque, 9 Liga Intervalo. Las respuestas largas se acortaron a 300 caracteres al registrarlas.

## Estado de la base de datos
- Baseline: partido 2, equipos 5, jugador 29, alineacion 21, estadistica 2, player_transfer 7, app_user 3, tournament 1 (Histórico), tournament_team 0, Flyway 9.
- Después de la limpieza: idéntico al baseline (ver 7.6).

## Resumen de resultados
| Tarea | Resultado |
|---|---|
| 7.1 Servidor, sesión y baseline | OK |
| 7.2 Calendario: válido (6 partidos en 3 fechas con cada equipo una vez por fecha, 6 parejas distintas), segunda llamada 400, sin fecha/hora 400, intervalo 0 y 31 400, sin token 401, `DT` 403; con intervalo 3 y 3 equipos: 3 partidos en 3 fechas separadas 3 días | OK |
| 7.3 Precondiciones: `KNOCKOUT` 400, liga en borrador 400, Histórico 400, inexistente 404, choque de fecha 400 nombrando equipo y fecha y sin crear ningún partido | OK |
| 7.4 Tabla: ceros ordenados por nombre sin partidos finalizados; con 5 partidos finalizados los puntos, la diferencia y el juego limpio son los esperados; un partido manual de reemplazo cuenta; `KNOCKOUT`/Histórico 400; inexistente 404; sin token y con `DT` 200 | OK |
| 7.6 Restauración | OK |

## Observaciones
- **Preparación de datos por SQL**: para finalizar partidos hay que iniciar cada uno, y eso exige 7 titulares alineados por equipo. Como la copia es desechable, los marcadores (`estado = FINALIZADO` y goles) y las tarjetas de prueba se cargaron con SQL directo; el cálculo real de goles desde estadísticas ya lo cubre el change anterior y los tests. Lo que se probó por API fue el calendario, la tabla (consulta real a MySQL, incluida la consulta agregada de tarjetas) y el partido manual.
- **Verificación de la tabla con 5 partidos finalizados** (partido 11 aplazado y aún programado): Macalister 7 puntos (DG +3), Socios.com 3 puntos (DG 0, juego limpio 1), Los de la T 2 puntos (juego limpio 4) y MRL 1 punto. Una tarjeta roja registrada en el partido aún programado **no** sumó al juego limpio de Los de la T (quedó en 4).
- **Partido manual de reemplazo**: se eliminó el partido aplazado 11 (Socios.com vs Los de la T), se creó a mano otro 2 vs 3 en el torneo y, marcado como 0-4, la tabla cambió a Macalister 7, Los de la T 5, Socios.com 3 y MRL 1. La tarjeta del partido eliminado ya no cuenta.
- Los criterios de desempate (enfrentamiento directo en 2 y 3 equipos, diferencia, goles a favor, juego limpio y nombre) están cubiertos por los 13 tests de `LeagueStandingsCalculatorTest`.
- `GET /v3/api-docs` lista `/api/tournaments/{id}/standings` y `/api/tournaments/{id}/schedule`, con los esquemas `StandingDTO` y `ScheduleRequestDTO`.
- **E2E con Playwright: no aplica** en este change. El front vive en el repositorio aparte `MasQueAmigos-Torneo` y no se modifica aquí; el contrato queda cubierto por la Parte 2 del prompt `docs/frontend-prompts/tournaments-module.md` (tarea 9.3).

## Comandos y respuestas

### Setup: inscribir equipo 1 en torneo 5 (Liga Calendario, LEAGUE, creado antes con POST)
```
curl -X POST http://localhost:57077/api/tournaments/5/teams -H 'Authorization: Bearer <token>' -d '{"teamId":1}'
{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":13,"nombre":"Daniel","posicion":"DEFENSA","edad":35,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":14,"nombre":"Brayan","posicion":"DEFENSA","edad":24,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":16,"nombre":"Alex","posicion":"MEDIOCAMPISTA","edad":31,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":15,"nombre":"Duvan","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":11,"nombre":"James","posicion":"MEDIOCAMPISTA","edad":28,"dorsal":11,"imagenUrl":"http://localhost:57075/uploads/jugadores/c26d17af-6da2-4ba6-848a-f5fd39dd0620.png","costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":7,"nombre":"Jose","posicion":"PORTERO","edad":22,"dorsal":22,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":29,"nombre":"dilan","posicion":"DEFENSA","edad":23,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":35,"nombre":"ZZTest SinCosto","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### Setup: inscribir equipo 2 en torneo 5 (Liga Calendario, LEAGUE, creado antes con POST)
```
curl -X POST http://localhost:57077/api/tournaments/5/teams -H 'Authorization: Bearer <token>' -d '{"teamId":2}'
{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":6,"nombre":"Didier","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":5,"nombre":"Yeiler","posicion":"DEFENSA","edad":21,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":2,"nombre":"James Rodríguez","posicion":"MEDIOCAMPISTA","edad":33,"dorsal":10,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":10,"nombre":"Erick","posicion":"DELANTERO","edad":34,"dorsal":15,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":4,"nombre":"Jhonatan","posicion":"DEFENSA","edad":33,"dorsal":21,"imagenUrl":null,"costo":100000.00,"indicadorRespuesta":null,"mensaje":null},{"id":34,"nombre":"ZZTest Costo","posicion":"DELANTERO","edad":22,"dorsal":97,"imagenUrl":null,"costo":1500.50,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### Setup: inscribir equipo 3 en torneo 5 (Liga Calendario, LEAGUE, creado antes con POST)
```
curl -X POST http://localhost:57077/api/tournaments/5/teams -H 'Authorization: Bearer <token>' -d '{"teamId":3}'
{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":18,"nombre":"Yair","posicion":"DEFENSA","edad":21,"dorsal":3,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":19,"nombre":"Alan","posicion":"MEDIOCAMPISTA","edad":25,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":20,"nombre":"Brandon","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":22,"nombre":"Raul","posicion":"DELANTERO","edad":27,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":21,"nombre":"Cristian","posicion":"MEDIOCAMPISTA","edad":36,"dorsal":14,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":23,"nombre":"Jhonatan","posicion":"DELANTERO","edad":21,"dorsal":21,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":17,"nombre":"Alex","posicion":"PORTERO","edad":25,"dorsal":31,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### Setup: inscribir equipo 4 en torneo 5 (Liga Calendario, LEAGUE, creado antes con POST)
```
curl -X POST http://localhost:57077/api/tournaments/5/teams -H 'Authorization: Bearer <token>' -d '{"teamId":4}'
{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":24,"nombre":"Carlos R","posicion":"DEFENSA","edad":23,"dorsal":2,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":25,"nombre":"Yair","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":8,"nombre":"Yeison","posicion":"MEDIOCAMPISTA","edad":38,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":26,"nombre":"Andres","posicion":"MEDIOCAMPISTA","edad":22,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":27,"nombre":"Jhon","posicion":"DELANTERO","edad":23,"dorsal":9,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":28,"nombre":"Alex","posicion":"DELANTERO","edad":34,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":9,"nombre":"Juan Carlos","posicion":"DELANTERO","edad":31,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### Setup: iniciar torneo 5
```
curl -X POST http://localhost:57077/api/tournaments/5/start -H 'Authorization: Bearer <token>'
{"id":5,"name":"Liga Calendario","format":"LEAGUE","status":"IN_PROGRESS","teamCount":4,"createdAt":"2026-11-03T11:37:52","startedAt":"2026-11-03T11:38:23.11"}
HTTP 200
```

### 7.2 schedule válido (ADMIN), inicio 2027-03-06 15:00, intervalo por defecto
```
curl -X POST http://localhost:57077/api/tournaments/5/schedule -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-03-06","time":"15:00:00"}'
[{"id":11,"equipoLocal":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"},"fecha":"2027-03-06","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":5,"estado":"PROGRAMADO","indicadorRespuesta":"Success"},{"id":12,"equipoLocal":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"},"equipoVisitante":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-03-06","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":5,"estado":"PROGRAMADO","indicadorRespuesta":"Success"},{"id":13,"equipoLocal":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"},"equipoVisitante":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-03-13","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":5,"estado":"PROGRAMADO","indicadorRespuesta":"Success"},{"id":14,"equipoLocal":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"},"equipoVisitante":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-03-13","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":5,"estado":"PROGRAMADO","indicadorRespuesta":"Success"},{"id":15,"equipoLocal":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"},"equipoVisitante":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"},"fecha":"2027-03-20","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":5,"estado":"PROGRAMADO","indicadorRespuesta":"Success"},{"id":16,"equipoLocal":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-03-20","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":5,"estado":"PROGRAMADO","indicadorRespuesta":"Success"}]
HTTP 201
```

### 7.2 schedule de nuevo (ya tiene partidos)
```
curl -X POST http://localhost:57077/api/tournaments/5/schedule -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-04-06","time":"15:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"El torneo ya tiene partidos: el calendario solo se genera en un torneo sin partidos."}
HTTP 400
```

### 7.2 schedule sin fecha ni hora
```
curl -X POST http://localhost:57077/api/tournaments/5/schedule -H 'Authorization: Bearer <token>' -d '{}'
{"time":"La hora de los partidos es obligatoria.","startDate":"La fecha de inicio es obligatoria."}
HTTP 400
```

### 7.2 schedule intervalo 0
```
curl -X POST http://localhost:57077/api/tournaments/5/schedule -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-04-06","time":"15:00:00","intervalDays":0}'
{"intervalDays":"El intervalo entre jornadas debe estar entre 1 y 30 días."}
HTTP 400
```

### 7.2 schedule intervalo 31
```
curl -X POST http://localhost:57077/api/tournaments/5/schedule -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-04-06","time":"15:00:00","intervalDays":31}'
{"intervalDays":"El intervalo entre jornadas debe estar entre 1 y 30 días."}
HTTP 400
```

### 7.2 schedule sin token
```
curl -X POST http://localhost:57077/api/tournaments/5/schedule -d '{"startDate":"2027-04-06","time":"15:00:00"}'
{"indicadorRespuesta":"No autorizado","mensaje":"Debes iniciar sesión para realizar esta acción."}
HTTP 401
```

### 7.2 schedule con token DT
```
curl -X POST http://localhost:57077/api/tournaments/5/schedule -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-04-06","time":"15:00:00"}'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```

### 7.3 setup: crear Copa K (KNOCKOUT)
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Copa K","format":"KNOCKOUT"}'
{"id":6,"name":"Copa K","format":"KNOCKOUT","status":"DRAFT","teamCount":0,"createdAt":"2026-11-03T11:39:35.961","startedAt":null}
HTTP 201
```

### 7.3 setup: crear Liga Borrador (LEAGUE, sin iniciar)
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Liga Borrador","format":"LEAGUE"}'
{"id":7,"name":"Liga Borrador","format":"LEAGUE","status":"DRAFT","teamCount":0,"createdAt":"2026-11-03T11:39:37.198","startedAt":null}
HTTP 201
```

### 7.3 setup: crear Liga Choque (LEAGUE)
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Liga Choque","format":"LEAGUE"}'
{"id":8,"name":"Liga Choque","format":"LEAGUE","status":"DRAFT","teamCount":0,"createdAt":"2026-11-03T11:39:38.639","startedAt":null}
HTTP 201
```

### 7.3 setup: crear Liga Intervalo (LEAGUE)
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Liga Intervalo","format":"LEAGUE"}'
{"id":9,"name":"Liga Intervalo","format":"LEAGUE","status":"DRAFT","teamCount":0,"createdAt":"2026-11-03T11:39:40.102","startedAt":null}
HTTP 201
```

### 7.3 setup: inscribir equipo 1 en Copa K (6)
```
curl -X POST http://localhost:57077/api/tournaments/6/teams -H 'Authorization: Bearer <token>' -d '{"teamId":1}'
{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":13,"nombre":"Daniel","posicion":"DEFENSA","edad":35,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":14,"nombre":"Brayan","posicion":"DEFENSA","edad":24,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":16,"nombre":"Alex","posicion":"MEDIOCAMPISTA","edad":31,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":15,"nombre":"Duvan","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":11,"nombre":"James","posicion":"MEDIOCAMPISTA","edad":28,"dorsal":11,"imagenUrl":"http://localhost:57075/uploads/jugadores/c26d17af-6da2-4ba6-848a-f5fd39dd0620.png","costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":7,"nombre":"Jose","posicion":"PORTERO","edad":22,"dorsal":22,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":29,"nombre":"dilan","posicion":"DEFENSA","edad":23,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":35,"nombre":"ZZTest SinCosto","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### 7.3 setup: inscribir equipo 2 en Copa K (6)
```
curl -X POST http://localhost:57077/api/tournaments/6/teams -H 'Authorization: Bearer <token>' -d '{"teamId":2}'
{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":6,"nombre":"Didier","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":5,"nombre":"Yeiler","posicion":"DEFENSA","edad":21,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":2,"nombre":"James Rodríguez","posicion":"MEDIOCAMPISTA","edad":33,"dorsal":10,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":10,"nombre":"Erick","posicion":"DELANTERO","edad":34,"dorsal":15,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":4,"nombre":"Jhonatan","posicion":"DEFENSA","edad":33,"dorsal":21,"imagenUrl":null,"costo":100000.00,"indicadorRespuesta":null,"mensaje":null},{"id":34,"nombre":"ZZTest Costo","posicion":"DELANTERO","edad":22,"dorsal":97,"imagenUrl":null,"costo":1500.50,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### 7.3 setup: iniciar Copa K
```
curl -X POST http://localhost:57077/api/tournaments/6/start -H 'Authorization: Bearer <token>'
{"id":6,"name":"Copa K","format":"KNOCKOUT","status":"IN_PROGRESS","teamCount":2,"createdAt":"2026-11-03T11:39:36","startedAt":"2026-11-03T11:40:15.026"}
HTTP 200
```

### 7.3 setup: inscribir equipo 1 en Liga Choque (8)
```
curl -X POST http://localhost:57077/api/tournaments/8/teams -H 'Authorization: Bearer <token>' -d '{"teamId":1}'
{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":13,"nombre":"Daniel","posicion":"DEFENSA","edad":35,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":14,"nombre":"Brayan","posicion":"DEFENSA","edad":24,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":16,"nombre":"Alex","posicion":"MEDIOCAMPISTA","edad":31,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":15,"nombre":"Duvan","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":11,"nombre":"James","posicion":"MEDIOCAMPISTA","edad":28,"dorsal":11,"imagenUrl":"http://localhost:57075/uploads/jugadores/c26d17af-6da2-4ba6-848a-f5fd39dd0620.png","costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":7,"nombre":"Jose","posicion":"PORTERO","edad":22,"dorsal":22,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":29,"nombre":"dilan","posicion":"DEFENSA","edad":23,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":35,"nombre":"ZZTest SinCosto","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### 7.3 setup: inscribir equipo 2 en Liga Choque (8)
```
curl -X POST http://localhost:57077/api/tournaments/8/teams -H 'Authorization: Bearer <token>' -d '{"teamId":2}'
{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":6,"nombre":"Didier","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":5,"nombre":"Yeiler","posicion":"DEFENSA","edad":21,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":2,"nombre":"James Rodríguez","posicion":"MEDIOCAMPISTA","edad":33,"dorsal":10,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":10,"nombre":"Erick","posicion":"DELANTERO","edad":34,"dorsal":15,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":4,"nombre":"Jhonatan","posicion":"DEFENSA","edad":33,"dorsal":21,"imagenUrl":null,"costo":100000.00,"indicadorRespuesta":null,"mensaje":null},{"id":34,"nombre":"ZZTest Costo","posicion":"DELANTERO","edad":22,"dorsal":97,"imagenUrl":null,"costo":1500.50,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### 7.3 setup: inscribir equipo 3 en Liga Choque (8)
```
curl -X POST http://localhost:57077/api/tournaments/8/teams -H 'Authorization: Bearer <token>' -d '{"teamId":3}'
{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":18,"nombre":"Yair","posicion":"DEFENSA","edad":21,"dorsal":3,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":19,"nombre":"Alan","posicion":"MEDIOCAMPISTA","edad":25,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":20,"nombre":"Brandon","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":22,"nombre":"Raul","posicion":"DELANTERO","edad":27,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":21,"nombre":"Cristian","posicion":"MEDIOCAMPISTA","edad":36,"dorsal":14,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":23,"nombre":"Jhonatan","posicion":"DELANTERO","edad":21,"dorsal":21,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":17,"nombre":"Alex","posicion":"PORTERO","edad":25,"dorsal":31,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### 7.3 setup: inscribir equipo 4 en Liga Choque (8)
```
curl -X POST http://localhost:57077/api/tournaments/8/teams -H 'Authorization: Bearer <token>' -d '{"teamId":4}'
{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":24,"nombre":"Carlos R","posicion":"DEFENSA","edad":23,"dorsal":2,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":25,"nombre":"Yair","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":8,"nombre":"Yeison","posicion":"MEDIOCAMPISTA","edad":38,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":26,"nombre":"Andres","posicion":"MEDIOCAMPISTA","edad":22,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":27,"nombre":"Jhon","posicion":"DELANTERO","edad":23,"dorsal":9,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":28,"nombre":"Alex","posicion":"DELANTERO","edad":34,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":9,"nombre":"Juan Carlos","posicion":"DELANTERO","edad":31,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### 7.3 setup: iniciar Liga Choque
```
curl -X POST http://localhost:57077/api/tournaments/8/start -H 'Authorization: Bearer <token>'
{"id":8,"name":"Liga Choque","format":"LEAGUE","status":"IN_PROGRESS","teamCount":4,"createdAt":"2026-11-03T11:39:39","startedAt":"2026-11-03T11:40:21.421"}
HTTP 200
```

### 7.3 setup: inscribir equipo 3 en Liga Intervalo (9)
```
curl -X POST http://localhost:57077/api/tournaments/9/teams -H 'Authorization: Bearer <token>' -d '{"teamId":3}'
{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":18,"nombre":"Yair","posicion":"DEFENSA","edad":21,"dorsal":3,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":19,"nombre":"Alan","posicion":"MEDIOCAMPISTA","edad":25,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":20,"nombre":"Brandon","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":22,"nombre":"Raul","posicion":"DELANTERO","edad":27,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":21,"nombre":"Cristian","posicion":"MEDIOCAMPISTA","edad":36,"dorsal":14,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":23,"nombre":"Jhonatan","posicion":"DELANTERO","edad":21,"dorsal":21,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":17,"nombre":"Alex","posicion":"PORTERO","edad":25,"dorsal":31,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### 7.3 setup: inscribir equipo 4 en Liga Intervalo (9)
```
curl -X POST http://localhost:57077/api/tournaments/9/teams -H 'Authorization: Bearer <token>' -d '{"teamId":4}'
{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":24,"nombre":"Carlos R","posicion":"DEFENSA","edad":23,"dorsal":2,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":25,"nombre":"Yair","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":8,"nombre":"Yeison","posicion":"MEDIOCAMPISTA","edad":38,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":26,"nombre":"Andres","posicion":"MEDIOCAMPISTA","edad":22,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":27,"nombre":"Jhon","posicion":"DELANTERO","edad":23,"dorsal":9,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":28,"nombre":"Alex","posicion":"DELANTERO","edad":34,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":9,"nombre":"Juan Carlos","posicion":"DELANTERO","edad":31,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### 7.3 setup: inscribir equipo 5 en Liga Intervalo (9)
```
curl -X POST http://localhost:57077/api/tournaments/9/teams -H 'Authorization: Bearer <token>' -d '{"teamId":5}'
{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### 7.3 setup: iniciar Liga Intervalo
```
curl -X POST http://localhost:57077/api/tournaments/9/start -H 'Authorization: Bearer <token>'
{"id":9,"name":"Liga Intervalo","format":"LEAGUE","status":"IN_PROGRESS","teamCount":3,"createdAt":"2026-11-03T11:39:40","startedAt":"2026-11-03T11:40:26.429"}
HTTP 200
```

### 7.3 schedule en torneo KNOCKOUT
```
curl -X POST http://localhost:57077/api/tournaments/6/schedule -H 'Authorization: Bearer <token>' -d '{"startDate":"2028-01-10","time":"18:30:00"}'
{"indicadorRespuesta":"Error","mensaje":"Este torneo no tiene calendario automático: solo los torneos de liga lo tienen."}
HTTP 400
```

### 7.3 schedule en liga en borrador
```
curl -X POST http://localhost:57077/api/tournaments/7/schedule -H 'Authorization: Bearer <token>' -d '{"startDate":"2028-01-10","time":"18:30:00"}'
{"indicadorRespuesta":"Error","mensaje":"El torneo no está en curso: el calendario se genera en un torneo en curso (estado actual: DRAFT)."}
HTTP 400
```

### 7.3 schedule en el Histórico
```
curl -X POST http://localhost:57077/api/tournaments/1/schedule -H 'Authorization: Bearer <token>' -d '{"startDate":"2028-01-10","time":"18:30:00"}'
{"indicadorRespuesta":"Error","mensaje":"Este torneo no tiene calendario automático: solo los torneos de liga lo tienen."}
HTTP 400
```

### 7.3 schedule torneo inexistente
```
curl -X POST http://localhost:57077/api/tournaments/999/schedule -H 'Authorization: Bearer <token>' -d '{"startDate":"2028-01-10","time":"18:30:00"}'
{"indicadorRespuesta":"Sin Registros","mensaje":"Fuera de juego! El torneo con Id 999 no existe."}
HTTP 404
```

### 7.3 schedule con choque de fecha (Liga Choque, inicio 2027-03-06)
```
curl -X POST http://localhost:57077/api/tournaments/8/schedule -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-03-06","time":"15:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"El equipo Socios.com ya tiene un partido el 2027-03-06: elige otra fecha de inicio o intervalo."}
HTTP 400
```

### 7.2 schedule con intervalo 3 y 3 equipos (Liga Intervalo)
```
curl -X POST http://localhost:57077/api/tournaments/9/schedule -H 'Authorization: Bearer <token>' -d '{"startDate":"2028-01-10","time":"18:30:00","intervalDays":3}'
[{"id":17,"equipoLocal":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2028-01-10","hora":"18:30:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":9,"estado":"PROGRAMADO","indicadorRespuesta":"Success"},{"id":18,"equipoLocal":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"},"fecha":"2028-01-13","hora":"18:30:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":9,"estado":"PROGRAMADO","indicadorRespuesta":"Success"},{"id":19,"equipoLocal":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"},"equipoVisitante":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2028-01-16","hora":"18:30:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":9,"estado":"PROGRAMADO","indicadorRespuesta":"Success"}]
HTTP 201
```

### 7.4 GET standings sin partidos finalizados (sin token)
```
curl -X GET http://localhost:57077/api/tournaments/5/standings
[{"position":1,"teamId":3,"teamName":"Los de la T","played":0,"won":0,"drawn":0,"lost":0,"goalsFor":0,"goalsAgainst":0,"goalDifference":0,"points":0,"fairPlayPoints":0},{"position":2,"teamId":1,"teamName":"Macalister","played":0,"won":0,"drawn":0,"lost":0,"goalsFor":0,"goalsAgainst":0,"goalDifference":0,"points":0,"fairPlayPoints":0},{"position":3,"teamId":4,"teamName":"MRL","played":0,"won":0,"drawn":0,"lost":0,"goalsFor":0,"goalsAgainst":0,"goalDifference":0,"points":0,"fairPlayPoints":0},{"position":4,"teamId":2,"teamName":"Socios.com","played":0,"won":0,"drawn":0,"lost":0,"goalsFor":0,"goalsAgainst":0,"goalDifference":0,"points":0,"fairPlayPoints":0}]
HTTP 200
```

### 7.4 GET standings con 5 partidos finalizados
```
curl -X GET http://localhost:57077/api/tournaments/5/standings
[{"position":1,"teamId":1,"teamName":"Macalister","played":3,"won":2,"drawn":1,"lost":0,"goalsFor":3,"goalsAgainst":0,"goalDifference":3,"points":7,"fairPlayPoints":0},{"position":2,"teamId":2,"teamName":"Socios.com","played":2,"won":1,"drawn":0,"lost":1,"goalsFor":3,"goalsAgainst":3,"goalDifference":0,"points":3,"fairPlayPoints":1},{"position":3,"teamId":3,"teamName":"Los de la T","played":2,"won":0,"drawn":2,"lost":0,"goalsFor":1,"goalsAgainst":1,"goalDifference":0,"points":2,"fairPlayPoints":4},{"position":4,"teamId":4,"teamName":"MRL","played":3,"won":0,"drawn":1,"lost":2,"goalsFor":2,"goalsAgainst":5,"goalDifference":-3,"points":1,"fairPlayPoints":0}]
HTTP 200
```

### 7.4 eliminar partido aplazado 11 (Socios.com vs Los de la T, programado)
```
curl -X DELETE http://localhost:57077/api/partidos/11 -H 'Authorization: Bearer <token>'

HTTP 204
```

### 7.4 crear partido manual de reemplazo 2 vs 3 en el torneo 5
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":2,"idEquipoVisitante":3,"fecha":"2027-04-03","hora":"15:00:00","fase":"FASE_DE_GRUPOS","idTorneo":5}'
{"id":20,"equipoLocal":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"},"fecha":"2027-04-03","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":5,"estado":"PROGRAMADO","indicadorRespuesta":"Success","mensaje":"Equipos, el partido ya fue programado"}
HTTP 200
```

### 7.4 standings de torneo KNOCKOUT
```
curl -X GET http://localhost:57077/api/tournaments/6/standings
{"indicadorRespuesta":"Error","mensaje":"Este torneo no tiene tabla de posiciones de liga: solo los torneos de formato LEAGUE la tienen."}
HTTP 400
```

### 7.4 standings del Histórico
```
curl -X GET http://localhost:57077/api/tournaments/1/standings
{"indicadorRespuesta":"Error","mensaje":"Este torneo no tiene tabla de posiciones de liga: solo los torneos de formato LEAGUE la tienen."}
HTTP 400
```

### 7.4 standings de torneo inexistente
```
curl -X GET http://localhost:57077/api/tournaments/999/standings
{"indicadorRespuesta":"Sin Registros","mensaje":"Fuera de juego! El torneo con Id 999 no existe."}
HTTP 404
```

### 7.4 standings de liga en borrador sin equipos
```
curl -X GET http://localhost:57077/api/tournaments/7/standings
[]
HTTP 200
```

### 7.4 standings con token DT (consulta)
```
curl -X GET http://localhost:57077/api/tournaments/5/standings -H 'Authorization: Bearer <token>'
[{"position":1,"teamId":1,"teamName":"Macalister","played":3,"won":2,"drawn":1,"lost":0,"goalsFor":3,"goalsAgainst":0,"goalDifference":3,"points":7,"fairPlayPoints":0},{"position":2,"teamId":3,"teamName":"Los de la T","played":3,"won":1,"drawn":2,"lost":0,"goalsFor":5,"goalsAgainst":1,"goalDifference":4,"points":5,"fairPlayPoints":4},{"position":3,"teamId":2,"teamName":"Socios.com","played":3,"won":1,"drawn":0,"lost":2,"goalsFor":3,"goalsAgainst":7,"goalDifference":-4,"points":3,"fairPlayPoints":1},{"position":4,"teamId":4,"teamName":"MRL","played":3,"won":0,"drawn":1,"lost":2,"goalsFor":2,"goalsAgainst":5,"goalDifference":-3,"points":1,"fairPlayPoints":0}]
HTTP 200
```

