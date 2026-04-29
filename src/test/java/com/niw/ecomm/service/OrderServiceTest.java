package com.niw.ecomm.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.niw.ecomm.domain.Customer;
import com.niw.ecomm.domain.Order;
import com.niw.ecomm.domain.OrderItem;
import com.niw.ecomm.domain.OrderStatus;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class OrderServiceTest {

  private OrderService service;
  private Customer alice;
  private Customer bruno;

  @BeforeEach
  void setUp() {
    service = new OrderService();
    alice = new Customer(1L, "Alice", "alice@example.com");
    bruno = new Customer(2L, "Bruno", "bruno@example.com");
  }

  // --- calculateTotal ---

  @Nested
  @DisplayName("calculateTotal")
  class CalculateTotal {

    /**
     * An order with no items has a total of {@link BigDecimal#ZERO}.
     */
    @Test
    @DisplayName("returns ZERO when order has no items")
    void emptyItems() {
      Order order = new Order(alice,
                              OrderStatus.PENDING,
                              LocalDate.now());

      Assertions.assertThat(service.calculateTotal(order))
                .as("total must be ZERO when the order has no items")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * Total for a single item equals {@code quantity × unitPrice}.
     *
     * <p>
     * 2 × €50.00 = <strong>€100.00</strong>.
     */
    @Test
    @DisplayName("returns correct total for a single item")
    void singleItem() {
      Order order = new Order(alice,
                              OrderStatus.PENDING,
                              LocalDate.now());

      order.addItem(new OrderItem("PROD-001",
                                  2,
                                  new BigDecimal("50.00")));

      Assertions.assertThat(service.calculateTotal(order))
                .as("total must be quantity × unit price: 2 × 50.00 = 100.00")
                .isEqualByComparingTo(new BigDecimal("100.00"));
    }

    /**
     * Total for multiple items equals the sum of all individual subtotals.
     *
     * <p>
     * 100.00 + 30.00 + 60.00 = <strong>190.00</strong>.
     */
    @Test
    @DisplayName("returns sum of all items for multiple items")
    void multipleItems() {
      Order order = new Order(alice,
                              OrderStatus.CONFIRMED,
                              LocalDate.now());

      order.addItem(new OrderItem("PROD-001",
                                  2,
                                  new BigDecimal("50.00"))); // 100.00

      order.addItem(new OrderItem("PROD-002",
                                  1,
                                  new BigDecimal("30.00"))); // 30.00

      order.addItem(new OrderItem("PROD-003",
                                  3,
                                  new BigDecimal("20.00"))); // 60.00

      Assertions.assertThat(service.calculateTotal(order))
                .as("total must be the sum of all item subtotals: 100.00 + 30.00 + 60.00 = 190.00")
                .isEqualByComparingTo(new BigDecimal("190.00"));
    }

    /**
     * An item with quantity zero contributes {@code 0 × unitPrice = 0} to the
     * total.
     */
    @Test
    @DisplayName("item with quantity zero contributes nothing")
    void itemWithZeroQuantity() {
      Order order = new Order(alice,
                              OrderStatus.PENDING,
                              LocalDate.now());

      order.addItem(new OrderItem("PROD-001",
                                  0,
                                  new BigDecimal("99.99")));

      Assertions.assertThat(service.calculateTotal(order))
                .as("an item with quantity zero must not contribute to the total")
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
  }

  // --- getOrdersByCustomer ---

  @Nested
  @DisplayName("getOrdersByCustomer")
  class GetOrdersByCustomer {

    /**
     * Only orders whose customer id matches the given argument are returned; orders
     * belonging to other customers are excluded.
     */
    @Test
    @DisplayName("returns only orders belonging to the given customer")
    void filtersByCustomer() {
      Order aliceOrder = new Order(alice,
                                   OrderStatus.PENDING,
                                   LocalDate.now());
      Order brunoOrder = new Order(bruno,
                                   OrderStatus.CONFIRMED,
                                   LocalDate.now());

      List<Order> result = service.getOrdersByCustomer(List.of(aliceOrder,
                                                               brunoOrder),
                                                       "1");

      Assertions.assertThat(result)
                .as("must return only the order belonging to customer '1' (Alice), excluding Bruno's")
                .containsExactly(aliceOrder);
    }

    /**
     * An empty list is returned when no order in the input matches the given
     * customer id.
     */
    @Test
    @DisplayName("returns empty list when no orders match")
    void noMatch() {
      Order brunoOrder = new Order(bruno,
                                   OrderStatus.PENDING,
                                   LocalDate.now());

      List<Order> result = service.getOrdersByCustomer(List.of(brunoOrder),
                                                       "999");

      Assertions.assertThat(result)
                .as("must return an empty list when no order belongs to customer id '999'")
                .isEmpty();
    }

    /**
     * A {@code null} customer id cannot match any order, so the result is always
     * empty.
     */
    @Test
    @DisplayName("returns empty list when customerId is null")
    void nullCustomerId() {
      Order order = new Order(alice, OrderStatus.PENDING, LocalDate.now());

      List<Order> result = service.getOrdersByCustomer(List.of(order), null);

      Assertions.assertThat(result)
                .as("must return an empty list when customerId is null")
                .isEmpty();
    }

    /**
     * An empty input list produces an empty result regardless of the customer id.
     */
    @Test
    @DisplayName("returns empty list when order list is empty")
    void emptyOrderList() {
      List<Order> result = service.getOrdersByCustomer(List.of(), "1");

      Assertions.assertThat(result)
                .as("must return an empty list when the input order list is empty")
                .isEmpty();
    }
  }

  // --- groupByStatus ---

  @Nested
  @DisplayName("groupByStatus")
  class GroupByStatus {

    /**
     * Orders are partitioned by their status; the resulting map contains an entry
     * for each status present in the input and no entry for statuses that are
     * absent.
     */
    @Test
    @DisplayName("groups orders correctly by status")
    void groupsMixedOrders() {
      Order pending = new Order(alice,
                                OrderStatus.PENDING,
                                LocalDate.now());
      Order confirmed = new Order(alice,
                                  OrderStatus.CONFIRMED,
                                  LocalDate.now());
      Order shipped = new Order(bruno,
                                OrderStatus.SHIPPED,
                                LocalDate.now());
      Order pending2 = new Order(bruno,
                                 OrderStatus.PENDING,
                                 LocalDate.now());

      Map<OrderStatus, List<Order>> grouped = service.groupByStatus(List.of(pending,
                                                                            confirmed,
                                                                            shipped,
                                                                            pending2));

      Assertions.assertThat(grouped.get(OrderStatus.PENDING))
                .as("PENDING group must contain exactly the two pending orders")
                .containsExactlyInAnyOrder(pending, pending2);

      Assertions.assertThat(grouped.get(OrderStatus.CONFIRMED))
                .as("CONFIRMED group must contain exactly the confirmed order")
                .containsExactly(confirmed);

      Assertions.assertThat(grouped.get(OrderStatus.SHIPPED))
                .as("SHIPPED group must contain exactly the shipped order")
                .containsExactly(shipped);

      Assertions.assertThat(grouped)
                .as("map must not contain a CANCELLED key when no cancelled orders are present")
                .doesNotContainKey(OrderStatus.CANCELLED);
    }

    /**
     * Grouping an empty list produces an empty map — no null keys or empty-list
     * entries.
     */
    @Test
    @DisplayName("returns empty map when list is empty")
    void emptyList() {
      Assertions.assertThat(service.groupByStatus(List.of()))
                .as("grouping an empty order list must produce an empty map")
                .isEmpty();
    }

    /**
     * When all orders share the same status the resulting map contains exactly one
     * entry.
     */
    @Test
    @DisplayName("returns single entry when all orders share the same status")
    void allSameStatus() {
      Order o1 = new Order(alice, OrderStatus.SHIPPED, LocalDate.now());
      Order o2 = new Order(bruno, OrderStatus.SHIPPED, LocalDate.now());

      Map<OrderStatus, List<Order>> grouped = service.groupByStatus(List.of(o1, o2));

      Assertions.assertThat(grouped)
                .as("map must have exactly one key (SHIPPED) when all orders share the same status")
                .containsOnlyKeys(OrderStatus.SHIPPED);

      Assertions.assertThat(grouped.get(OrderStatus.SHIPPED))
                .as("SHIPPED group must contain all 2 orders")
                .hasSize(2);
    }
  }

  // --- findMostExpensive ---

  @Nested
  @DisplayName("findMostExpensive")
  class FindMostExpensive {

    /**
     * An empty input list produces an empty {@link Optional}.
     */
    @Test
    @DisplayName("returns empty when list is empty")
    void emptyList() {
      Assertions.assertThat(service.findMostExpensive(List.of()))
                .as("must return an empty Optional when no orders are provided")
                .isEmpty();
    }

    /**
     * When the list contains a single order it is trivially the most expensive.
     */
    @Test
    @DisplayName("returns the only order when list has one element")
    void singleOrder() {
      Order order = new Order(alice,
                              OrderStatus.PENDING,
                              LocalDate.now());

      order.addItem(new OrderItem("PROD-001",
                                  1,
                                  new BigDecimal("50.00")));

      Optional<Order> result = service.findMostExpensive(List.of(order));

      Assertions.assertThat(result)
                .as("the sole available order must be returned as the most expensive")
                .contains(order);
    }

    /**
     * Among multiple orders the one with the highest total is returned.
     *
     * <p>
     * cheap = €10.00, expensive = 2 × €200.00 = <strong>€400.00</strong>, medium =
     * €50.00.
     */
    @Test
    @DisplayName("returns order with highest total among multiple orders")
    void picksHighestTotal() {
      Order cheap = new Order(alice,
                              OrderStatus.CONFIRMED,
                              LocalDate.now());
      Order expensive = new Order(alice,
                                 OrderStatus.CONFIRMED,
                                 LocalDate.now());
      Order medium = new Order(bruno,
                               OrderStatus.PENDING,
                               LocalDate.now());

      cheap.addItem(new OrderItem("PROD-A",
                                  1,
                                  new BigDecimal("10.00")));
      expensive.addItem(new OrderItem("PROD-B",
                                       2,
                                       new BigDecimal("200.00")));
      medium.addItem(new OrderItem("PROD-C",
                                   1,
                                   new BigDecimal("50.00")));

      Optional<Order> result = service.findMostExpensive(List.of(cheap, expensive, medium));

      Assertions.assertThat(result)
                .as("order with total 400.00 (2 × 200.00) must be returned as the most expensive")
                .contains(expensive);
    }

    /**
     * When two orders have equal totals the first one encountered in the list is
     * returned, reflecting the stable {@code max} reduction used by the service.
     */
    @Test
    @DisplayName("returns first order when totals are tied")
    void tiedTotals() {
      Order first = new Order(alice,
                              OrderStatus.PENDING,
                              LocalDate.now());
      Order second = new Order(bruno,
                               OrderStatus.PENDING,
                               LocalDate.now());

      first.addItem(new OrderItem("PROD-A",
                                  1,
                                  new BigDecimal("100.00")));
      second.addItem(new OrderItem("PROD-B",
                                   1,
                                   new BigDecimal("100.00")));

      Optional<Order> result = service.findMostExpensive(List.of(first, second));

      Assertions.assertThat(result)
                .as("when totals are tied, the first order in the list must be returned")
                .contains(first);
    }
  }
}
