package com.bank.management_system.admin;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import com.bank.management_system.account.AccountDtos.AccountResponse;
import com.bank.management_system.account.AccountDtos.StatusChangeRequest;
import com.bank.management_system.account.AccountRepository;
import com.bank.management_system.account.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/** Admin-only endpoints. Access is enforced in SecurityConfig. */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin")
@SecurityRequirement(name = "bearer")
public class AdminController {

    private final AccountRepository accounts;
    private final AccountService accountService;

    public AdminController(AccountRepository accounts, AccountService accountService) {
        this.accounts = accounts;
        this.accountService = accountService;
    }

    @GetMapping("/accounts")
    @Operation(summary = "All accounts")
    public Page<AccountResponse> all(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var pr = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by("createdAt").descending());
        return accounts.findAllBy(pr).map(AccountResponse::from);
    }

    @PatchMapping("/accounts/{accountNumber}/status")
    @Operation(summary = "Freeze, unfreeze or close an account")
    public AccountResponse setStatus(@PathVariable String accountNumber, @Valid @RequestBody StatusChangeRequest req) {
        return AccountResponse.from(accountService.changeStatus(accountNumber, req.status()));
    }
}
