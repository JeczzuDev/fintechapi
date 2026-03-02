package com.jeczzu.fintechapi.config;

import java.math.BigDecimal;

public final class TransactionConstants {
  private TransactionConstants() {
  }

  public static final BigDecimal MAX_TRANSACTION_AMOUNT = new BigDecimal("1000000.00");
  public static final BigDecimal MAX_BALANCE = new BigDecimal("99999999.99");
  public static final int MAX_DECIMAL_PLACES = 2;
}
