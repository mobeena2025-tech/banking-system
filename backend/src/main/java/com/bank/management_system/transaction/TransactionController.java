package com.bank.management_system.transaction;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import com.bank.management_system.auth.CurrentUser;
import com.bank.management_system.transaction.TransactionDtos.AmountRequest;
import com.bank.management_system.transaction.TransactionDtos.StatementEntry;
import com.bank.management_system.transaction.TransactionDtos.TransactionResponse;
import com.bank.management_system.transaction.TransactionDtos.TransferRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api")
@Tag(name = "Transactions")
@SecurityRequirement(name = "bearer")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping("/accounts/{accountNumber}/deposit")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Deposit money into one of my accounts")
    public TransactionResponse deposit(@AuthenticationPrincipal Jwt jwt, @PathVariable String accountNumber,
            @Valid @RequestBody AmountRequest req) {
        return TransactionResponse.from(
                service.deposit(CurrentUser.id(jwt), accountNumber, req.amount(), req.description()));
    }

    @PostMapping("/accounts/{accountNumber}/withdraw")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Withdraw money from one of my accounts")
    public TransactionResponse withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable String accountNumber,
            @Valid @RequestBody AmountRequest req) {
        return TransactionResponse.from(
                service.withdraw(CurrentUser.id(jwt), accountNumber, req.amount(), req.description()));
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Transfer money to any active account")
    public TransactionResponse transfer(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TransferRequest req) {
        return TransactionResponse.from(service.transfer(CurrentUser.id(jwt), req));
    }

    @GetMapping("/accounts/{accountNumber}/transactions")
    @Operation(summary = "Account statement, newest first")
    public Page<StatementEntry> statement(@AuthenticationPrincipal Jwt jwt, @PathVariable String accountNumber,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        PageRequest pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100));
        return service.statement(CurrentUser.id(jwt), accountNumber, pr);
    }
}
