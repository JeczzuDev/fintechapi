package com.jeczzu.fintechapi.exception;

public class BusinessException extends RuntimeException{
  public BusinessException(String message) {
    super(message);
  }
}
