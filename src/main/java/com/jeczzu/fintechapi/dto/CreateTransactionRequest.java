package com.jeczzu.fintechapi.dto;

import java.math.BigDecimal;

import com.jeczzu.fintechapi.entity.TransactionType;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateTransactionRequest(

    @NotNull @Positive BigDecimal amount,

    @NotNull TransactionType type) {
}