package com.bank.management_system;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiTest extends ApiTestSupport {

    @Test
    void registerLoginAndMe() throws Exception {
        Resp reg = call(post("/api/auth/register"), null,
                Map.of("name", "Asha", "email", "Asha@Example.com", "password", "Secret123"));
        assertThat(reg.status()).isEqualTo(201);
        assertThat(reg.body().at("/user/email").asText()).isEqualTo("asha@example.com");

        String token = login("asha@example.com", "Secret123");
        Resp me = call(get("/api/me"), token, null);
        assertThat(me.status()).isEqualTo(200);
        assertThat(me.body().get("name").asText()).isEqualTo("Asha");
        assertThat(me.body().get("role").asText()).isEqualTo("USER");
        assertThat(me.body().has("passwordHash")).isFalse();
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        Map<String, String> body = Map.of("name", "Dup", "email", "dup@example.com", "password", "Secret123");
        assertThat(call(post("/api/auth/register"), null, body).status()).isEqualTo(201);
        assertThat(call(post("/api/auth/register"), null, body).status()).isEqualTo(409);
    }

    @Test
    void weakPasswordAndBadEmailFailValidation() throws Exception {
        Resp r = call(post("/api/auth/register"), null,
                Map.of("name", "X", "email", "not-an-email", "password", "short"));
        assertThat(r.status()).isEqualTo(400);
        assertThat(r.body().get("errors").has("email")).isTrue();
        assertThat(r.body().get("errors").has("password")).isTrue();
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        call(post("/api/auth/register"), null,
                Map.of("name", "Ravi", "email", "ravi@example.com", "password", "Secret123"));
        Resp r = call(post("/api/auth/login"), null, Map.of("email", "ravi@example.com", "password", "Wrong1234"));
        assertThat(r.status()).isEqualTo(401);
    }

    @Test
    void protectedEndpointsNeedAToken() throws Exception {
        assertThat(call(get("/api/accounts"), null, null).status()).isEqualTo(401);
        assertThat(call(get("/api/accounts"), "not.a.jwt", null).status()).isEqualTo(401);
    }

    @Test
    void healthAndDocsArePublic() throws Exception {
        assertThat(call(get("/actuator/health"), null, null).status()).isEqualTo(200);
        assertThat(call(get("/v3/api-docs"), null, null).status()).isEqualTo(200);
    }
}
