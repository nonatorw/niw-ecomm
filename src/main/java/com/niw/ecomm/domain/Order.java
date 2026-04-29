package com.niw.ecomm.domain;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/**
 * Represents a purchase order placed by a {@link Customer}.
 *
 * <p>
 * Aggregates one or more {@link OrderItem} instances and tracks the order
 * lifecycle through {@link OrderStatus}. The total value is computed on demand
 * from the items — there is no stored {@code amount} column.
 */
@Entity
@Table(name = "orders")
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id", nullable = false)
  private Customer customer;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status;

  @Column(name = "created_at", nullable = false)
  private LocalDate createdAt;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<OrderItem> items = new ArrayList<>();

  /** Required by JPA. */
  protected Order() {
  }

  /**
   * Creates a new order for the given customer.
   *
   * @param customer  the customer placing the order; must not be {@code null}
   * @param status    initial lifecycle status; must not be {@code null}
   * @param createdAt the date the order was created; must not be {@code null}
   */
  public Order(Customer customer, OrderStatus status, LocalDate createdAt) {
    this.customer = customer;
    this.status = status;
    this.createdAt = createdAt;
  }

  /**
   * Adds an item to this order and establishes the back-reference from the item
   * to this order.
   *
   * @param item the item to add; must not be {@code null}
   */
  public void addItem(OrderItem item) {
    item.setOrder(this);
    items.add(item);
  }

  /**
   * Returns the unique identifier of this order.
   *
   * @return the database-generated id, or {@code null} before first persistence
   */
  public Long getId() {
    return id;
  }

  /**
   * Returns the customer who placed this order.
   *
   * @return the associated {@link Customer}
   */
  public Customer getCustomer() {
    return customer;
  }

  /**
   * Returns the customer's id as a {@link String} for use in service-layer
   * filtering. Returns {@code null} when the customer is not set or has no id
   * yet.
   *
   * @return string representation of the customer id, or {@code null}
   */
  public String getCustomerId() {
    return customer != null ? String.valueOf(customer.getId()) : null;
  }

  /**
   * Returns the current lifecycle status of this order.
   *
   * @return the {@link OrderStatus}
   */
  public OrderStatus getStatus() {
    return status;
  }

  /**
   * Returns the date on which this order was created.
   *
   * @return creation date
   */
  public LocalDate getCreatedAt() {
    return createdAt;
  }

  /**
   * Returns the list of items in this order.
   *
   * @return mutable list of {@link OrderItem}; never {@code null}
   */
  public List<OrderItem> getItems() {
    return items;
  }
}
