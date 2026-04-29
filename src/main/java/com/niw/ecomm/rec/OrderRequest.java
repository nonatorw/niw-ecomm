package com.niw.ecomm.rec;

import java.util.List;

/**
 * Request body for the order creation endpoint.
 *
 * @param customerId the id of the customer placing the order; must reference an
 *                   existing customer
 * @param items      the list of line items to include in the order; must not be
 *                   empty
 */
public record OrderRequest(
  Long customerId,
  List<OrderItemRequest> items
) {}
