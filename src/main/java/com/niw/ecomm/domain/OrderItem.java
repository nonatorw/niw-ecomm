package com.niw.ecomm.domain;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Represents a single line item within an {@link Order}.
 *
 * <p>
 * Holds a product reference, quantity, and unit price. The line subtotal is
 * {@code quantity × unitPrice} and is computed on demand — it is not stored.
 */
@Entity
@Table(name = "order_items")
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(name = "product_id", nullable = false, length = 100)
  private String productId;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
  private BigDecimal unitPrice;

  /** Required by JPA. */
  protected OrderItem() {
  }

  /**
   * Creates a new order item.
   *
   * @param productId the identifier of the product being ordered; must not be
   *                  {@code null}
   * @param quantity  the number of units ordered; must be zero or greater
   * @param unitPrice the price per unit; must not be {@code null} or negative
   */
  public OrderItem(String productId, int quantity, BigDecimal unitPrice) {
    this.productId = productId;
    this.quantity = quantity;
    this.unitPrice = unitPrice;
  }

  /**
   * Sets the owning order. Called by {@link Order#addItem(OrderItem)} to maintain
   * the bidirectional association.
   *
   * @param order the parent order
   */
  void setOrder(Order order) {
    this.order = order;
  }

  /**
   * Returns the unique identifier of this item.
   *
   * @return the database-generated id, or {@code null} before first persistence
   */
  public Long getId() {
    return id;
  }

  /**
   * Returns the product identifier for this line item.
   *
   * @return product id
   */
  public String getProductId() {
    return productId;
  }

  /**
   * Returns the number of units ordered.
   *
   * @return quantity (zero or greater)
   */
  public int getQuantity() {
    return quantity;
  }

  /**
   * Returns the price per unit for this item.
   *
   * @return unit price; never {@code null}
   */
  public BigDecimal getUnitPrice() {
    return unitPrice;
  }
}
