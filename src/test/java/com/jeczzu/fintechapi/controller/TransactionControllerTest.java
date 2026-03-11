package com.jeczzu.fintechapi.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jeczzu.fintechapi.config.ApiRoutes;
import com.jeczzu.fintechapi.entity.Account;
import com.jeczzu.fintechapi.entity.Transaction;
import com.jeczzu.fintechapi.entity.TransactionType;
import com.jeczzu.fintechapi.exception.InsufficientFundsException;
import com.jeczzu.fintechapi.exception.ResourceNotFoundException;
import com.jeczzu.fintechapi.repository.UserRepository;
import com.jeczzu.fintechapi.service.JwtService;
import com.jeczzu.fintechapi.service.TransactionService;
import com.jeczzu.fintechapi.utils.AccountUtils;
import com.jeczzu.fintechapi.utils.TransactionUtils;

@WebMvcTest(TransactionController.class)
@AutoConfigureMockMvc(addFilters = false)
public class TransactionControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private TransactionService transactionService;

	@MockitoBean
	private JwtService jwtService;

	@MockitoBean
	private UserRepository userRepository;

	@Nested
	@DisplayName("POST " + ApiRoutes.TRANSACTIONS)
	class CreateTransaction {

		@Test
		@DisplayName("should return 201 and transaction response when request is valid")
		void shouldReturn201_whenRequestIsValid() throws Exception {

			UUID accountId = UUID.randomUUID();
			Account account = AccountUtils.buildAccount(accountId, "Juan Pérez", "juan@mail.com");
			BigDecimal depositAmount = new BigDecimal("200.00");
			Transaction transaction = TransactionUtils.buildTransaction(
					account, depositAmount, TransactionType.DEPOSIT);

			when(transactionService.createTransaction(accountId, depositAmount, TransactionType.DEPOSIT))
					.thenReturn(transaction);

			mockMvc.perform(
					post(ApiRoutes.TRANSACTIONS, accountId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									    "amount": %s,
									    "type": "%s"
									}
									""".formatted(depositAmount, TransactionType.DEPOSIT.name())))
					.andExpect(status().isCreated())
					.andExpect(header().string("Location",
							"/api/accounts/" + accountId + "/transactions/" + transaction.getId()))
					.andExpect(jsonPath("$.id").value(transaction.getId().toString()))
					.andExpect(jsonPath("$.accountId").value(accountId.toString()))
					.andExpect(jsonPath("$.amount").value(depositAmount.doubleValue()))
					.andExpect(jsonPath("$.type").value(TransactionType.DEPOSIT.name()));
		}

		@Test
		@DisplayName("should return 409 when insufficient funds")
		void shouldReturn409_whenInsufficientFunds() throws Exception {

			UUID accountId = UUID.randomUUID();
			Account account = AccountUtils.buildAccount(accountId, "Juan Pérez", "juan@mail.com");
			BigDecimal withdrawAmount = new BigDecimal("100.00");

			when(transactionService.createTransaction(accountId, withdrawAmount, TransactionType.WITHDRAW))
					.thenThrow(new InsufficientFundsException(
							"Insufficient funds. Available: " + account.getBalance()
									+ ", Requested: " + withdrawAmount));

			mockMvc.perform(
					post(ApiRoutes.TRANSACTIONS, accountId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									    "amount": %s,
									    "type": "%s"
									}
									""".formatted(withdrawAmount, TransactionType.WITHDRAW.name())))
					.andExpect(status().isConflict())
					.andExpect(jsonPath("$.title").value("Conflict"))
					.andExpect(jsonPath("$.detail").value(
							"Insufficient funds. Available: " + account.getBalance()
									+ ", Requested: " + withdrawAmount));
		}

		@Test
		@DisplayName("should return 400 when amount is null")
		void shouldReturn400_whenAmountIsNull() throws Exception {

			UUID accountId = UUID.randomUUID();

			mockMvc.perform(
					post(ApiRoutes.TRANSACTIONS, accountId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									    "type": "%s"
									}
									""".formatted(TransactionType.DEPOSIT.name())))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.title").value("Validation Failed"))
					.andExpect(jsonPath("$.detail").value("One or more fields are invalid"))
					.andExpect(jsonPath("$.errors[0]").value("amount: must not be null"));
		}

		@Test
		@DisplayName("should return 400 when amount is negative")
		void shouldReturn400_whenAmountIsNegative() throws Exception {

			UUID accountId = UUID.randomUUID();

			mockMvc.perform(
					post(ApiRoutes.TRANSACTIONS, accountId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									    "amount": -100,
									    "type": "%s"
									}
									""".formatted(TransactionType.DEPOSIT.name())))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.title").value("Validation Failed"))
					.andExpect(jsonPath("$.detail").value("One or more fields are invalid"))
					.andExpect(jsonPath("$.errors[0]").value("amount: must be greater than 0"));
		}

		@Test
		@DisplayName("should return 400 when accountId is not a valid UUID")
		void shouldReturn400_whenAccountIdIsNotValidUUID() throws Exception {

			String invalidAccountId = "not-a-uuid";

			mockMvc.perform(
					post(ApiRoutes.TRANSACTIONS, invalidAccountId)
							.contentType(MediaType.APPLICATION_JSON)
							.content("""
									{
									    "amount": 100,
									    "type": "%s"
									}
									""".formatted(TransactionType.DEPOSIT.name())))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.title").value("Invalid Parameter"))
					.andExpect(jsonPath("$.detail").value(
							"Invalid value 'not-a-uuid' for parameter 'accountId'. Expected type: UUID"));
		}
	}

	@Nested
	@DisplayName("GET " + ApiRoutes.TRANSACTION_BY_ID)
	class GetTransaction {

		@Test
		@DisplayName("should return 200 and transaction response when transaction exists")
		void shouldReturn200_whenTransactionExists() throws Exception {

			UUID accountId = UUID.randomUUID();
			Account account = AccountUtils.buildAccount(accountId, "Juan Pérez", "juan@mail.com");
			Transaction transaction = TransactionUtils.buildTransaction(account, new BigDecimal("100.00"),
					TransactionType.DEPOSIT);
			UUID transactionId = transaction.getId();

			when(transactionService.getTransactionById(accountId, transactionId)).thenReturn(transaction);

			mockMvc.perform(
					get(ApiRoutes.TRANSACTIONS + "/" + transactionId, accountId))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.id").value(transactionId.toString()))
					.andExpect(jsonPath("$.accountId").value(accountId.toString()))
					.andExpect(jsonPath("$.amount").value(100.00))
					.andExpect(jsonPath("$.type").value(TransactionType.DEPOSIT.name()));
		}

		@Test
		@DisplayName("should return 404 when transaction does not exist")
		void shouldReturn404_whenTransactionDoesNotExist() throws Exception {

			UUID accountId = UUID.randomUUID();
			UUID transactionId = UUID.randomUUID();

			when(transactionService.getTransactionById(accountId, transactionId))
					.thenThrow(new ResourceNotFoundException("Transaction not found with id: " + transactionId));

			mockMvc.perform(
					get(ApiRoutes.TRANSACTIONS + "/" + transactionId, accountId))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.title").value("Resource Not Found"))
					.andExpect(jsonPath("$.detail").value("Transaction not found with id: " + transactionId));
		}

		@Test
		@DisplayName("should return 400 when transactionId is not a valid UUID")
		void shouldReturn400_whenTransactionIdIsNotValidUUID() throws Exception {

			UUID accountId = UUID.randomUUID();
			String invalidTransactionId = "not-a-uuid";

			mockMvc.perform(
					get(ApiRoutes.TRANSACTIONS + "/" + invalidTransactionId, accountId))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.title").value("Invalid Parameter"))
					.andExpect(jsonPath("$.detail").value(
							"Invalid value 'not-a-uuid' for parameter 'transactionId'. Expected type: UUID"));
		}
	}

	@Nested
	@DisplayName("GET " + ApiRoutes.TRANSACTIONS)
	class GetAccountTransactions {

		@Test
		@DisplayName("should return 200 and paginated transactions when account has transactions")
		void shouldReturn200_whenAccountHasTransactions() throws Exception {

			UUID accountId = UUID.randomUUID();
			Account account = AccountUtils.buildAccount(accountId, "Juan Pérez", "juan@mail.com");

			Transaction transaction1 = TransactionUtils.buildTransaction(account, new BigDecimal("100.00"),
					TransactionType.DEPOSIT);
			Transaction transaction2 = TransactionUtils.buildTransaction(account, new BigDecimal("50.00"),
					TransactionType.WITHDRAW);

			when(transactionService.getTransactionsByAccountId(accountId, PageRequest.of(0, 20)))
					.thenReturn(new PageImpl<>(List.of(transaction1, transaction2)));

			mockMvc.perform(
					get(ApiRoutes.TRANSACTIONS, accountId))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content.length()").value(2))
					.andExpect(jsonPath("$.content[0].id").value(transaction1.getId().toString()))
					.andExpect(jsonPath("$.content[1].id").value(transaction2.getId().toString()));
		}

		@Test
		@DisplayName("should return 200 and empty page when account has no transactions")
		void shouldReturn200_whenAccountHasNoTransactions() throws Exception {

			UUID accountId = UUID.randomUUID();

			when(transactionService.getTransactionsByAccountId(accountId, PageRequest.of(0, 20)))
					.thenReturn(Page.empty());

			mockMvc.perform(
					get(ApiRoutes.TRANSACTIONS, accountId))
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.content.length()").value(0));
		}

		@Test
		@DisplayName("should return 404 when account does not exist")
		void shouldReturn404_whenAccountDoesNotExist() throws Exception {

			UUID accountId = UUID.randomUUID();

			when(transactionService.getTransactionsByAccountId(accountId, PageRequest.of(0, 20)))
					.thenThrow(new ResourceNotFoundException("Account not found with id: " + accountId));

			mockMvc.perform(
					get(ApiRoutes.TRANSACTIONS, accountId))
					.andExpect(status().isNotFound())
					.andExpect(jsonPath("$.title").value("Resource Not Found"))
					.andExpect(jsonPath("$.detail").value("Account not found with id: " + accountId));
		}

		@Test
		@DisplayName("should return 400 when accountId is not a valid UUID")
		void shouldReturn400_whenAccountIdIsNotValidUUID() throws Exception {

			String invalidAccountId = "not-a-uuid";

			mockMvc.perform(
					get(ApiRoutes.TRANSACTIONS, invalidAccountId))
					.andExpect(status().isBadRequest())
					.andExpect(jsonPath("$.title").value("Invalid Parameter"))
					.andExpect(jsonPath("$.detail").value(
							"Invalid value 'not-a-uuid' for parameter 'accountId'. Expected type: UUID"));
		}
	}
}
