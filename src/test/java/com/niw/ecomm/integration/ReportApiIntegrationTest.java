package com.niw.ecomm.integration;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Full-stack integration tests for the {@code /api/reports} REST endpoints.
 *
 * <p>
 * Starts the complete Spring Boot application on a random port and exercises
 * the HTTP layer end-to-end against an in-memory H2 database pre-loaded with
 * the standard seed data ({@code db/data.sql}).
 *
 * <p>
 * <strong>Seed data summary (customers with more than 2 orders):</strong>
 * <ul>
 *     <li>Alice Ferreira — 4 orders, total €420.00 (highest spender)</li>
 *     <li>Bruno Matos — 3 orders, total €290.00 (second spender)</li>
 * </ul>
 *
 * <p>
 * Customers with 2 or fewer orders (Carla Sousa, Daniel Pinto) are excluded by
 * the {@code HAVING COUNT(DISTINCT o.id) > 2} predicate in the repository
 * query, so the ranking always contains exactly two entries.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = {
                                "spring.datasource.url=jdbc:h2:mem:report-it-db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                                "spring.datasource.username=sa",
                                "spring.datasource.password=",
                                "spring.jpa.hibernate.ddl-auto=none",
                                "spring.sql.init.mode=always",
                                "spring.sql.init.schema-locations=classpath:db/schema.sql",
                                "spring.sql.init.data-locations=classpath:db/data.sql"
                             }
                )
class ReportApiIntegrationTest {

  @LocalServerPort
  private int port;

  /**
   * Configures REST Assured to target the randomly assigned server port before
   * each test.
   */
  @BeforeEach
  void setUp() {
    RestAssured.baseURI = "http://localhost";
    RestAssured.port = port;
  }

  /**
   * Verifies that the top-spenders endpoint returns exactly two customers ranked
   * by total spend in descending order.
   *
   * <p>
   * Expected ranking from seed data:
   * <ol>
   *     <li>Alice Ferreira — 4 orders, €420.00</li>
   *     <li>Bruno Matos — 3 orders, €290.00</li>
   * </ol>
   *
   * <p>
   * Carla Sousa (2 orders) and Daniel Pinto (1 order) are excluded because the
   * query filters for customers with more than 2 orders.
   */
  @Test
  @DisplayName("GET /api/reports/top-spenders returns ranked seeded customers")
  void topSpendersReturnsExpectedRanking() {
    RestAssured.given().accept(ContentType.JSON)
               .when().get("/api/reports/top-spenders")
               .then().statusCode(200)
                      .body("size()", Matchers.equalTo(2))
                      .body("[0].customerName", Matchers.equalTo("Alice Ferreira"))
                      .body("[0].orderCount", Matchers.equalTo(4))
                      .body("[0].totalSpent", Matchers.equalTo(420.00f))
                      .body("[1].customerName", Matchers.equalTo("Bruno Matos"))
                      .body("[1].orderCount", Matchers.equalTo(3))
                      .body("[1].totalSpent", Matchers.equalTo(290.00f));
  }
}
