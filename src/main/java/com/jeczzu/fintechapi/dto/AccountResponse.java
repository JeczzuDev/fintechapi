package com.jeczzu.fintechapi.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountResponse(
    UUID id,
    String ownerName,
    String email,
    BigDecimal balance,
    OffsetDateTime createdAt) {
}
