package com.jeczzu.fintechapi.config;

public final class ApiRoutes {

  private ApiRoutes() {
  }

  public static final String API_BASE = "/api";

  // AccountController
  public static final String ACCOUNTS = API_BASE + "/accounts";
  public static final String ACCOUNT_BY_ID = "/{id}";

  // TransactionController
  public static final String TRANSACTIONS = ACCOUNTS + "/{accountId}/transactions";
  public static final String TRANSACTION_BY_ID = "/{transactionId}";

  // AuthController
  public static final String AUTH = API_BASE + "/auth";
  public static final String AUTH_REGISTER = "/register";
  public static final String AUTH_LOGIN = "/login";
}
