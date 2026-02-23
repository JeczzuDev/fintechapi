package com.jeczzu.fintechapi.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.jeczzu.fintechapi.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

  Page<Transaction> findByAccountId(UUID accountId, Pageable pageable);

  Optional<Transaction> findByIdAndAccountId(UUID id, UUID accountId);
}
