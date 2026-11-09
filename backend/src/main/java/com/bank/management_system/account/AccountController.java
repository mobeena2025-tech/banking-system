package com.bank.management_system.account;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.bank.management_system.account.AccountDtos.AccountResponse;
import com.bank.management_system.account.AccountDtos.OpenAccountRequest;
import com.bank.management_system.auth.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts")
@SecurityRequirement(name = "bearer")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Open a new account")
    public AccountResponse open(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody OpenAccountRequest req) {
        return AccountResponse.from(service.open(CurrentUser.id(jwt), req.type()));
    }

    @GetMapping
    @Operation(summary = "List my accounts")
    public List<AccountResponse> mine(@AuthenticationPrincipal Jwt jwt) {
        return service.listForUser(CurrentUser.id(jwt)).stream().map(AccountResponse::from).toList();
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get one of my accounts")
    public AccountResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable String accountNumber) {
        return AccountResponse.from(service.getOwned(accountNumber, CurrentUser.id(jwt)));
    }

    @PostMapping("/{accountNumber}/close")
    @Operation(summary = "Close an account with zero balance")
    public AccountResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable String accountNumber) {
        return AccountResponse.from(service.close(accountNumber, CurrentUser.id(jwt)));
    }
}
