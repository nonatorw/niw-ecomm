package com.niw.ecomm.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.niw.ecomm.domain.Order;
import com.niw.ecomm.domain.OrderItem;
import com.niw.ecomm.domain.OrderStatus;
import com.niw.ecomm.rec.OrderRequest;
import com.niw.ecomm.rec.OrderResponse;
import com.niw.ecomm.repository.CustomerRepository;
import com.niw.ecomm.repository.OrderRepository;
import com.niw.ecomm.service.OrderService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for order management operations.
 *
 * <p>
 * Exposes the {@link OrderService} business logic through HTTP endpoints and
 * delegates persistence to {@link OrderRepository} and
 * {@link CustomerRepository}.
 *
 * <p>
 * Base path: {@code /api/orders}
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order management operations")
public class OrderController {
  private final OrderRepository orderRepository;
  private final CustomerRepository customerRepository;
  private final OrderService orderService;

  /**
   * Creates the controller with its required dependencies.
   *
   * @param orderRepository    repository for persisting and retrieving orders
   * @param customerRepository repository for looking up customers by id
   * @param orderService       service containing the order business logic
   */
  public OrderController(OrderRepository orderRepository,
                         CustomerRepository customerRepository,
                         OrderService orderService) {
    this.orderRepository = orderRepository;
    this.customerRepository = customerRepository;
    this.orderService = orderService;
  }

  /**
   * Creates a new order with {@link OrderStatus#PENDING} status for the specified
   * customer.
   *
   * @param request the order creation payload containing the customer id and line
   *                items
   * @return {@code 201 Created} with the persisted {@link OrderResponse} and a
   *         {@code Location} header; {@code 400 Bad Request} if the referenced
   *         customer does not exist or if {@code items} is null or empty
   */
  @PostMapping
  @Operation(summary = "Create order",
             description = "Creates a new order with the provided items")
  public ResponseEntity<OrderResponse> create(@RequestBody OrderRequest request) {
    if (request.items() == null
    ||  request.items().isEmpty()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                        "items must not be null or empty");
    }

    var customer =
        this.customerRepository.findById(request.customerId())
                               .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                                                              "Customer not found: "
                                                                              + request.customerId()));

    Order order = new Order(customer,
                            OrderStatus.PENDING,
                            LocalDate.now());

    request.items()
           .forEach(i -> order.addItem(new OrderItem(i.productId(),
                                                     i.quantity(),
                                                     i.unitPrice())));

    Order saved = this.orderRepository.save(order);

    var uri = ServletUriComponentsBuilder.fromCurrentRequest()
                                         .path("/{id}")
                                         .buildAndExpand(saved.getId())
                                         .toUri();

    return ResponseEntity.created(uri)
                         .body(OrderResponse.from(saved,
                                                  this.orderService.calculateTotal(saved)));
  }

  /**
   * Returns all orders in the system.
   *
   * @return {@code 200 OK} with the list of all {@link OrderResponse};
   *         empty array if none exist
   */
  @GetMapping
  @Operation(summary = "List all orders")
  public ResponseEntity<List<OrderResponse>> listAll() {
    List<Order> orders = this.orderRepository.findAll();

    List<OrderResponse> response =
        orders.stream()
              .map(order -> OrderResponse.from(order,
                                               this.orderService.calculateTotal(order)))
              .toList();

    return ResponseEntity.ok(response);
  }

  /**
   * Returns a single order by its id.
   *
   * @param id the order id
   * @return {@code 200 OK} with the {@link OrderResponse}, or
   *         {@code 404 Not Found}
   */
  @GetMapping("/{id}")
  @Operation(summary = "Get order by id")
  public ResponseEntity<OrderResponse> getById(@PathVariable Long id) {
    return this.orderRepository.findById(id)
                               .map(order -> ResponseEntity.ok(
                                                OrderResponse.from(order,
                                                                   this.orderService.calculateTotal(order))))
                               .orElse(ResponseEntity.notFound()
                                                     .build());
  }

  /**
   * Calculates and returns the total monetary value of a specific order.
   *
   * @param id the order id
   * @return {@code 200 OK} with the total as a {@link BigDecimal}, or
   *         {@code 404 Not Found}
   */
  @GetMapping("/{id}/total")
  @Operation(summary = "Calculate order total",
             description = "Returns the total value of a specific order")
  public ResponseEntity<BigDecimal> total(@PathVariable Long id) {
    return this.orderRepository.findById(id)
               .map(order -> ResponseEntity.ok(this.orderService.calculateTotal(order)))
               .orElse(ResponseEntity.notFound()
                                     .build());
  }

  /**
   * Returns all orders placed by a specific customer.
   *
   * @param customerId the string representation of the customer's id
   * @return {@code 200 OK} with the list of matching {@link OrderResponse};
   *         empty array if none
   */
  @GetMapping("/customer/{customerId}")
  @Operation(summary = "Orders by customer",
             description = "Returns all orders for a given customer id")
  public ResponseEntity<List<OrderResponse>> byCustomer(@PathVariable String customerId) {
    List<Order> all = this.orderRepository.findAll();

    List<OrderResponse> result =
        this.orderService.getOrdersByCustomer(all, customerId)
                         .stream()
                         .map(order -> OrderResponse.from(order,
                                                          this.orderService.calculateTotal(order)))
                         .toList();

    return ResponseEntity.ok(result);
  }

  /**
   * Returns all orders grouped by their {@link OrderStatus}.
   *
   * @return {@code 200 OK} with a map from status name to list of
   *         {@link OrderResponse}
   */
  @GetMapping("/grouped-by-status")
  @Operation(summary = "Orders grouped by status")
  public ResponseEntity<Map<OrderStatus, List<OrderResponse>>> groupedByStatus() {
    List<Order> all = this.orderRepository.findAll();

    Collector<Entry<OrderStatus, List<Order>>, ?, Map<OrderStatus, List<OrderResponse>>> map =
        Collectors.toMap(Map.Entry::getKey,
                         e -> e.getValue()
                               .stream()
                               .map(order -> OrderResponse.from(order,
                                                                this.orderService.calculateTotal(order)))
                               .toList());

    Map<OrderStatus, List<OrderResponse>> grouped =
        this.orderService.groupByStatus(all)
                         .entrySet()
                         .stream()
                         .collect(map);

    return ResponseEntity.ok(grouped);
  }

  /**
   * Returns the order with the highest total value.
   *
   * @return {@code 200 OK} with the most expensive {@link OrderResponse}, or
   *         {@code 404 Not Found} if no orders exist
   */
  @GetMapping("/most-expensive")
  @Operation(summary = "Most expensive order",
             description = "Returns the order with the highest total value")
  public ResponseEntity<OrderResponse> mostExpensive() {
    List<Order> all = this.orderRepository.findAll();

    return this.orderService.findMostExpensive(all)
                            .map(order -> ResponseEntity.ok(
                                              OrderResponse.from(order,
                                                                 this.orderService.calculateTotal(order))))
                            .orElse(ResponseEntity.notFound()
                            .build());
  }
}
