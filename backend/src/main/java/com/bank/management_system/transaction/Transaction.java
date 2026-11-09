package com.bank.management_system.transaction;

import java.math.BigDecimal;
import java.time.Instant;

import com.bank.management_system.account.Account;
import com.bank.management_system.user.User;
import jakarta.persistence.*;

/**
 * One money movement. Deposits have only a "to" account, withdrawals only a
 * "from" account, transfers have both. Rows are never updated or deleted.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionType type;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private Account fromAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private Account toAccount;

    @Column(name = "from_balance_after", precision = 19, scale = 2)
    private BigDecimal fromBalanceAfter;

    @Column(name = "to_balance_after", precision = 19, scale = 2)
    private BigDecimal toBalanceAfter;

    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "initiated_by")
    private User initiatedBy;

    @Column(name = "idempotency_key", length = 64)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected Transaction() {
    }

    public Transaction(String reference, TransactionType type, BigDecimal amount, Account from, Account to,
            String description, User initiatedBy, String idempotencyKey) {
        this.reference = reference;
        this.type = type;
        this.amount = amount;
        this.fromAccount = from;
        this.toAccount = to;
        this.fromBalanceAfter = from == null ? null : from.getBalance();
        this.toBalanceAfter = to == null ? null : to.getBalance();
        this.description = description;
        this.initiatedBy = initiatedBy;
        this.idempotencyKey = idempotencyKey;
    }

    public Long getId() { return id; }
    public String getReference() { return reference; }
    public TransactionType getType() { return type; }
    public BigDecimal getAmount() { return amount; }
    public Account getFromAccount() { return fromAccount; }
    public Account getToAccount() { return toAccount; }
    public BigDecimal getFromBalanceAfter() { return fromBalanceAfter; }
    public BigDecimal getToBalanceAfter() { return toBalanceAfter; }
    public String getDescription() { return description; }
    public Instant getCreatedAt() { return createdAt; }
}
