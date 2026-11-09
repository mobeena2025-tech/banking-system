package com.bank.management_system.transaction;

import java.math.BigDecimal;
import java.time.Instant;

import com.bank.management_system.account.Account;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class TransactionDtos {

    private TransactionDtos() {
    }

    public record AmountRequest(
            @NotNull @DecimalMin(value = "0.01", message = "must be at least 0.01")
            @Digits(integer = 13, fraction = 2, message = "must have at most 2 decimal places") BigDecimal amount,
            @Size(max = 255) String description) {
    }

    public record TransferRequest(
            @NotBlank String fromAccount,
            @NotBlank @Pattern(regexp = "\\d{12}", message = "must be a 12-digit account number") String toAccount,
            @NotNull @DecimalMin(value = "0.01", message = "must be at least 0.01")
            @Digits(integer = 13, fraction = 2, message = "must have at most 2 decimal places") BigDecimal amount,
            @Size(max = 255) String description,
            /** Optional. Retrying with the same key returns the original transfer instead of sending twice. */
            @Size(max = 64) String idempotencyKey) {
    }

    /** A transaction seen from one account's point of view. */
    public record StatementEntry(String reference, TransactionType type, String direction, BigDecimal amount,
            BigDecimal balanceAfter, String counterparty, String description, Instant createdAt) {

        public static StatementEntry of(Transaction t, Long accountId) {
            boolean outgoing = t.getFromAccount() != null && t.getFromAccount().getId().equals(accountId);
            Account other = outgoing ? t.getToAccount() : t.getFromAccount();
            return new StatementEntry(t.getReference(), t.getType(), outgoing ? "DEBIT" : "CREDIT",
                    t.getAmount(), outgoing ? t.getFromBalanceAfter() : t.getToBalanceAfter(),
                    other == null ? null : other.getAccountNumber(), t.getDescription(), t.getCreatedAt());
        }
    }

    public record TransactionResponse(String reference, TransactionType type, BigDecimal amount,
            String fromAccount, String toAccount, BigDecimal balanceAfter, String description, Instant createdAt) {

        /** balanceAfter is shown for the caller's own account (the source, or the target for deposits). */
        public static TransactionResponse from(Transaction t) {
            return new TransactionResponse(t.getReference(), t.getType(), t.getAmount(),
                    t.getFromAccount() == null ? null : t.getFromAccount().getAccountNumber(),
                    t.getToAccount() == null ? null : t.getToAccount().getAccountNumber(),
                    t.getFromAccount() != null ? t.getFromBalanceAfter() : t.getToBalanceAfter(),
                    t.getDescription(), t.getCreatedAt());
        }
    }
}
