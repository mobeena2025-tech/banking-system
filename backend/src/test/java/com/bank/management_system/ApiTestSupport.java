package com.bank.management_system;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/** Small helpers so tests read like API calls. */
public abstract class ApiTestSupport {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected ObjectMapper json;

    protected record Resp(int status, JsonNode body) {
    }

    protected Resp call(MockHttpServletRequestBuilder req, String token, Object body) throws Exception {
        if (token != null) {
            req.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            req.contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        }
        MvcResult r = mvc.perform(req).andReturn();
        String content = r.getResponse().getContentAsString();
        return new Resp(r.getResponse().getStatus(), content.isEmpty() ? null : json.readTree(content));
    }

    protected String register(String name) throws Exception {
        String email = name.toLowerCase() + "-" + UUID.randomUUID().toString().substring(0, 8) + "@test.local";
        Resp r = call(post("/api/auth/register"), null,
                Map.of("name", name, "email", email, "password", "Secret123"));
        return r.body().get("token").asText();
    }

    protected String login(String email, String password) throws Exception {
        return call(post("/api/auth/login"), null, Map.of("email", email, "password", password))
                .body().get("token").asText();
    }

    protected String openAccount(String token) throws Exception {
        return call(post("/api/accounts"), token, Map.of("type", "SAVINGS")).body().get("accountNumber").asText();
    }

    protected Resp deposit(String token, String account, String amount) throws Exception {
        return call(post("/api/accounts/" + account + "/deposit"), token, Map.of("amount", amount));
    }

    protected Resp withdraw(String token, String account, String amount) throws Exception {
        return call(post("/api/accounts/" + account + "/withdraw"), token, Map.of("amount", amount));
    }

    protected Resp transfer(String token, String from, String to, String amount) throws Exception {
        return call(post("/api/transfers"), token, Map.of("fromAccount", from, "toAccount", to, "amount", amount));
    }

    protected String balance(String token, String account) throws Exception {
        return call(get("/api/accounts/" + account), token, null).body().get("balance").decimalValue()
                .setScale(2).toPlainString();
    }
}
