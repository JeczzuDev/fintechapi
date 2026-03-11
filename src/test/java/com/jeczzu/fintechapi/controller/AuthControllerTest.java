package com.jeczzu.fintechapi.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jeczzu.fintechapi.config.ApiRoutes;
import com.jeczzu.fintechapi.exception.ConflictException;
import com.jeczzu.fintechapi.repository.UserRepository;
import com.jeczzu.fintechapi.service.AuthService;
import com.jeczzu.fintechapi.service.JwtService;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthService authService;

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private UserRepository userRepository;

  private static final String REGISTER_URL = ApiRoutes.AUTH + ApiRoutes.AUTH_REGISTER;
  private static final String LOGIN_URL = ApiRoutes.AUTH + ApiRoutes.AUTH_LOGIN;

  @Nested
  @DisplayName("POST " + ApiRoutes.AUTH + ApiRoutes.AUTH_REGISTER)
  class Register {

    @Test
    @DisplayName("should return 201 with token when registration succeeds")
    void shouldReturn201_whenRegistrationSucceeds() throws Exception {
      when(authService.register("user@mail.com", "password123")).thenReturn("jwt-token-123");

      mockMvc.perform(post(REGISTER_URL)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {"email": "user@mail.com", "password": "password123"}
              """))
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.token").value("jwt-token-123"));
    }

    @Test
    @DisplayName("should return 409 when email is already registered")
    void shouldReturn409_whenEmailAlreadyRegistered() throws Exception {
      when(authService.register("exists@mail.com", "password123"))
          .thenThrow(new ConflictException("Email already registered: exists@mail.com"));

      mockMvc.perform(post(REGISTER_URL)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {"email": "exists@mail.com", "password": "password123"}
              """))
          .andExpect(status().isConflict())
          .andExpect(jsonPath("$.title").value("Conflict"));
    }

    @Test
    @DisplayName("should return 400 when email is invalid")
    void shouldReturn400_whenEmailIsInvalid() throws Exception {
      mockMvc.perform(post(REGISTER_URL)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {"email": "not-an-email", "password": "password123"}
              """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("should return 400 when password is too short")
    void shouldReturn400_whenPasswordTooShort() throws Exception {
      mockMvc.perform(post(REGISTER_URL)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {"email": "user@mail.com", "password": "short"}
              """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("should return 400 when fields are blank")
    void shouldReturn400_whenFieldsAreBlank() throws Exception {
      mockMvc.perform(post(REGISTER_URL)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {"email": "", "password": ""}
              """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("should return 400 when request body is missing")
    void shouldReturn400_whenRequestBodyIsMissing() throws Exception {
      mockMvc.perform(post(REGISTER_URL)
          .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());
    }
  }

  @Nested
  @DisplayName("POST " + ApiRoutes.AUTH + ApiRoutes.AUTH_LOGIN)
  class Login {

    @Test
    @DisplayName("should return 200 with token when login succeeds")
    void shouldReturn200_whenLoginSucceeds() throws Exception {
      when(authService.login("user@mail.com", "password123")).thenReturn("jwt-token-456");

      mockMvc.perform(post(LOGIN_URL)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {"email": "user@mail.com", "password": "password123"}
              """))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.token").value("jwt-token-456"));
    }

    @Test
    @DisplayName("should return 401 when credentials are invalid")
    void shouldReturn401_whenCredentialsAreInvalid() throws Exception {
      when(authService.login("user@mail.com", "wrong-password"))
          .thenThrow(new BadCredentialsException("Invalid email or password"));

      mockMvc.perform(post(LOGIN_URL)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {"email": "user@mail.com", "password": "wrong-password"}
              """))
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.title").value("Authentication Failed"));
    }

    @Test
    @DisplayName("should return 400 when fields are blank")
    void shouldReturn400_whenFieldsAreBlank() throws Exception {
      mockMvc.perform(post(LOGIN_URL)
          .contentType(MediaType.APPLICATION_JSON)
          .content("""
              {"email": "", "password": ""}
              """))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.title").value("Validation Failed"));
    }

    @Test
    @DisplayName("should return 400 when request body is missing")
    void shouldReturn400_whenRequestBodyIsMissing() throws Exception {
      mockMvc.perform(post(LOGIN_URL)
          .contentType(MediaType.APPLICATION_JSON))
          .andExpect(status().isBadRequest());
    }
  }
}
