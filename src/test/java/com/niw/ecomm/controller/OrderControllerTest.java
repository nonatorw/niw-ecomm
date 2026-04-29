package com.niw.ecomm.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.niw.ecomm.domain.Customer;
import com.niw.ecomm.domain.Order;
import com.niw.ecomm.domain.OrderItem;
import com.niw.ecomm.domain.OrderStatus;
import com.niw.ecomm.rec.OrderItemRequest;
import com.niw.ecomm.rec.OrderRequest;
import com.niw.ecomm.repository.CustomerRepository;
import com.niw.ecomm.repository.OrderRepository;
import com.niw.ecomm.service.OrderService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

/**
 * Unit tests for {@link OrderController}.
 *
 * <p>
 * Uses
 * {@link org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest} to
 * load only the Spring MVC web layer in isolation. All repository and service
 * dependencies are replaced with Mockito mocks via
 * {@link org.springframework.test.context.bean.override.mockito.MockitoBean}.
 * No database is involved and no full application context is started.
 *
 * <p>
 * Tests in this class verify: HTTP routing, response serialisation, HTTP status
 * codes, and Bean Validation constraint wiring at the controller layer.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

  @MockitoBean
  private OrderRepository orderRepository;

  @MockitoBean
  private CustomerRepository customerRepository;

  @MockitoBean
  private OrderService orderService;

  private final MockMvc mockMvc;
  private final ObjectMapper objectMapper;

  OrderControllerTest(MockMvc mockMvc, ObjectMapper objectMapper) {
    this.mockMvc = mockMvc;
    this.objectMapper = objectMapper;
  }

  private Customer alice;
  private Order order;

  @BeforeEach
  void setUp() {
    alice = new Customer(1L,
                         "Alice",
                         "alice@example.com");

    order = new Order(alice,
                      OrderStatus.CONFIRMED,
                      LocalDate.of(2024, 1, 10));

    order.addItem(new OrderItem("PROD-001",
                                2,
                                new BigDecimal("50.00")));
  }

  /**
   * A successful {@code GET /api/orders} returns 
   *  - HTTP 200 with a JSON array containing all orders, including
   *    their customer name, status, and total.
   */
  @Test
  @DisplayName("GET /api/orders returns 200 with list of orders")
  void listAllReturns200() throws Exception {
    Mockito.when(orderRepository.findAll())
           .thenReturn(List.of(order));

    Mockito.when(orderService.calculateTotal(order))
           .thenReturn(new BigDecimal("100.00"));

    mockMvc.perform(MockMvcRequestBuilders.get("/api/orders"))
           .andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(1))
           .andExpect(MockMvcResultMatchers.jsonPath("$[0].customerName").value("Alice"))
           .andExpect(MockMvcResultMatchers.jsonPath("$[0].status").value("CONFIRMED"))
           .andExpect(MockMvcResultMatchers.jsonPath("$[0].total").value(100.00));
  }

  /**
   * {@code GET /api/orders/{id}} returns 
   *  - HTTP 404 when no order exists for the given id.
   */
  @Test
  @DisplayName("GET /api/orders/{id} returns 404 when order not found")
  void getByIdReturns404() throws Exception {
    Mockito.when(orderRepository.findById(99L))
           .thenReturn(Optional.empty());

    mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/99"))
           .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  /**
   * {@code GET /api/orders/{id}/total} returns
   *   - HTTP 200 with the plain-text total value for a known order.
   */
  @Test
  @DisplayName("GET /api/orders/{id}/total returns total value")
  void totalReturnsValue() throws Exception {
    Mockito.when(orderRepository.findById(1L))
           .thenReturn(Optional.of(order));

    Mockito.when(orderService.calculateTotal(order))
           .thenReturn(new BigDecimal("100.00"));

    mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/1/total"))
           .andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers.content().string("100.00"));
  }

  /**
   * {@code GET /api/orders/most-expensive} returns 
   *  - HTTP 404 when the order repository is empty.
   */
  @Test
  @DisplayName("GET /api/orders/most-expensive returns 404 when no orders exist")
  void mostExpensiveReturns404WhenEmpty() throws Exception {
    Mockito.when(orderRepository.findAll())
           .thenReturn(List.of());

    Mockito.when(orderService.findMostExpensive(List.of()))
           .thenReturn(Optional.empty());

    mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/most-expensive"))
           .andExpect(MockMvcResultMatchers.status().isNotFound());
  }

  /**
   * {@code GET /api/orders/most-expensive} returns
   *  - HTTP 200 with the order identified as having the highest total.
   */
  @Test
  @DisplayName("GET /api/orders/most-expensive returns order with highest total")
  void mostExpensiveReturnsOrder() throws Exception {
    Mockito.when(orderRepository.findAll())
           .thenReturn(List.of(order));

    Mockito.when(orderService.findMostExpensive(List.of(order)))
           .thenReturn(Optional.of(order));

    Mockito.when(orderService.calculateTotal(order))
           .thenReturn(new BigDecimal("100.00"));

    mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/most-expensive"))
           .andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers.jsonPath("$.customerName").value("Alice"))
           .andExpect(MockMvcResultMatchers.jsonPath("$.total").value(100.00));
  }

  /**
   * {@code POST /api/orders} returns
   *  - HTTP 400 when the referenced customer does not exist in the repository.
   */
  @Test
  @DisplayName("POST /api/orders returns 400 when customer not found")
  void createReturns400WhenCustomerNotFound() throws Exception {
    Mockito.when(customerRepository.findById(99L))
           .thenReturn(Optional.empty());

    OrderRequest request = new OrderRequest(99L,
                                            List.of(new OrderItemRequest("PROD-001",
                                                                         1,
                                                                         new BigDecimal("10.00"))));
    String requestBody = Objects.requireNonNull(objectMapper.writeValueAsString(request));

    mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                                          .contentType(MediaType.APPLICATION_JSON_VALUE)
                                          .content(requestBody))
           .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  /**
   * {@code POST /api/orders} returns 
   *  - HTTP 400 when {@code customerId} is {@code null}.
   */
  @Test
  @DisplayName("POST /api/orders returns 400 when customerId is null")
  void createReturns400WhenCustomerIdIsNull() throws Exception {
    OrderRequest request = new OrderRequest(null,
                                            List.of(new OrderItemRequest("PROD-001",
                                                                         1,
                                                                         new BigDecimal("10.00"))));
    mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                                          .contentType(MediaType.APPLICATION_JSON_VALUE)
                                          .content(objectMapper.writeValueAsString(request)))
           .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  /**
   * {@code POST /api/orders} returns 
   *  - HTTP 400 when {@code items} is empty.
   */
  @Test
  @DisplayName("POST /api/orders returns 400 when items is empty")
  void createReturns400WhenItemsIsEmpty() throws Exception {
    OrderRequest request = new OrderRequest(1L, List.of());
    mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                                          .contentType(MediaType.APPLICATION_JSON_VALUE)
                                          .content(objectMapper.writeValueAsString(request)))
           .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  /**
   * {@code POST /api/orders} returns 
   *  - HTTP 400 when any item has a blank {@code productId}.
   */
  @Test
  @DisplayName("POST /api/orders returns 400 when productId is blank")
  void createReturns400WhenProductIdIsBlank() throws Exception {
    OrderRequest request = new OrderRequest(1L,
                                            List.of(new OrderItemRequest("  ",
                                                                         1,
                                                                         new BigDecimal("10.00"))));
    mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                                          .contentType(MediaType.APPLICATION_JSON_VALUE)
                                          .content(objectMapper.writeValueAsString(request)))
           .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  /**
   * {@code POST /api/orders} returns 
   *  - HTTP 400 when any item has a {@code quantity} of zero.
   */
  @Test
  @DisplayName("POST /api/orders returns 400 when quantity is zero")
  void createReturns400WhenQuantityIsZero() throws Exception {
    OrderRequest request = new OrderRequest(1L,
                                            List.of(new OrderItemRequest("PROD-001",
                                                                         0,
                                                                         new BigDecimal("10.00"))));
    mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                                          .contentType(MediaType.APPLICATION_JSON_VALUE)
                                          .content(objectMapper.writeValueAsString(request)))
           .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  /**
   * {@code POST /api/orders} returns 
   *  - HTTP 400 when any item has a {@code null} {@code unitPrice}.
   */
  @Test
  @DisplayName("POST /api/orders returns 400 when unitPrice is null")
  void createReturns400WhenUnitPriceIsNull() throws Exception {
    String body = """
        {
          "customerId": 1,
          "items": [{ "productId": "PROD-001", "quantity": 1, "unitPrice": null }]
        }
        """;
    mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
                                          .contentType(MediaType.APPLICATION_JSON_VALUE)
                                          .content(body))
           .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  /**
   * {@code GET /api/orders/{id}} returns 
   *  - HTTP 400 when {@code id} is zero.
   */
  @Test
  @DisplayName("GET /api/orders/{id} returns 400 when id is zero")
  void getByIdReturns400WhenIdIsZero() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/0"))
           .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  /**
   * {@code GET /api/orders/{id}} returns 
   *  - HTTP 400 when {@code id} is negative.
   */
  @Test
  @DisplayName("GET /api/orders/{id} returns 400 when id is negative")
  void getByIdReturns400WhenIdIsNegative() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/-1"))
           .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  /**
   * {@code GET /api/orders/{id}/total} returns 
   *  - HTTP 400 when {@code id} is negative.
   */
  @Test
  @DisplayName("GET /api/orders/{id}/total returns 400 when id is negative")
  void totalReturns400WhenIdIsNegative() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/-1/total"))
           .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  /**
   * {@code GET /api/orders/customer/{customerId}} returns 
   *  - HTTP 400 when {@code customerId} is blank.
   */
  @Test
  @DisplayName("GET /api/orders/customer/{customerId} returns 400 when customerId is blank")
  void byCustomerReturns400WhenCustomerIdIsBlank() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/customer/%20"))
           .andExpect(MockMvcResultMatchers.status().isBadRequest());
  }

  /**
   * {@code GET /api/orders/grouped-by-status} returns 
   *  - HTTP 200 with a JSON object whose keys are order status names 
   *    and whose values are lists of orders.
   */
  @Test
  @DisplayName("GET /api/orders/grouped-by-status returns grouped map")
  void groupedByStatusReturnsMap() throws Exception {
    Mockito.when(orderRepository.findAll())
           .thenReturn(List.of(order));

    Mockito.when(orderService.groupByStatus(List.of(order)))
           .thenReturn(Map.of(OrderStatus.CONFIRMED, List.of(order)));

    Mockito.when(orderService.calculateTotal(order))
           .thenReturn(new BigDecimal("100.00"));

    mockMvc.perform(MockMvcRequestBuilders.get("/api/orders/grouped-by-status"))
           .andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers.jsonPath("$.CONFIRMED.length()").value(1));
  }
}
