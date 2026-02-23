package com.jeczzu.fintechapi.mapper;

import com.jeczzu.fintechapi.dto.TransactionResponse;
import com.jeczzu.fintechapi.entity.Transaction;

public final class TransactionMapper {

  private TransactionMapper() {
  }

  public static TransactionResponse toResponse(Transaction transaction) {
    return new TransactionResponse(
        transaction.getId(),
        transaction.getAccount().getId(),
        transaction.getAmount(),
        transaction.getType(),
        transaction.getCreatedAt());
  }
}
