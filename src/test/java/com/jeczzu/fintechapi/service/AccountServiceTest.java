package com.jeczzu.fintechapi.service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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

import com.jeczzu.fintechapi.entity.Account;
import com.jeczzu.fintechapi.exception.ConflictException;
import com.jeczzu.fintechapi.exception.ResourceNotFoundException;
import com.jeczzu.fintechapi.repository.AccountRepository;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

  @Mock
  private AccountRepository accountRepository;

  @InjectMocks
  private AccountService accountService;

  @Nested
  @DisplayName("createAccount")
  class CreateAccount {

    @Test
    @DisplayName("should create account when email is not taken")
    void shouldCreateAccount_whenEmailIsNotTaken() {

      String ownerName = "Juan Pérez";
      String email = "juan@mail.com";

      Account savedAccount = Account.builder()
          .id(UUID.randomUUID())
          .ownerName(ownerName)
          .email(email)
          .balance(BigDecimal.ZERO)
          .createdAt(OffsetDateTime.now())
          .version(0L)
          .build();

      when(accountRepository.findByEmail(email))
          .thenReturn(Optional.empty());
      when(accountRepository.save(any(Account.class)))
          .thenReturn(savedAccount);

      Account result = accountService.createAccount(ownerName, email);

      assertThat(result).isNotNull();
      assertThat(result.getOwnerName()).isEqualTo(ownerName);
      assertThat(result.getEmail()).isEqualTo(email);
      assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);

      verify(accountRepository).findByEmail(email);
      verify(accountRepository).save(any(Account.class));
    }

    @Test
    @DisplayName("should throw ConflictException when email is already taken")
    void shouldThrowConflictException_whenEmailIsAlreadyTaken() {

      String ownerName = "Juan Pérez";
      String email = "juan@mail.com";

      Account existingAccount = Account.builder()
          .id(UUID.randomUUID())
          .ownerName(ownerName)
          .email(email)
          .balance(BigDecimal.ZERO)
          .createdAt(OffsetDateTime.now())
          .version(0L)
          .build();

      when(accountRepository.findByEmail(email))
          .thenReturn(Optional.of(existingAccount));

      assertThatThrownBy(() -> accountService.createAccount(ownerName, email))
          .isInstanceOf(ConflictException.class)
          .hasMessageContaining("An account with email '" + email + "' already exists");

      verify(accountRepository).findByEmail(email);
      verify(accountRepository, never()).save(any(Account.class));
    }
  }

  @Nested
  @DisplayName("getAccountById")
  class GetAccountById {

    @Test
    @DisplayName("should throw ResourceNotFoundException when account does not exist")
    void shouldThrowResourceNotFoundException_whenAccountDoesNotExist() {

      UUID id = UUID.randomUUID();

      when(accountRepository.findById(id))
          .thenReturn(Optional.empty());

      assertThatThrownBy(() -> accountService.getAccountById(id))
          .isInstanceOf(ResourceNotFoundException.class)
          .hasMessageContaining("Account not found with id: " + id);

      verify(accountRepository).findById(id);
    }

    @Test
    @DisplayName("should return account when it exists")
    void shouldReturnAccount_whenItExists() {

      UUID id = UUID.randomUUID();
      String ownerName = "Juan Pérez";
      String email = "juan@mail.com";

      Account account = Account.builder()
          .id(id)
          .ownerName(ownerName)
          .email(email)
          .balance(BigDecimal.ZERO)
          .createdAt(OffsetDateTime.now())
          .version(0L)
          .build();

      when(accountRepository.findById(account.getId()))
          .thenReturn(Optional.of(account));

      Account result = accountService.getAccountById(account.getId());

      assertThat(result).isNotNull();
      assertThat(result.getId()).isEqualTo(id);
      assertThat(result.getOwnerName()).isEqualTo(ownerName);
      assertThat(result.getEmail()).isEqualTo(email);

      verify(accountRepository).findById(id);
    }
  }
}
