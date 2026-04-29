package com.niw.ecomm.rec;

import java.math.BigDecimal;

/**
 * Projection record used as the result of the customer spending aggregation
 * query.
 *
 * <p>
 * Instances are constructed directly by JPQL using the {@code new} expression,
 * so the canonical constructor parameter order must not be changed.
 *
 * @param customerName the full name of the customer
 * @param orderCount   the total number of distinct orders placed by the
 *                     customer
 * @param totalSpent   the sum of {@code quantity × unitPrice} across all items
 *                     of all orders
 */
public record CustomerOrderSummary(
  String customerName,
  long orderCount,
  BigDecimal totalSpent
) {}
