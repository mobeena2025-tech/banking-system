package com.bank.management_system.account;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.validation.constraints.NotNull;

public final class AccountDtos {

    private AccountDtos() {
    }

    public record OpenAccountRequest(@NotNull AccountType type) {
    }

    public record AccountResponse(String accountNumber, AccountType type, AccountStatus status,
            BigDecimal balance, Instant createdAt, String ownerName, String ownerEmail) {

        public static AccountResponse from(Account a) {
            return new AccountResponse(a.getAccountNumber(), a.getType(), a.getStatus(), a.getBalance(),
                    a.getCreatedAt(), a.getOwner().getName(), a.getOwner().getEmail());
        }
    }

    public record StatusChangeRequest(@NotNull AccountStatus status) {
    }
}
