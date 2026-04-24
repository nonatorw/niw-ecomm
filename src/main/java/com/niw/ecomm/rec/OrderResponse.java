package com.niw.ecomm.rec;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.niw.ecomm.domain.Order;
import com.niw.ecomm.domain.OrderStatus;

/**
 * REST response representation of an {@link Order}.
 *
 * <p>
 * Includes a pre-computed {@code total} so callers do not need to sum items
 * client-side.
 *
 * @param id           the order id
 * @param customerId   the id of the customer who placed the order
 * @param customerName the name of the customer who placed the order
 * @param status       the current lifecycle status of the order
 * @param createdAt    the date the order was created
 * @param items        the line items included in this order
 * @param total        the total value computed as the sum of all item subtotals
 */
public record OrderResponse(Long id,
                            Long customerId,
                            String customerName,
                            OrderStatus status,
                            LocalDate createdAt,
                            List<OrderItemResponse> items,
                            BigDecimal total) {

  /**
   * REST response representation of a single line item within an order.
   *
   * @param id        the item id
   * @param productId the product identifier
   * @param quantity  the number of units ordered
   * @param unitPrice the price per unit
   * @param subtotal  the line total computed as {@code quantity × unitPrice}
   */
  public record OrderItemResponse(Long id,
                                  String productId,
                                  int quantity,
                                  BigDecimal unitPrice,
                                  BigDecimal subtotal) {
  }

  /**
   * Factory method that maps an {@link Order} entity and its pre-computed total
   * into an {@link OrderResponse}.
   *
   * @param order the source order entity; must not be {@code null}
   * @param total the pre-computed total value of the order; must not be
   *              {@code null}
   * @return a fully populated {@link OrderResponse}
   */
  public static OrderResponse from(Order order, BigDecimal total) {
    List<OrderItemResponse> itemResponses =
            order.getItems()
                 .stream()
                 .map(i -> new OrderItemResponse(i.getId(),
                                                 i.getProductId(),
                                                 i.getQuantity(),
                                                 i.getUnitPrice(),
                                                 i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity()))))
                 .toList();

    return new OrderResponse(order.getId(),
                             order.getCustomer().getId(),
                             order.getCustomer().getName(),
                             order.getStatus(),
                             order.getCreatedAt(),
                             itemResponses,
                             total);
  }
}
