package com.niw.ecomm.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
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
        alice = new Customer(1L, "Alice", "alice@example.com");
        order = new Order(alice, OrderStatus.CONFIRMED, LocalDate.of(2024, 1, 10));
        order.addItem(new OrderItem("PROD-001", 2, new BigDecimal("50.00")));
    }

    /**
     * A successful {@code GET /api/orders} returns HTTP 200 with a JSON array
     * containing all orders, including their customer name, status, and total.
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
     * {@code GET /api/orders/{id}} returns HTTP 404 when no order exists for the
     * given id.
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
     * {@code GET /api/orders/{id}/total} returns HTTP 200 with the plain-text
     * total value for a known order.
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
     * {@code GET /api/orders/most-expensive} returns HTTP 404 when the order
     * repository is empty.
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
     * {@code GET /api/orders/most-expensive} returns HTTP 200 with the order
     * identified as having the highest total.
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
     * {@code POST /api/orders} returns HTTP 400 when the referenced customer does
     * not exist in the repository.
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

        mockMvc.perform(MockMvcRequestBuilders.post("/api/orders")
               .contentType(MediaType.APPLICATION_JSON)
               .content(objectMapper.writeValueAsString(request)))
               .andExpect(MockMvcResultMatchers.status().isBadRequest());
    }

    /**
     * {@code GET /api/orders/grouped-by-status} returns HTTP 200 with a JSON object
     * whose keys are order status names and whose values are lists of orders.
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
