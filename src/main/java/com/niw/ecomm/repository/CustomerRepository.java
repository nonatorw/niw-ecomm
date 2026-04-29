package com.niw.ecomm.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.niw.ecomm.domain.Customer;
import com.niw.ecomm.rec.CustomerOrderSummary;

/**
 * Spring Data JPA repository for {@link Customer} entities.
 *
 * <p>
 * Extends {@link JpaRepository} for standard CRUD operations and adds a custom
 * aggregation query to support the customer spending report (Problem A of the
 * exercise).
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

  /**
   * Returns a projection of customers who have placed more than two orders,
   * ranked by their total amount spent in descending order.
   *
   * <p>
   * The total is computed inline as {@code SUM(item.quantity × item.unitPrice)}
   * across all items of all orders for each customer.
   *
   * @return list of {@link CustomerOrderSummary} projections; empty if no
   *         customer qualifies
   */
  @Query("""
         SELECT new com.niw.ecomm.rec.CustomerOrderSummary(c.name,
                                                           COUNT(DISTINCT o.id),
                                                           SUM(oi.quantity * oi.unitPrice))
           FROM Customer c
           JOIN c.orders o
           JOIN o.items oi
          GROUP BY c.id,
                   c.name
         HAVING COUNT(DISTINCT o.id) > 2
          ORDER BY SUM(oi.quantity * oi.unitPrice) DESC
         """)
  List<CustomerOrderSummary> findTopSpenders();
}
