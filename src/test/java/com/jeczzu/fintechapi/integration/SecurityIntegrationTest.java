package com.jeczzu.fintechapi.integration;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.jeczzu.fintechapi.config.ApiRoutes;
import com.jeczzu.fintechapi.dto.AuthResponse;
import com.jeczzu.fintechapi.dto.CreateAccountRequest;
import com.jeczzu.fintechapi.dto.LoginRequest;
import com.jeczzu.fintechapi.dto.RegisterRequest;
import com.jeczzu.fintechapi.repository.UserRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityIntegrationTest {

  @Autowired
  private TestRestTemplate restTemplate;

  @Autowired
  private UserRepository userRepository;

  private static final String REGISTER_URL = ApiRoutes.AUTH + ApiRoutes.AUTH_REGISTER;
  private static final String LOGIN_URL = ApiRoutes.AUTH + ApiRoutes.AUTH_LOGIN;
  private static final String ACCOUNTS_URL = ApiRoutes.ACCOUNTS;
  private static final String ACCOUNT_BY_ID_URL = ApiRoutes.ACCOUNTS + ApiRoutes.ACCOUNT_BY_ID;

  @BeforeEach
  void setUp() {
    userRepository.deleteAll();
  }

  private String registerAndGetToken(String email, String password) {
    ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
        REGISTER_URL,
        new RegisterRequest(email, password),
        AuthResponse.class);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    assertThat(response.getBody()).isNotNull();
    return response.getBody().token();
  }

  @Nested
  @DisplayName("Public endpoints (no token required)")
  class PublicEndpoints {

    @Test
    @DisplayName("POST " + REGISTER_URL + " should be accessible without token")
    void registerShouldBeAccessibleWithoutToken() {
      ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
          REGISTER_URL,
          new RegisterRequest("user@test.com", "password123"),
          AuthResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    @DisplayName("POST " + LOGIN_URL + " should be accessible without token")
    void loginShouldBeAccessibleWithoutToken() {
      registerAndGetToken("user@test.com", "password123");

      ResponseEntity<AuthResponse> response = restTemplate.postForEntity(
          LOGIN_URL,
          new LoginRequest("user@test.com", "password123"),
          AuthResponse.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
      assertThat(response.getBody()).isNotNull();
      assertThat(response.getBody().token()).isNotBlank();
    }

    @Test
    @DisplayName("POST " + REGISTER_URL + " should return 409 for duplicate email")
    void registerShouldReturn409ForDuplicateEmail() {
      registerAndGetToken("user@test.com", "password123");

      ResponseEntity<String> response = restTemplate.postForEntity(
          REGISTER_URL,
          new RegisterRequest("user@test.com", "password123"),
          String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName("POST " + LOGIN_URL + " should return 401 for wrong password")
    void loginShouldReturn401ForWrongPassword() {
      registerAndGetToken("user@test.com", "password123");

      ResponseEntity<String> response = restTemplate.postForEntity(
          LOGIN_URL,
          new LoginRequest("user@test.com", "wrongpassword"),
          String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST " + LOGIN_URL + " should return 401 for non-existent email")
    void loginShouldReturn401ForNonExistentEmail() {
      ResponseEntity<String> response = restTemplate.postForEntity(
          LOGIN_URL,
          new LoginRequest("nobody@test.com", "password123"),
          String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
  }

  @Nested
  @DisplayName("Protected endpoints (token required)")
  class ProtectedEndpoints {

    @Test
    @DisplayName("GET " + ACCOUNT_BY_ID_URL + " should return 401 without token")
    void shouldReturn401WithoutToken() {
      ResponseEntity<String> response = restTemplate.getForEntity(
          ACCOUNT_BY_ID_URL.replace("{id}", UUID.randomUUID().toString()), String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET " + ACCOUNT_BY_ID_URL + " should return 401 with invalid token")
    void shouldReturn401WithInvalidToken() {
      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth("invalid.jwt.token");

      ResponseEntity<String> response = restTemplate.exchange(
          ACCOUNT_BY_ID_URL.replace("{id}", UUID.randomUUID().toString()),
          HttpMethod.GET,
          new HttpEntity<>(headers),
          String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("POST " + ACCOUNTS_URL + " should return 401 without token")
    void shouldReturn401WithoutTokenForPost() {
      ResponseEntity<String> response = restTemplate.postForEntity(
          ACCOUNTS_URL,
          new CreateAccountRequest("Test", "test@mail.com"),
          String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("GET " + ACCOUNT_BY_ID_URL + "/transactions should return 401 without token")
    void shouldReturn401WithoutTokenForTransactions() {
      ResponseEntity<String> response = restTemplate.getForEntity(
          ACCOUNT_BY_ID_URL.replace("{id}", UUID.randomUUID().toString()) + "/transactions?page=0&size=10",
          String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("should pass security and reach controller with valid token")
    void shouldPassSecurityWithValidToken() {
      String token = registerAndGetToken("user@test.com", "password123");

      HttpHeaders headers = new HttpHeaders();
      headers.setBearerAuth(token);

      // 404 = passed security, reached controller (account doesn't exist)
      ResponseEntity<String> response = restTemplate.exchange(
          ACCOUNT_BY_ID_URL.replace("{id}", UUID.randomUUID().toString()),
          HttpMethod.GET,
          new HttpEntity<>(headers),
          String.class);

      assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
  }
}
