package com.niw.ecomm.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.niw.ecomm.rec.CustomerOrderSummary;
import com.niw.ecomm.service.CustomerReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing aggregated business reports.
 *
 * <p>
 * Base path: {@code /api/reports}
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Reports",
     description = "Aggregated business reports")
public class ReportController {

  private final CustomerReportService reportService;

  /**
   * Creates the controller with its required service dependency.
   *
   * @param reportService the service used to compute customer spending reports
   */
  public ReportController(CustomerReportService reportService) {
    this.reportService = reportService;
  }

  /**
   * Returns customers who have placed more than two orders, ranked by total
   * amount spent.
   *
   * @return {@code 200 OK} with the ordered list of {@link CustomerOrderSummary};
   *         an empty array when no customer qualifies
   */
  @GetMapping("/top-spenders")
  @Operation(summary = "Top spending customers",
             description = "Returns customers with more than 2 orders, ordered by total spent descending")
  public ResponseEntity<List<CustomerOrderSummary>> topSpenders() {
    return ResponseEntity.ok(reportService.getTopSpenders());
  }
}
