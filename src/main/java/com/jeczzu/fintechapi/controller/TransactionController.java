package com.jeczzu.fintechapi.controller;

import java.net.URI;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jeczzu.fintechapi.config.ApiRoutes;
import com.jeczzu.fintechapi.dto.CreateTransactionRequest;
import com.jeczzu.fintechapi.dto.TransactionResponse;
import com.jeczzu.fintechapi.entity.Transaction;
import com.jeczzu.fintechapi.mapper.TransactionMapper;
import com.jeczzu.fintechapi.service.TransactionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiRoutes.TRANSACTIONS)
@RequiredArgsConstructor
public class TransactionController {

  private final TransactionService transactionService;

  @PostMapping
  public ResponseEntity<TransactionResponse> createTransaction(
      @PathVariable UUID accountId,
      @Valid @RequestBody CreateTransactionRequest request) {

    Transaction transaction = transactionService.createTransaction(
        accountId, request.amount(), request.type());

    TransactionResponse response = TransactionMapper.toResponse(transaction);

    URI location = URI.create(
        ApiRoutes.ACCOUNTS + "/" + accountId + "/transactions/" + transaction.getId());
    return ResponseEntity.created(location).body(response);
  }

  @GetMapping
  public ResponseEntity<Page<TransactionResponse>> getTransactions(
      @PathVariable UUID accountId,
      @PageableDefault(size = 20) Pageable pageable) {

    Page<TransactionResponse> transactions = transactionService
        .getTransactionsByAccountId(accountId, pageable)
        .map(TransactionMapper::toResponse);

    return ResponseEntity.ok(transactions);
  }

  @GetMapping("/{transactionId}")
  public ResponseEntity<TransactionResponse> getTransaction(
      @PathVariable UUID accountId,
      @PathVariable UUID transactionId) {

    Transaction transaction = transactionService
        .getTransactionById(accountId, transactionId);

    return ResponseEntity.ok(TransactionMapper.toResponse(transaction));
  }
}
