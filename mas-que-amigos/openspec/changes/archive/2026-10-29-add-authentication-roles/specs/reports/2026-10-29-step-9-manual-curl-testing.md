# Reporte del Paso 9 - Pruebas manuales con curl

- Fecha: 2026-10-29
- Change: add-authentication-roles
- Agente: Claude (Sonnet 5.5)

## Entorno
- Backend local (`./mvnw clean spring-boot:run`) en el puerto 57076, contra el MySQL de `TorneoContainer` (esquema `masqueamigos`, puerto 3308), con `APP_SECURITY_JWT_SECRET`, `APP_SECURITY_ADMIN_USERNAME` y `APP_SECURITY_ADMIN_PASSWORD` tomadas del `.env` (nunca impresas ni registradas aquí).
- Para 9.3 se reinició con `APP_SECURITY_JWT_TTL=PT10S`.
- Los tokens se muestran como `<token>` y las contraseñas como `***`.

## Resultado por tarea
- 9.1 OK: Flyway ya estaba en la versión 8 (sin migración pendiente) y `AdminSeeder` registró `Initial admin 'admin' created.`; las consultas públicas responden 200.
- 9.2 OK: login correcto 200, otras mayúsculas 200, contraseña incorrecta y usuario inexistente 401 con el mismo mensaje, campos vacíos 400, `me` y `refresh` 200 con token y 401 sin él.
- 9.3 OK: con `PT10S` el token vence a los 10 s (401 "Tu sesión expiró"), un token vencido no se renueva (401) y renovar a tiempo extiende la sesión. Se ejecutó después de 9.4 a 9.6 porque requiere reiniciar con otra vigencia.
- 9.4 OK: todos los casos de gestión de usuarios se comportan como las specs; ninguna respuesta incluye contraseña ni hash.
- 9.5 OK: visitante, DT con equipo, DT sin equipo y ADMIN según la matriz; la clave fija `X-Director-Tecnico-Key` ya no autoriza (401) y `/api/director-tecnico/validar` ya no existe (404).
- 9.6 OK: `/v3/api-docs` (200) declara `bearerAuth`, contiene `/api/auth/login`, `/refresh`, `/me`, `/api/users` y `/api/users/{id}/team`, y no contiene `directorTecnicoKey` ni `/api/director-tecnico/validar`; `/swagger-ui/index.html` responde 200 sin sesión.

## Observaciones
- `PUT /api/users/{id}/team` con una cuenta inexistente responde 404 con el cuerpo `{"error": "..."}`, no con `{indicadorRespuesta, mensaje}`. Es la convención ya existente para los `*NotFoundException` (`JugadorNotFoundException`, `TransferNotFoundException`), no un defecto de este change; el front debe leer `error` en los 404.
- En 9.5, las dos primeras filas de "DT: confirmar" y "DT: rechazar" usaron por un error de mi script el traspaso 5 (no pendiente) en lugar del 11; el 403 lo responde el filtro de seguridad antes de llegar al servicio, así que no hubo efecto. Se repitieron con el id correcto (11) y dieron 403.
- Se detuvo el contenedor `mas-que-amigos-backend-1` (imagen anterior al change, sin login) durante las pruebas y se volvió a iniciar al terminar; no se reconstruyó ninguna imagen.

## Estado de la base de datos
- Baseline previo a las pruebas: jugador 29 · equipos 5 · alineacion 21 · estadistica 2 · player_transfer 6 · app_user 1 · Flyway 8.
- La cuenta `qa-admin` (ADMIN, creada el 2026-10-21 por una ejecución local del usuario, posterior al reporte del paso 8, donde `app_user` estaba vacía) existía antes de estas pruebas; por decisión del usuario se conserva y el baseline se toma con `app_user = 1`.
- Datos creados por las pruebas: cuentas `admin` (sembrada), `zz-dt-uno`, `zz-admin-dos` y `zz-dt-equipo`; traspasos 11 (rechazado) y 12 (confirmado, movió al jugador de prueba 34 al equipo 3); el jugador `ZZ Admin Prueba` (id 39) se creó y se borró por la API.
- Restauración: se eliminaron los traspasos 11 y 12, se devolvió el jugador 34 al equipo 2 y se eliminaron las cuentas creadas, conservando `qa-admin`.
- Validación posterior: jugador 29 · equipos 5 · alineacion 21 · estadistica 2 · player_transfer 6 · app_user 1 · Flyway 8. Estado restaurado: Sí.
- Backend de prueba detenido; el puerto 57076 quedó libre.

## Comandos y respuestas (en el orden de ejecución)

## 9.2 Login, me y refresh

### login correcto
```
curl -X POST http://localhost:57076/api/auth/login -d '{"username":"admin","password":"***"}'
{"token":"<token>","tokenType":"Bearer","expiresInSeconds":180,"user":{"id":2,"username":"admin","role":"ADMIN","teamId":null,"teamName":null}}
HTTP 200
```

### login otras mayusculas
```
curl -X POST http://localhost:57076/api/auth/login -d '{"username":"ADMIN","password":"***"}'
{"token":"<token>","tokenType":"Bearer","expiresInSeconds":180,"user":{"id":2,"username":"admin","role":"ADMIN","teamId":null,"teamName":null}}
HTTP 200
```

### contraseña incorrecta
```
curl -X POST http://localhost:57076/api/auth/login -d '{"username":"admin","password":"***"}'
{"indicadorRespuesta":"No autorizado","mensaje":"Usuario o contraseña incorrectos"}
HTTP 401
```

### usuario inexistente
```
curl -X POST http://localhost:57076/api/auth/login -d '{"username":"nadie-existe","password":"***"}'
{"indicadorRespuesta":"No autorizado","mensaje":"Usuario o contraseña incorrectos"}
HTTP 401
```

### campos vacios
```
curl -X POST http://localhost:57076/api/auth/login -d '{"username":"","password":"***"}'
{"password":"La contraseña es obligatoria.","username":"El usuario es obligatorio."}
HTTP 400
```

### me con token
```
curl -X GET http://localhost:57076/api/auth/me -H 'Authorization: Bearer <token>'
{"id":2,"username":"admin","role":"ADMIN","teamId":null,"teamName":null}
HTTP 200
```

### me sin token
```
curl -X GET http://localhost:57076/api/auth/me
{"indicadorRespuesta":"No autorizado","mensaje":"Debes iniciar sesión para realizar esta acción."}
HTTP 401
```

### refresh con token
```
curl -X POST http://localhost:57076/api/auth/refresh -H 'Authorization: Bearer <token>'
{"token":"<token>","tokenType":"Bearer","expiresInSeconds":180,"user":{"id":2,"username":"admin","role":"ADMIN","teamId":null,"teamName":null}}
HTTP 200
```

### refresh sin token
```
curl -X POST http://localhost:57076/api/auth/refresh
{"indicadorRespuesta":"No autorizado","mensaje":"Debes iniciar sesión para realizar esta acción."}
HTTP 401
```


## 9.4 Gestion de usuarios (ADMIN)

### crear DT por defecto (sin rol)
```
curl -X POST http://localhost:57076/api/users -H 'Authorization: Bearer <token>' -d '{"username":"zz-dt-uno","password":"***"}'
{"id":3,"username":"zz-dt-uno","role":"DT","teamId":null,"teamName":null,"createdAt":"2026-10-29T11:29:54.963"}
HTTP 201
```

### crear otro ADMIN
```
curl -X POST http://localhost:57076/api/users -H 'Authorization: Bearer <token>' -d '{"username":"zz-admin-dos","password":"***","role":"ADMIN"}'
{"id":4,"username":"zz-admin-dos","role":"ADMIN","teamId":null,"teamName":null,"createdAt":"2026-10-29T11:29:56.275"}
HTTP 201
```

### rol invalido
```
curl -X POST http://localhost:57076/api/users -H 'Authorization: Bearer <token>' -d '{"username":"zz-rol-malo","password":"***","role":"SUPERUSER"}'
{"indicadorRespuesta":"Error","mensaje":"El rol 'SUPERUSER' no es válido. Valores permitidos: ADMIN, DT."}
HTTP 400
```

### usuario duplicado otras mayusculas
```
curl -X POST http://localhost:57076/api/users -H 'Authorization: Bearer <token>' -d '{"username":"ZZ-DT-UNO","password":"***"}'
{"indicadorRespuesta":"Error","mensaje":"El usuario 'zz-dt-uno' ya existe."}
HTTP 400
```

### contraseña corta
```
curl -X POST http://localhost:57076/api/users -H 'Authorization: Bearer <token>' -d '{"username":"zz-corta","password":"***"}'
{"password":"La contraseña debe tener al menos 8 caracteres."}
HTTP 400
```

### DT con equipo 1
```
curl -X POST http://localhost:57076/api/users -H 'Authorization: Bearer <token>' -d '{"username":"zz-dt-equipo","password":"***","teamId":1}'
{"id":5,"username":"zz-dt-equipo","role":"DT","teamId":1,"teamName":"Macalister","createdAt":"2026-10-29T11:30:01.585"}
HTTP 201
```

### equipo ya asignado (1)
```
curl -X POST http://localhost:57076/api/users -H 'Authorization: Bearer <token>' -d '{"username":"zz-dt-otro","password":"***","teamId":1}'
{"indicadorRespuesta":"Error","mensaje":"El equipo 'Macalister' ya tiene un director técnico con cuenta asignada."}
HTTP 400
```

### equipo inexistente (999)
```
curl -X POST http://localhost:57076/api/users -H 'Authorization: Bearer <token>' -d '{"username":"zz-dt-fantasma","password":"***","teamId":999}'
{"indicadorRespuesta":"Error","mensaje":"Fuera de juego! No se ha seleccionado un o unos equipos existentes."}
HTTP 400
```

### ADMIN con equipo
```
curl -X POST http://localhost:57076/api/users -H 'Authorization: Bearer <token>' -d '{"username":"zz-admin-eq","password":"***","role":"ADMIN","teamId":2}'
{"indicadorRespuesta":"Error","mensaje":"Un administrador no puede tener equipo asignado."}
HTTP 400
```

### listar usuarios
```
curl -X GET http://localhost:57076/api/users -H 'Authorization: Bearer <token>'
[{"id":2,"username":"admin","role":"ADMIN","teamId":null,"teamName":null,"createdAt":"2026-10-29T11:26:52"},{"id":1,"username":"qa-admin","role":"ADMIN","teamId":null,"teamName":null,"createdAt":"2026-10-21T14:51:39"},{"id":4,"username":"zz-admin-dos","role":"ADMIN","teamId":null,"teamName":null,"createdAt":"2026-10-29T11:29:56"},{"id":5,"username":"zz-dt-equipo","role":"DT","teamId":1,"teamName":"Macalister","createdAt":"2026-10-29T11:30:02"},{"id":3,"username":"zz-dt-uno","role":"DT","teamId":null,"teamName":null,"createdAt":"2026-10-29T11:29:55"}]
HTTP 200
```

### asignar equipo 2 a dt-uno
```
curl -X PUT http://localhost:57076/api/users/3/team -H 'Authorization: Bearer <token>' -d '{"teamId":2}'
{"id":3,"username":"zz-dt-uno","role":"DT","teamId":2,"teamName":"Socios.com","createdAt":"2026-10-29T11:29:55"}
HTTP 200
```

### cambiar a equipo 3 (libera el 2)
```
curl -X PUT http://localhost:57076/api/users/3/team -H 'Authorization: Bearer <token>' -d '{"teamId":3}'
{"id":3,"username":"zz-dt-uno","role":"DT","teamId":3,"teamName":"Los de la T","createdAt":"2026-10-29T11:29:55"}
HTTP 200
```

### quitar equipo (teamId nulo)
```
curl -X PUT http://localhost:57076/api/users/3/team -H 'Authorization: Bearer <token>' -d '{"teamId":null}'
{"id":3,"username":"zz-dt-uno","role":"DT","teamId":null,"teamName":null,"createdAt":"2026-10-29T11:29:55"}
HTTP 200
```

### asignar equipo a un ADMIN
```
curl -X PUT http://localhost:57076/api/users/4/team -H 'Authorization: Bearer <token>' -d '{"teamId":2}'
{"indicadorRespuesta":"Error","mensaje":"Un administrador no puede tener equipo asignado."}
HTTP 400
```

### asignar a cuenta inexistente
```
curl -X PUT http://localhost:57076/api/users/99999/team -H 'Authorization: Bearer <token>' -d '{"teamId":2}'
{"error":"Fuera de juego! No se encontró la cuenta con Id 99999."}
HTTP 404
```


## 9.5 Permisos por rol

#### Visitante

### V: GET equipos
```
curl -X GET http://localhost:57076/api/equipos
[{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":13,"nombre":"Daniel","posicion":"DEFENSA","edad":35,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":14,"nombre":"Brayan","posicion":"DEFENSA","edad":24,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":16,"nombre":"Alex","posicion":"MEDIOCAMPISTA","edad":31,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":15,"nombre":"Duvan","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":2,"nombre":"James Rodríguez","posicion":"MEDIOCAMPISTA","edad":33,"dorsal":10,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":11,"nombre":"James","posicion":"MEDIOCAMPISTA","edad":28,"dorsal":11,"imagenUrl":"http://localhost:57075/uploads/jugadores/c26d17af-6da2-4ba6-848a-f5fd39dd0620.png","costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":7,"nombre":"Jose","posicion":"PORTERO","edad":22,"dorsal":22,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":29,"nombre":"dilan","posicion":"DEFENSA","edad":23,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":35,"nombre":"ZZTest SinCosto","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]},{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":6,"nombre":"Didier","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":5,"nombre":"Yeiler","posicion":"DEFENSA","edad":21,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":10,"nombre":"Erick","posicion":"DELANTERO","edad":34,"dorsal":15,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":4,"nombre":"Jhonatan","posicion":"DEFENSA","edad":33,"dorsal":21,"imagenUrl":null,"costo":100000.00,"indicadorRespuesta":null,"mensaje":null},{"id":34,"nombre":"ZZTest Costo","posicion":"DELANTERO","edad":22,"dorsal":97,"imagenUrl":null,"costo":1500.50,"indicadorRespuesta":null,"mensaje":null}]},{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":18,"nombre":"Yair","posicion":"DEFENSA","edad":21,"dorsal":3,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":19,"nombre":"Alan","posicion":"MEDIOCAMPISTA","edad":25,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":20,"nombre":"Brandon","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":22,"nombre":"Raul","posicion":"DELANTERO","edad":27,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":21,"nombre":"Cristian","posicion":"MEDIOCAMPISTA","edad":36,"dorsal":14,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":23,"nombre":"Jhonatan","posicion":"DELANTERO","edad":21,"dorsal":21,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":17,"nombre":"Alex","posicion":"PORTERO","edad":25,"dorsal":31,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]},{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":24,"nombre":"Carlos R","posicion":"DEFENSA","edad":23,"dorsal":2,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":25,"nombre":"Yair","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":8,"nombre":"Yeison","posicion":"MEDIOCAMPISTA","edad":38,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":26,"nombre":"Andres","posicion":"MEDIOCAMPISTA","edad":22,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":27,"nombre":"Jhon","posicion":"DELANTERO","edad":23,"dorsal":9,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":28,"nombre":"Alex","posicion":"DELANTERO","edad":34,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":9,"nombre":"Juan Carlos","posicion":"DELANTERO","edad":31,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]},{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success"}]
HTTP 200
```

### V: GET jugadores
```
curl -X GET http://localhost:57076/api/jugadores
[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":1,"equipo":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"}},{"id":2,"nombre":"James Rodríguez","posicion":"MEDIOCAMPISTA","edad":33,"dorsal":10,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":1,"equipo":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"}},{"id":4,"nombre":"Jhonatan","posicion":"DEFENSA","edad":33,"dorsal":21,"imagenUrl":null,"costo":100000.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":2,"equipo":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"}},{"id":5,"nombre":"Yeiler","posicion":"DEFENSA","edad":21,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":2,"equipo":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"}},{"id":6,"nombre":"Didier","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":2,"equipo":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"}},{"id":7,"nombre":"Jose","posicion":"PORTERO","edad":22,"dorsal":22,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":1,"equipo":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"}},{"id":8,"nombre":"Yeison","posicion":"MEDIOCAMPISTA","edad":38,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":4,"equipo":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"}},{"id":9,"nombre":"Juan Carlos","posicion":"DELANTERO","edad":31,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":4,"equipo":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"}},{"id":10,"nombre":"Erick","posicion":"DELANTERO","edad":34,"dorsal":15,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":2,"equipo":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"}},{"id":11,"nombre":"James","posicion":"MEDIOCAMPISTA","edad":28,"dorsal":11,"imagenUrl":"http://localhost:57075/uploads/jugadores/c26d17af-6da2-4ba6-848a-f5fd39dd0620.png","costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":1,"equipo":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"}},{"id":13,"nombre":"Daniel","posicion":"DEFENSA","edad":35,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":1,"equipo":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"}},{"id":14,"nombre":"Brayan","posicion":"DEFENSA","edad":24,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":1,"equipo":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"}},{"id":15,"nombre":"Duvan","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":1,"equipo":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"}},{"id":16,"nombre":"Alex","posicion":"MEDIOCAMPISTA","edad":31,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":1,"equipo":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"}},{"id":17,"nombre":"Alex","posicion":"PORTERO","edad":25,"dorsal":31,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":3,"equipo":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"}},{"id":18,"nombre":"Yair","posicion":"DEFENSA","edad":21,"dorsal":3,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":3,"equipo":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"}},{"id":19,"nombre":"Alan","posicion":"MEDIOCAMPISTA","edad":25,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":3,"equipo":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"}},{"id":20,"nombre":"Brandon","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":3,"equipo":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"}},{"id":21,"nombre":"Cristian","posicion":"MEDIOCAMPISTA","edad":36,"dorsal":14,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":3,"equipo":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"}},{"id":22,"nombre":"Raul","posicion":"DELANTERO","edad":27,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":3,"equipo":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"}},{"id":23,"nombre":"Jhonatan","posicion":"DELANTERO","edad":21,"dorsal":21,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":3,"equipo":{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE"}},{"id":24,"nombre":"Carlos R","posicion":"DEFENSA","edad":23,"dorsal":2,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":4,"equipo":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"}},{"id":25,"nombre":"Yair","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":4,"equipo":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"}},{"id":26,"nombre":"Andres","posicion":"MEDIOCAMPISTA","edad":22,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":4,"equipo":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"}},{"id":27,"nombre":"Jhon","posicion":"DELANTERO","edad":23,"dorsal":9,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":4,"equipo":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"}},{"id":28,"nombre":"Alex","posicion":"DELANTERO","edad":34,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":4,"equipo":{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA"}},{"id":29,"nombre":"dilan","posicion":"DEFENSA","edad":23,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":1,"equipo":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"}},{"id":34,"nombre":"ZZTest Costo","posicion":"DELANTERO","edad":22,"dorsal":97,"imagenUrl":null,"costo":1500.50,"indicadorRespuesta":"Success","mensaje":"","idEquipo":2,"equipo":{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA"}},{"id":35,"nombre":"ZZTest SinCosto","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":"Success","mensaje":"","idEquipo":1,"equipo":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"}}]
HTTP 200
```

### V: GET transfers
```
curl -X GET http://localhost:57076/api/transfers
[{"id":10,"playerId":4,"playerName":"Jhonatan","originTeamId":1,"originTeamName":"Macalister","destinationTeamId":2,"destinationTeamName":"Socios.com","cost":100000.00,"status":"CONFIRMED","destinationDorsal":null,"requestedAt":"2026-10-19T17:12:31","resolvedAt":"2026-10-19T17:12:53"},{"id":9,"playerId":4,"playerName":"Jhonatan","originTeamId":1,"originTeamName":"Macalister","destinationTeamId":2,"destinationTeamName":"Socios.com","cost":0.00,"status":"REJECTED","destinationDorsal":21,"requestedAt":"2026-10-19T17:09:51","resolvedAt":"2026-10-19T17:10:49"},{"id":8,"playerId":35,"playerName":"ZZTest SinCosto","originTeamId":1,"originTeamName":"Macalister","destinationTeamId":2,"destinationTeamName":"Socios.com","cost":0.00,"status":"REJECTED","destinationDorsal":null,"requestedAt":"2026-10-19T17:07:11","resolvedAt":"2026-10-19T17:07:17"},{"id":7,"playerId":34,"playerName":"ZZTest Costo","originTeamId":1,"originTeamName":"Macalister","destinationTeamId":3,"destinationTeamName":"Los de la T","cost":1500.50,"status":"REJECTED","destinationDorsal":55,"requestedAt":"2026-10-19T17:07:10","resolvedAt":"2026-10-19T17:07:19"},{"id":6,"playerId":34,"playerName":"ZZTest Costo","originTeamId":1,"originTeamName":"Macalister","destinationTeamId":5,"destinationTeamName":"Real","cost":1500.50,"status":"REJECTED","destinationDorsal":null,"requestedAt":"2026-10-19T17:05:38","resolvedAt":"2026-10-19T17:07:19"},{"id":5,"playerId":34,"playerName":"ZZTest Costo","originTeamId":1,"originTeamName":"Macalister","destinationTeamId":2,"destinationTeamName":"Socios.com","cost":1500.50,"status":"CONFIRMED","destinationDorsal":null,"requestedAt":"2026-10-19T17:05:37","resolvedAt":"2026-10-19T17:07:19"}]
HTTP 200
```

### V: POST equipos
```
curl -X POST http://localhost:57076/api/equipos -d '{"nombre":"ZZ"}'
{"indicadorRespuesta":"No autorizado","mensaje":"Debes iniciar sesión para realizar esta acción."}
HTTP 401
```

### V: POST jugadores
```
curl -X POST http://localhost:57076/api/jugadores -d '{"nombre":"ZZ","posicion":"PORTERO"}'
{"indicadorRespuesta":"No autorizado","mensaje":"Debes iniciar sesión para realizar esta acción."}
HTTP 401
```

### V: POST transfers
```
curl -X POST http://localhost:57076/api/transfers -d '{"playerId":34,"destinationTeamId":1}'
{"indicadorRespuesta":"No autorizado","mensaje":"Debes iniciar sesión para realizar esta acción."}
HTTP 401
```

### V: GET users
```
curl -X GET http://localhost:57076/api/users
{"indicadorRespuesta":"No autorizado","mensaje":"Debes iniciar sesión para realizar esta acción."}
HTTP 401
```


#### DT con equipo (zz-dt-equipo, equipo 1)

### DT: GET equipos
```
curl -X GET http://localhost:57076/api/equipos -H 'Authorization: Bearer <token>'
[{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":1,"nombre":"Andres","posicion":"PORTERO","edad":19,"dorsal":1,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":13,"nombre":"Daniel","posicion":"DEFENSA","edad":35,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":14,"nombre":"Brayan","posicion":"DEFENSA","edad":24,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":16,"nombre":"Alex","posicion":"MEDIOCAMPISTA","edad":31,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":15,"nombre":"Duvan","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":2,"nombre":"James Rodríguez","posicion":"MEDIOCAMPISTA","edad":33,"dorsal":10,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":11,"nombre":"James","posicion":"MEDIOCAMPISTA","edad":28,"dorsal":11,"imagenUrl":"http://localhost:57075/uploads/jugadores/c26d17af-6da2-4ba6-848a-f5fd39dd0620.png","costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":7,"nombre":"Jose","posicion":"PORTERO","edad":22,"dorsal":22,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":29,"nombre":"dilan","posicion":"DEFENSA","edad":23,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":35,"nombre":"ZZTest SinCosto","posicion":"DEFENSA","edad":23,"dorsal":98,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]},{"id":2,"nombre":"Socios.com","directorTecnico":"Joan","titulos":3,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":6,"nombre":"Didier","posicion":"MEDIOCAMPISTA","edad":18,"dorsal":4,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":5,"nombre":"Yeiler","posicion":"DEFENSA","edad":21,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":10,"nombre":"Erick","posicion":"DELANTERO","edad":34,"dorsal":15,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":4,"nombre":"Jhonatan","posicion":"DEFENSA","edad":33,"dorsal":21,"imagenUrl":null,"costo":100000.00,"indicadorRespuesta":null,"mensaje":null},{"id":34,"nombre":"ZZTest Costo","posicion":"DELANTERO","edad":22,"dorsal":97,"imagenUrl":null,"costo":1500.50,"indicadorRespuesta":null,"mensaje":null}]},{"id":3,"nombre":"Los de la T","directorTecnico":"Jhonatan Renteria","imagenUrl":"http://localhost:57075/uploads/equipos/a59a0d2c-b619-4cae-ab1f-cbbe5297fdd2.png","titulos":1,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success","jugadores":[{"id":18,"nombre":"Yair","posicion":"DEFENSA","edad":21,"dorsal":3,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":19,"nombre":"Alan","posicion":"MEDIOCAMPISTA","edad":25,"dorsal":6,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":20,"nombre":"Brandon","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":22,"nombre":"Raul","posicion":"DELANTERO","edad":27,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":21,"nombre":"Cristian","posicion":"MEDIOCAMPISTA","edad":36,"dorsal":14,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":23,"nombre":"Jhonatan","posicion":"DELANTERO","edad":21,"dorsal":21,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":17,"nombre":"Alex","posicion":"PORTERO","edad":25,"dorsal":31,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]},{"id":4,"nombre":"MRL","directorTecnico":"Jhonatan Renteria","titulos":1,"tipoClasificacion":"ELIMINATORIA","indicadorRespuesta":"Success","jugadores":[{"id":24,"nombre":"Carlos R","posicion":"DEFENSA","edad":23,"dorsal":2,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":25,"nombre":"Yair","posicion":"MEDIOCAMPISTA","edad":24,"dorsal":5,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":8,"nombre":"Yeison","posicion":"MEDIOCAMPISTA","edad":38,"dorsal":7,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":26,"nombre":"Andres","posicion":"MEDIOCAMPISTA","edad":22,"dorsal":8,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":27,"nombre":"Jhon","posicion":"DELANTERO","edad":23,"dorsal":9,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":28,"nombre":"Alex","posicion":"DELANTERO","edad":34,"dorsal":11,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null},{"id":9,"nombre":"Juan Carlos","posicion":"DELANTERO","edad":31,"dorsal":24,"imagenUrl":null,"costo":0.00,"indicadorRespuesta":null,"mensaje":null}]},{"id":5,"nombre":"Real","directorTecnico":"Juan","titulos":0,"tipoClasificacion":"REPECHAJE","indicadorRespuesta":"Success"}]
HTTP 200
```

### DT: solicitar traspaso hacia su equipo
```
curl -X POST http://localhost:57076/api/transfers -H 'Authorization: Bearer <token>' -d '{"playerId":34,"destinationTeamId":1}'
{"id":11,"playerId":34,"playerName":"ZZTest Costo","originTeamId":2,"originTeamName":"Socios.com","destinationTeamId":1,"destinationTeamName":"Macalister","cost":1500.50,"status":"PENDING","destinationDorsal":null,"requestedAt":"2026-10-29T11:32:11.622","resolvedAt":null}
HTTP 201
```

### DT: solicitar traspaso hacia otro equipo (3)
```
curl -X POST http://localhost:57076/api/transfers -H 'Authorization: Bearer <token>' -d '{"playerId":34,"destinationTeamId":3}'
{"indicadorRespuesta":"Prohibido","mensaje":"Solo puedes solicitar traspasos hacia tu equipo."}
HTTP 403
```

### DT: confirmar
```
curl -X POST http://localhost:57076/api/transfers/5/confirm -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```

### DT: rechazar
```
curl -X POST http://localhost:57076/api/transfers/5/reject -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```

### DT: crear jugador
```
curl -X POST http://localhost:57076/api/jugadores -H 'Authorization: Bearer <token>' -d '{"nombre":"ZZ DT","posicion":"PORTERO","edad":20,"dorsal":90,"idEquipo":1}'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```

### DT: GET users
```
curl -X GET http://localhost:57076/api/users -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```


#### DT sin equipo (zz-dt-uno)

### DT0: solicitar traspaso
```
curl -X POST http://localhost:57076/api/transfers -H 'Authorization: Bearer <token>' -d '{"playerId":34,"destinationTeamId":1}'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes un equipo asignado, por eso no puedes solicitar traspasos. Pídele a la organización que te asigne uno."}
HTTP 403
```


#### Repeticion con el id correcto del traspaso pendiente (11); las dos filas anteriores usaron el id 5 por un error del script, sin efecto porque el filtro responde 403 antes del servicio

### DT: confirmar traspaso 11
```
curl -X POST http://localhost:57076/api/transfers/11/confirm -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```

### DT: rechazar traspaso 11
```
curl -X POST http://localhost:57076/api/transfers/11/reject -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"Prohibido","mensaje":"No tienes permisos para realizar esta acción."}
HTTP 403
```


#### ADMIN

### ADMIN: GET users
```
curl -X GET http://localhost:57076/api/users -H 'Authorization: Bearer <token>'
[{"id":2,"username":"admin","role":"ADMIN","teamId":null,"teamName":null,"createdAt":"2026-10-29T11:26:52"},{"id":1,"username":"qa-admin","role":"ADMIN","teamId":null,"teamName":null,"createdAt":"2026-10-21T14:51:39"},{"id":4,"username":"zz-admin-dos","role":"ADMIN","teamId":null,"teamName":null,"createdAt":"2026-10-29T11:29:56"},{"id":5,"username":"zz-dt-equipo","role":"DT","teamId":1,"teamName":"Macalister","createdAt":"2026-10-29T11:30:02"},{"id":3,"username":"zz-dt-uno","role":"DT","teamId":null,"teamName":null,"createdAt":"2026-10-29T11:29:55"}]
HTTP 200
```

### ADMIN: rechazar traspaso 11
```
curl -X POST http://localhost:57076/api/transfers/11/reject -H 'Authorization: Bearer <token>'
{"id":11,"playerId":34,"playerName":"ZZTest Costo","originTeamId":2,"originTeamName":"Socios.com","destinationTeamId":1,"destinationTeamName":"Macalister","cost":1500.50,"status":"REJECTED","destinationDorsal":null,"requestedAt":"2026-10-29T11:32:12","resolvedAt":"2026-10-29T11:32:36"}
HTTP 200
```

### ADMIN: crear jugador de prueba
```
curl -X POST http://localhost:57076/api/jugadores -H 'Authorization: Bearer <token>' -d '{"nombre":"ZZ Admin Prueba","posicion":"PORTERO","edad":20,"dorsal":91,"costo":0,"idEquipo":1}'
{"id":39,"nombre":"ZZ Admin Prueba","posicion":"PORTERO","edad":20,"dorsal":91,"imagenUrl":null,"costo":0,"indicadorRespuesta":"Success","mensaje":"Gooool! El jugador ZZ Admin Prueba se guardó en la base de datos.","idEquipo":1,"equipo":{"id":1,"nombre":"Macalister","directorTecnico":"Joan","titulos":2,"tipoClasificacion":"REPECHAJE"}}
HTTP 200
```

### ADMIN: borrar jugador de prueba
```
curl -X DELETE http://localhost:57076/api/jugadores/39 -H 'Authorization: Bearer <token>'

HTTP 204
```

### ADMIN: solicitar traspaso hacia cualquier equipo (3)
```
curl -X POST http://localhost:57076/api/transfers -H 'Authorization: Bearer <token>' -d '{"playerId":34,"destinationTeamId":3}'
{"id":12,"playerId":34,"playerName":"ZZTest Costo","originTeamId":2,"originTeamName":"Socios.com","destinationTeamId":3,"destinationTeamName":"Los de la T","cost":1500.50,"status":"PENDING","destinationDorsal":null,"requestedAt":"2026-10-29T11:32:39.261","resolvedAt":null}
HTTP 201
```

### ADMIN: confirmar traspaso 12
```
curl -X POST http://localhost:57076/api/transfers/12/confirm -H 'Authorization: Bearer <token>'
{"id":12,"playerId":34,"playerName":"ZZTest Costo","originTeamId":2,"originTeamName":"Socios.com","destinationTeamId":3,"destinationTeamName":"Los de la T","cost":1500.50,"status":"CONFIRMED","destinationDorsal":null,"requestedAt":"2026-10-29T11:32:39","resolvedAt":"2026-10-29T11:32:50.676"}
HTTP 200
```


#### Clave fija retirada

### solo clave fija: POST equipos
```
curl -X POST http://localhost:57076/api/equipos -d '{"nombre":"ZZ Clave"}' -H 'X-Director-Tecnico-Key: <valor>'
{"indicadorRespuesta":"No autorizado","mensaje":"Debes iniciar sesión para realizar esta acción."}
HTTP 401
```

### solo clave fija: POST director-tecnico/validar
```
curl -X POST http://localhost:57076/api/director-tecnico/validar -H 'X-Director-Tecnico-Key: <valor>'
{"indicadorRespuesta":"No autorizado","mensaje":"Debes iniciar sesión para realizar esta acción."}
HTTP 401
```

### ADMIN: POST director-tecnico/validar (ya no existe)
```
curl -X POST http://localhost:57076/api/director-tecnico/validar -H 'Authorization: Bearer <token>'
{"timestamp":"2026-10-29T16:32:53.520+00:00","status":404,"error":"Not Found","message":"No message available","path":"/api/director-tecnico/validar"}
HTTP 404
```


## 9.6 OpenAPI


## 9.3 Vencimiento y renovacion (backend reiniciado con APP_SECURITY_JWT_TTL=PT10S)

### t=0s: me con token A
```
curl -X GET http://localhost:57076/api/auth/me -H 'Authorization: Bearer <token>'
{"id":2,"username":"admin","role":"ADMIN","teamId":null,"teamName":null}
HTTP 200
```

### t=6s: refresh con A vigente
```
curl -X POST http://localhost:57076/api/auth/refresh -H 'Authorization: Bearer <token>'
{"token":"<token>","tokenType":"Bearer","expiresInSeconds":10,"user":{"id":2,"username":"admin","role":"ADMIN","teamId":null,"teamName":null}}
HTTP 200
```

### t=12s: me con A vencido
```
curl -X GET http://localhost:57076/api/auth/me -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"No autorizado","mensaje":"Tu sesión expiró. Inicia sesión de nuevo."}
HTTP 401
```

### t=12s: refresh con A vencido
```
curl -X POST http://localhost:57076/api/auth/refresh -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"No autorizado","mensaje":"Tu sesión expiró. Inicia sesión de nuevo."}
HTTP 401
```

### t=12s: me con B (renovado a tiempo)
```
curl -X GET http://localhost:57076/api/auth/me -H 'Authorization: Bearer <token>'
{"id":2,"username":"admin","role":"ADMIN","teamId":null,"teamName":null}
HTTP 200
```

### t=19s: me con B vencido
```
curl -X GET http://localhost:57076/api/auth/me -H 'Authorization: Bearer <token>'
{"indicadorRespuesta":"No autorizado","mensaje":"Tu sesión expiró. Inicia sesión de nuevo."}
HTTP 401
```


## Alcance de frontend (10.1)
- Este change no modifica ningun archivo de frontend: el front vive en el repositorio aparte `MasQueAmigos-Torneo` y `git status` solo muestra cambios en `src/`, `openspec/`, `docs/` y `pom.xml`. Por eso el testing E2E con Playwright MCP no aplica aqui; la adaptacion del front se entrega como prompt en `docs/frontend-prompts/add-authentication-roles.md` (tarea 12.1).

## Documentacion pendiente fuera de este change
- `GUIA_PRUEBAS.md` y `HERRAMIENTAS_TESTING.md` (repositorio padre) muestran ejemplos de POST/PUT/DELETE sin token; ahora responderian 401. No estaban en las tareas del change.

## Docker (11.4)
- El `docker-compose.yml` actual del repositorio padre no pasa `APP_SECURITY_*` al contenedor; con el compose actual una imagen reconstruida con este change **no arrancaria** (sin `APP_SECURITY_JWT_SECRET` el backend falla a proposito). El diff propuesto se mostro al usuario y **no se aplico**.
- La reconstruccion de la imagen (skill `docker-rebuild`) requiere antes aplicar ese diff y tener las variables en el `.env`; se hace solo cuando el usuario lo pida.
