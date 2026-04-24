package com.niw.ecomm.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.niw.ecomm.domain.Order;

/**
 * Spring Data JPA repository for {@link Order} entities.
 *
 * <p>
 * Provides standard CRUD and pagination operations via {@link JpaRepository}.
 * Business-level filtering and aggregation are handled in the service layer.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {
}
