package com.jeczzu.fintechapi.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.jeczzu.fintechapi.config.ApiRoutes;
import com.jeczzu.fintechapi.dto.AuthResponse;
import com.jeczzu.fintechapi.dto.LoginRequest;
import com.jeczzu.fintechapi.dto.RegisterRequest;
import com.jeczzu.fintechapi.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(ApiRoutes.AUTH)
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping(ApiRoutes.AUTH_REGISTER)
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    String token = authService.register(request.email(), request.password());
    return ResponseEntity.status(HttpStatus.CREATED).body(new AuthResponse(token));
  }

  @PostMapping(ApiRoutes.AUTH_LOGIN)
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    String token = authService.login(request.email(), request.password());
    return ResponseEntity.ok(new AuthResponse(token));
  }
}
