package com.jeczzu.fintechapi.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ResponseEntity;

import com.jeczzu.fintechapi.dto.AccountResponse;
import com.jeczzu.fintechapi.dto.CreateAccountRequest;
import com.jeczzu.fintechapi.entity.Account;
import com.jeczzu.fintechapi.mapper.AccountMapper;
import com.jeczzu.fintechapi.service.AccountService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

  private final AccountService accountService;

  @PostMapping
  public ResponseEntity<AccountResponse> createAccount(
      @Valid @RequestBody CreateAccountRequest request) {

    Account account = accountService.createAccount(
        request.ownerName(),
        request.email());

    return ResponseEntity
        .created(URI.create("/api/accounts/" + account.getId()))
        .body(AccountMapper.toResponse(account));
  }

  @GetMapping("/{id}")
  public ResponseEntity<AccountResponse> getAccount(@PathVariable UUID id) {

    Account account = accountService.getAccountById(id);
    return ResponseEntity.ok(AccountMapper.toResponse(account));
  }
}
