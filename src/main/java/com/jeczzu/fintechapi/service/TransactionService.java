package com.jeczzu.fintechapi.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.jeczzu.fintechapi.config.TransactionConstants;
import com.jeczzu.fintechapi.entity.Account;
import com.jeczzu.fintechapi.entity.Transaction;
import com.jeczzu.fintechapi.entity.TransactionType;
import com.jeczzu.fintechapi.exception.InsufficientFundsException;
import com.jeczzu.fintechapi.exception.InvalidAmountException;
import com.jeczzu.fintechapi.exception.ResourceNotFoundException;
import com.jeczzu.fintechapi.repository.AccountRepository;
import com.jeczzu.fintechapi.repository.TransactionRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TransactionService {

  private final TransactionRepository transactionRepository;
  private final AccountRepository accountRepository;

  @Transactional
  public Transaction createTransaction(UUID accountId, BigDecimal amount, TransactionType type) {

    validateAmount(amount);

    Account account = accountRepository.findById(accountId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Account not found with id: " + accountId));

    if (type == TransactionType.WITHDRAW) {
      if (account.getBalance().compareTo(amount) < 0) {
        throw new InsufficientFundsException(
            "Insufficient funds. Available: " + account.getBalance()
                + ", Requested: " + amount);
      }
      account.setBalance(account.getBalance().subtract(amount));
    }

    if (type == TransactionType.DEPOSIT) {
      BigDecimal newBalance = account.getBalance().add(amount);
      if (newBalance.compareTo(TransactionConstants.MAX_BALANCE) > 0) {
        throw new InvalidAmountException(
            "Deposit would exceed maximum allowed balance of " + TransactionConstants.MAX_BALANCE);
      }
      account.setBalance(newBalance);
    }

    Transaction transaction = Transaction.builder()
        .account(account)
        .amount(amount)
        .type(type)
        .build();

    accountRepository.save(account);
    return transactionRepository.save(transaction);
  }

  @Transactional
  public Page<Transaction> getTransactionsByAccountId(UUID accountId, Pageable pageable) {
    if (!accountRepository.existsById(accountId)) {
      throw new ResourceNotFoundException("Account not found with id: " + accountId);
    }
    return transactionRepository.findByAccountId(accountId, pageable);
  }

  @Transactional
  public Transaction getTransactionById(UUID accountId, UUID transactionId) {
    if (!accountRepository.existsById(accountId)) {
      throw new ResourceNotFoundException("Account not found with id: " + accountId);
    }
    return transactionRepository.findByIdAndAccountId(transactionId, accountId)
        .orElseThrow(() -> new ResourceNotFoundException(
            "Transaction not found with id: " + transactionId
                + " for account: " + accountId));
  }

  private void validateAmount(BigDecimal amount) {
    if (amount.stripTrailingZeros().scale() > TransactionConstants.MAX_DECIMAL_PLACES) {
      throw new InvalidAmountException(
          "Amount cannot have more than " + TransactionConstants.MAX_DECIMAL_PLACES + " decimal places");
    }
    if (amount.compareTo(TransactionConstants.MAX_TRANSACTION_AMOUNT) > 0) {
      throw new InvalidAmountException(
          "Amount exceeds maximum allowed per transaction: " + TransactionConstants.MAX_TRANSACTION_AMOUNT);
    }
  }
}
