package com.jeczzu.fintechapi.controller;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jeczzu.fintechapi.config.ApiRoutes;
import com.jeczzu.fintechapi.config.SecurityConfig;
import com.jeczzu.fintechapi.entity.Account;
import com.jeczzu.fintechapi.exception.ConflictException;
import com.jeczzu.fintechapi.exception.ResourceNotFoundException;
import com.jeczzu.fintechapi.service.AccountService;
import com.jeczzu.fintechapi.utils.AccountUtils;

@WebMvcTest(AccountController.class)
@Import(SecurityConfig.class)
class AccountControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AccountService accountService;

  @Nested
  @DisplayName("POST " + ApiRoutes.ACCOUNTS)
  class CreateAccount {

    @Test
    @DisplayName("should return 201 and account response when request is valid")
    void shouldReturn201_whenRequestIsValid() throws Exception {

      UUID id = UUID.randomUUID();
      Account account = AccountUtils.buildAccount(id, "Juan Pérez", "juan@mail.com");

      when(accountService.createAccount("Juan Pérez", "juan@mail.com"))
          .thenReturn(account);

      mockMvc.perform(
          post(ApiRoutes.ACCOUNTS)
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {"ownerName": "Juan Pérez", "email": "juan@mail.com"}
                  """))
          .andExpect(status().isCreated())
          .andExpect(header().string("Location", ApiRoutes.ACCOUNTS + "/" + id))
          .andExpect(jsonPath("$.id").value(id.toString()))
          .andExpect(jsonPath("$.ownerName").value("Juan Pérez"))
          .andExpect(jsonPath("$.email").value("juan@mail.com"))
          .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    @DisplayName("should return 409 when email is already taken")
    void shouldReturn409_whenEmailIsAlreadyTaken() throws Exception {

      UUID id = UUID.randomUUID();
      Account account = AccountUtils.buildAccount(id, "Juan Pérez", "juan@mail.com");

      when(accountService.createAccount(account.getOwnerName(), account.getEmail()))
          .thenThrow(new ConflictException(
              "An account with email 'juan@email.com' already exists"));

      mockMvc.perform(
          post(ApiRoutes.ACCOUNTS)
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {"ownerName": "Juan Pérez", "email": "juan@mail.com"}
                  """))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.title").value("Conflict"))
          .andExpect(jsonPath("$.detail").value(
              "An account with email 'juan@email.com' already exists"));
    }

    @Test
    @DisplayName("should return 400 when ownerName is empty")
    void shouldReturn400_whenOwnerNameIsEmpty() throws Exception {

      mockMvc.perform(
          post(ApiRoutes.ACCOUNTS)
              .contentType(MediaType.APPLICATION_JSON)
              .content("""
                  {"ownerName": "", "email": "juan@mail.com"}
                  """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Validation Failed"))
          .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    @DisplayName("should return 400 when request body is missing")
    void shouldReturn400_whenRequestBodyIsMissing() throws Exception {

      mockMvc.perform(
          post(ApiRoutes.ACCOUNTS)
              .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Malformed Request"))
          .andExpect(jsonPath("$.detail").value(
              "The request body is missing or contains invalid JSON"));
    }
  }

  @Nested
  @DisplayName("GET " + ApiRoutes.ACCOUNTS + ApiRoutes.ACCOUNT_BY_ID)
  class GetAccount {

    @Test
    @DisplayName("should return 200 and account response when account exists")
    void shouldReturn200_whenAccountExists() throws Exception {

      UUID id = UUID.randomUUID();
      Account account = AccountUtils.buildAccount(id, "Juan Pérez", "juan@mail.com");

      when(accountService.getAccountById(id))
          .thenReturn(account);

      mockMvc.perform(get(ApiRoutes.ACCOUNTS + "/" + id))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(id.toString()))
          .andExpect(jsonPath("$.ownerName").value("Juan Pérez"))
          .andExpect(jsonPath("$.email").value("juan@mail.com"));
    }

    @Test
    @DisplayName("should return 404 when account does not exist")
    void shouldReturn404_whenAccountDoesNotExist() throws Exception {

      UUID id = UUID.randomUUID();

      when(accountService.getAccountById(id))
          .thenThrow(new ResourceNotFoundException("Account not found with id: " + id));

      mockMvc.perform(get(ApiRoutes.ACCOUNTS + "/" + id))
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.title").value("Resource Not Found"))
          .andExpect(jsonPath("$.detail").value("Account not found with id: " + id));
    }

    @Test
    @DisplayName("should return 400 when id path variable is not a valid UUID")
    void shouldReturn400_whenIdPathVariableIsNotValidUUID() throws Exception {

      mockMvc.perform(get(ApiRoutes.ACCOUNTS + "/not-a-uuid"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Invalid Parameter"))
          .andExpect(jsonPath("$.detail").value(
              "Invalid value 'not-a-uuid' for parameter 'id'. Expected type: UUID"));
    }
  }
}
