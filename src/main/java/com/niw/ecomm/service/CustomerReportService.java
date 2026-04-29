package com.niw.ecomm.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.niw.ecomm.rec.CustomerOrderSummary;
import com.niw.ecomm.repository.CustomerRepository;

/**
 * Service responsible for customer-level reporting.
 *
 * <p>
 * Delegates aggregation queries to {@link CustomerRepository}, isolating
 * controllers from direct repository access.
 */
@Service
public class CustomerReportService {

  private final CustomerRepository customerRepository;

  /**
   * Creates the service with its required repository dependency.
   *
   * @param customerRepository the repository used to execute aggregation queries
   */
  public CustomerReportService(CustomerRepository customerRepository) {
    this.customerRepository = customerRepository;
  }

  /**
   * Returns a summary of customers who have placed more than two orders, ordered
   * by their total amount spent in descending order.
   *
   * <p>
   * Each entry contains the customer name, order count, and total spent, as
   * computed by the JPQL aggregation query in
   * {@link CustomerRepository#findTopSpenders()}.
   *
   * @return list of {@link CustomerOrderSummary}; empty if no customer qualifies
   */
  public List<CustomerOrderSummary> getTopSpenders() {
    return customerRepository.findTopSpenders();
  }
}
