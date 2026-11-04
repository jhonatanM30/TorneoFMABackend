# Reporte del Paso 14 - Testing manual de endpoints con curl

- Fecha: 2026-11-03
- Change: add-knockout-and-groups
- Agente: Claude (Sonnet 5.5)

## Entorno
- Backend local (`./mvnw spring-boot:run`) en el puerto **57077**, con el código del branch `feature/add-knockout-and-groups-backend`.
- Base de datos: **copia** `masqueamigos_t10` del MySQL del contenedor `TorneoContainer` (volcado de la base real, que ya estaba en V9), con Flyway aplicando `V10` al arrancar. La base real no se tocó; antes se hizo un respaldo.
- Sesión: login real como `ADMIN` con las credenciales de `.env` (no se muestran) y una cuenta `DT` temporal sin equipo (`dt-t9`). Los tokens duran 3 minutos, así que se pidieron nuevos antes de cada tanda; aquí aparecen como `<token>` y las contraseñas como `***`.
- Equipos: los 5 existentes (1 Macalister, 2 Socios.com, 3 Los de la T, 4 MRL, 5 Real) y 8 equipos de prueba creados por API (`Test Team 1` a `Test Team 8`, ids 6 a 13). Torneos de prueba: Copa Ocho (`KNOCKOUT`, 8 equipos), Copa Vacia (`KNOCKOUT`, sin llaves), Copa Cinco (`KNOCKOUT`, 5 equipos con repechaje), Mundial Tres, Mundial Cuatro y Mundial Ocho (`GROUPS_KNOCKOUT`). Las respuestas largas se acortaron a 300 caracteres al registrarlas; los códigos HTTP y los cuerpos de error están completos.

## Estado de la base de datos
- Baseline (con `V10` aplicada): partido 2, equipos 5, jugador 29, alineacion 21, estadistica 2, player_transfer 7, app_user 3, tournament 1 (Histórico), tournament_team 0, tournament_group 0, bracket_tie 0, Flyway 10.
- Después de la limpieza: idéntico al baseline (ver 14.10).

## Resumen de resultados
| Tarea | Resultado |
|---|---|
| 14.1 Servidor, sesión, cuenta `DT` y baseline; `V10` aplicada; `/v3/api-docs` lista las rutas nuevas y los campos de penales | OK |
| 14.2 Penales: válidos 5-4 (200), iguales 400, negativos 400, un valor ausente 400, partido programado 400, partido que no exige ganador (normal en el Histórico) 400, inexistente 404, sin token 401, `DT` 403 | OK |
| 14.3 Eliminatoria de 8 equipos: sin fecha/hora 400, intervalo 0 400, sin token 401, `DT` 403, sorteo 201 con 3 rondas (cuartos, semifinales y final), las fechas de las rondas separadas 7 días y la final y el tercer puesto el mismo día, los 8 equipos repartidos una sola vez, de nuevo 400, inexistente 404, sobre un torneo `FINISHED` 400 | OK |
| 14.4 Eliminatoria de 5 equipos: 400 explicando la potencia de 2 y el repechaje; repechaje sin finalizar 400; repechaje empatado exige penales y con penales finaliza; fecha de inicio igual al último partido 400; cruces manuales incompletos, con equipo repetido y con el equipo eliminado 400; cruces manuales válidos 201 sin tercer puesto | OK |
| 14.5 Avance: el ganador pasa de ronda, el partido siguiente se crea al completarse el cruce, los perdedores de semifinales juegan el tercer puesto con la fecha de la final, un partido empatado se decide por penales, la final deja al campeón; eliminar un partido de las llaves 400, cambiar sus equipos 400, cambiar su fecha 200 | OK |
| 14.6 Finalizar el torneo: con partidos pendientes 400 con el conteo, con la final finalizada 200; un torneo de eliminatoria sin llaves ni partidos 400 por campeón no definido | OK |
| 14.7 Grupos: 3 equipos en 2 grupos 400, clasificados 3 y clasificados no menores al grupo más pequeño 400, 3 grupos 400, sin datos 400, composición manual incompleta 400, `DT` 403, sin token 401, torneo `KNOCKOUT` 400, choque de fecha 400 nombrando al equipo y sin crear grupos ni partidos; 8 equipos en 2 grupos 201 con 4 y 4 equipos, 12 partidos en 3 fechas (4 por fecha); de nuevo 400; partido de grupo aplazado eliminado y reemplazado a mano que cuenta en la tabla | OK |
| 14.8 Eliminatoria tras los grupos: sin grupos 400, con 12 partidos pendientes 400 con la cantidad, cruces manuales 400, fecha de inicio no posterior 400, y con los grupos terminados 201 con los cruces `1.º A vs 2.º B` y `1.º B vs 2.º A`; avance hasta el campeón y torneo finalizado 200 | OK |
| 14.10 Restauración | OK |

## Observaciones
- **Preparación de datos por SQL**: para iniciar un partido hay que alinear 7 titulares por equipo, y los goles salen de las estadísticas. En la copia desechable se llevó cada partido a `EN_CURSO` y se cargó su marcador con SQL, o se marcó `FINALIZADO` en bloque los 12 partidos de grupo. Todo lo demás fue por API: penales, `PUT /api/partidos/{id}/finalizar` (que dispara las reglas de empate y el avance de las llaves), creación de partidos manuales y de repechaje, edición y eliminación.
- **Verificación de las tablas de grupo**: con ganador siempre el equipo de menor id por 2-0 (incluido el partido manual de reemplazo, que sumó partido y goles al equipo ganador), cada grupo quedó 9-6-3-0 puntos y los clasificados salieron en ese orden. Los partidos de las llaves no cambiaron las tablas de los grupos.
- **Fecha de un cruce editado**: en la prueba, el `GET /bracket` seguía mostrando la fecha original de la ronda aunque el administrador hubiera editado la fecha del partido. Se corrigió: ahora el cruce muestra la fecha y la hora del partido en cuanto existe (y las de su ronda mientras no existe), con un test nuevo en `BracketServiceTest`.
- **Campeón no definido en torneos con partidos pendientes**: el caso "no hay partidos pendientes pero falta el campeón" solo se da en un torneo de eliminatoria sin llaves; con llaves, entre rondas siempre hay un partido programado, así que el mensaje de partidos pendientes aparece primero, como indica la spec.
- **E2E con Playwright: no aplica** en este change. El front vive en el repositorio aparte `MasQueAmigos-Torneo` y no se modifica aquí; el contrato queda cubierto por la Parte 3 del prompt `docs/frontend-prompts/tournaments-module.md` (tarea 16.3).

## Comandos y respuestas

### Setup: crear cuenta DT temporal
```
curl -X POST http://localhost:57077/api/users -H 'Authorization: Bearer <token>' -d '{"username":"dt-t9","password":"***","role":"DT"}'
{"id":8,"username":"dt-t9","role":"DT","teamId":null,"teamName":null,"createdAt":"2026-11-03T18:58:38.789"}
HTTP 201
```

### Setup: crear equipo de prueba 1
```
curl -X POST http://localhost:57077/api/equipos -H 'Authorization: Bearer <token>' -d '{"nombre":"Test Team 1","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"}'
{"id":6,"nombre":"Test Team 1","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","mensaje":"Gooool! El equipo Test Team 1 se guardó en la base de datos."}
HTTP 200
```

### Setup: crear equipo de prueba 2
```
curl -X POST http://localhost:57077/api/equipos -H 'Authorization: Bearer <token>' -d '{"nombre":"Test Team 2","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"}'
{"id":7,"nombre":"Test Team 2","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","mensaje":"Gooool! El equipo Test Team 2 se guardó en la base de datos."}
HTTP 200
```

### Setup: crear equipo de prueba 3
```
curl -X POST http://localhost:57077/api/equipos -H 'Authorization: Bearer <token>' -d '{"nombre":"Test Team 3","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"}'
{"id":8,"nombre":"Test Team 3","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","mensaje":"Gooool! El equipo Test Team 3 se guardó en la base de datos."}
HTTP 200
```

### Setup: crear equipo de prueba 4
```
curl -X POST http://localhost:57077/api/equipos -H 'Authorization: Bearer <token>' -d '{"nombre":"Test Team 4","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"}'
{"id":9,"nombre":"Test Team 4","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","mensaje":"Gooool! El equipo Test Team 4 se guardó en la base de datos."}
HTTP 200
```

### Setup: crear equipo de prueba 5
```
curl -X POST http://localhost:57077/api/equipos -H 'Authorization: Bearer <token>' -d '{"nombre":"Test Team 5","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"}'
{"id":10,"nombre":"Test Team 5","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","mensaje":"Gooool! El equipo Test Team 5 se guardó en la base de datos."}
HTTP 200
```

### Setup: crear equipo de prueba 6
```
curl -X POST http://localhost:57077/api/equipos -H 'Authorization: Bearer <token>' -d '{"nombre":"Test Team 6","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"}'
{"id":11,"nombre":"Test Team 6","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","mensaje":"Gooool! El equipo Test Team 6 se guardó en la base de datos."}
HTTP 200
```

### Setup: crear equipo de prueba 7
```
curl -X POST http://localhost:57077/api/equipos -H 'Authorization: Bearer <token>' -d '{"nombre":"Test Team 7","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"}'
{"id":12,"nombre":"Test Team 7","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","mensaje":"Gooool! El equipo Test Team 7 se guardó en la base de datos."}
HTTP 200
```

### Setup: crear equipo de prueba 8
```
curl -X POST http://localhost:57077/api/equipos -H 'Authorization: Bearer <token>' -d '{"nombre":"Test Team 8","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"}'
{"id":13,"nombre":"Test Team 8","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","mensaje":"Gooool! El equipo Test Team 8 se guardó en la base de datos."}
HTTP 200
```

### Setup: crear torneo Copa Ocho (KNOCKOUT)
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Copa Ocho","format":"KNOCKOUT"}'
{"id":2,"name":"Copa Ocho","format":"KNOCKOUT","status":"DRAFT","teamCount":0,"createdAt":"2026-11-03T19:07:27.433","startedAt":null}
HTTP 201
```

### Setup: inscribir equipo 6 en Copa Ocho
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":6}'
{"id":6,"nombre":"Test Team 1","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 7 en Copa Ocho
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":7}'
{"id":7,"nombre":"Test Team 2","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 8 en Copa Ocho
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":8}'
{"id":8,"nombre":"Test Team 3","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 9 en Copa Ocho
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":9}'
{"id":9,"nombre":"Test Team 4","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 10 en Copa Ocho
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":10}'
{"id":10,"nombre":"Test Team 5","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 11 en Copa Ocho
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":11}'
{"id":11,"nombre":"Test Team 6","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 12 en Copa Ocho
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":12}'
{"id":12,"nombre":"Test Team 7","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 13 en Copa Ocho
```
curl -X POST http://localhost:57077/api/tournaments/2/teams -H 'Authorization: Bearer <token>' -d '{"teamId":13}'
{"id":13,"nombre":"Test Team 8","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: iniciar Copa Ocho
```
curl -X POST http://localhost:57077/api/tournaments/2/start -H 'Authorization: Bearer <token>'
{"id":2,"name":"Copa Ocho","format":"KNOCKOUT","status":"IN_PROGRESS","teamCount":8,"createdAt":"2026-11-03T19:07:27","startedAt":"2026-11-03T19:07:43.731"}
HTTP 200
```

### 14.3 bracket sin fecha ni hora
```
curl -X POST http://localhost:57077/api/tournaments/2/bracket -H 'Authorization: Bearer <token>' -d '{}'
{"time":"La hora de los partidos es obligatoria.","startDate":"La fecha de inicio es obligatoria."}
HTTP 400
```

### 14.3 bracket con intervalo 0
```
curl -X POST http://localhost:57077/api/tournaments/2/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-05-01","time":"18:00:00","intervalDays":0}'
{"intervalDays":"El intervalo entre rondas debe estar entre 1 y 30 días."}
HTTP 400
```

### 14.3 bracket sin token
```
curl -X POST http://localhost:57077/api/tournaments/2/bracket -d '{"startDate":"2027-05-01","time":"18:00:00"}'
{"indicadorRespuesta":"No autorizado","mensaje":"Debes iniciar sesión para realizar esta acción."}
HTTP 401
```

### 14.3 bracket con token DT
```
curl -X POST http://localhost:57077/api/tournaments/2/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-05-01","time":"18:00:00"}'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```

### 14.3 GET bracket sin llaves (público)
```
curl -X GET http://localhost:57077/api/tournaments/2/bracket
{"rounds":[],"thirdPlace":null,"champion":null}
HTTP 200
```

### 14.3 POST bracket 8 equipos (sorteo), 2027-05-01 18:00, intervalo 7
```
curl -X POST http://localhost:57077/api/tournaments/2/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-05-01","time":"18:00:00","intervalDays":7}'
{"rounds":[{"round":1,"name":"Cuartos de final","ties":[{"id":1,"position":0,"homeTeam":{"id":8,"name":"Test Team 3"},"awayTeam":{"id":10,"name":"Test Team 5"},"matchId":8,"matchStatus":"PROGRAMADO","homeGoals":0,"awayGoals":0,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-05-01","time":"18:00:00"},{"id":2,"position":1,"homeTeam":{"id":11,"name":"Test Team 6"},"awayTeam":{"id":7,"name":"Test Team 2"},"matchId":9,"matchStatus":"PROGRAMADO","homeGoals":0,"awayGoals":0,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-05-01","time":"18:00:00"},{"id":3,"position":2,"homeTeam":{"id":6,"name":"Test Team 1"},"awayTeam":{"id":13,"name":"Test Team 8"},"matchId":10,"matchStatus":"PROGRAMADO","homeGoals":0,"awayGoals":0,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-05-01","time":"18:00:00"},{"id":4,"position":3,"homeTeam":{"id":9,"name":"Test Team 4"},"awayTeam":{"id":12,"name":"Test Team 7"},"matchId":11,"matchStatus":"PROGRAMADO","homeGoals":0,"awayGoals":0,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-05-01","time":"18:00:00"}]},{"round":2,"name":"Semifinales","ties":[{"id":5,"position":0,"homeTeam":null,"awayTeam":null,"matchId":null,"matchStatus":null,"homeGoals":null,"awayGoals":null,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-05-08","time":"18:00:00"},{"id":6,"position":1,"homeTeam":null,"awayTeam":null,"matchId":null,"matchStatus":null,"homeGoals":null,"awayGoals":null,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-05-08","time":"18:00:00"}]},{"round":3,"name":"Final","ties":[{"id":7,"position":0,"homeTeam":null,"awayTeam":null,"matchId":null,"matchStatus":null,"homeGoals":null,"awayGoals":null,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-05-15","time":"18:00:00"}]}],"thirdPlace":{"id":8,"position":0,"homeTeam":null,"awayTeam":null,"matchId":null,"matchStatus":null,"homeGoals":null,"awayGoals":null,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-05-15","time":"18:00:00"},"champion":null}
HTTP 201
```

### 14.3 POST bracket de nuevo (ya generadas)
```
curl -X POST http://localhost:57077/api/tournaments/2/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-06-01","time":"18:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"Las llaves de este torneo ya fueron generadas."}
HTTP 400
```

### 14.3 POST bracket torneo inexistente
```
curl -X POST http://localhost:57077/api/tournaments/999/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-06-01","time":"18:00:00"}'
{"indicadorRespuesta":"Sin Registros","mensaje":"Fuera de juego! El torneo con Id 999 no existe."}
HTTP 404
```

### 14.3 GET bracket torneo inexistente
```
curl -X GET http://localhost:57077/api/tournaments/999/bracket
{"indicadorRespuesta":"Sin Registros","mensaje":"Fuera de juego! El torneo con Id 999 no existe."}
HTTP 404
```

### 14.2 penales en partido PROGRAMADO
```
curl -X PUT http://localhost:57077/api/partidos/8/penales -H 'Authorization: Bearer <token>' -d '{"penalesLocal":5,"penalesVisitante":4}'
{"indicadorRespuesta":"Error","mensaje":"Solo se pueden registrar penales en un partido en curso (estado actual: PROGRAMADO)."}
HTTP 400
```

### 14.2 penales iguales
```
curl -X PUT http://localhost:57077/api/partidos/8/penales -H 'Authorization: Bearer <token>' -d '{"penalesLocal":4,"penalesVisitante":4}'
{"indicadorRespuesta":"Error","mensaje":"Los penales no pueden terminar empatados: una tanda de penales tiene ganador."}
HTTP 400
```

### 14.2 penales negativos
```
curl -X PUT http://localhost:57077/api/partidos/8/penales -H 'Authorization: Bearer <token>' -d '{"penalesLocal":-1,"penalesVisitante":4}'
{"homePenalties":"Los penales del equipo local no pueden ser negativos."}
HTTP 400
```

### 14.2 penales sin un valor
```
curl -X PUT http://localhost:57077/api/partidos/8/penales -H 'Authorization: Bearer <token>' -d '{"penalesLocal":5}'
{"awayPenalties":"Los penales del equipo visitante son obligatorios."}
HTTP 400
```

### 14.2 penales sin token
```
curl -X PUT http://localhost:57077/api/partidos/8/penales -d '{"penalesLocal":5,"penalesVisitante":4}'
{"indicadorRespuesta":"No autorizado","mensaje":"Debes iniciar sesión para realizar esta acción."}
HTTP 401
```

### 14.2 penales con token DT
```
curl -X PUT http://localhost:57077/api/partidos/8/penales -H 'Authorization: Bearer <token>' -d '{"penalesLocal":5,"penalesVisitante":4}'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```

### 14.2 penales de partido inexistente
```
curl -X PUT http://localhost:57077/api/partidos/99999/penales -H 'Authorization: Bearer <token>' -d '{"penalesLocal":5,"penalesVisitante":4}'
{"indicadorRespuesta":"Sin Registros","mensaje":"Fuera de juego! El partido al que quieres registrar penales no existe."}
HTTP 404
```

### 14.5 finalizar empatado sin penales
```
curl -X PUT http://localhost:57077/api/partidos/8/finalizar -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"Este partido no puede terminar empatado: registra los penales antes de finalizarlo."}
HTTP 400
```

### 14.2 penales válidos 5-4
```
curl -X PUT http://localhost:57077/api/partidos/8/penales -H 'Authorization: Bearer <token>' -d '{"penalesLocal":5,"penalesVisitante":4}'
{"id":8,"equipoLocal":{"id":8,"nombre":"Test Team 3","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":10,"nombre":"Test Team 5","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-05-01","hora":"18:00:00","golesLocal":1,"golesVisitante":1,"fase":"ELIMINACION_DIRECTA","idTorneo":2,"penalesLocal":5,"penalesVisitante":4,"estado":"EN_CURSO","indicadorRespuesta":"Success","mensaje":"Penales registrados."}
HTTP 200
```

### 14.5 finalizar empatado con penales 5-4
```
curl -X PUT http://localhost:57077/api/partidos/8/finalizar -H 'Authorization: Bearer <token>'
{"id":8,"equipoLocal":{"id":8,"nombre":"Test Team 3","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":10,"nombre":"Test Team 5","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-05-01","hora":"18:00:00","golesLocal":1,"golesVisitante":1,"fase":"ELIMINACION_DIRECTA","idTorneo":2,"penalesLocal":5,"penalesVisitante":4,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.5 finalizar cuarto 1
```
curl -X PUT http://localhost:57077/api/partidos/9/finalizar -H 'Authorization: Bearer <token>'
{"id":9,"equipoLocal":{"id":11,"nombre":"Test Team 6","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":7,"nombre":"Test Team 2","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-05-01","hora":"18:00:00","golesLocal":0,"golesVisitante":2,"fase":"ELIMINACION_DIRECTA","idTorneo":2,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.5 finalizar cuarto 2
```
curl -X PUT http://localhost:57077/api/partidos/10/finalizar -H 'Authorization: Bearer <token>'
{"id":10,"equipoLocal":{"id":6,"nombre":"Test Team 1","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":13,"nombre":"Test Team 8","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-05-01","hora":"18:00:00","golesLocal":3,"golesVisitante":0,"fase":"ELIMINACION_DIRECTA","idTorneo":2,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.5 finalizar cuarto 3
```
curl -X PUT http://localhost:57077/api/partidos/11/finalizar -H 'Authorization: Bearer <token>'
{"id":11,"equipoLocal":{"id":9,"nombre":"Test Team 4","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":12,"nombre":"Test Team 7","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-05-01","hora":"18:00:00","golesLocal":1,"golesVisitante":0,"fase":"ELIMINACION_DIRECTA","idTorneo":2,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.5 eliminar partido de las llaves
```
curl -X DELETE http://localhost:57077/api/partidos/12 -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"Este partido es parte de las llaves del torneo: no se puede eliminar."}
HTTP 400
```

### 14.5 cambiar equipos del partido de las llaves
```
curl -X PUT http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"id":12,"idEquipoLocal":8,"idEquipoVisitante":13,"fecha":"2027-05-08","hora":"18:00:00","fase":"ELIMINACION_DIRECTA"}'
{"indicadorRespuesta":"Error","mensaje":"Este partido es parte de las llaves del torneo: no se pueden cambiar sus equipos."}
HTTP 400
```

### 14.5 cambiar la fecha del partido de las llaves
```
curl -X PUT http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"id":12,"idEquipoLocal":8,"idEquipoVisitante":7,"fecha":"2027-05-09","hora":"18:00:00","fase":"ELIMINACION_DIRECTA"}'
{"id":12,"equipoLocal":{"id":8,"nombre":"Test Team 3","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":7,"nombre":"Test Team 2","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-05-09","hora":"18:00:00","golesLocal":0,"golesVisitante":0,"fase":"ELIMINACION_DIRECTA","idTorneo":2,"estado":"PROGRAMADO","indicadorRespuesta":"Success","mensaje":"Partido actualizado correctamente."}
HTTP 200
```

### 14.2 crear partido manual normal en el Histórico (equipos 1 y 2)
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":1,"idEquipoVisitante":2,"fecha":"2026-12-01","hora":"15:00:00","fase":"FASE_DE_GRUPOS"}'
{"id":14,"equipoLocal":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"},"equipoVisitante":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2026-12-01","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":1,"estado":"PROGRAMADO","indicadorRespuesta":"Success","mensaje":"Equipos, el partido ya fue programado"}
HTTP 200
```

### 14.2 penales en partido que no exige ganador
```
curl -X PUT http://localhost:57077/api/partidos/14/penales -H 'Authorization: Bearer <token>' -d '{"penalesLocal":5,"penalesVisitante":4}'
{"indicadorRespuesta":"Error","mensaje":"Este partido no usa penales: solo los partidos de las llaves y de repechaje exigen ganador."}
HTTP 400
```

### 14.5 un partido normal empatado sí se finaliza
```
curl -X PUT http://localhost:57077/api/partidos/14/finalizar -H 'Authorization: Bearer <token>'
{"id":14,"equipoLocal":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"},"equipoVisitante":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2026-12-01","hora":"15:00:00","golesLocal":1,"golesVisitante":1,"fase":"FASE_DE_GRUPOS","idTorneo":1,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.6 finalizar Copa Ocho con partidos pendientes
```
curl -X POST http://localhost:57077/api/tournaments/2/finish -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"No se puede finalizar el torneo: faltan por finalizar 2 partido(s). Juégalos o elimínalos primero."}
HTTP 400
```

### 14.5 finalizar semifinal 0
```
curl -X PUT http://localhost:57077/api/partidos/12/finalizar -H 'Authorization: Bearer <token>'
{"id":12,"equipoLocal":{"id":8,"nombre":"Test Team 3","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":7,"nombre":"Test Team 2","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-05-09","hora":"18:00:00","golesLocal":3,"golesVisitante":0,"fase":"ELIMINACION_DIRECTA","idTorneo":2,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.5 finalizar semifinal 1
```
curl -X PUT http://localhost:57077/api/partidos/13/finalizar -H 'Authorization: Bearer <token>'
{"id":13,"equipoLocal":{"id":6,"nombre":"Test Team 1","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":9,"nombre":"Test Team 4","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-05-08","hora":"18:00:00","golesLocal":0,"golesVisitante":1,"fase":"ELIMINACION_DIRECTA","idTorneo":2,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.5 tercer puesto empatado sin penales
```
curl -X PUT http://localhost:57077/api/partidos/16/finalizar -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"Este partido no puede terminar empatado: registra los penales antes de finalizarlo."}
HTTP 400
```

### 14.2 penales del tercer puesto 3-5
```
curl -X PUT http://localhost:57077/api/partidos/16/penales -H 'Authorization: Bearer <token>' -d '{"penalesLocal":3,"penalesVisitante":5}'
{"id":16,"equipoLocal":{"id":7,"nombre":"Test Team 2","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":6,"nombre":"Test Team 1","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-05-15","hora":"18:00:00","golesLocal":2,"golesVisitante":2,"fase":"ELIMINACION_DIRECTA","idTorneo":2,"penalesLocal":3,"penalesVisitante":5,"estado":"EN_CURSO","indicadorRespuesta":"Success","mensaje":"Penales registrados."}
HTTP 200
```

### 14.5 finalizar tercer puesto
```
curl -X PUT http://localhost:57077/api/partidos/16/finalizar -H 'Authorization: Bearer <token>'
{"id":16,"equipoLocal":{"id":7,"nombre":"Test Team 2","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":6,"nombre":"Test Team 1","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-05-15","hora":"18:00:00","golesLocal":2,"golesVisitante":2,"fase":"ELIMINACION_DIRECTA","idTorneo":2,"penalesLocal":3,"penalesVisitante":5,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.6 finalizar torneo con la final en curso
```
curl -X POST http://localhost:57077/api/tournaments/2/finish -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"No se puede finalizar el torneo: faltan por finalizar 1 partido(s). Juégalos o elimínalos primero."}
HTTP 400
```

### 14.5 finalizar la final
```
curl -X PUT http://localhost:57077/api/partidos/15/finalizar -H 'Authorization: Bearer <token>'
{"id":15,"equipoLocal":{"id":8,"nombre":"Test Team 3","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":9,"nombre":"Test Team 4","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-05-15","hora":"18:00:00","golesLocal":1,"golesVisitante":0,"fase":"FINAL","idTorneo":2,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.6 finalizar torneo con campeón
```
curl -X POST http://localhost:57077/api/tournaments/2/finish -H 'Authorization: Bearer <token>'
{"id":2,"name":"Copa Ocho","format":"KNOCKOUT","status":"FINISHED","teamCount":8,"createdAt":"2026-11-03T19:07:27","startedAt":"2026-11-03T19:07:44"}
HTTP 200
```

### 14.3 llaves sobre torneo FINISHED (no en curso)
```
curl -X POST http://localhost:57077/api/tournaments/2/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2028-01-01","time":"18:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"El torneo no está en curso: las llaves se generan en un torneo en curso (estado actual: FINISHED)."}
HTTP 400
```

### 14.6 crear torneo Copa Vacia (KNOCKOUT)
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Copa Vacia","format":"KNOCKOUT"}'
{"id":3,"name":"Copa Vacia","format":"KNOCKOUT","status":"DRAFT","teamCount":0,"createdAt":"2026-11-03T19:10:32.354","startedAt":null}
HTTP 201
```

### Setup: inscribir equipos 1 y 2 en Copa Vacia
```
curl -X POST http://localhost:57077/api/tournaments/3/teams -H 'Authorization: Bearer <token>' -d '{"teamId":1}'
{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":13,"nombre":"Daniel","posicion":"DEFENSA","edad":35,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":14,"nombre":"Brayan","posicion":"DEFENSA","edad":24,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":16,"nombre":"Alex","posicion":"MEDIOCAMPISTA","edad":31,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":15,"nombre":"Duvan","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":11,"nombre":"James","posicion":"MEDIOCAMPISTA","edad":28,"dorsal":11,"imagenUrl":"http://localhost:57075/uploads/jugadores/c26d17af-6da2-4ba6-848a-f5fd39dd0620.png","costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":7,"nombre":"Jose","posicion":"PORTERO","edad":22,"dorsal":22,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":29,"nombre":"dilan","posicion":"DEFENSA","edad":23,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":35,"nombre":"ZZTest SinCosto","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### Setup: inscribir equipo 2
```
curl -X POST http://localhost:57077/api/tournaments/3/teams -H 'Authorization: Bearer <token>' -d '{"teamId":2}'
{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":6,"nombre":"Didier","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":5,"nombre":"Yeiler","posicion":"DEFENSA","edad":21,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":2,"nombre":"James Rodríguez","posicion":"MEDIOCAMPISTA","edad":33,"dorsal":10,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":10,"nombre":"Erick","posicion":"DELANTERO","edad":34,"dorsal":15,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":4,"nombre":"Jhonatan","posicion":"DEFENSA","edad":33,"dorsal":21,"imagenUrl":null,"costo":100000.00,"indicadorRespuesta":null,"mensaje":null},{"id":34,"nombre":"ZZTest Costo","posicion":"DELANTERO","edad":22,"dorsal":97,"imagenUrl":null,"costo":1500.50,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### Setup: iniciar Copa Vacia
```
curl -X POST http://localhost:57077/api/tournaments/3/start -H 'Authorization: Bearer <token>'
{"id":3,"name":"Copa Vacia","format":"KNOCKOUT","status":"IN_PROGRESS","teamCount":2,"createdAt":"2026-11-03T19:10:32","startedAt":"2026-11-03T19:10:37.607"}
HTTP 200
```

### 14.6 finalizar torneo de eliminatoria sin llaves
```
curl -X POST http://localhost:57077/api/tournaments/3/finish -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"Falta definir al campeón: genera las llaves y finaliza la final antes de finalizar el torneo."}
HTTP 400
```

### Setup: crear torneo Copa Cinco (KNOCKOUT)
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Copa Cinco","format":"KNOCKOUT"}'
{"id":4,"name":"Copa Cinco","format":"KNOCKOUT","status":"DRAFT","teamCount":0,"createdAt":"2026-11-03T19:12:09.253","startedAt":null}
HTTP 201
```

### Setup: inscribir equipo 1 en Copa Cinco
```
curl -X POST http://localhost:57077/api/tournaments/4/teams -H 'Authorization: Bearer <token>' -d '{"teamId":1}'
{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":13,"nombre":"Daniel","posicion":"DEFENSA","edad":35,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":14,"nombre":"Brayan","posicion":"DEFENSA","edad":24,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":16,"nombre":"Alex","posicion":"MEDIOCAMPISTA","edad":31,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":15,"nombre":"Duvan","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":11,"nombre":"James","posicion":"MEDIOCAMPISTA","edad":28,"dorsal":11,"imagenUrl":"http://localhost:57075/uploads/jugadores/c26d17af-6da2-4ba6-848a-f5fd39dd0620.png","costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":7,"nombre":"Jose","posicion":"PORTERO","edad":22,"dorsal":22,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":29,"nombre":"dilan","posicion":"DEFENSA","edad":23,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":35,"nombre":"ZZTest SinCosto","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### Setup: inscribir equipo 2 en Copa Cinco
```
curl -X POST http://localhost:57077/api/tournaments/4/teams -H 'Authorization: Bearer <token>' -d '{"teamId":2}'
{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":6,"nombre":"Didier","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":5,"nombre":"Yeiler","posicion":"DEFENSA","edad":21,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":2,"nombre":"James Rodríguez","posicion":"MEDIOCAMPISTA","edad":33,"dorsal":10,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":10,"nombre":"Erick","posicion":"DELANTERO","edad":34,"dorsal":15,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":4,"nombre":"Jhonatan","posicion":"DEFENSA","edad":33,"dorsal":21,"imagenUrl":null,"costo":100000.00,"indicadorRespuesta":null,"mensaje":null},{"id":34,"nombre":"ZZTest Costo","posicion":"DELANTERO","edad":22,"dorsal":97,"imagenUrl":null,"costo":1500.50,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### Setup: inscribir equipo 3 en Copa Cinco
```
curl -X POST http://localhost:57077/api/tournaments/4/teams -H 'Authorization: Bearer <token>' -d '{"teamId":3}'
{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":18,"nombre":"Yair","posicion":"DEFENSA","edad":21,"dorsal":3,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":19,"nombre":"Alan","posicion":"MEDIOCAMPISTA","edad":25,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":20,"nombre":"Brandon","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":22,"nombre":"Raul","posicion":"DELANTERO","edad":27,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":21,"nombre":"Cristian","posicion":"MEDIOCAMPISTA","edad":36,"dorsal":14,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":23,"nombre":"Jhonatan","posicion":"DELANTERO","edad":21,"dorsal":21,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":17,"nombre":"Alex","posicion":"PORTERO","edad":25,"dorsal":31,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### Setup: inscribir equipo 4 en Copa Cinco
```
curl -X POST http://localhost:57077/api/tournaments/4/teams -H 'Authorization: Bearer <token>' -d '{"teamId":4}'
{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":24,"nombre":"Carlos R","posicion":"DEFENSA","edad":23,"dorsal":2,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":25,"nombre":"Yair","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":8,"nombre":"Yeison","posicion":"MEDIOCAMPISTA","edad":38,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":26,"nombre":"Andres","posicion":"MEDIOCAMPISTA","edad":22,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":27,"nombre":"Jhon","posicion":"DELANTERO","edad":23,"dorsal":9,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":28,"nombre":"Alex","posicion":"DELANTERO","edad":34,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":9,"nombre":"Juan Carlos","posicion":"DELANTERO","edad":31,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### Setup: inscribir equipo 5 en Copa Cinco
```
curl -X POST http://localhost:57077/api/tournaments/4/teams -H 'Authorization: Bearer <token>' -d '{"teamId":5}'
{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: iniciar Copa Cinco
```
curl -X POST http://localhost:57077/api/tournaments/4/start -H 'Authorization: Bearer <token>'
{"id":4,"name":"Copa Cinco","format":"KNOCKOUT","status":"IN_PROGRESS","teamCount":5,"createdAt":"2026-11-03T19:12:09","startedAt":"2026-11-03T19:12:20.364"}
HTTP 200
```

### 14.4 bracket con 5 equipos (no potencia de 2)
```
curl -X POST http://localhost:57077/api/tournaments/4/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-06-01","time":"18:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"Los equipos que siguen en competencia (5) deben ser una potencia de 2 (2, 4, 8...) para formar las llaves: arma un partido de repechaje entre dos equipos para eliminar a uno."}
HTTP 400
```

### 14.4 crear partido de REPECHAJE 4 vs 5
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":4,"idEquipoVisitante":5,"fecha":"2027-04-01","hora":"15:00:00","fase":"REPECHAJE","idTorneo":4}'
{"id":17,"equipoLocal":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-04-01","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"REPECHAJE","idTorneo":4,"estado":"PROGRAMADO","indicadorRespuesta":"Success","mensaje":"Equipos, el partido ya fue programado"}
HTTP 200
```

### 14.4 bracket con repechaje sin finalizar
```
curl -X POST http://localhost:57077/api/tournaments/4/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-06-01","time":"18:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"Hay 1 partido(s) de repechaje sin finalizar: finalízalos antes de generar las llaves."}
HTTP 400
```

### 14.4 finalizar repechaje empatado sin penales
```
curl -X PUT http://localhost:57077/api/partidos/17/finalizar -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"Este partido no puede terminar empatado: registra los penales antes de finalizarlo."}
HTTP 400
```

### 14.4 penales del repechaje 2-3 (gana el 5)
```
curl -X PUT http://localhost:57077/api/partidos/17/penales -H 'Authorization: Bearer <token>' -d '{"penalesLocal":2,"penalesVisitante":3}'
{"id":17,"equipoLocal":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-04-01","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"REPECHAJE","idTorneo":4,"penalesLocal":2,"penalesVisitante":3,"estado":"EN_CURSO","indicadorRespuesta":"Success","mensaje":"Penales registrados."}
HTTP 200
```

### 14.4 finalizar repechaje con penales
```
curl -X PUT http://localhost:57077/api/partidos/17/finalizar -H 'Authorization: Bearer <token>'
{"id":17,"equipoLocal":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-04-01","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"REPECHAJE","idTorneo":4,"penalesLocal":2,"penalesVisitante":3,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.4 bracket con fecha de inicio igual a la del último partido
```
curl -X POST http://localhost:57077/api/tournaments/4/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-04-01","time":"18:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"La fecha de inicio de las llaves debe ser posterior a la del último partido del torneo (2027-04-01)."}
HTTP 400
```

### 14.4 cruces manuales incompletos
```
curl -X POST http://localhost:57077/api/tournaments/4/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-06-01","time":"18:00:00","pairings":[{"homeTeamId":1,"awayTeamId":2}]}'
{"indicadorRespuesta":"Error","mensaje":"Los cruces indicados deben cubrir a los 4 equipos que participan (2 cruces)."}
HTTP 400
```

### 14.4 cruces manuales con equipo repetido
```
curl -X POST http://localhost:57077/api/tournaments/4/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-06-01","time":"18:00:00","pairings":[{"homeTeamId":1,"awayTeamId":2},{"homeTeamId":2,"awayTeamId":3}]}'
{"indicadorRespuesta":"Error","mensaje":"La lista de cruces repite al equipo con Id 2."}
HTTP 400
```

### 14.4 cruces manuales con el equipo eliminado (4)
```
curl -X POST http://localhost:57077/api/tournaments/4/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-06-01","time":"18:00:00","pairings":[{"homeTeamId":1,"awayTeamId":2},{"homeTeamId":3,"awayTeamId":4}]}'
{"indicadorRespuesta":"Error","mensaje":"El equipo con Id 4 no participa en las llaves de este torneo."}
HTTP 400
```

### 14.4 cruces manuales válidos 1v2 y 5v3, sin tercer puesto
```
curl -X POST http://localhost:57077/api/tournaments/4/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-06-01","time":"18:00:00","thirdPlaceMatch":false,"pairings":[{"homeTeamId":1,"awayTeamId":2},{"homeTeamId":5,"awayTeamId":3}]}'
{"rounds":[{"round":1,"name":"Semifinales","ties":[{"id":9,"position":0,"homeTeam":{"id":1,"name":"Macalister"},"awayTeam":{"id":2,"name":"Socios.com"},"matchId":18,"matchStatus":"PROGRAMADO","homeGoals":0,"awayGoals":0,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-06-01","time":"18:00:00"},{"id":10,"position":1,"homeTeam":{"id":5,"name":"Real"},"awayTeam":{"id":3,"name":"Los de la T"},"matchId":19,"matchStatus":"PROGRAMADO","homeGoals":0,"awayGoals":0,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-06-01","time":"18:00:00"}]},{"round":2,"name":"Final","ties":[{"id":11,"position":0,"homeTeam":null,"awayTeam":null,"matchId":null,"matchStatus":null,"homeGoals":null,"awayGoals":null,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-06-08","time":"18:00:00"}]}],"thirdPlace":null,"champion":null}
HTTP 201
```

### Setup: crear Mundial Tres (GROUPS_KNOCKOUT)
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Mundial Tres","format":"GROUPS_KNOCKOUT"}'
{"id":5,"name":"Mundial Tres","format":"GROUPS_KNOCKOUT","status":"DRAFT","teamCount":0,"createdAt":"2026-11-03T19:14:33.645","startedAt":null}
HTTP 201
```

### Setup: inscribir equipo 1 en Mundial Tres
```
curl -X POST http://localhost:57077/api/tournaments/5/teams -H 'Authorization: Bearer <token>' -d '{"teamId":1}'
{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":13,"nombre":"Daniel","posicion":"DEFENSA","edad":35,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":14,"nombre":"Brayan","posicion":"DEFENSA","edad":24,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":16,"nombre":"Alex","posicion":"MEDIOCAMPISTA","edad":31,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":15,"nombre":"Duvan","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":11,"nombre":"James","posicion":"MEDIOCAMPISTA","edad":28,"dorsal":11,"imagenUrl":"http://localhost:57075/uploads/jugadores/c26d17af-6da2-4ba6-848a-f5fd39dd0620.png","costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":7,"nombre":"Jose","posicion":"PORTERO","edad":22,"dorsal":22,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":29,"nombre":"dilan","posicion":"DEFENSA","edad":23,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":35,"nombre":"ZZTest SinCosto","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### Setup: inscribir equipo 2 en Mundial Tres
```
curl -X POST http://localhost:57077/api/tournaments/5/teams -H 'Authorization: Bearer <token>' -d '{"teamId":2}'
{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":6,"nombre":"Didier","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":5,"nombre":"Yeiler","posicion":"DEFENSA","edad":21,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":2,"nombre":"James Rodríguez","posicion":"MEDIOCAMPISTA","edad":33,"dorsal":10,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":10,"nombre":"Erick","posicion":"DELANTERO","edad":34,"dorsal":15,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":4,"nombre":"Jhonatan","posicion":"DEFENSA","edad":33,"dorsal":21,"imagenUrl":null,"costo":100000.00,"indicadorRespuesta":null,"mensaje":null},{"id":34,"nombre":"ZZTest Costo","posicion":"DELANTERO","edad":22,"dorsal":97,"imagenUrl":null,"costo":1500.50,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### Setup: inscribir equipo 3 en Mundial Tres
```
curl -X POST http://localhost:57077/api/tournaments/5/teams -H 'Authorization: Bearer <token>' -d '{"teamId":3}'
{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":18,"nombre":"Yair","posicion":"DEFENSA","edad":21,"dorsal":3,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":19,"nombre":"Alan","posicion":"MEDIOCAMPISTA","edad":25,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":20,"nombre":"Brandon","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":22,"nombre":"Raul","posicion":"DELANTERO","edad":27,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":21,"nombre":"Cristian","posicion":"MEDIOCAMPISTA","edad":36,"dorsal":14,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":23,"nombre":"Jhonatan","posicion":"DELANTERO","edad":21,"dorsal":21,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":17,"nombre":"Alex","posicion":"PORTERO","edad":25,"dorsal":31,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]}
HTTP 201
```

### Setup: iniciar Mundial Tres
```
curl -X POST http://localhost:57077/api/tournaments/5/start -H 'Authorization: Bearer <token>'
{"id":5,"name":"Mundial Tres","format":"GROUPS_KNOCKOUT","status":"IN_PROGRESS","teamCount":3,"createdAt":"2026-11-03T19:14:34","startedAt":"2026-11-03T19:14:41.291"}
HTTP 200
```

### 14.7 grupos con 3 equipos y 2 grupos
```
curl -X POST http://localhost:57077/api/tournaments/5/groups -H 'Authorization: Bearer <token>' -d '{"groupCount":2,"qualifiersPerGroup":1,"startDate":"2027-02-06","time":"15:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"Cada grupo necesita al menos 2 equipos: con 3 equipos inscritos no se pueden formar 2 grupos."}
HTTP 400
```

### Setup: crear Mundial Cuatro (GROUPS_KNOCKOUT)
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Mundial Cuatro","format":"GROUPS_KNOCKOUT"}'
{"id":6,"name":"Mundial Cuatro","format":"GROUPS_KNOCKOUT","status":"DRAFT","teamCount":0,"createdAt":"2026-11-03T19:14:44.165","startedAt":null}
HTTP 201
```

### Setup: inscribir equipo 6 en Mundial Cuatro
```
curl -X POST http://localhost:57077/api/tournaments/6/teams -H 'Authorization: Bearer <token>' -d '{"teamId":6}'
{"id":6,"nombre":"Test Team 1","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 7 en Mundial Cuatro
```
curl -X POST http://localhost:57077/api/tournaments/6/teams -H 'Authorization: Bearer <token>' -d '{"teamId":7}'
{"id":7,"nombre":"Test Team 2","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 8 en Mundial Cuatro
```
curl -X POST http://localhost:57077/api/tournaments/6/teams -H 'Authorization: Bearer <token>' -d '{"teamId":8}'
{"id":8,"nombre":"Test Team 3","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 9 en Mundial Cuatro
```
curl -X POST http://localhost:57077/api/tournaments/6/teams -H 'Authorization: Bearer <token>' -d '{"teamId":9}'
{"id":9,"nombre":"Test Team 4","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: iniciar Mundial Cuatro
```
curl -X POST http://localhost:57077/api/tournaments/6/start -H 'Authorization: Bearer <token>'
{"id":6,"name":"Mundial Cuatro","format":"GROUPS_KNOCKOUT","status":"IN_PROGRESS","teamCount":4,"createdAt":"2026-11-03T19:14:44","startedAt":"2026-11-03T19:14:52.27"}
HTTP 200
```

### 14.7 clasificados 2 con grupos de 2 equipos
```
curl -X POST http://localhost:57077/api/tournaments/6/groups -H 'Authorization: Bearer <token>' -d '{"groupCount":2,"qualifiersPerGroup":2,"startDate":"2027-02-06","time":"15:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"Los clasificados por grupo (2) deben ser menos que los equipos del grupo más pequeño (2)."}
HTTP 400
```

### 14.7 clasificados 3
```
curl -X POST http://localhost:57077/api/tournaments/6/groups -H 'Authorization: Bearer <token>' -d '{"groupCount":2,"qualifiersPerGroup":3,"startDate":"2027-02-06","time":"15:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"Los clasificados por grupo deben ser 1 o 2."}
HTTP 400
```

### 14.7 cantidad de grupos 3 (no potencia de 2)
```
curl -X POST http://localhost:57077/api/tournaments/6/groups -H 'Authorization: Bearer <token>' -d '{"groupCount":3,"qualifiersPerGroup":1,"startDate":"2027-02-06","time":"15:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"La cantidad de grupos debe ser una potencia de 2 (2, 4, 8...)."}
HTTP 400
```

### 14.7 grupos sin datos obligatorios
```
curl -X POST http://localhost:57077/api/tournaments/6/groups -H 'Authorization: Bearer <token>' -d '{}'
{"groupCount":"La cantidad de grupos es obligatoria.","qualifiersPerGroup":"Los clasificados por grupo son obligatorios.","time":"La hora de los partidos es obligatoria.","startDate":"La fecha de inicio es obligatoria."}
HTTP 400
```

### 14.7 composición manual que omite equipos
```
curl -X POST http://localhost:57077/api/tournaments/6/groups -H 'Authorization: Bearer <token>' -d '{"groupCount":2,"qualifiersPerGroup":1,"startDate":"2027-02-06","time":"15:00:00","groups":[{"teamIds":[6,7]},{"teamIds":[8]}]}'
{"indicadorRespuesta":"Error","mensaje":"La composición debe incluir a todos los equipos inscritos, cada uno una sola vez."}
HTTP 400
```

### 14.7 grupos con token DT
```
curl -X POST http://localhost:57077/api/tournaments/6/groups -H 'Authorization: Bearer <token>' -d '{"groupCount":2,"qualifiersPerGroup":1,"startDate":"2027-02-06","time":"15:00:00"}'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```

### 14.7 grupos sin token
```
curl -X POST http://localhost:57077/api/tournaments/6/groups -d '{"groupCount":2,"qualifiersPerGroup":1,"startDate":"2027-02-06","time":"15:00:00"}'
{"indicadorRespuesta":"No autorizado","mensaje":"Debes iniciar sesión para realizar esta acción."}
HTTP 401
```

### 14.7 grupos en un torneo KNOCKOUT (Copa Cinco)
```
curl -X POST http://localhost:57077/api/tournaments/4/groups -H 'Authorization: Bearer <token>' -d '{"groupCount":2,"qualifiersPerGroup":1,"startDate":"2027-02-06","time":"15:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"Este torneo no tiene fase de grupos: solo los torneos GROUPS_KNOCKOUT la tienen."}
HTTP 400
```

### 14.7 choque de fecha (equipos 6 a 9 juegan el 2027-05-01 en Copa Ocho)
```
curl -X POST http://localhost:57077/api/tournaments/6/groups -H 'Authorization: Bearer <token>' -d '{"groupCount":2,"qualifiersPerGroup":1,"startDate":"2027-05-01","time":"15:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"El equipo Test Team 1 ya tiene un partido el 2027-05-01: elige otra fecha de inicio o intervalo."}
HTTP 400
```

### 14.7 GET grupos de torneo inexistente
```
curl -X GET http://localhost:57077/api/tournaments/999/groups
{"indicadorRespuesta":"Sin Registros","mensaje":"Fuera de juego! El torneo con Id 999 no existe."}
HTTP 404
```

### Setup: crear Mundial Ocho (GROUPS_KNOCKOUT)
```
curl -X POST http://localhost:57077/api/tournaments -H 'Authorization: Bearer <token>' -d '{"name":"Mundial Ocho","format":"GROUPS_KNOCKOUT"}'
{"id":7,"name":"Mundial Ocho","format":"GROUPS_KNOCKOUT","status":"DRAFT","teamCount":0,"createdAt":"2026-11-03T19:15:58.832","startedAt":null}
HTTP 201
```

### Setup: inscribir equipo 6 en Mundial Ocho
```
curl -X POST http://localhost:57077/api/tournaments/7/teams -H 'Authorization: Bearer <token>' -d '{"teamId":6}'
{"id":6,"nombre":"Test Team 1","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 7 en Mundial Ocho
```
curl -X POST http://localhost:57077/api/tournaments/7/teams -H 'Authorization: Bearer <token>' -d '{"teamId":7}'
{"id":7,"nombre":"Test Team 2","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 8 en Mundial Ocho
```
curl -X POST http://localhost:57077/api/tournaments/7/teams -H 'Authorization: Bearer <token>' -d '{"teamId":8}'
{"id":8,"nombre":"Test Team 3","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 9 en Mundial Ocho
```
curl -X POST http://localhost:57077/api/tournaments/7/teams -H 'Authorization: Bearer <token>' -d '{"teamId":9}'
{"id":9,"nombre":"Test Team 4","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 10 en Mundial Ocho
```
curl -X POST http://localhost:57077/api/tournaments/7/teams -H 'Authorization: Bearer <token>' -d '{"teamId":10}'
{"id":10,"nombre":"Test Team 5","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 11 en Mundial Ocho
```
curl -X POST http://localhost:57077/api/tournaments/7/teams -H 'Authorization: Bearer <token>' -d '{"teamId":11}'
{"id":11,"nombre":"Test Team 6","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 12 en Mundial Ocho
```
curl -X POST http://localhost:57077/api/tournaments/7/teams -H 'Authorization: Bearer <token>' -d '{"teamId":12}'
{"id":12,"nombre":"Test Team 7","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: inscribir equipo 13 en Mundial Ocho
```
curl -X POST http://localhost:57077/api/tournaments/7/teams -H 'Authorization: Bearer <token>' -d '{"teamId":13}'
{"id":13,"nombre":"Test Team 8","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success"}
HTTP 201
```

### Setup: iniciar Mundial Ocho
```
curl -X POST http://localhost:57077/api/tournaments/7/start -H 'Authorization: Bearer <token>'
{"id":7,"name":"Mundial Ocho","format":"GROUPS_KNOCKOUT","status":"IN_PROGRESS","teamCount":8,"createdAt":"2026-11-03T19:15:59","startedAt":"2026-11-03T19:16:19.47"}
HTTP 200
```

### 14.8 llaves sin grupos formados
```
curl -X POST http://localhost:57077/api/tournaments/7/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-03-20","time":"18:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"Este torneo todavía no tiene grupos: forma los grupos antes de generar las llaves."}
HTTP 400
```

### 14.7 formar 2 grupos con 8 equipos (sorteo), 2 clasificados, 2027-02-06 15:00
```
curl -X POST http://localhost:57077/api/tournaments/7/groups -H 'Authorization: Bearer <token>' -d '{"groupCount":2,"qualifiersPerGroup":2,"startDate":"2027-02-06","time":"15:00:00"}'
[{"id":1,"name":"A","teams":[{"id":6,"name":"Test Team 1"},{"id":8,"name":"Test Team 3"},{"id":11,"name":"Test Team 6"},{"id":13,"name":"Test Team 8"}],"qualifiersCount":2,"standings":[{"position":1,"teamId":6,"teamName":"Test Team 1","played":0,"won":0,"drawn":0,"lost":0,"goalsFor":0,"goalsAgainst":0,"goalDifference":0,"points":0,"fairPlayPoints":0},{"position":2,"teamId":8,"teamName":"Test Team 3","played":0,"won":0,"drawn":0,"lost":0,"goalsFor":0,"goalsAgainst":0,"goalDifference":0,"points":0,"fairPlayPoints":0},{"position":3,"teamId":11,"teamName":"Test Team 6","played":0,"won":0,"drawn":0,"lost":0,"goalsFor":0,"goalsAgainst":0,"goalDifference":0,"points":0,"fairPlayPoints":0},{"position":4,"teamId":13,"teamName":"Test Team 8","played":0,"won":0,"drawn":0,"lost":0,"goalsFor":0,"goalsAgainst":0,"goalDifference":0,"points":0,"fairPlayPoints":0}]},{"id":2,"name":"B","teams":[{"id":7,"name":"Test Team 2"},{"id":9,"name":"Test Team 4"},{"id":10,"name":"Test Team 5"},{"id":12,"name":"Test Team 7"}],"qualifiersCount":2,"standings":[{"position":1,"teamId":7,"teamName":"Test Team 2","played":0,"won":0,"drawn":0,"lost":0,"goalsFor":0,"goalsAgainst":0,"goalDifference":0,"points":0,"fairPlayPoints":0},{"position":2,"teamId":9,"teamName":"Test Team 4","played":0,"won":0,"drawn":0,"lost":0,"goalsFor":0,"goalsAgainst":0,"goalDifference":0,"points":0,"fairPlayPoints":0},{"position":3,"teamId":10,"teamName":"Test Team 5","played":0,"won":0,"drawn":0,"lost":0,"goalsFor":0,"goalsAgainst":0,"goalDifference":0,"points":0,"fairPlayPoints":0},{"position":4,"teamId":12,"teamName":"Test Team 7","played":0,"won":0,"drawn":0,"lost":0,"goalsFor":0,"goalsAgainst":0,"goalDifference":0,"points":0,"fairPlayPoints":0}]}]
HTTP 201
```

### 14.7 formar grupos de nuevo
```
curl -X POST http://localhost:57077/api/tournaments/7/groups -H 'Authorization: Bearer <token>' -d '{"groupCount":2,"qualifiersPerGroup":2,"startDate":"2027-02-06","time":"15:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"Los grupos de este torneo ya fueron formados."}
HTTP 400
```

### 14.8 llaves con partidos de grupo sin finalizar
```
curl -X POST http://localhost:57077/api/tournaments/7/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-03-20","time":"18:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"Faltan 12 partido(s) de la fase de grupos por finalizar."}
HTTP 400
```

### 14.7 eliminar el partido aplazado
```
curl -X DELETE http://localhost:57077/api/partidos/20 -H 'Authorization: Bearer <token>'

HTTP 204
```

### 14.7 armar a mano 13 vs 6 (fase FASE_DE_GRUPOS) en el torneo
```
curl -X POST http://localhost:57077/api/partidos -H 'Authorization: Bearer <token>' -d '{"idEquipoLocal":13,"idEquipoVisitante":6,"fecha":"2027-02-27","hora":"15:00:00","fase":"FASE_DE_GRUPOS","idTorneo":7}'
{"id":32,"equipoLocal":{"id":13,"nombre":"Test Team 8","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":6,"nombre":"Test Team 1","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-02-27","hora":"15:00:00","golesLocal":0,"golesVisitante":0,"fase":"FASE_DE_GRUPOS","idTorneo":7,"estado":"PROGRAMADO","indicadorRespuesta":"Success","mensaje":"Equipos, el partido ya fue programado"}
HTTP 200
```

### 14.8 llaves con 1 partido de grupo manual aún pendiente (todos pendientes)
```
curl -X POST http://localhost:57077/api/tournaments/7/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-03-20","time":"18:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"Faltan 12 partido(s) de la fase de grupos por finalizar."}
HTTP 400
```

### 14.8 llaves con cruces manuales (no permitido)
```
curl -X POST http://localhost:57077/api/tournaments/7/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-03-20","time":"18:00:00","pairings":[{"homeTeamId":6,"awayTeamId":9},{"homeTeamId":7,"awayTeamId":8}]}'
{"indicadorRespuesta":"Error","mensaje":"Los cruces de la eliminatoria de un torneo con grupos los define la clasificación de los grupos: no se pueden indicar a mano."}
HTTP 400
```

### 14.8 llaves con fecha de inicio no posterior al último partido
```
curl -X POST http://localhost:57077/api/tournaments/7/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-02-27","time":"18:00:00"}'
{"indicadorRespuesta":"Error","mensaje":"La fecha de inicio de las llaves debe ser posterior a la del último partido del torneo (2027-02-27)."}
HTTP 400
```

### 14.8 generar llaves con los clasificados
```
curl -X POST http://localhost:57077/api/tournaments/7/bracket -H 'Authorization: Bearer <token>' -d '{"startDate":"2027-03-20","time":"18:00:00"}'
{"rounds":[{"round":1,"name":"Semifinales","ties":[{"id":12,"position":0,"homeTeam":{"id":6,"name":"Test Team 1"},"awayTeam":{"id":9,"name":"Test Team 4"},"matchId":33,"matchStatus":"PROGRAMADO","homeGoals":0,"awayGoals":0,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-03-20","time":"18:00:00"},{"id":13,"position":1,"homeTeam":{"id":7,"name":"Test Team 2"},"awayTeam":{"id":8,"name":"Test Team 3"},"matchId":34,"matchStatus":"PROGRAMADO","homeGoals":0,"awayGoals":0,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-03-20","time":"18:00:00"}]},{"round":2,"name":"Final","ties":[{"id":14,"position":0,"homeTeam":null,"awayTeam":null,"matchId":null,"matchStatus":null,"homeGoals":null,"awayGoals":null,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-03-27","time":"18:00:00"}]}],"thirdPlace":{"id":15,"position":0,"homeTeam":null,"awayTeam":null,"matchId":null,"matchStatus":null,"homeGoals":null,"awayGoals":null,"homePenalties":null,"awayPenalties":null,"winnerTeamId":null,"date":"2027-03-27","time":"18:00:00"},"champion":null}
HTTP 201
```

### 14.8 finalizar semifinal 0
```
curl -X PUT http://localhost:57077/api/partidos/33/finalizar -H 'Authorization: Bearer <token>'
{"id":33,"equipoLocal":{"id":6,"nombre":"Test Team 1","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":9,"nombre":"Test Team 4","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-03-20","hora":"18:00:00","golesLocal":1,"golesVisitante":0,"fase":"ELIMINACION_DIRECTA","idTorneo":7,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.8 finalizar semifinal 1
```
curl -X PUT http://localhost:57077/api/partidos/34/finalizar -H 'Authorization: Bearer <token>'
{"id":34,"equipoLocal":{"id":7,"nombre":"Test Team 2","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":8,"nombre":"Test Team 3","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-03-20","hora":"18:00:00","golesLocal":0,"golesVisitante":2,"fase":"ELIMINACION_DIRECTA","idTorneo":7,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.8 tercer puesto empatado sin penales
```
curl -X PUT http://localhost:57077/api/partidos/36/finalizar -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Error","mensaje":"Este partido no puede terminar empatado: registra los penales antes de finalizarlo."}
HTTP 400
```

### 14.8 penales del tercer puesto 4-2
```
curl -X PUT http://localhost:57077/api/partidos/36/penales -H 'Authorization: Bearer <token>' -d '{"penalesLocal":4,"penalesVisitante":2}'
{"id":36,"equipoLocal":{"id":9,"nombre":"Test Team 4","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":7,"nombre":"Test Team 2","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-03-27","hora":"18:00:00","golesLocal":1,"golesVisitante":1,"fase":"ELIMINACION_DIRECTA","idTorneo":7,"penalesLocal":4,"penalesVisitante":2,"estado":"EN_CURSO","indicadorRespuesta":"Success","mensaje":"Penales registrados."}
HTTP 200
```

### 14.8 finalizar tercer puesto
```
curl -X PUT http://localhost:57077/api/partidos/36/finalizar -H 'Authorization: Bearer <token>'
{"id":36,"equipoLocal":{"id":9,"nombre":"Test Team 4","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":7,"nombre":"Test Team 2","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-03-27","hora":"18:00:00","golesLocal":1,"golesVisitante":1,"fase":"ELIMINACION_DIRECTA","idTorneo":7,"penalesLocal":4,"penalesVisitante":2,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.8 finalizar la final
```
curl -X PUT http://localhost:57077/api/partidos/35/finalizar -H 'Authorization: Bearer <token>'
{"id":35,"equipoLocal":{"id":6,"nombre":"Test Team 1","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"equipoVisitante":{"id":8,"nombre":"Test Team 3","directorTecnico":"DT Test","titulos":0,"tipoClasificacion":"ELIMINATORIA"},"fecha":"2027-03-27","hora":"18:00:00","golesLocal":3,"golesVisitante":1,"fase":"FINAL","idTorneo":7,"estado":"FINALIZADO","indicadorRespuesta":"Success","mensaje":"Partido finalizado."}
HTTP 200
```

### 14.8 finalizar el torneo con campeón
```
curl -X POST http://localhost:57077/api/tournaments/7/finish -H 'Authorization: Bearer <token>'
{"id":7,"name":"Mundial Ocho","format":"GROUPS_KNOCKOUT","status":"FINISHED","teamCount":8,"createdAt":"2026-11-03T19:15:59","startedAt":"2026-11-03T19:16:19"}
HTTP 200
```

