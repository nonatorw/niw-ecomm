package com.niw.ecomm.controller;

import java.math.BigDecimal;
import java.util.List;

import com.niw.ecomm.rec.CustomerOrderSummary;
import com.niw.ecomm.service.CustomerReportService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

  @MockitoBean
  private CustomerReportService reportService;

  private final MockMvc mockMvc;

  ReportControllerTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  /**
   * {@code GET /api/reports/top-spenders} returns HTTP 200 with a JSON array
   * ranked by total spent descending, including customer name, order count, and
   * total spent.
   */
  @Test
  @DisplayName("GET /api/reports/top-spenders returns 200 with ranked list")
  void topSpendersReturnsRankedList() throws Exception {
    Mockito.when(reportService.getTopSpenders())
           .thenReturn(List.of(new CustomerOrderSummary("Alice",
                                                        4,
                                                        new BigDecimal("420.00")),
                               new CustomerOrderSummary("Bruno",
                                                        3,
                                                        new BigDecimal("290.00"))));

    mockMvc.perform(MockMvcRequestBuilders.get("/api/reports/top-spenders"))
           .andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(2))
           .andExpect(MockMvcResultMatchers.jsonPath("$[0].customerName").value("Alice"))
           .andExpect(MockMvcResultMatchers.jsonPath("$[0].orderCount").value(4))
           .andExpect(MockMvcResultMatchers.jsonPath("$[0].totalSpent").value(420.00))
           .andExpect(MockMvcResultMatchers.jsonPath("$[1].customerName").value("Bruno"));
  }

  /**
   * {@code GET /api/reports/top-spenders} returns HTTP 200 with an empty JSON
   * array when no customer qualifies for the report.
   */
  @Test
  @DisplayName("GET /api/reports/top-spenders returns empty array when no results")
  void topSpendersReturnsEmptyList() throws Exception {
    Mockito.when(reportService.getTopSpenders())
           .thenReturn(List.of());

    mockMvc.perform(MockMvcRequestBuilders.get("/api/reports/top-spenders"))
           .andExpect(MockMvcResultMatchers.status().isOk())
           .andExpect(MockMvcResultMatchers.jsonPath("$.length()").value(0));
  }
}
