# Tournament Knockout Specification

## Purpose

Da comportamiento propio a la eliminatoria directa: penales para desempatar, partido previo cuando los equipos no son potencia de 2, llaves que se arman por sorteo o a mano y que avanzan solas hasta el campeón, con partido por el tercer puesto.

## Requirements

### Requirement: Penales de un partido
El sistema SHALL permitir al administrador registrar los penales de un partido mediante `PUT /api/partidos/{id}/penales` con los penales de cada equipo. Los penales SHALL registrarse solo en un partido en curso que exija ganador (ver el requisito siguiente), SHALL ser enteros mayores o iguales a 0 y SHALL ser distintos entre sí (una tanda de penales tiene ganador). El marcador normal del partido NO SHALL cambiar. Las respuestas de partido SHALL incluir los penales de cada equipo cuando están registrados.

#### Scenario: Registro de penales
- **WHEN** se registran penales 5 y 4 en un partido en curso que exige ganador
- **THEN** el sistema responde con estado 200 y el partido con `penalesLocal` 5 y `penalesVisitante` 4, y su marcador no cambia

#### Scenario: Penales iguales o negativos
- **WHEN** se registran penales iguales (por ejemplo 4 y 4) o negativos
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin guardar nada

#### Scenario: Partido que no exige ganador
- **WHEN** se intentan registrar penales en un partido de liga o de fase de grupos
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que ese partido no usa penales

#### Scenario: Partido que no está en curso
- **WHEN** se intentan registrar penales en un partido programado o ya finalizado
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin guardar nada

#### Scenario: Partido inexistente
- **WHEN** se intentan registrar penales en un partido que no existe
- **THEN** el sistema responde con estado 404 y un mensaje en español, con el mismo formato que los demás recursos de partidos y equipos (`{"indicadorRespuesta":"Sin Registros","mensaje":"..."}`)

### Requirement: Partidos que exigen ganador
El sistema SHALL tratar como partidos que exigen ganador a los partidos de las llaves y a los partidos de fase `REPECHAJE` de un torneo `KNOCKOUT`. Un partido que exige ganador NO SHALL poder finalizarse empatado: si el marcador es igual, SHALL exigir que tenga registrados penales distintos. El ganador SHALL ser el equipo con más goles y, con el marcador igual, el de más penales.

#### Scenario: Finalizar con ganador en el marcador
- **WHEN** se finaliza un partido de las llaves con marcador 2-1
- **THEN** el partido queda finalizado y su ganador es el equipo con 2 goles, sin necesidad de penales

#### Scenario: Finalizar empatado sin penales
- **WHEN** se intenta finalizar un partido que exige ganador con marcador 1-1 y sin penales registrados
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que no puede terminar empatado y que hay que registrar los penales, y el partido sigue en curso

#### Scenario: Finalizar empatado con penales
- **WHEN** se finaliza un partido que exige ganador con marcador 1-1 y penales 5-4
- **THEN** el partido queda finalizado y su ganador es el equipo con 5 penales

### Requirement: Repechaje previo a las llaves
El sistema SHALL permitir al administrador armar, en un torneo `KNOCKOUT` en curso, partidos de fase `REPECHAJE` entre dos equipos inscritos con el `POST /api/partidos` existente. El perdedor de un partido de repechaje finalizado SHALL quedar eliminado y no SHALL entrar en las llaves.

#### Scenario: Repechaje con 5 equipos
- **WHEN** un torneo `KNOCKOUT` con 5 equipos juega un repechaje entre dos de ellos y el equipo C lo pierde
- **THEN** C queda eliminado y los otros 4 equipos pueden formar las llaves

#### Scenario: Repechaje empatado
- **WHEN** un partido de repechaje termina empatado en el marcador
- **THEN** exige penales distintos para finalizarse, igual que un partido de las llaves

### Requirement: Generación de las llaves de eliminatoria directa
El sistema SHALL permitir al administrador generar las llaves de un torneo `KNOCKOUT` en curso con `POST /api/tournaments/{id}/bracket` indicando una fecha de inicio, una hora y, opcionalmente, un intervalo en días entre rondas (7 por defecto, de 1 a 30) y si se incluye el partido por el tercer puesto (incluido por defecto). Los equipos que participan SHALL ser los inscritos que no hayan sido eliminados en un repechaje, y su cantidad SHALL ser una potencia de 2 de al menos 2. No SHALL haber partidos de repechaje sin finalizar. La fecha de inicio SHALL ser posterior a la fecha del último partido del torneo. Las llaves SHALL generarse una sola vez.

#### Scenario: Generación con 8 equipos
- **WHEN** se generan las llaves de un torneo `KNOCKOUT` en curso con 8 equipos, fecha de inicio 2027-05-01 y hora 18:00
- **THEN** el sistema crea 3 rondas (cuartos de final, semifinales y final), crea los 4 partidos de cuartos con fecha 2027-05-01 y hora 18:00 en estado programado y fase `ELIMINACION_DIRECTA`, deja las semifinales y la final sin equipos hasta que se conozcan, y responde con estado 201 y las llaves

#### Scenario: Fechas de las rondas
- **WHEN** se generan las llaves con fecha de inicio 2027-05-01 e intervalo 7
- **THEN** los partidos de cuartos son el 2027-05-01, los de semifinales el 2027-05-08 y la final (y el tercer puesto) el 2027-05-15

#### Scenario: Cantidad de equipos que no es potencia de 2
- **WHEN** se intentan generar las llaves con 5 equipos que siguen en competencia
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que deben ser una potencia de 2 (2, 4, 8...) y que se arme un partido de repechaje, sin crear nada

#### Scenario: Repechaje sin finalizar
- **WHEN** se intentan generar las llaves y hay un partido de repechaje programado o en curso
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear nada

#### Scenario: Llaves ya generadas
- **WHEN** se intentan generar las llaves de un torneo que ya las tiene
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear nada

#### Scenario: Torneo que no es de eliminatoria o no está en curso
- **WHEN** se intentan generar las llaves de un torneo `LEAGUE`, del Histórico o de un torneo en `DRAFT` o `FINISHED`
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear nada

#### Scenario: Fecha de inicio no posterior al último partido
- **WHEN** la fecha de inicio es igual o anterior a la del último partido del torneo
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear nada

#### Scenario: Datos inválidos
- **WHEN** se generan las llaves sin fecha de inicio o sin hora, o con un intervalo fuera de 1 a 30
- **THEN** el sistema responde con estado 400 y un mapa campo → mensaje en español

#### Scenario: Torneo inexistente
- **WHEN** se intentan generar las llaves de un torneo que no existe
- **THEN** el sistema responde con estado 404 y un mensaje en español, con el mismo formato que los demás recursos de partidos y equipos (`{"indicadorRespuesta":"Sin Registros","mensaje":"..."}`)

### Requirement: Sorteo y cruces manuales
Al generar las llaves de un torneo `KNOCKOUT`, el sistema SHALL sortear al azar los cruces de la primera ronda cuando no se indican, y SHALL usar los cruces indicados por el administrador cuando se envía la lista de cruces. Los cruces indicados SHALL incluir a todos los equipos que participan exactamente una vez, cada uno con su equipo local y su equipo visitante.

#### Scenario: Sorteo aleatorio
- **WHEN** se generan las llaves con 8 equipos sin indicar cruces
- **THEN** los 8 equipos quedan repartidos en los 4 partidos de la primera ronda, cada equipo en uno solo

#### Scenario: Cruces manuales
- **WHEN** el administrador indica los cruces A vs B, C vs D, E vs F y G vs H
- **THEN** la primera ronda queda exactamente con esos cruces, en ese orden, y los ganadores de cruces consecutivos se enfrentan en la ronda siguiente

#### Scenario: Cruces manuales incompletos o con equipos repetidos
- **WHEN** los cruces indicados no incluyen a todos los equipos participantes, repiten un equipo o incluyen un equipo eliminado o no inscrito
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin crear nada

### Requirement: Avance de ronda
Al finalizar un partido de las llaves, el sistema SHALL llevar al ganador al partido de la ronda siguiente y, cuando ese partido ya tiene sus dos equipos, SHALL crearlo programado con la fecha y la hora de su ronda y fase `ELIMINACION_DIRECTA` (fase `FINAL` para la final). Los ganadores de dos cruces consecutivos de una ronda SHALL enfrentarse entre sí en la ronda siguiente. Cuando se finaliza la final, el ganador SHALL ser el campeón.

#### Scenario: Ganador pasa a la ronda siguiente
- **WHEN** se finaliza el primer cuarto de final y gana A
- **THEN** A ocupa su lugar en la primera semifinal y esta aún no se crea porque falta el rival

#### Scenario: Se crea el partido cuando se conocen ambos equipos
- **WHEN** se finaliza el segundo cuarto de final y gana B, cuyo cruce alimenta la misma semifinal que el de A
- **THEN** el sistema crea el partido de semifinal A vs B, programado, con la fecha y la hora de las semifinales

#### Scenario: Campeón
- **WHEN** se finaliza la final y gana el equipo X
- **THEN** las llaves muestran a X como campeón

### Requirement: Partido por el tercer puesto
Cuando las llaves incluyen el partido por el tercer puesto y tienen al menos 4 equipos, el sistema SHALL llevar a los perdedores de las semifinales a ese partido y SHALL crearlo, programado y con la fecha y la hora de la final, cuando ambos se conocen. Este partido SHALL exigir ganador como los demás.

#### Scenario: Perdedores de semifinales
- **WHEN** se finalizan las dos semifinales
- **THEN** los dos perdedores se enfrentan en el partido por el tercer puesto, creado con la fecha de la final

#### Scenario: Sin tercer puesto
- **WHEN** se generan las llaves indicando que no se incluye el partido por el tercer puesto
- **THEN** las llaves no tienen ese partido y los perdedores de semifinales quedan eliminados

#### Scenario: Solo dos equipos
- **WHEN** se generan las llaves con 2 equipos
- **THEN** hay una sola ronda con la final y no hay partido por el tercer puesto

### Requirement: Consulta de las llaves
El sistema SHALL ofrecer, sin sesión, las llaves de un torneo con `GET /api/tournaments/{id}/bracket`: las rondas en orden con su nombre (octavos de final, cuartos de final, semifinales, final), cada cruce con sus equipos (vacíos si aún no se conocen), su partido, su marcador, sus penales y su ganador, el cruce por el tercer puesto si existe y el campeón si ya está definido.

#### Scenario: Llaves en juego
- **WHEN** se consultan las llaves de un torneo con 8 equipos tras finalizar un cuarto de final
- **THEN** el sistema responde con estado 200, las 3 rondas, el cruce finalizado con su ganador y los demás pendientes, y sin campeón

#### Scenario: Torneo sin llaves
- **WHEN** se consultan las llaves de un torneo que aún no las tiene
- **THEN** el sistema responde con estado 200 y una lista de rondas vacía

#### Scenario: Torneo inexistente
- **WHEN** se consultan las llaves de un torneo que no existe
- **THEN** el sistema responde con estado 404 y un mensaje en español, con el mismo formato que los demás recursos de partidos y equipos (`{"indicadorRespuesta":"Sin Registros","mensaje":"..."}`)

### Requirement: Partidos de las llaves protegidos
El sistema NO SHALL permitir eliminar un partido de las llaves ni cambiar sus equipos, porque los demás cruces dependen de él. SHALL permitir editar su fecha y su hora mientras no esté finalizado.

#### Scenario: Eliminar un partido de las llaves
- **WHEN** se intenta eliminar un partido que pertenece a las llaves
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que es parte de las llaves, sin eliminarlo

#### Scenario: Cambiar los equipos de un partido de las llaves
- **WHEN** se intenta editar un partido de las llaves cambiando uno de sus equipos
- **THEN** el sistema responde con estado 400 y un mensaje en español, sin cambiar el partido

#### Scenario: Cambiar la fecha
- **WHEN** se edita la fecha de un partido de las llaves programado
- **THEN** el sistema acepta el cambio y el partido conserva su cruce y sus equipos

### Requirement: Permisos de la eliminatoria
El sistema SHALL permitir consultar las llaves sin sesión y SHALL exigir una sesión de administrador para generar las llaves y para registrar penales.

#### Scenario: Sin sesión
- **WHEN** un visitante sin sesión intenta generar las llaves o registrar penales
- **THEN** el sistema responde con estado 401

#### Scenario: Director Técnico
- **WHEN** un usuario con rol `DT` intenta generar las llaves o registrar penales
- **THEN** el sistema responde con estado 403
