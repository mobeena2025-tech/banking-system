package com.bank.management_system.transaction;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.management_system.account.Account;
import com.bank.management_system.account.AccountRepository;
import com.bank.management_system.account.AccountService;
import com.bank.management_system.common.ApiException;
import com.bank.management_system.common.Money;
import com.bank.management_system.transaction.TransactionDtos.StatementEntry;
import com.bank.management_system.transaction.TransactionDtos.TransferRequest;
import com.bank.management_system.user.User;
import com.bank.management_system.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
public class TransactionService {

    private final AccountRepository accounts;
    private final AccountService accountService;
    private final TransactionRepository transactions;
    private final UserRepository users;
    private final BigDecimal maxAmount;
    private final EntityManager em;

    public TransactionService(AccountRepository accounts, AccountService accountService,
            TransactionRepository transactions, UserRepository users, EntityManager em,
            @Value("${app.limits.max-transaction-amount}") BigDecimal maxAmount) {
        this.accounts = accounts;
        this.accountService = accountService;
        this.transactions = transactions;
        this.users = users;
        this.maxAmount = maxAmount;
        this.em = em;
    }

    @Transactional
    public Transaction deposit(Long userId, String accountNumber, BigDecimal amount, String description) {
        BigDecimal value = checkAmount(amount);
        Account account = lock(accountService.getOwned(accountNumber, userId));
        account.credit(value);
        return transactions.save(new Transaction(newReference(), TransactionType.DEPOSIT, value, null, account,
                description, user(userId), null));
    }

    @Transactional
    public Transaction withdraw(Long userId, String accountNumber, BigDecimal amount, String description) {
        BigDecimal value = checkAmount(amount);
        Account account = lock(accountService.getOwned(accountNumber, userId));
        account.debit(value);
        return transactions.save(new Transaction(newReference(), TransactionType.WITHDRAWAL, value, account, null,
                description, user(userId), null));
    }

    @Transactional
    public Transaction transfer(Long userId, TransferRequest req) {
        String key = blankToNull(req.idempotencyKey());
        if (key != null) {
            var previous = transactions.findByInitiatedByIdAndIdempotencyKey(userId, key);
            if (previous.isPresent()) {
                return previous.get();
            }
        }

        BigDecimal value = checkAmount(req.amount());
        Account from = accountService.getOwned(req.fromAccount(), userId);
        Account to = accounts.findByAccountNumber(req.toAccount())
                .orElseThrow(() -> ApiException.notFound("Destination account not found"));
        if (from.getId().equals(to.getId())) {
            throw ApiException.badRequest("Can't transfer to the same account");
        }

        // Always lock the lower id first so two opposite transfers can't deadlock.
        if (from.getId() < to.getId()) {
            from = lock(from);
            to = lock(to);
        } else {
            to = lock(to);
            from = lock(from);
        }

        to.requireActive();
        from.debit(value);
        to.credit(value);
        return transactions.save(new Transaction(newReference(), TransactionType.TRANSFER, value, from, to,
                req.description(), user(userId), key));
    }

    @Transactional(readOnly = true)
    public Page<StatementEntry> statement(Long userId, String accountNumber, Pageable pageable) {
        Account account = accountService.getOwned(accountNumber, userId);
        return transactions.findForAccount(account.getId(), pageable)
                .map(t -> StatementEntry.of(t, account.getId()));
    }

    private BigDecimal checkAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            throw ApiException.badRequest("Amount must be positive");
        }
        if (amount.compareTo(maxAmount) > 0) {
            throw ApiException.unprocessable("Amount exceeds the per-transaction limit of " + maxAmount);
        }
        try {
            return Money.of(amount);
        } catch (ArithmeticException e) {
            throw ApiException.badRequest("Amount can have at most 2 decimal places");
        }
    }

    /**
     * Takes a row lock (SELECT ... FOR UPDATE) and re-reads the row. Re-reading matters: the
     * account was already loaded without a lock, and its balance may be stale by now.
     */
    private Account lock(Account account) {
        em.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        return account;
    }

    private User user(Long id) {
        return users.getReferenceById(id);
    }

    private static String newReference() {
        String date = LocalDate.now(ZoneOffset.UTC).format(DateTimeFormatter.BASIC_ISO_DATE);
        return "TXN" + date + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
