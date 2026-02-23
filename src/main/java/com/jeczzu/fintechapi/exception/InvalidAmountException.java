package com.jeczzu.fintechapi.exception;

import org.springframework.http.HttpStatus;

public class InvalidAmountException extends BusinessException {

  public InvalidAmountException(String message) {
    super(message, HttpStatus.UNPROCESSABLE_ENTITY);
  }
}
