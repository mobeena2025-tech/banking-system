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
class BankingApiTest extends ApiTestSupport {

    @Test
    void openAccountStartsAtZero() throws Exception {
        String token = register("Meera");
        Resp r = call(post("/api/accounts"), token, Map.of("type", "CHECKING"));
        assertThat(r.status()).isEqualTo(201);
        assertThat(r.body().get("accountNumber").asText()).matches("\\d{12}");
        assertThat(r.body().get("status").asText()).isEqualTo("ACTIVE");
        assertThat(balance(token, r.body().get("accountNumber").asText())).isEqualTo("0.00");
        assertThat(call(get("/api/accounts"), token, null).body().size()).isEqualTo(1);
    }

    @Test
    void depositAndWithdraw() throws Exception {
        String token = register("Kiran");
        String acc = openAccount(token);

        Resp d = deposit(token, acc, "500.50");
        assertThat(d.status()).isEqualTo(201);
        assertThat(d.body().get("balanceAfter").decimalValue()).isEqualByComparingTo("500.50");

        assertThat(withdraw(token, acc, "200.25").status()).isEqualTo(201);
        assertThat(balance(token, acc)).isEqualTo("300.25");
    }

    @Test
    void cannotOverdraw() throws Exception {
        String token = register("Neha");
        String acc = openAccount(token);
        deposit(token, acc, "100");
        Resp r = withdraw(token, acc, "100.01");
        assertThat(r.status()).isEqualTo(422);
        assertThat(r.body().get("detail").asText()).contains("Insufficient funds");
        assertThat(balance(token, acc)).isEqualTo("100.00");
    }

    @Test
    void invalidAmountsAreRejected() throws Exception {
        String token = register("Om");
        String acc = openAccount(token);
        assertThat(deposit(token, acc, "0").status()).isEqualTo(400);
        assertThat(deposit(token, acc, "-5").status()).isEqualTo(400);
        assertThat(deposit(token, acc, "1.999").status()).isEqualTo(400);
        assertThat(deposit(token, acc, "5000000").status()).isEqualTo(422);
    }

    @Test
    void transferMovesMoneyAndShowsOnBothStatements() throws Exception {
        String alice = register("Alice");
        String bob = register("Bob");
        String a = openAccount(alice);
        String b = openAccount(bob);
        deposit(alice, a, "1000");

        Resp t = transfer(alice, a, b, "250");
        assertThat(t.status()).isEqualTo(201);
        assertThat(t.body().get("reference").asText()).startsWith("TXN");

        assertThat(balance(alice, a)).isEqualTo("750.00");
        assertThat(balance(bob, b)).isEqualTo("250.00");

        Resp aliceStmt = call(get("/api/accounts/" + a + "/transactions"), alice, null);
        assertThat(aliceStmt.body().at("/content/0/direction").asText()).isEqualTo("DEBIT");
        assertThat(aliceStmt.body().at("/content/0/balanceAfter").decimalValue()).isEqualByComparingTo("750");
        assertThat(aliceStmt.body().at("/content/0/counterparty").asText()).isEqualTo(b);
        assertThat(aliceStmt.body().at("/content/1/type").asText()).isEqualTo("DEPOSIT");

        Resp bobStmt = call(get("/api/accounts/" + b + "/transactions"), bob, null);
        assertThat(bobStmt.body().at("/content/0/direction").asText()).isEqualTo("CREDIT");
        assertThat(bobStmt.body().at("/content/0/balanceAfter").decimalValue()).isEqualByComparingTo("250");
    }

    @Test
    void cannotUseSomeoneElsesAccount() throws Exception {
        String owner = register("Owner");
        String thief = register("Thief");
        String acc = openAccount(owner);
        deposit(owner, acc, "100");
        String thiefAcc = openAccount(thief);

        assertThat(call(get("/api/accounts/" + acc), thief, null).status()).isEqualTo(404);
        assertThat(withdraw(thief, acc, "10").status()).isEqualTo(404);
        assertThat(transfer(thief, acc, thiefAcc, "10").status()).isEqualTo(404);
        assertThat(call(get("/api/accounts/" + acc + "/transactions"), thief, null).status()).isEqualTo(404);
        assertThat(balance(owner, acc)).isEqualTo("100.00");
    }

    @Test
    void transferRules() throws Exception {
        String user = register("Rules");
        String a = openAccount(user);
        deposit(user, a, "50");
        assertThat(transfer(user, a, a, "10").status()).isEqualTo(400);
        assertThat(transfer(user, a, "000000000000", "10").status()).isEqualTo(404);
        assertThat(transfer(user, a, "123", "10").status()).isEqualTo(400);
    }

    @Test
    void sameIdempotencyKeyOnlyTransfersOnce() throws Exception {
        String user = register("Idem");
        String a = openAccount(user);
        String b = openAccount(user);
        deposit(user, a, "100");
        Map<String, String> body = Map.of("fromAccount", a, "toAccount", b, "amount", "40",
                "idempotencyKey", "order-42");

        Resp first = call(post("/api/transfers"), user, body);
        Resp retry = call(post("/api/transfers"), user, body);
        assertThat(retry.body().get("reference").asText()).isEqualTo(first.body().get("reference").asText());
        assertThat(balance(user, a)).isEqualTo("60.00");
        assertThat(balance(user, b)).isEqualTo("40.00");
    }

    @Test
    void closeAccountOnlyWhenEmpty() throws Exception {
        String user = register("Closer");
        String acc = openAccount(user);
        deposit(user, acc, "5");
        assertThat(call(post("/api/accounts/" + acc + "/close"), user, null).status()).isEqualTo(422);
        withdraw(user, acc, "5");
        Resp closed = call(post("/api/accounts/" + acc + "/close"), user, null);
        assertThat(closed.body().get("status").asText()).isEqualTo("CLOSED");
        assertThat(deposit(user, acc, "1").status()).isEqualTo(422);
    }

    @Test
    void adminCanFreezeAccountsAndUsersCannotUseAdminApi() throws Exception {
        String user = register("Frozen");
        String other = register("Other");
        String acc = openAccount(user);
        String otherAcc = openAccount(other);
        deposit(user, acc, "100");

        assertThat(call(get("/api/admin/accounts"), user, null).status()).isEqualTo(403);

        String admin = login("admin@test.local", "AdminPass123");
        assertThat(call(get("/api/admin/accounts"), admin, null).status()).isEqualTo(200);
        Resp frozen = call(patch("/api/admin/accounts/" + acc + "/status"), admin, Map.of("status", "FROZEN"));
        assertThat(frozen.body().get("status").asText()).isEqualTo("FROZEN");

        assertThat(withdraw(user, acc, "10").status()).isEqualTo(422);
        assertThat(transfer(other, otherAcc, acc, "0.01").status()).isIn(422);

        call(patch("/api/admin/accounts/" + acc + "/status"), admin, Map.of("status", "ACTIVE"));
        assertThat(withdraw(user, acc, "10").status()).isEqualTo(201);
    }
}
