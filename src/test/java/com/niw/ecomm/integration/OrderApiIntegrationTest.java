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
 * Full-stack integration tests for the {@code /api/orders} REST endpoints.
 *
 * <p>
 * Starts the complete Spring Boot application on a random port and exercises
 * the HTTP layer end-to-end against an in-memory H2 database pre-loaded with
 * the standard seed data ({@code db/data.sql}). The identity sequences are
 * reset to 100 after seeding so that new rows created during tests do not clash
 * with the fixed seed IDs (1–11).
 *
 * <p>
 * <strong>Seed data summary:</strong>
 * <ul>
 *     <li>Customer 1 — Alice Ferreira, 4 orders (ids 1–4), total €420.00</li>
 *     <li>Customer 2 — Bruno Matos, 3 orders (ids 5–7), total €290.00</li>
 *     <li>Customer 3 — Carla Sousa, 2 orders (ids 8–9), total €550.00</li>
 *     <li>Customer 4 — Daniel Pinto, 1 order (id 10), total €80.00</li>
 * </ul>
 *
 * <p>
 * Most expensive single order: order 8 (Carla Sousa, 1 item × €500.00).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = {
                                "spring.datasource.url=jdbc:h2:mem:order-it-db;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
                                "spring.datasource.username=sa",
                                "spring.datasource.password=",
                                "spring.jpa.hibernate.ddl-auto=none",
                                "spring.sql.init.mode=always",
                                "spring.sql.init.schema-locations=classpath:db/schema.sql",
                                "spring.sql.init.data-locations=classpath:db/data.sql,classpath:db/reset-identities.sql"
                              }
                )
class OrderApiIntegrationTest {

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
   * Verifies that filtering orders by customer id returns only that customer's
   * orders.
   *
   * <p>
   * Alice Ferreira (id=1) has exactly 4 seeded orders.
   */
  @Test
  @DisplayName("GET /api/orders/customer/{customerId} returns seeded customer orders")
  void byCustomerReturnsSeededOrders() {
    RestAssured.given().accept(ContentType.JSON)
               .when().get("/api/orders/customer/{customerId}", 1)
               .then().statusCode(200)
                      .body("size()", Matchers.equalTo(4));
  }

  /**
   * Verifies that the total of order 1 matches the seeded item values.
   *
   * <p>
   * Order 1: 2 × €50.00 (PROD-001) + 1 × €30.00 (PROD-002) =
   * <strong>€130.00</strong>.
   */
  @Test
  @DisplayName("GET /api/orders/{id}/total returns order total from persisted data")
  void totalReturnsSeededValue() {
    RestAssured.given().accept(ContentType.JSON)
               .when().get("/api/orders/{id}/total", 1)
               .then().statusCode(200)
                      .body(Matchers.equalTo("130.00"));
  }

  /**
   * Verifies that the most expensive order across all seed data is correctly
   * identified.
   *
   * <p>
   * Order 8 (Carla Sousa): 1 × €500.00 — the highest single-order total in the
   * seed set.
   */
  @Test
  @DisplayName("GET /api/orders/most-expensive returns highest-value order")
  void mostExpensiveReturnsExpectedOrder() {
    RestAssured.given().accept(ContentType.JSON)
               .when().get("/api/orders/most-expensive")
               .then().statusCode(200)
                      .body("id", Matchers.equalTo(8))
                      .body("customerName", Matchers.equalTo("Carla Sousa"))
                      .body("total", Matchers.equalTo(500.00f))
                      .body("items", Matchers.hasSize(1));
  }

  /**
   * Verifies that a reference to a non-existent customer produces a 400 response.
   */
  @Test
  @DisplayName("POST /api/orders returns 400 when customer does not exist")
  void createReturnsBadRequestForUnknownCustomer() {
    String body = """
        {
          "customerId": 999,
          "items": [{ "productId": "PROD-XYZ", "quantity": 1, "unitPrice": 10.00 }]
        }
        """;

    RestAssured.given().contentType(ContentType.JSON)
                       .body(body)
               .when().post("/api/orders")
               .then().statusCode(400);
  }

  /**
   * Verifies full order creation with a valid payload.
   *
   * <p>
   * Expected total: 2 × €49.99 + 1 × €15.00 = <strong>€114.98</strong>. The
   * response must include a {@code Location} header pointing to the created
   * resource.
   */
  @Test
  @DisplayName("POST /api/orders returns 201 and persisted order payload")
  void createReturnsCreatedWhenPayloadIsValid() {
    String body = """
        {
          "customerId": 1,
          "items": [
            { "productId": "PROD-NEW-1", "quantity": 2, "unitPrice": 49.99 },
            { "productId": "PROD-NEW-2", "quantity": 1, "unitPrice": 15.00 }
          ]
        }
        """;

    RestAssured.given().contentType(ContentType.JSON)
                       .accept(ContentType.JSON)
                       .body(body)
               .when().post("/api/orders")
               .then().statusCode(201)
                      .header("Location", Matchers.containsString("/api/orders/"))
                      .body("id", Matchers.notNullValue())
                      .body("customerId", Matchers.equalTo(1))
                      .body("customerName", Matchers.equalTo("Alice Ferreira"))
                      .body("status", Matchers.equalTo("PENDING"))
                      .body("items", Matchers.hasSize(2))
                      .body("total", Matchers.equalTo(114.98f));
  }

  /**
   * Verifies that requesting a non-existent order by id returns 404.
   */
  @Test
  @DisplayName("GET /api/orders/{id} returns 404 for unknown id")
  void getByIdReturnsNotFoundWhenOrderDoesNotExist() {
    RestAssured.given().accept(ContentType.JSON)
               .when().get("/api/orders/{id}", 99999)
               .then().statusCode(404);
  }

  /**
   * Verifies that requesting the total of a non-existent order returns 404.
   */
  @Test
  @DisplayName("GET /api/orders/{id}/total returns 404 for unknown id")
  void totalReturnsNotFoundWhenOrderDoesNotExist() {
    RestAssured.given().accept(ContentType.JSON)
               .when().get("/api/orders/{id}/total", 99999)
               .then().statusCode(404);
  }

  /**
   * Verifies that a null {@code customerId} in the request body returns 400.
   */
  @Test
  @DisplayName("POST /api/orders returns 400 when customerId is null")
  void createReturnsBadRequestWhenCustomerIdIsNull() {
    String body = """
        {
          "customerId": null,
          "items": [{ "productId": "PROD-001", "quantity": 1, "unitPrice": 10.00 }]
        }
        """;
    RestAssured.given().contentType(ContentType.JSON)
                       .body(body)
               .when().post("/api/orders")
               .then().statusCode(400)
                      .body("error", Matchers.notNullValue());
  }

  /**
   * Verifies that a zero {@code quantity} in any item returns 400.
   */
  @Test
  @DisplayName("POST /api/orders returns 400 when quantity is zero")
  void createReturnsBadRequestWhenQuantityIsZero() {
    String body = """
        {
          "customerId": 1,
          "items": [{ "productId": "PROD-001", "quantity": 0, "unitPrice": 10.00 }]
        }
        """;
    RestAssured.given().contentType(ContentType.JSON)
                       .body(body)
               .when().post("/api/orders")
               .then().statusCode(400);
  }

  /**
   * Verifies that a blank {@code productId} in any item returns 400.
   */
  @Test
  @DisplayName("POST /api/orders returns 400 when productId is blank")
  void createReturnsBadRequestWhenProductIdIsBlank() {
    String body = """
        {
          "customerId": 1,
          "items": [{ "productId": "  ", "quantity": 1, "unitPrice": 10.00 }]
        }
        """;
    RestAssured.given().contentType(ContentType.JSON)
                       .body(body)
               .when().post("/api/orders")
               .then().statusCode(400);
  }

  /**
   * Verifies that a zero order id returns 400.
   */
  @Test
  @DisplayName("GET /api/orders/{id} returns 400 when id is zero")
  void getByIdReturnsBadRequestWhenIdIsZero() {
    RestAssured.given().accept(ContentType.JSON)
               .when().get("/api/orders/{id}", 0)
               .then().statusCode(400);
  }

  /**
   * Verifies that a negative order id returns 400.
   */
  @Test
  @DisplayName("GET /api/orders/{id} returns 400 when id is negative")
  void getByIdReturnsBadRequestWhenIdIsNegative() {
    RestAssured.given().accept(ContentType.JSON)
               .when().get("/api/orders/{id}", -1)
               .then().statusCode(400);
  }

  /**
   * Verifies that a negative order id on the total endpoint returns 400.
   */
  @Test
  @DisplayName("GET /api/orders/{id}/total returns 400 when id is negative")
  void totalReturnsBadRequestWhenIdIsNegative() {
    RestAssured.given().accept(ContentType.JSON)
               .when().get("/api/orders/{id}/total", -1)
               .then().statusCode(400);
  }

  /**
   * Verifies that submitting an order with a null {@code items} field returns
   * 400.
   *
   * <p>
   * Bean Validation rejects the request before the controller body is entered,
   * so the failure is explicit and client-visible rather than an unhandled NPE.
   */
  @Test
  @DisplayName("POST /api/orders returns 400 when items field is null")
  void createReturnsBadRequestWhenItemsIsNull() {
    String body = """
        {
          "customerId": 1,
          "items": null
        }
        """;

    RestAssured.given().contentType(ContentType.JSON)
                       .accept(ContentType.JSON)
                       .body(body)
               .when().post("/api/orders")
               .then().statusCode(400);
  }
}
