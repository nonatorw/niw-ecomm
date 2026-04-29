package com.niw.ecomm.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.niw.ecomm.domain.Customer;
import com.niw.ecomm.domain.Order;
import com.niw.ecomm.domain.OrderItem;
import com.niw.ecomm.domain.OrderStatus;
import com.niw.ecomm.rec.CustomerOrderSummary;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

@DataJpaTest
/**
 * Data layer tests for {@link CustomerRepository}.
 *
 * <p>
 * Uses {@link org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest}
 * to load only the JPA context with an auto-configured in-memory H2 database.
 * No web layer, service beans, or seed data are loaded. Each test sets up its
 * own data directly via repositories, so tests are fully independent of one
 * another.
 *
 * <p>
 * Tests in this class verify the JPQL aggregation query
 * {@link CustomerRepository#findTopSpenders()}, including filtering, ordering,
 * and edge cases.
 */
class CustomerRepositoryTest {

  private final CustomerRepository customerRepository;
  private final OrderRepository orderRepository;

  CustomerRepositoryTest(CustomerRepository customerRepository,
                         OrderRepository orderRepository) {
    this.customerRepository = customerRepository;
    this.orderRepository = orderRepository;
  }

  private Customer savedCustomer(String name,
                                 String email) {
    return customerRepository.save(new Customer(name, email));
  }

  private Order orderWithItems(Customer customer,
                               OrderStatus status,
                               double... prices) {
    Order order = new Order(customer, status, LocalDate.now());

    for (double price : prices) {
      order.addItem(new OrderItem("PROD",
                                  1,
                                  BigDecimal.valueOf(price)));
    }

    return orderRepository.save(order);
  }

  /**
   * Customers with more than 2 orders qualify for the top-spenders report and
   * must be returned ordered by total spent descending.
   *
   * <p>
   * Alice has 3 orders totalling €600.00; Bruno has 3 orders totalling €150.00.
   * Carla has only 2 orders and must be excluded.
   */
  @Test
  @DisplayName("returns customers with more than 2 orders, ordered by total spent descending")
  void returnsTopSpendersOrderedByTotal() {
    // Alice: 3 orders → total = 100 + 200 + 300 = 600 (qualifies)
    Customer alice = savedCustomer("Alice",
                                   "alice@test.com");
    orderWithItems(alice,
                   OrderStatus.CONFIRMED,
                   100.0);
    orderWithItems(alice,
                   OrderStatus.CONFIRMED,
                   200.0);
    orderWithItems(alice,
                   OrderStatus.SHIPPED,
                   300.0);

    // Bruno: 3 orders → total = 50 + 50 + 50 = 150 (qualifies, but lower total)
    Customer bruno = savedCustomer("Bruno", "bruno@test.com");
    orderWithItems(bruno,
                   OrderStatus.CONFIRMED,
                   50.0);
    orderWithItems(bruno,
                   OrderStatus.CONFIRMED,
                   50.0);
    orderWithItems(bruno,
                   OrderStatus.PENDING,
                   50.0);

    // Carla: 2 orders → does NOT qualify
    Customer carla = savedCustomer("Carla",
                                   "carla@test.com");
    orderWithItems(carla,
                   OrderStatus.CONFIRMED,
                   999.0);
    orderWithItems(carla,
                   OrderStatus.SHIPPED,
                   999.0);

    List<CustomerOrderSummary> results = customerRepository.findTopSpenders();

    Assertions.assertThat(results)
              .as("only the two qualifying customers (Alice and Bruno) must be returned")
              .hasSize(2);

    Assertions.assertThat(results.get(0).customerName())
              .as("Alice must appear first as the highest spender (€600.00)")
              .isEqualTo("Alice");

    Assertions.assertThat(results.get(0).orderCount())
              .as("Alice must have exactly 3 orders")
              .isEqualTo(3);

    Assertions.assertThat(results.get(0).totalSpent())
              .as("Alice's total must be 100.00 + 200.00 + 300.00 = 600.00")
              .isEqualByComparingTo(new BigDecimal("600.0"));

    Assertions.assertThat(results.get(1).customerName())
              .as("Bruno must appear second as the lower spender (€150.00)")
              .isEqualTo("Bruno");

    Assertions.assertThat(results.get(1).totalSpent())
              .as("Bruno's total must be 50.00 + 50.00 + 50.00 = 150.00")
              .isEqualByComparingTo(new BigDecimal("150.0"));
  }

  /**
   * When no customer has placed more than 2 orders the query must return an empty
   * list.
   *
   * <p>
   * Daniel has exactly 2 orders and therefore does not meet the
   * {@code HAVING > 2} threshold.
   */
  @Test
  @DisplayName("returns empty list when no customer has more than 2 orders")
  void returnsEmptyWhenNoQualifyingCustomers() {
    Customer daniel = savedCustomer("Daniel",
                                    "daniel@test.com");
    orderWithItems(daniel,
                   OrderStatus.PENDING,
                   100.0);
    orderWithItems(daniel,
                   OrderStatus.PENDING,
                   100.0);

    Assertions.assertThat(customerRepository.findTopSpenders())
              .as("result must be empty when no customer has more than 2 orders")
              .isEmpty();
  }
}
