package com.jeczzu.fintechapi.service;

import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jeczzu.fintechapi.entity.Role;
import com.jeczzu.fintechapi.entity.User;
import com.jeczzu.fintechapi.exception.ConflictException;
import com.jeczzu.fintechapi.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  @Transactional
  public String register(String email, String password) {
    if (userRepository.existsByEmail(email)) {
      throw new ConflictException("Email already registered: " + email);
    }

    User user = User.builder()
        .email(email)
        .password(passwordEncoder.encode(password))
        .role(Role.USER)
        .build();

    userRepository.save(user);
    return jwtService.generateToken(user);
  }

  @Transactional(readOnly = true)
  public String login(String email, String password) {
    User user = userRepository.findByEmail(email)
        .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

    if (!passwordEncoder.matches(password, user.getPassword())) {
      throw new BadCredentialsException("Invalid email or password");
    }

    return jwtService.generateToken(user);
  }
}
