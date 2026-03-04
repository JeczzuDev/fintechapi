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
}
