package com.jeczzu.fintechapi.mapper;

import com.jeczzu.fintechapi.dto.AccountResponse;
import com.jeczzu.fintechapi.entity.Account;

public final class AccountMapper {

  private AccountMapper() {
  }

  public static AccountResponse toResponse(Account account) {
    return new AccountResponse(
        account.getId(),
        account.getOwnerName(),
        account.getEmail(),
        account.getBalance(),
        account.getCreatedAt());
  }
}
