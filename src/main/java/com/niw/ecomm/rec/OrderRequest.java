package com.niw.ecomm.rec;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * Request body for the order creation endpoint.
 *
 * @param customerId the id of the customer placing the order; must reference an
 *                   existing customer
 * @param items      the list of line items to include in the order; must not be
 *                   empty
 */
public record OrderRequest(
  @NotNull
  Long customerId,

  @NotNull
  @NotEmpty
  @Valid List<OrderItemRequest> items
) {}
