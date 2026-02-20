package com.jeczzu.fintechapi.service;


import com.jeczzu.fintechapi.entity.Account;
import com.jeczzu.fintechapi.exception.BusinessException;
import com.jeczzu.fintechapi.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

  private final AccountRepository accountRepository;

  public Account createAccount(String ownerName, String email) {

    if (accountRepository.findByEmail(email).isPresent()) {
      throw new BusinessException("Email already registered");
    }

    Account account = Account
        .builder()
        .ownerName(ownerName)
        .email(email)
        .build();

    return accountRepository.save(account);
  }

  public Account getAccountById(UUID id) {
    return accountRepository.findById(id)
        .orElseThrow(() -> new BusinessException("Account not found"));
  }
}
