package com.niw.ecomm.rec;

import java.math.BigDecimal;

/**
 * Represents a single line item in an {@link OrderRequest}.
 *
 * @param productId the identifier of the product being ordered
 * @param quantity  the number of units to order; must be greater than zero
 * @param unitPrice the price per unit; must be positive
 */
public record OrderItemRequest(
  String productId,
  int quantity,
  BigDecimal unitPrice
) {}
