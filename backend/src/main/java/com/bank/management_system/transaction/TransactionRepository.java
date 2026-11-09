package com.bank.management_system.transaction;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @EntityGraph(attributePaths = {"fromAccount", "toAccount"})
    @Query("""
            select t from Transaction t
            where t.fromAccount.id = :accountId or t.toAccount.id = :accountId
            order by t.createdAt desc, t.id desc
            """)
    Page<Transaction> findForAccount(@Param("accountId") Long accountId, Pageable pageable);

    @EntityGraph(attributePaths = {"fromAccount", "toAccount"})
    Optional<Transaction> findByInitiatedByIdAndIdempotencyKey(Long userId, String idempotencyKey);
}
