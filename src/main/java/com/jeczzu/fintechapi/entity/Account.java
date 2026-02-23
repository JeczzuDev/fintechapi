package com.jeczzu.fintechapi.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Account {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "owner_name", nullable = false)
  private String ownerName;

  @Column(nullable = false, unique = true)
  private String email;

  @Column(nullable=false, precision=19, scale=2)
  private BigDecimal balance;

  @Column(name="created_at", nullable=false, updatable=false)
  private OffsetDateTime createdAt;

  @Version
  private Long version;

  @PrePersist
  public void prePersist() {
    this.createdAt = OffsetDateTime.now();
    if (this.balance == null) {
      this.balance = BigDecimal.ZERO;
    }
  }
}