package com.jeczzu.fintechapi.integration;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import com.jayway.jsonpath.JsonPath;
import com.jeczzu.fintechapi.config.ApiRoutes;
import com.jeczzu.fintechapi.dto.AccountResponse;
import com.jeczzu.fintechapi.dto.CreateAccountRequest;
import com.jeczzu.fintechapi.dto.CreateTransactionRequest;
import com.jeczzu.fintechapi.dto.TransactionResponse;
import com.jeczzu.fintechapi.entity.TransactionType;
import com.jeczzu.fintechapi.repository.AccountRepository;
import com.jeczzu.fintechapi.repository.TransactionRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountTransactionIntegrationTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private TransactionRepository transactionRepository;

  @Autowired
  private AccountRepository accountRepository;

  @BeforeEach
  void cleanDatabase() {
    transactionRepository.deleteAll();
    accountRepository.deleteAll();
  }

  private AccountResponse createAccount(String name, String email) {
    ResponseEntity<AccountResponse> response = restTemplate.postForEntity(
        ApiRoutes.ACCOUNTS,
        new CreateAccountRequest(name, email),
        AccountResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    return response.getBody();
  }

  private TransactionResponse createTransaction(AccountResponse account, BigDecimal amount, TransactionType type) {
    ResponseEntity<TransactionResponse> response = restTemplate.postForEntity(
        ApiRoutes.TRANSACTIONS,
        new CreateTransactionRequest(amount, type),
        TransactionResponse.class,
        account.id());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    return response.getBody();
  }

  @Test
  @DisplayName("full flow: create account → deposit → withdraw → verify balance")
  void fullFlow_createAccount_deposit_withdraw_verifyBalance() {

    AccountResponse account = createAccount("Juan Pérez", "juan@mail.com");
    assertThat(account.balance()).isEqualByComparingTo(BigDecimal.ZERO);

    TransactionResponse deposit = createTransaction(account, new BigDecimal("500.00"), TransactionType.DEPOSIT);
    assertThat(deposit.amount()).isEqualByComparingTo(new BigDecimal("500.00"));
    assertThat(deposit.type()).isEqualTo(TransactionType.DEPOSIT);
    assertThat(deposit.accountId()).isEqualTo(account.id());

    TransactionResponse withdraw = createTransaction(account, new BigDecimal("200.00"), TransactionType.WITHDRAW);
    assertThat(withdraw.amount()).isEqualByComparingTo(new BigDecimal("200.00"));
    assertThat(withdraw.type()).isEqualTo(TransactionType.WITHDRAW);

    ResponseEntity<AccountResponse> getResponse = restTemplate.getForEntity(
        ApiRoutes.ACCOUNTS + "/{id}",
        AccountResponse.class,
        account.id());

    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody()).isNotNull();
    assertThat(getResponse.getBody().balance()).isEqualByComparingTo(new BigDecimal("300.00"));
  }

  @Test
  @DisplayName("should return 409 when withdrawing with insufficient funds")
  void shouldReturn409_whenWithdrawingWithInsufficientFunds() {

    AccountResponse account = createAccount("Juan Pérez", "juan@mail.com");
    createTransaction(account, new BigDecimal("100.00"), TransactionType.DEPOSIT);

    ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
        ApiRoutes.TRANSACTIONS,
        new CreateTransactionRequest(new BigDecimal("500.00"), TransactionType.WITHDRAW),
        ProblemDetail.class,
        account.id());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getTitle()).isEqualTo("Conflict");

    ResponseEntity<AccountResponse> getResponse = restTemplate.getForEntity(
        ApiRoutes.ACCOUNTS + "/{id}",
        AccountResponse.class,
        account.id());

    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody()).isNotNull();
    assertThat(getResponse.getBody().balance()).isEqualByComparingTo(new BigDecimal("100.00"));
  }

  @Test
  @DisplayName("should return 409 when creating account with duplicate email")
  void shouldReturn409_whenCreatingAccountWithDuplicateEmail() {

    createAccount("Juan Pérez", "juan@mail.com");
    ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
        ApiRoutes.ACCOUNTS,
        new CreateAccountRequest("Juan Pérez", "juan@mail.com"),
        ProblemDetail.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getTitle()).isEqualTo("Conflict");
    assertThat(accountRepository.count()).isEqualTo(1);
  }

  @Test
  @DisplayName("should return 404 when depositing to non-existent account")
  void shouldReturn404_whenDepositingToNonExistentAccount() {

    ResponseEntity<ProblemDetail> response = restTemplate.postForEntity(
        ApiRoutes.TRANSACTIONS,
        new CreateTransactionRequest(new BigDecimal("100.00"), TransactionType.DEPOSIT),
        ProblemDetail.class,
        java.util.UUID.randomUUID());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().getTitle()).isEqualTo("Resource Not Found");
  }

  @Test
  @DisplayName("should return paginated transactions after multiple deposits")
  void shouldReturnPaginatedTransactions_afterMultipleDeposits() {

    AccountResponse account = createAccount("Juan Pérez", "juan@mail.com");
    createTransaction(account, new BigDecimal("100.00"), TransactionType.DEPOSIT);
    createTransaction(account, new BigDecimal("200.00"), TransactionType.DEPOSIT);
    createTransaction(account, new BigDecimal("300.00"), TransactionType.DEPOSIT);

    ResponseEntity<String> response = restTemplate.getForEntity(
        ApiRoutes.TRANSACTIONS + "?page=0&size=10",
        String.class,
        account.id());

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).isNotNull();

    String body = response.getBody();
    int totalElements = JsonPath.read(body, "$.page.totalElements");
    assertThat(totalElements).isEqualTo(3);

    List<Number> amounts = JsonPath.read(body, "$.content[*].amount");
    assertThat(amounts).hasSize(3).containsExactlyInAnyOrder(100.00, 200.00, 300.00);

    // Verificar que el balance final de la cuenta es 600.00
    ResponseEntity<AccountResponse> getResponse = restTemplate.getForEntity(
        ApiRoutes.ACCOUNTS + "/{id}",
        AccountResponse.class,
        account.id());

    assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(getResponse.getBody()).isNotNull();
    assertThat(getResponse.getBody().balance()).isEqualByComparingTo(new BigDecimal("600.00"));
  }
}
