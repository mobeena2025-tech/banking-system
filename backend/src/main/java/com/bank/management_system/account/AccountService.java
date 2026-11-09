package com.bank.management_system.account;

import java.security.SecureRandom;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.management_system.common.ApiException;
import com.bank.management_system.user.User;
import com.bank.management_system.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;

@Service
public class AccountService {

    private static final int MAX_OPEN_ACCOUNTS = 5;
    private final SecureRandom random = new SecureRandom();
    private final AccountRepository accounts;
    private final UserRepository users;
    private final EntityManager em;

    public AccountService(AccountRepository accounts, UserRepository users, EntityManager em) {
        this.accounts = accounts;
        this.users = users;
        this.em = em;
    }

    @Transactional
    public Account open(Long userId, AccountType type) {
        User owner = users.findById(userId).orElseThrow(() -> ApiException.notFound("User not found"));
        long open = accounts.findByOwnerIdOrderByCreatedAtAsc(userId).stream()
                .filter(a -> a.getStatus() != AccountStatus.CLOSED).count();
        if (open >= MAX_OPEN_ACCOUNTS) {
            throw ApiException.unprocessable("You can have at most " + MAX_OPEN_ACCOUNTS + " open accounts");
        }
        return accounts.save(new Account(newAccountNumber(), owner, type));
    }

    @Transactional(readOnly = true)
    public List<Account> listForUser(Long userId) {
        return accounts.findByOwnerIdOrderByCreatedAtAsc(userId);
    }

    /** Loads an account the user owns. Other people's accounts look "not found" so they can't be probed. */
    @Transactional(readOnly = true)
    public Account getOwned(String accountNumber, Long userId) {
        return accounts.findByAccountNumber(accountNumber)
                .filter(a -> a.isOwnedBy(userId))
                .orElseThrow(() -> ApiException.notFound("Account not found"));
    }

    @Transactional
    public Account close(String accountNumber, Long userId) {
        Account account = getOwned(accountNumber, userId);
        em.refresh(account, LockModeType.PESSIMISTIC_WRITE);
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw ApiException.conflict("Account is already closed");
        }
        if (account.getBalance().signum() != 0) {
            throw ApiException.unprocessable("Withdraw or transfer the remaining balance before closing");
        }
        account.setStatus(AccountStatus.CLOSED);
        return account;
    }

    @Transactional
    public Account changeStatus(String accountNumber, AccountStatus status) {
        Account account = accounts.findByAccountNumber(accountNumber)
                .orElseThrow(() -> ApiException.notFound("Account not found"));
        if (account.getStatus() == AccountStatus.CLOSED) {
            throw new ApiException(HttpStatus.CONFLICT, "Closed accounts can't be changed");
        }
        if (status == AccountStatus.CLOSED && account.getBalance().signum() != 0) {
            throw ApiException.unprocessable("Account still has a balance");
        }
        account.setStatus(status);
        return account;
    }

    private String newAccountNumber() {
        String number;
        do {
            number = String.format("%012d", random.nextLong(1_000_000_000_000L));
        } while (accounts.existsByAccountNumber(number));
        return number;
    }
}
