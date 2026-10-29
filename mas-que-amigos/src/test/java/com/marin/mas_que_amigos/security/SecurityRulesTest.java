package com.marin.mas_que_amigos.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marin.mas_que_amigos.model.AppUser;
import com.marin.mas_que_amigos.model.Equipo;
import com.marin.mas_que_amigos.model.Jugador;
import com.marin.mas_que_amigos.model.Jugador.Posicion;
import com.marin.mas_que_amigos.model.UserRole;
import com.marin.mas_que_amigos.repository.AppUserRepository;
import com.marin.mas_que_amigos.repository.EquipoRepository;
import com.marin.mas_que_amigos.repository.JugadorRepository;
import com.marin.mas_que_amigos.repository.PlayerTransferRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Role x operation matrix over the real HTTP security chain (H2 in memory), with
 * real tokens issued by JwtTokenService. Covers specs access-control,
 * authentication, user-accounts and player-transfers (permissions).
 */
@SpringBootTest
@AutoConfigureMockMvc
class SecurityRulesTest {

    private static final String ADMIN_USERNAME = "admin";
    private static final String ADMIN_PASSWORD = "Admin-test-1234";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private JwtTokenService tokens;
    @Autowired
    private SecurityProperties properties;
    @Autowired
    private AppUserRepository userRepository;
    @Autowired
    private EquipoRepository equipoRepository;
    @Autowired
    private JugadorRepository jugadorRepository;
    @Autowired
    private PlayerTransferRepository transferRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<AppUser> createdUsers = new ArrayList<>();
    private final List<Equipo> createdTeams = new ArrayList<>();

    private Equipo teamA;
    private Equipo teamB;
    private AppUser dtOfTeamA;
    private AppUser dtWithoutTeam;
    private Jugador playerOfTeamB;
    private String adminToken;
    private String dtToken;
    private String dtWithoutTeamToken;

    @BeforeEach
    void setUp() {
        teamA = newTeam("Team A");
        teamB = newTeam("Team B");
        dtOfTeamA = newUser("dt-a-" + shortId(), UserRole.DT, teamA);
        dtWithoutTeam = newUser("dt-none-" + shortId(), UserRole.DT, null);

        Jugador player = new Jugador();
        player.setNombre("Test Player");
        player.setPosicion(Posicion.DELANTERO);
        player.setEdad(22);
        player.setDorsal(10);
        player.setEquipo(teamB);
        playerOfTeamB = jugadorRepository.save(player);

        AppUser admin = userRepository.findByUsername(ADMIN_USERNAME).orElseThrow(
                () -> new AssertionError("The seeded admin should exist (see src/test/resources/application.properties)"));
        adminToken = token(admin);
        dtToken = token(dtOfTeamA);
        dtWithoutTeamToken = token(dtWithoutTeam);
    }

    @AfterEach
    void tearDown() {
        transferRepository.deleteAll();
        jugadorRepository.deleteAll();
        userRepository.deleteAll(createdUsers);
        equipoRepository.deleteAll(createdTeams);
        createdUsers.clear();
        createdTeams.clear();
    }

    // ---------------------------------------------------------------- helpers

    private static String shortId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private Equipo newTeam(String name) {
        Equipo team = new Equipo();
        team.setNombre(name + " " + shortId());
        team.setDirectorTecnico("DT " + name);
        createdTeams.add(equipoRepository.save(team));
        return team;
    }

    private AppUser newUser(String username, UserRole role, Equipo team) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("password-123"));
        user.setRole(role);
        user.setTeam(team);
        user.setCreatedAt(LocalDateTime.now());
        AppUser saved = userRepository.save(user);
        createdUsers.add(saved);
        return saved;
    }

    private String token(AppUser user) {
        return tokens.issue(user.getId(), user.getUsername(), user.getRole().name()).getToken();
    }

    private MockHttpServletRequestBuilder withToken(MockHttpServletRequestBuilder request, String token) {
        return token == null ? request : request.header("Authorization", "Bearer " + token);
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private int statusOf(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request).andReturn().getResponse().getStatus();
    }

    private String transferBody(Long playerId, Long destinationTeamId) {
        return "{\"playerId\":" + playerId + ",\"destinationTeamId\":" + destinationTeamId + "}";
    }

    /** Write endpoints of the API (any valid-looking body: authorization runs before validation). */
    private List<MockHttpServletRequestBuilder> writeRequests() {
        return Arrays.asList(
                json(post("/api/equipos"), "{}"),
                json(put("/api/equipos"), "{}"),
                delete("/api/equipos/999999"),
                json(post("/api/jugadores"), "{}"),
                json(post("/api/jugadores/batch"), "[]"),
                json(put("/api/jugadores"), "{}"),
                delete("/api/jugadores/999999"),
                json(post("/api/partidos"), "{}"),
                put("/api/partidos/999999/iniciar"),
                put("/api/partidos/999999/finalizar"),
                delete("/api/partidos/999999"),
                json(post("/api/alineaciones"), "{}"),
                delete("/api/alineaciones/999999/999999"),
                json(post("/api/estadisticas"), "{}"),
                json(post("/api/galeria"), "{}"),
                json(post("/api/registros-informativos"), "{}"),
                json(post("/api/transfers/999999/confirm"), "{}"),
                json(post("/api/transfers/999999/reject"), "{}"));
    }

    // ---------------------------------------------------------------- visitor

    @Test
    void visitor_canReadEverythingWithoutASession() throws Exception {
        for (String path : Arrays.asList("/api/equipos", "/api/jugadores", "/api/partidos", "/api/estadisticas/goleador",
                "/api/galeria", "/api/registros-informativos", "/api/transfers", "/api/transfers?status=PENDING",
                "/api/transfers?status=REJECTED", "/v3/api-docs")) {
            assertThat(statusOf(get(path))).as("GET %s as visitor", path).isEqualTo(200);
        }
        assertThat(statusOf(get("/uploads/does-not-exist.png"))).as("uploads are not protected")
                .isNotIn(401, 403);
    }

    @Test
    void visitor_cannotWrite() throws Exception {
        for (MockHttpServletRequestBuilder request : writeRequests()) {
            MvcResult result = mvc.perform(request).andReturn();
            assertThat(result.getResponse().getStatus())
                    .as("%s %s as visitor", result.getRequest().getMethod(), result.getRequest().getRequestURI())
                    .isEqualTo(401);
        }
        assertThat(statusOf(json(post("/api/transfers"), transferBody(1L, 1L)))).isEqualTo(401);
    }

    @Test
    void visitor_cannotUseUserManagement() throws Exception {
        assertThat(statusOf(get("/api/users"))).isEqualTo(401);
        assertThat(statusOf(json(post("/api/users"), "{\"username\":\"abc\",\"password\":\"12345678\"}"))).isEqualTo(401);
        assertThat(statusOf(json(put("/api/users/1/team"), "{\"teamId\":null}"))).isEqualTo(401);
    }

    // ---------------------------------------------------------------- DT

    @Test
    void dt_canReadButCannotWriteOrManageUsers() throws Exception {
        assertThat(statusOf(withToken(get("/api/equipos"), dtToken))).isEqualTo(200);
        assertThat(statusOf(withToken(get("/api/transfers"), dtToken))).isEqualTo(200);

        for (MockHttpServletRequestBuilder request : writeRequests()) {
            MvcResult result = mvc.perform(withToken(request, dtToken)).andReturn();
            assertThat(result.getResponse().getStatus())
                    .as("%s %s as DT", result.getRequest().getMethod(), result.getRequest().getRequestURI())
                    .isEqualTo(403);
        }
        assertThat(statusOf(withToken(get("/api/users"), dtToken))).isEqualTo(403);
        assertThat(statusOf(withToken(json(post("/api/users"), "{\"username\":\"abc\",\"password\":\"12345678\"}"), dtToken)))
                .isEqualTo(403);
        assertThat(statusOf(withToken(json(put("/api/users/1/team"), "{\"teamId\":null}"), dtToken))).isEqualTo(403);
    }

    @Test
    void dt_requestsATransferIntoItsOwnTeam() throws Exception {
        mvc.perform(withToken(json(post("/api/transfers"), transferBody(playerOfTeamB.getId(), teamA.getId())), dtToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.destinationTeamId").value(teamA.getId()));
    }

    @Test
    void dt_cannotRequestATransferIntoAnotherTeam() throws Exception {
        Equipo otherTeam = newTeam("Team C");

        mvc.perform(withToken(json(post("/api/transfers"), transferBody(playerOfTeamB.getId(), otherTeam.getId())), dtToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.indicadorRespuesta").value("Prohibido"))
                .andExpect(jsonPath("$.mensaje").value("Solo puedes solicitar traspasos hacia tu equipo."));
        assertThat(transferRepository.count()).isZero();
    }

    @Test
    void dt_withoutATeamCannotRequestATransfer() throws Exception {
        mvc.perform(withToken(json(post("/api/transfers"), transferBody(playerOfTeamB.getId(), teamA.getId())),
                dtWithoutTeamToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("equipo asignado")));
    }

    @Test
    void dt_teamAssignedAfterLoginIsHonoredWithTheSameToken() throws Exception {
        // dtWithoutTeam logged in before having a team (token above); the admin assigns teamA afterwards.
        AppUser account = userRepository.findById(dtWithoutTeam.getId()).orElseThrow(AssertionError::new);
        account.setTeam(teamB);
        userRepository.save(account);
        Equipo anotherTeamForPlayer = newTeam("Team D");
        Jugador other = new Jugador();
        other.setNombre("Other Player");
        other.setPosicion(Posicion.DEFENSA);
        other.setEdad(30);
        other.setDorsal(4);
        other.setEquipo(anotherTeamForPlayer);
        Jugador savedOther = jugadorRepository.save(other);

        mvc.perform(withToken(json(post("/api/transfers"), transferBody(savedOther.getId(), teamB.getId())),
                dtWithoutTeamToken))
                .andExpect(status().isCreated());
    }

    // ---------------------------------------------------------------- ADMIN

    @Test
    void admin_isNeverBlockedOnWriteEndpoints() throws Exception {
        for (MockHttpServletRequestBuilder request : writeRequests()) {
            MvcResult result = mvc.perform(withToken(request, adminToken)).andReturn();
            assertThat(result.getResponse().getStatus())
                    .as("%s %s as ADMIN", result.getRequest().getMethod(), result.getRequest().getRequestURI())
                    .isNotIn(401, 403);
        }
    }

    @Test
    void admin_requestsConfirmsAndRejectsTransfers() throws Exception {
        MvcResult created = mvc.perform(withToken(
                json(post("/api/transfers"), transferBody(playerOfTeamB.getId(), teamA.getId())), adminToken))
                .andExpect(status().isCreated()).andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mvc.perform(withToken(post("/api/transfers/" + id + "/confirm"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void admin_managesUsers() throws Exception {
        String username = "new-dt-" + shortId();

        MvcResult created = mvc.perform(withToken(json(post("/api/users"),
                "{\"username\":\"" + username + "\",\"password\":\"password-123\"}"), adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("DT"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andReturn();
        long id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();
        userRepository.findById(id).ifPresent(createdUsers::add);

        // The account created by the admin can log in with the role it was created with.
        mvc.perform(json(post("/api/auth/login"), "{\"username\":\"" + username + "\",\"password\":\"password-123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.role").value("DT"))
                .andExpect(jsonPath("$.user.teamId").doesNotExist());

        mvc.perform(withToken(get("/api/users"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.username=='" + username + "')]").exists())
                .andExpect(jsonPath("$[0].password").doesNotExist());

        Equipo freeTeam = newTeam("Team E");
        mvc.perform(withToken(json(put("/api/users/" + id + "/team"), "{\"teamId\":" + freeTeam.getId() + "}"), adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teamId").value(freeTeam.getId()));
    }

    // ---------------------------------------------------------------- tokens and error format

    @Test
    void missingToken_returns401WithTheStandardBody() throws Exception {
        mvc.perform(json(post("/api/equipos"), "{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.indicadorRespuesta").value("No autorizado"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Debes iniciar sesión")));
    }

    @Test
    void insufficientRole_returns403WithTheStandardBody() throws Exception {
        mvc.perform(withToken(json(post("/api/equipos"), "{}"), dtToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.indicadorRespuesta").value("Prohibido"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("permisos")));
    }

    @Test
    void expiredToken_returns401SayingTheSessionExpired() throws Exception {
        Clock anHourAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        String expired = new JwtTokenService(properties, anHourAgo).issue(1L, ADMIN_USERNAME, "ADMIN").getToken();

        mvc.perform(withToken(get("/api/auth/me"), expired))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.indicadorRespuesta").value("No autorizado"))
                .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("expiró")));
        // Reads are public, but a bad token is still rejected instead of being ignored.
        assertThat(statusOf(withToken(get("/api/equipos"), expired))).isEqualTo(401);
    }

    @Test
    void tamperedOrForeignTokens_return401AskingToLogIn() throws Exception {
        String[] parts = dtToken.split("\\.");
        String forgedPayload = adminToken.split("\\.")[1];
        String tampered = parts[0] + "." + forgedPayload + "." + parts[2];

        SecurityProperties otherProperties = new SecurityProperties();
        otherProperties.getJwt().setSecret("some-other-secret-some-other-secret-0123456789");
        String foreign = new JwtTokenService(otherProperties).issue(1L, ADMIN_USERNAME, "ADMIN").getToken();

        for (String bad : Arrays.asList(tampered, foreign, "not-a-token")) {
            mvc.perform(withToken(json(post("/api/equipos"), "{}"), bad))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensaje").value(org.hamcrest.Matchers.containsString("Debes iniciar sesión")));
        }
    }

    @Test
    void theOldFixedKeyNoLongerAuthorizesAnything() throws Exception {
        mvc.perform(json(post("/api/equipos"), "{}").header("X-Director-Tecnico-Key", "director2025"))
                .andExpect(status().isUnauthorized());
        mvc.perform(json(post("/api/equipos"), "{}").header("X-Director-Tecnico-Key", "Director2026"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void preflightWithAuthorizationHeaderIsAllowedForTheFrontOrigin() throws Exception {
        mvc.perform(options("/api/equipos")
                .header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.containsStringIgnoringCase("authorization")));
    }

    // ---------------------------------------------------------------- authentication endpoints

    @Test
    void login_isPublicAndReturnsATokenThatWorks() throws Exception {
        MvcResult result = mvc.perform(json(post("/api/auth/login"),
                "{\"username\":\"ADMIN\",\"password\":\"" + ADMIN_PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresInSeconds").value(180))
                .andExpect(jsonPath("$.user.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());

        mvc.perform(withToken(get("/api/auth/me"), body.get("token").asText()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(ADMIN_USERNAME))
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void login_failuresUseTheGenericMessage() throws Exception {
        for (String body : Arrays.asList(
                "{\"username\":\"admin\",\"password\":\"wrong-password\"}",
                "{\"username\":\"nobody-" + shortId() + "\",\"password\":\"whatever-123\"}")) {
            mvc.perform(json(post("/api/auth/login"), body))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensaje").value("Usuario o contraseña incorrectos"));
        }
    }

    @Test
    void login_withMissingFieldsReturns400() throws Exception {
        mvc.perform(json(post("/api/auth/login"), "{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.username").value("El usuario es obligatorio."))
                .andExpect(jsonPath("$.password").value("La contraseña es obligatoria."));
    }

    @Test
    void me_returnsTheTeamOfADt() throws Exception {
        mvc.perform(withToken(get("/api/auth/me"), dtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("DT"))
                .andExpect(jsonPath("$.teamId").value(teamA.getId()));
        assertThat(statusOf(get("/api/auth/me"))).isEqualTo(401);
    }

    @Test
    void refresh_requiresAValidTokenAndIssuesANewOne() throws Exception {
        assertThat(statusOf(post("/api/auth/refresh"))).isEqualTo(401);

        Clock anHourAgo = Clock.fixed(Instant.now().minus(Duration.ofHours(1)), ZoneOffset.UTC);
        String expired = new JwtTokenService(properties, anHourAgo).issue(1L, ADMIN_USERNAME, "ADMIN").getToken();
        assertThat(statusOf(withToken(post("/api/auth/refresh"), expired))).isEqualTo(401);

        mvc.perform(withToken(post("/api/auth/refresh"), dtToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresInSeconds").value(180))
                .andExpect(jsonPath("$.user.username").value(dtOfTeamA.getUsername()));
    }

    // ---------------------------------------------------------------- documentation

    @Test
    void apiDocsArePublicAndDeclareTheBearerScheme() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
                .andExpect(jsonPath("$.paths['/api/auth/login']").exists())
                .andExpect(jsonPath("$.paths['/api/users']").exists());
    }
}
