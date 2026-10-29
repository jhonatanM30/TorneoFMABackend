# Reporte del Paso 8 - Tests unitarios y verificación de la base de datos

- Fecha: 2026-10-21
- Change: add-authentication-roles
- Agente: Claude (Sonnet 5.5)

## Comandos ejecutados
- `./mvnw clean test -Dtest='JwtTokenServiceTest,UserServiceTest,UserCreateRequestDTOValidationTest,AdminSeederTest,AuthServiceTest,LoginRequestDTOValidationTest,PlayerTransferServiceTest,GlobalExceptionHandlerTest,SecurityRulesTest'` (focalizados)
- `./mvnw clean test` (suite completa)
- `./mvnw clean test -Dtest='SecurityRulesTest#admin_managesUsers'` (re-ejecución tras agregar la comprobación de login de la cuenta creada)
- `docker exec TorneoContainer mysql ... -e "select count(*) ..."` (baseline y verificación posterior de la BD MySQL real)

## Resultados de los tests unitarios
- Tests focalizados: 105 pasaron, 0 fallaron, 0 omitidos (7 min 18 s; `SecurityRulesTest` levanta el contexto Spring completo con H2 y recorre 23 casos)
- Suite completa/requerida: 144 pasaron, 0 fallaron, 0 omitidos (4 min 21 s)
- Tests nuevos de este change: `JwtTokenServiceTest` (8), `UserServiceTest` (17), `UserCreateRequestDTOValidationTest` (6), `AdminSeederTest` (4), `AuthServiceTest` (9), `LoginRequestDTOValidationTest` (4), `SecurityRulesTest` (23), 5 en `PlayerTransferServiceTest` y 2 en `GlobalExceptionHandlerTest` nuevos
- Tests existentes ajustados (sin debilitarlos): `PlayerTransferServiceTest` (los 9 usos de `requestTransfer` ahora pasan a quien solicita y el service recibe `AppUserRepository`)
- Notas:
  - Se usó siempre `clean`: el IDE deja clases a medio compilar en `target/` y sin `clean` Maven puede responder "Nothing to compile".
  - Los tests (incluido el contexto Spring) usan H2 en memoria (`src/test/resources/application.properties`), con el secreto del JWT y las credenciales del admin de prueba definidas ahí; no tocan MySQL.
  - Los tiempos varían mucho según la carga de la máquina (la misma clase `SecurityRulesTest` tardó entre 127 s y 329 s); el arranque del contexto domina el tiempo.
  - Sin tests flaky ni reintentos.

## Verificación del estado de la base de datos (MySQL `TorneoContainer`, esquema `masqueamigos`)
- Baseline previo a los tests (incluye datos de prueba que el usuario creó por su cuenta con Swagger):
  - jugador: 29 · equipos: 5 · alineacion: 21 · estadistica: 2 · player_transfer: 6 · app_user: 0
  - Última migración Flyway aplicada: 8 (`V8__app_user.sql`, aplicada el 2026-10-21 por una ejecución local del usuario; la tabla `app_user` existe y está vacía)
- Validación posterior a los tests:
  - jugador: 29 · equipos: 5 · alineacion: 21 · estadistica: 2 · player_transfer: 6 · app_user: 0
  - Última migración Flyway aplicada: 8
- Estado restaurado: Sí (no hubo mutaciones; los tests no usan la BD real)
- Acciones de restauración: ninguna necesaria
- Importante: `V8__app_user.sql` ya está aplicada en la base real, así que no se debe editar (Flyway rechazaría el arranque por checksum distinto).

## Correspondencia escenario → test

### authentication
| Escenario | Test(s) |
|---|---|
| Credenciales correctas | `AuthServiceTest.login_withCorrectCredentials_returnsATokenAndTheUser`; `SecurityRulesTest.login_isPublicAndReturnsATokenThatWorks` |
| Usuario escrito con otras mayúsculas | `AuthServiceTest.login_ignoresTheCaseOfTheUsername`; `SecurityRulesTest.login_isPublic...` (usa `ADMIN`) |
| Contraseña incorrecta | `AuthServiceTest.login_withAWrongPassword_throwsTheGenericMessage`; `SecurityRulesTest.login_failuresUseTheGenericMessage` |
| Usuario inexistente | `AuthServiceTest.login_withAnUnknownUser_throwsTheSameMessageAndStillComparesAPassword`; `SecurityRulesTest.login_failuresUseTheGenericMessage` |
| Datos obligatorios vacíos | `LoginRequestDTOValidationTest` (4); `SecurityRulesTest.login_withMissingFieldsReturns400` |
| Token vigente | `JwtTokenServiceTest.decode_acceptsTheTokenBeforeItExpires`; todos los tests autenticados de `SecurityRulesTest` |
| Token vencido | `JwtTokenServiceTest.decode_rejectsTheTokenRightAfterItsLifetimeWithoutClockSkewTolerance`; `SecurityRulesTest.expiredToken_returns401SayingTheSessionExpired` |
| Token alterado o inválido | `JwtTokenServiceTest.decode_rejectsATamperedToken`, `...AnotherSecret`, `...MalformedTokens`; `SecurityRulesTest.tamperedOrForeignTokens_return401AskingToLogIn` |
| Sesión cerrada por inactividad | `JwtTokenServiceTest` (vencimiento a los 181 s sin tolerancia) |
| Renovación con token vigente / vencido / sin token | `AuthServiceTest.refresh_issuesANewTokenWithTheRoleCurrentlyInTheDatabase`; `SecurityRulesTest.refresh_requiresAValidTokenAndIssuesANewOne` |
| Consulta de la sesión con y sin token | `AuthServiceTest.me_returnsTheCurrentAccountWithItsTeam`; `SecurityRulesTest.me_returnsTheTeamOfADt` y `login_isPublic...` |

### user-accounts
| Escenario | Test(s) |
|---|---|
| Usuario único sin importar mayúsculas | `UserServiceTest.createUser_whenTheUsernameExistsIgnoringCase_throwsBusinessException` |
| Arranque con configuración / con admin existente / sin configuración | `AdminSeederTest` (4 tests) |
| Las respuestas no exponen contraseñas | `UserServiceTest.listUsers_...NeverExposesThePassword`; `SecurityRulesTest.admin_managesUsers` |
| Creación de DT (sin rol) / de otro administrador / rol inexistente / usuario existente | `UserServiceTest.createUser_*` |
| Usuario o contraseña inválidos | `UserCreateRequestDTOValidationTest` (6) |
| La cuenta creada puede iniciar sesión | `SecurityRulesTest.admin_managesUsers` (crea y luego inicia sesión) |
| Equipo del DT: crear con equipo, inexistente, ocupado, ADMIN con equipo, asignar, cambiar, quitar, cuenta inexistente | `UserServiceTest.createUser_with*Team*`, `createUser_adminWithTeam_*`, `assignTeam_*`; `SecurityRulesTest.admin_managesUsers` (PUT del equipo) |
| Listado | `UserServiceTest.listUsers_...`; `SecurityRulesTest.admin_managesUsers` |

### access-control
| Escenario | Test(s) |
|---|---|
| Consulta sin sesión / escritura sin sesión | `SecurityRulesTest.visitor_canReadEverythingWithoutASession`, `visitor_cannotWrite` |
| Escritura y gestión de usuarios de un administrador | `admin_isNeverBlockedOnWriteEndpoints`, `admin_managesUsers` |
| DT: consulta, solicitud de traspaso, otras escrituras, gestión de usuarios | `dt_canReadButCannotWriteOrManageUsers`, `dt_requestsATransferIntoItsOwnTeam` |
| Listado de usuarios sin sesión | `visitor_cannotUseUserManagement` |
| Falta el token / rol insuficiente (formato del cuerpo) | `missingToken_returns401WithTheStandardBody`, `insufficientRole_returns403WithTheStandardBody` |
| Escritura con la clave fija | `theOldFixedKeyNoLongerAuthorizesAnything` |
| Preflight con Authorization | `preflightWithAuthorizationHeaderIsAllowedForTheFrontOrigin` |

### player-transfers
| Escenario | Test(s) |
|---|---|
| Administrador solicita hacia cualquier equipo | `PlayerTransferServiceTest.requestTransfer_adminToAnyTeam_...`; `SecurityRulesTest.admin_requestsConfirmsAndRejectsTransfers` |
| DT confirma o rechaza → 403 | `SecurityRulesTest.dt_canReadButCannotWriteOrManageUsers` (confirm y reject dentro de las escrituras) |
| Administrador confirma o rechaza | `admin_requestsConfirmsAndRejectsTransfers`, `admin_isNeverBlockedOnWriteEndpoints` |
| Visitante solicita, confirma o rechaza → 401 | `visitor_cannotWrite` |
| Consulta abierta de traspasos | `visitor_canReadEverythingWithoutASession` (incluye `status=PENDING` y `REJECTED`) |
| DT hacia su equipo / hacia otro equipo / sin equipo | `PlayerTransferServiceTest.requestTransfer_dt*`; `SecurityRulesTest.dt_requestsATransferIntoItsOwnTeam`, `dt_cannotRequestATransferIntoAnotherTeam`, `dt_withoutATeamCannotRequestATransfer` |
| El equipo se toma de la cuenta y no del token | `PlayerTransferServiceTest.requestTransfer_dtToItsOwnTeam_isAcceptedAndTheTeamIsReadFromTheAccount`; `SecurityRulesTest.dt_teamAssignedAfterLoginIsHonoredWithTheSameToken` |

## Resultado
- Estado del Paso 8: PASS
- Problemas bloqueantes: ninguno
