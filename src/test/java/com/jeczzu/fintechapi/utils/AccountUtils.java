package com.jeczzu.fintechapi.utils;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.jeczzu.fintechapi.entity.Account;

public final class AccountUtils {

  private AccountUtils() {
    throw new UnsupportedOperationException("Utility class");
  }

  public static Account buildAccount(UUID id, String ownerName, String email) {
    return Account.builder()
        .id(id)
        .ownerName(ownerName)
        .email(email)
        .balance(BigDecimal.ZERO)
        .createdAt(OffsetDateTime.now())
        .version(0L)
        .build();
  }

  public static Account buildAccount(BigDecimal balance) {
    return Account.builder()
        .id(UUID.randomUUID())
        .ownerName("Juan Pérez")
        .email("juan@mail.com")
        .balance(balance)
        .createdAt(OffsetDateTime.now())
        .version(0L)
        .build();
  }
}
