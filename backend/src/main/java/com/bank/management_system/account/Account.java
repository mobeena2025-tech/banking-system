package com.bank.management_system.account;

import java.math.BigDecimal;
import java.time.Instant;

import com.bank.management_system.common.ApiException;
import com.bank.management_system.user.User;
import jakarta.persistence.*;

@Entity
@Table(name = "accounts")
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_number", nullable = false, unique = true, length = 20)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id")
    private User owner;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AccountStatus status = AccountStatus.ACTIVE;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO.setScale(2);

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    /** Optimistic lock: a second line of defence against lost updates. */
    @Version
    private Long version;

    protected Account() {
    }

    public Account(String accountNumber, User owner, AccountType type) {
        this.accountNumber = accountNumber;
        this.owner = owner;
        this.type = type;
    }

    public void credit(BigDecimal amount) {
        requireActive();
        balance = balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        requireActive();
        if (balance.compareTo(amount) < 0) {
            throw ApiException.unprocessable("Insufficient funds in account " + accountNumber);
        }
        balance = balance.subtract(amount);
    }

    public void requireActive() {
        if (status != AccountStatus.ACTIVE) {
            throw ApiException.unprocessable("Account " + accountNumber + " is " + status.name().toLowerCase());
        }
    }

    public boolean isOwnedBy(Long userId) {
        return owner.getId().equals(userId);
    }

    public Long getId() { return id; }
    public String getAccountNumber() { return accountNumber; }
    public User getOwner() { return owner; }
    public AccountType getType() { return type; }
    public AccountStatus getStatus() { return status; }
    public BigDecimal getBalance() { return balance; }
    public Instant getCreatedAt() { return createdAt; }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }
}
