package com.jeczzu.fintechapi.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import com.jeczzu.fintechapi.entity.TransactionType;

public record TransactionResponse(
    UUID id,
    UUID accountId,
    BigDecimal amount,
    TransactionType type,
    OffsetDateTime createdAt) {
}
