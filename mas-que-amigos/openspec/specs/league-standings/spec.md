# League Standings Specification

## Purpose

Calcula la tabla de posiciones de un torneo de liga a partir de sus partidos finalizados, con puntuación 3-1-0 y criterios de desempate definidos, de modo que todos los partidos del torneo cuenten igual sin importar cómo se crearon.

## Requirements

### Requirement: Tabla de posiciones de una liga
El sistema SHALL ofrecer, sin sesión, la tabla de posiciones de un torneo de formato `LEAGUE`. La tabla SHALL incluir a todos los equipos inscritos en el torneo, aunque no hayan jugado, y para cada uno SHALL mostrar su posición, partidos jugados, ganados, empatados y perdidos, goles a favor y en contra, diferencia de goles, puntos y puntos de juego limpio. Solo SHALL contar los partidos del torneo en estado `FINALIZADO`, usando el marcador del partido. Un partido armado a mano SHALL contar igual que cualquier otro.

#### Scenario: Tabla con partidos finalizados
- **WHEN** en un torneo de liga con 3 equipos A, B y C se finalizan A 2-0 B y A 1-1 C
- **THEN** la tabla muestra a A con 2 jugados, 1 ganado, 1 empatado, 0 perdidos, 3 goles a favor, 1 en contra, diferencia +2 y 4 puntos; a B con 1 jugado, 0 ganados, 0 empatados, 1 perdido, 0 a favor, 2 en contra, diferencia −2 y 0 puntos; y a C con 1 jugado, 0 ganados, 1 empatado, 0 perdidos, 1 a favor, 1 en contra, diferencia 0 y 1 punto

#### Scenario: Equipo que aún no jugó
- **WHEN** se consulta la tabla de un torneo de liga con un equipo inscrito que no tiene partidos finalizados
- **THEN** ese equipo aparece en la tabla con todos sus valores en 0

#### Scenario: Partidos que no han terminado no cuentan
- **WHEN** un partido del torneo está programado o en curso
- **THEN** no suma partidos jugados, goles ni puntos a ninguno de sus equipos

#### Scenario: Un partido manual cuenta como cualquier otro
- **WHEN** A vs B se aplaza y el administrador crea A vs C en el torneo y lo finaliza
- **THEN** el resultado de A vs C suma partidos jugados, goles y puntos a A y a C igual que los demás

#### Scenario: Torneo que no es de liga
- **WHEN** se consulta la tabla de un torneo de formato `GROUPS_KNOCKOUT`, `KNOCKOUT` o el torneo Histórico
- **THEN** el sistema responde con estado 400 y un mensaje en español que indica que ese torneo no tiene tabla de posiciones de liga

#### Scenario: Torneo inexistente
- **WHEN** se consulta la tabla de un torneo que no existe
- **THEN** el sistema responde con estado 404 y un mensaje en español, con el mismo formato que los demás recursos de partidos y equipos (`{"indicadorRespuesta":"Sin Registros","mensaje":"..."}`)

### Requirement: Puntuación de la liga
El sistema SHALL otorgar 3 puntos por victoria, 1 por empate y 0 por derrota.

#### Scenario: Victoria, empate y derrota
- **WHEN** se finalizan A 3-1 B, C 0-0 D y E 0-2 F
- **THEN** A y F suman 3 puntos, C y D suman 1 punto cada uno, y B y E suman 0

### Requirement: Criterios de desempate
Cuando dos o más equipos tienen los mismos puntos, el sistema SHALL ordenarlos aplicando, en este orden: (1) mayor puntaje obtenido en los partidos jugados solo entre los equipos empatados (enfrentamiento directo), con su diferencia de goles y sus goles a favor entre ellos como subcriterios; (2) mayor diferencia de goles en toda la tabla; (3) más goles a favor en toda la tabla; (4) menos puntos de juego limpio; (5) orden alfabético del nombre del equipo.

#### Scenario: Desempate de dos equipos por enfrentamiento directo
- **WHEN** A y B tienen los mismos puntos y A ganó el partido entre ambos, aunque B tenga mejor diferencia de goles total
- **THEN** A queda por encima de B

#### Scenario: Desempate de tres equipos con mini tabla
- **WHEN** A, B y C empatan en puntos y, considerando solo los partidos entre ellos, B tiene más puntos que A y A más que C
- **THEN** el orden entre ellos es B, A, C

#### Scenario: Enfrentamiento directo sin diferencia
- **WHEN** dos equipos empatados en puntos igualaron su partido directo (o aún no lo jugaron)
- **THEN** el sistema pasa a la diferencia de goles total para ordenarlos

#### Scenario: Diferencia de goles y goles a favor
- **WHEN** dos equipos empatan en puntos y en el enfrentamiento directo, y uno tiene mejor diferencia de goles total
- **THEN** ese equipo queda por encima; si también empatan en la diferencia, queda por encima el que tiene más goles a favor

#### Scenario: Juego limpio
- **WHEN** dos equipos empatan en todo lo anterior y uno tiene menos puntos de juego limpio
- **THEN** el de menos puntos de juego limpio queda por encima

#### Scenario: Empate total
- **WHEN** dos equipos empatan también en juego limpio
- **THEN** queda primero el que va antes en orden alfabético del nombre

### Requirement: Puntos de juego limpio
El sistema SHALL calcular los puntos de juego limpio de un equipo como la suma de sus tarjetas amarillas por 1 más sus tarjetas rojas por 2, contando las tarjetas de los jugadores del equipo en los partidos finalizados del torneo.

#### Scenario: Cálculo con amarillas y rojas
- **WHEN** los jugadores de un equipo reciben 3 amarillas y 1 roja en sus partidos finalizados del torneo
- **THEN** el equipo suma 5 puntos de juego limpio

#### Scenario: Las tarjetas de partidos sin terminar o de otro torneo no cuentan
- **WHEN** hay tarjetas registradas en un partido en curso o en partidos de otro torneo
- **THEN** no suman a los puntos de juego limpio de este torneo

### Requirement: Posiciones consecutivas
El sistema SHALL asignar a cada equipo una posición única, de 1 hasta la cantidad de equipos inscritos, sin posiciones compartidas.

#### Scenario: Posiciones sin repetir
- **WHEN** se consulta la tabla de un torneo con equipos que empatan en todos los criterios
- **THEN** cada equipo tiene una posición distinta y la lista está ordenada por posición

### Requirement: La tabla no se guarda
El sistema SHALL calcular la tabla en cada consulta, de modo que refleje siempre el estado actual de los partidos y estadísticas.

#### Scenario: Corrección de una estadística
- **WHEN** se corrige una estadística de un partido ya finalizado y cambia su marcador
- **THEN** la siguiente consulta de la tabla ya refleja el marcador nuevo
