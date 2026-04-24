package com.niw.ecomm.domain;

/**
 * Lifecycle states of an {@link Order}.
 *
 * <p>Valid transitions:
 * <ul>
 *   <li>{@code PENDING} → {@code CONFIRMED} → {@code SHIPPED}</li>
 *   <li>{@code PENDING} → {@code CANCELLED}</li>
 *   <li>{@code CONFIRMED} → {@code CANCELLED}</li>
 * </ul>
 */
public enum OrderStatus {

    /** Order has been received but not yet confirmed. */
    PENDING,

    /** Order has been confirmed and is being prepared. */
    CONFIRMED,

    /** Order has been dispatched to the customer. */
    SHIPPED,

    /** Order was cancelled before shipment. */
    CANCELLED
}
