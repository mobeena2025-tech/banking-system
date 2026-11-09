package com.bank.management_system.account;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @EntityGraph(attributePaths = "owner")
    Optional<Account> findByAccountNumber(String accountNumber);

    @EntityGraph(attributePaths = "owner")
    List<Account> findByOwnerIdOrderByCreatedAtAsc(Long ownerId);

    @EntityGraph(attributePaths = "owner")
    Page<Account> findAllBy(Pageable pageable);

    boolean existsByAccountNumber(String accountNumber);
}
