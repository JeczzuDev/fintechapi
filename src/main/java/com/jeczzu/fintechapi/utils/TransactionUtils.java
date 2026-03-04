package com.jeczzu.fintechapi.utils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.jeczzu.fintechapi.entity.Account;
import com.jeczzu.fintechapi.entity.Transaction;
import com.jeczzu.fintechapi.entity.TransactionType;

public final class TransactionUtils {

  private TransactionUtils() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Transaction buildTransaction(Account account, BigDecimal amount, TransactionType type) {
    return Transaction.builder()
        .id(UUID.randomUUID())
        .account(account)
        .amount(amount)
        .type(type)
        .createdAt(OffsetDateTime.now())
        .build();
  }
}
