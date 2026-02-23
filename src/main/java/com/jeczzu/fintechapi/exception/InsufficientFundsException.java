package com.jeczzu.fintechapi.exception;

import org.springframework.http.HttpStatus;

public class InsufficientFundsException extends BusinessException {

  public InsufficientFundsException(String message) {
    super(message, HttpStatus.CONFLICT);
  }
}
