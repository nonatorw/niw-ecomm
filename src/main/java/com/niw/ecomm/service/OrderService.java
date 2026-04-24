package com.niw.ecomm.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.niw.ecomm.domain.Order;
import com.niw.ecomm.domain.OrderStatus;

/**
 * Stateless service containing the core business logic for order management.
 *
 * <p>
 * All methods operate on {@link List} parameters as specified by the exercise,
 * keeping this service independent of the persistence layer and easily
 * unit-testable.
 */
@Service
public class OrderService {

  /**
   * Calculates the total monetary value of an order.
   *
   * <p>
   * The total is the sum of {@code quantity × unitPrice} for every
   * {@link com.niw.ecomm.domain.OrderItem}
   * in the order. Returns {@link BigDecimal#ZERO} for an order with no items.
   *
   * @param order the order whose total is to be calculated; must not be
   *              {@code null}
   * @return the total value; never {@code null}
   */
  public BigDecimal calculateTotal(Order order) {
    return order.getItems()
                .stream()
                .map(item -> item.getUnitPrice()
                                 .multiply(BigDecimal.valueOf(item.getQuantity())))
                                 .reduce(BigDecimal.ZERO,
                                         BigDecimal::add);
  }

  /**
   * Filters a list of orders to those belonging to a specific customer.
   *
   * <p>
   * Comparison is performed against the string representation of the customer's
   * id via {@link Order#getCustomerId()}. Returns an empty list if
   * {@code customerId} is {@code null} or no orders match.
   *
   * @param orders     the full list of orders to filter; must not be {@code null}
   * @param customerId the customer id to filter by; may be {@code null}
   * @return a list of matching orders; never {@code null}
   */
  public List<Order> getOrdersByCustomer(List<Order> orders, String customerId) {
    if (customerId == null){
      return List.of();
    }

    return orders.stream()
                 .filter(o -> Objects.equals(o.getCustomerId(), customerId))
                 .collect(Collectors.toList());
  }

  /**
   * Groups a list of orders by their {@link OrderStatus}.
   *
   * <p>
   * Only statuses that have at least one order appear as keys in the result map.
   * Returns an empty map for an empty input list.
   *
   * @param orders the orders to group; must not be {@code null}
   * @return a map from status to the list of orders with that status; never
   *         {@code null}
   */
  public Map<OrderStatus, List<Order>> groupByStatus(List<Order> orders) {
    return orders.stream()
                 .collect(Collectors.groupingBy(Order::getStatus));
  }

  /**
   * Returns the order with the highest total value from a list.
   *
   * <p>
   * The total for each order is computed via {@link #calculateTotal(Order)}.
   * When two orders have equal totals, the first one encountered is returned.
   * Returns {@link Optional#empty()} for an empty input list.
   *
   * @param orders the orders to search; must not be {@code null}
   * @return an {@link Optional} containing the most expensive order,
   *         or {@link Optional#empty()} if the list is empty
   */
  public Optional<Order> findMostExpensive(List<Order> orders) {
    return orders.stream()
                 .reduce((a, b) -> calculateTotal(a).compareTo(calculateTotal(b)) >= 0 ? a : b);
  }
}
