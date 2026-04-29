package com.niw.ecomm.rec;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Represents a single line item in an {@link OrderRequest}.
 *
 * @param productId the identifier of the product being ordered
 * @param quantity  the number of units to order; must be greater than zero
 * @param unitPrice the price per unit; must be positive
 */
public record OrderItemRequest(
  @NotBlank
  String productId,

  @Positive
  int quantity,

  @NotNull
  @DecimalMin("0.01")
  BigDecimal unitPrice
) {}
