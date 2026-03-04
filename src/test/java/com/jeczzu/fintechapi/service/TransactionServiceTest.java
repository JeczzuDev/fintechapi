package com.jeczzu.fintechapi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import com.jeczzu.fintechapi.config.TransactionConstants;
import com.jeczzu.fintechapi.entity.Account;
import com.jeczzu.fintechapi.entity.Transaction;
import com.jeczzu.fintechapi.entity.TransactionType;
import com.jeczzu.fintechapi.exception.InsufficientFundsException;
import com.jeczzu.fintechapi.exception.InvalidAmountException;
import com.jeczzu.fintechapi.exception.ResourceNotFoundException;
import com.jeczzu.fintechapi.repository.AccountRepository;
import com.jeczzu.fintechapi.repository.TransactionRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

  @Mock
  private TransactionRepository transactionRepository;

  @Mock
  private AccountRepository accountRepository;

  @InjectMocks
  private TransactionService transactionService;

  private Account buildAccount(BigDecimal balance) {
    return Account.builder()
        .id(UUID.randomUUID())
        .ownerName("Juan Pérez")
        .email("juan@mail.com")
        .balance(balance)
        .createdAt(OffsetDateTime.now())
        .version(0L)
        .build();
  }

  private Transaction buildTransaction(Account account, BigDecimal amount, TransactionType type) {
    return Transaction.builder()
        .id(UUID.randomUUID())
        .account(account)
        .amount(amount)
        .type(type)
        .createdAt(OffsetDateTime.now())
        .build();
  }

  @Nested
  @DisplayName("createTransaction")
  class CreateTransaction {

    @Test
    @DisplayName("should deposit and update balance")
    void shouldDeposit_andUpdateBalance() {

      Account account = buildAccount(new BigDecimal("500.00"));
      UUID accountId = account.getId();
      BigDecimal depositAmount = new BigDecimal("200.00");

      Transaction savedTransaction = buildTransaction(account, depositAmount, TransactionType.DEPOSIT);

      when(accountRepository.findById(accountId))
          .thenReturn(Optional.of(account));
      when(transactionRepository.save(any(Transaction.class)))
          .thenReturn(savedTransaction);

      Transaction result = transactionService.createTransaction(
          accountId, depositAmount, TransactionType.DEPOSIT);

      assertThat(result).isNotNull();
      assertThat(result.getAmount()).isEqualByComparingTo(depositAmount);
      assertThat(result.getType()).isEqualTo(TransactionType.DEPOSIT);

      assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));

      verify(accountRepository).findById(accountId);
      verify(accountRepository).save(account);
      verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("should withdraw and update balance")
    void shouldWithdraw_andUpdateBalance() {

      Account account = buildAccount(new BigDecimal("1000.00"));
      UUID accountId = account.getId();
      BigDecimal withdrawAmount = new BigDecimal("300.00");

      Transaction savedTransaction = buildTransaction(account, withdrawAmount, TransactionType.WITHDRAW);

      when(accountRepository.findById(accountId))
          .thenReturn(Optional.of(account));
      when(transactionRepository.save(any(Transaction.class)))
          .thenReturn(savedTransaction);

      Transaction result = transactionService.createTransaction(
          accountId, withdrawAmount, TransactionType.WITHDRAW);

      assertThat(result).isNotNull();
      assertThat(result.getAmount()).isEqualByComparingTo(withdrawAmount);
      assertThat(result.getType()).isEqualTo(TransactionType.WITHDRAW);
      assertThat(account.getBalance()).isEqualByComparingTo(new BigDecimal("700.00"));

      verify(accountRepository).findById(accountId);
      verify(accountRepository).save(account);
      verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InsufficientFundsException when balance is insufficient")
    void shouldThrowInsufficientFundsException_whenBalanceIsInsufficient() {

      Account account = buildAccount(new BigDecimal("100.00"));
      UUID accountId = account.getId();
      BigDecimal withdrawAmount = new BigDecimal("500.00");

      when(accountRepository.findById(accountId))
          .thenReturn(Optional.of(account));

      assertThatThrownBy(
          () -> transactionService.createTransaction(accountId, withdrawAmount, TransactionType.WITHDRAW))
          .isInstanceOf(InsufficientFundsException.class)
          .hasMessageContaining(
              "Insufficient funds. Available: " + account.getBalance() + ", Requested: " + withdrawAmount);

      verify(accountRepository).findById(accountId);
      verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidAmountException when amount has more than 2 decimal places")
    void shouldThrowInvalidAmountException_whenAmountHasMoreThanTwoDecimalPlaces() {

      UUID accountId = UUID.randomUUID();
      BigDecimal withdrawAmount = new BigDecimal("100.123");

      assertThatThrownBy(
          () -> transactionService.createTransaction(accountId, withdrawAmount, TransactionType.WITHDRAW))
          .isInstanceOf(InvalidAmountException.class)
          .hasMessageContaining(
              "Amount cannot have more than " + TransactionConstants.MAX_DECIMAL_PLACES + " decimal places");

      verify(accountRepository, never()).findById(accountId);
      verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidAmountException when amount exceeds maximum per transaction")
    void shouldThrowInvalidAmountException_whenAmountExceedsMaximumPerTransaction() {

      UUID accountId = UUID.randomUUID();
      BigDecimal withdrawAmount = new BigDecimal("1000001.00");

      assertThatThrownBy(
          () -> transactionService.createTransaction(accountId, withdrawAmount, TransactionType.WITHDRAW))
          .isInstanceOf(InvalidAmountException.class)
          .hasMessageContaining(
              "Amount exceeds maximum allowed per transaction: " + TransactionConstants.MAX_TRANSACTION_AMOUNT);

      verify(accountRepository, never()).findById(accountId);
      verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw InvalidAmountException when deposit would exceed maximum balance")
    void shouldThrowInvalidAmountException_whenDepositWouldExceedMaximumBalance() {

      Account account = buildAccount(new BigDecimal("99999999.00"));
      UUID accountId = account.getId();
      BigDecimal depositAmount = new BigDecimal("1.00");

      when(accountRepository.findById(accountId))
          .thenReturn(Optional.of(account));

      assertThatThrownBy(
          () -> transactionService.createTransaction(accountId, depositAmount, TransactionType.DEPOSIT))
          .isInstanceOf(InvalidAmountException.class)
          .hasMessageContaining("Deposit would exceed maximum allowed balance of " + TransactionConstants.MAX_BALANCE);

      verify(accountRepository).findById(account.getId());
      verify(transactionRepository, never()).save(any(Transaction.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when account does not exist")
    void shouldThrowResourceNotFoundException_whenAccountDoesNotExist() {

      UUID accountId = UUID.randomUUID();
      BigDecimal depositAmount = new BigDecimal("200.00");

      when(accountRepository.findById(accountId))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> transactionService.createTransaction(accountId, depositAmount, TransactionType.DEPOSIT))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Account not found with id: " + accountId);

      verify(accountRepository).findById(accountId);
      verify(transactionRepository, never()).save(any(Transaction.class));
    }
  }

  @Nested
  @DisplayName("getTransactionsByAccountId")
  class GetTransactionsByAccountId {

    @Test
    @DisplayName("should return page of transactions when account exists")
    void shouldReturnPageOfTransactions_whenAccountExists() {

      Account account = buildAccount(new BigDecimal("1000.00"));
      UUID accountId = account.getId();

      List<Transaction> transactions = List.of(
          buildTransaction(account, new BigDecimal("100.00"), TransactionType.DEPOSIT),
          buildTransaction(account, new BigDecimal("50.00"), TransactionType.WITHDRAW));

      when(accountRepository.existsById(accountId))
          .thenReturn(true);
      when(transactionRepository.findByAccountId(any(UUID.class), any(Pageable.class)))
          .thenReturn(new PageImpl<>(transactions));

      Page<Transaction> result = transactionService.getTransactionsByAccountId(accountId, Pageable.unpaged());

      assertThat(result).isNotNull();
      assertThat(result.getContent()).hasSize(2);
      assertThat(result.getContent().get(0).getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
      assertThat(result.getContent().get(0).getType()).isEqualTo(TransactionType.DEPOSIT);
      assertThat(result.getContent().get(1).getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
      assertThat(result.getContent().get(1).getType()).isEqualTo(TransactionType.WITHDRAW);

      verify(accountRepository).existsById(accountId);
      verify(transactionRepository).findByAccountId(accountId, Pageable.unpaged());
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when account does not exist")
    void shouldThrowResourceNotFoundException_whenAccountDoesNotExist() {

      UUID accountId = UUID.randomUUID();
      Pageable pageable = Pageable.unpaged();

      when(accountRepository.existsById(accountId))
          .thenReturn(false);

      assertThatThrownBy(() -> transactionService.getTransactionsByAccountId(accountId, pageable))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Account not found with id: " + accountId);

      verify(accountRepository).existsById(accountId);
      verify(transactionRepository, never()).findByAccountId(any(UUID.class), any(Pageable.class));
    }
  }

  @Nested
  @DisplayName("getTransactionById")
  class GetTransactionById {

    @Test
    @DisplayName("should return transaction when account and transaction exist")
    void shouldReturnTransaction_whenAccountAndTransactionExist() {

      Account account = buildAccount(new BigDecimal("1000.00"));
      UUID accountId = account.getId();
      UUID transactionId = UUID.randomUUID();

      Transaction transaction = buildTransaction(account, new BigDecimal("100.00"), TransactionType.DEPOSIT);
      transaction.setId(transactionId);

      when(accountRepository.existsById(accountId))
          .thenReturn(true);
      when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
          .thenReturn(Optional.of(transaction));

      Transaction result = transactionService.getTransactionById(accountId, transactionId);

      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(transactionId);
      assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("100.00"));
      assertThat(result.getType()).isEqualTo(TransactionType.DEPOSIT);

      verify(accountRepository).existsById(accountId);
      verify(transactionRepository).findByIdAndAccountId(transactionId, accountId);
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when account does not exist")
    void shouldThrowResourceNotFoundException_whenAccountDoesNotExist() {

      UUID accountId = UUID.randomUUID();
      UUID transactionId = UUID.randomUUID();

      when(accountRepository.existsById(accountId))
          .thenReturn(false);

      assertThatThrownBy(() -> transactionService.getTransactionById(accountId, transactionId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Account not found with id: " + accountId);

      verify(accountRepository).existsById(accountId);
      verify(transactionRepository, never()).findByIdAndAccountId(any(UUID.class), any(UUID.class));
    }

    @Test
    @DisplayName("should throw ResourceNotFoundException when transaction does not exist for account")
    void shouldThrowResourceNotFoundException_whenTransactionDoesNotExistForAccount() {

      Account account = buildAccount(new BigDecimal("1000.00"));
      UUID accountId = account.getId();
      UUID transactionId = UUID.randomUUID();

      when(accountRepository.existsById(accountId))
          .thenReturn(true);
      when(transactionRepository.findByIdAndAccountId(transactionId, accountId))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> transactionService.getTransactionById(accountId, transactionId))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining(
              "Transaction not found with id: " + transactionId + " for account: " + accountId);

      verify(accountRepository).existsById(accountId);
      verify(transactionRepository).findByIdAndAccountId(transactionId, accountId);
    }
  }
}
