package com.jeczzu.fintechapi.service;


import java.util.UUID;

import org.springframework.stereotype.Service;

import com.jeczzu.fintechapi.entity.Account;
import com.jeczzu.fintechapi.exception.ConflictException;
import com.jeczzu.fintechapi.exception.ResourceNotFoundException;
import com.jeczzu.fintechapi.repository.AccountRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AccountService {

  private final AccountRepository accountRepository;

  public Account createAccount(String ownerName, String email) {

    if (accountRepository.findByEmail(email).isPresent()) {
      throw new ConflictException("Email already registered");
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
        .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
  }
}
