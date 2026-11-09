package com.bank.management_system;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import com.bank.management_system.account.Account;
import com.bank.management_system.account.AccountRepository;
import com.bank.management_system.account.AccountService;
import com.bank.management_system.account.AccountType;
import com.bank.management_system.transaction.TransactionDtos.TransferRequest;
import com.bank.management_system.transaction.TransactionService;
import com.bank.management_system.user.Role;
import com.bank.management_system.user.User;
import com.bank.management_system.user.UserRepository;

/** Many requests hitting the same accounts at once must never create or lose money. */
@SpringBootTest
@ActiveProfiles("test")
class ConcurrencyTest {

    @Autowired
    UserRepository users;
    @Autowired
    AccountService accounts;
    @Autowired
    AccountRepository accountRepo;
    @Autowired
    TransactionService tx;

    private Long newUser(String name) {
        return users.save(new User(name, name + System.nanoTime() + "@test.local", "x", null, Role.USER)).getId();
    }

    @Test
    void parallelWithdrawalsNeverOverdraw() throws Exception {
        Long user = newUser("par");
        Account acc = accounts.open(user, AccountType.SAVINGS);
        tx.deposit(user, acc.getAccountNumber(), new BigDecimal("100"), null);

        AtomicInteger ok = new AtomicInteger();
        List<Callable<Void>> jobs = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            jobs.add(() -> {
                try {
                    tx.withdraw(user, acc.getAccountNumber(), new BigDecimal("10"), null);
                    ok.incrementAndGet();
                } catch (RuntimeException expected) {
                    // insufficient funds once the balance hits zero
                }
                return null;
            });
        }
        runAll(jobs);

        assertThat(ok.get()).isEqualTo(10);
        assertThat(accountRepo.findById(acc.getId()).orElseThrow().getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void opposingTransfersDontDeadlockAndKeepTotals() throws Exception {
        Long u1 = newUser("a");
        Long u2 = newUser("b");
        Account a = accounts.open(u1, AccountType.SAVINGS);
        Account b = accounts.open(u2, AccountType.SAVINGS);
        tx.deposit(u1, a.getAccountNumber(), new BigDecimal("1000"), null);
        tx.deposit(u2, b.getAccountNumber(), new BigDecimal("1000"), null);

        List<Callable<Void>> jobs = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            jobs.add(() -> {
                tx.transfer(u1, new TransferRequest(a.getAccountNumber(), b.getAccountNumber(),
                        new BigDecimal("7"), null, null));
                return null;
            });
            jobs.add(() -> {
                tx.transfer(u2, new TransferRequest(b.getAccountNumber(), a.getAccountNumber(),
                        new BigDecimal("3"), null, null));
                return null;
            });
        }
        runAll(jobs);

        BigDecimal balA = accountRepo.findById(a.getId()).orElseThrow().getBalance();
        BigDecimal balB = accountRepo.findById(b.getId()).orElseThrow().getBalance();
        assertThat(balA).isEqualByComparingTo("920");  // 1000 - 20*7 + 20*3
        assertThat(balB).isEqualByComparingTo("1080");
    }

    private static void runAll(List<Callable<Void>> jobs) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            for (Future<Void> f : pool.invokeAll(jobs)) {
                f.get();
            }
        } finally {
            pool.shutdown();
        }
    }
}
