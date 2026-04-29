# niw-ecomm

Spring Boot REST API for order management, implementing two problems from a Java technical exercise.

---

## Requirements

| Tool  | Version                   |
|-------|---------------------------|
| Java  | 25 LTS                    |
| Maven | 3.9.x (wrapper included)  |

No other dependencies are required — the H2 database is embedded.

---

## Running the Application

```bash
# Clone the repository
git clone <repo-url>
cd niw-ecomm

# Run with the Maven wrapper (downloads dependencies automatically)
./mvnw spring-boot:run
```

The application starts on **<http://localhost:8080>**.

A file-based H2 database is created at `./data/niw-ecomm` on first run and persists between restarts. Seed data is loaded automatically from `src/main/resources/db/data.sql`.

---

## Running the Tests

```bash
./mvnw test
```

Test reports are generated at `target/surefire-reports/`.

---

## API Documentation (Swagger UI)

Open in browser after starting the application: http://localhost:8080/swagger-ui.html

Raw OpenAPI spec (JSON): http://localhost:8080/api-docs

---

## H2 Console

Access the embedded database browser: http://localhost:8080/h2-console


| Field    | Value                                            |
|----------|--------------------------------------------------|
| JDBC URL | `jdbc:h2:file:./data/niw-ecomm;AUTO_SERVER=TRUE` |
| Username | `sa`                                             |
| Password | *(empty)*                                        |

---

## REST Endpoints

### Orders — `/api/orders`

| Method | Path                                | Description                       |
|--------|-------------------------------------|-----------------------------------|
| `POST` | `/api/orders`                       | Create a new order                |
| `GET`  | `/api/orders`                       | List all orders                   |
| `GET`  | `/api/orders/{id}`                  | Get order by id                   |
| `GET`  | `/api/orders/{id}/total`            | Calculate total value of an order |
| `GET`  | `/api/orders/customer/{customerId}` | List orders by customer id        |
| `GET`  | `/api/orders/grouped-by-status`     | Orders grouped by status          |
| `GET`  | `/api/orders/most-expensive`        | Order with the highest total      |

### Reports — `/api/reports`

| Method | Path                        | Description                                                          |
|--------|-----------------------------|----------------------------------------------------------------------|
| `GET`  | `/api/reports/top-spenders` | Customers with more than 2 orders, ordered by total spent descending |

---

## Example Requests

### Create an order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "items": [
      { "productId": "PROD-001", "quantity": 2, "unitPrice": 49.99 },
      { "productId": "PROD-002", "quantity": 1, "unitPrice": 15.00 }
    ]
  }'
```

### Get the most expensive order

```bash
curl http://localhost:8080/api/orders/most-expensive
```

### Get top-spending customers (Problem A)

```bash
curl http://localhost:8080/api/reports/top-spenders
```

Expected response (based on seed data):

```json
[
  { "customerName": "Alice Ferreira", "orderCount": 4, "totalSpent": 420.00 },
  { "customerName": "Bruno Matos",    "orderCount": 3, "totalSpent": 290.00 }
]
```

### Get orders grouped by status

```bash
curl http://localhost:8080/api/orders/grouped-by-status
```

---

## Seed Data

The following customers and orders are loaded on every startup:

| Customer        | Orders | Total Spent | Qualifies (>2 orders) |
|-----------------|--------|-------------|-----------------------|
| Alice Ferreira  | 4      | €420.00     | Yes                   |
| Bruno Matos     | 3      | €290.00     | Yes                   |
| Carla Sousa     | 2      | €550.00     | No                    |
| Daniel Pinto    | 1      | €80.00      | No                    |

---

## Project Structure

```bash
src/
├── main/java/com/niw/ecomm/
│   ├── domain/          # JPA entities and OrderStatus enum
│   ├── repository/      # Spring Data repositories
│   ├── rec/             # Request/response records (Java records)
│   ├── service/         # Business logic (OrderService, CustomerReportService)
│   └── controller/      # REST controllers
└── test/java/com/niw/ecomm/
    ├── service/         # Unit tests (no Spring context)
    ├── repository/      # @DataJpaTest slice tests
    ├── controller/      # @WebMvcTest slice tests
    └── integration/     # Full @SpringBootTest integration tests
```

---

## Design Notes

- **OrderService** methods accept `List<Order>` as per the exercise specification — pure, stateless functions testable without Spring context.
- **Problem A** query is a JPQL aggregation that computes `SUM(quantity × unitPrice)` inline, avoiding a denormalized `amount` column.
- **H2 file mode** (`AUTO_SERVER=TRUE`) allows multiple connections simultaneously, including the H2 console while the app is running.
- **Mockito subclass mock maker** is configured in `src/test/resources/mockito-extensions/` to ensure compatibility with Java 25.
