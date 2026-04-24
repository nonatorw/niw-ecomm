-- Customers
MERGE INTO customers (id, name, email) KEY(id) VALUES
    (1, 'Alice Ferreira',  'alice@example.com'),
    (2, 'Bruno Matos',     'bruno@example.com'),
    (3, 'Carla Sousa',     'carla@example.com'),
    (4, 'Daniel Pinto',    'daniel@example.com');

-- Orders
-- Alice: 4 orders (qualifies for Problem A)
-- Bruno: 3 orders (qualifies for Problem A)
-- Carla: 2 orders (does NOT qualify)
-- Daniel: 1 order (does NOT qualify)
MERGE INTO orders (id, customer_id, status, created_at) KEY(id) VALUES
    (1,  1, 'CONFIRMED', '2024-01-10'),
    (2,  1, 'SHIPPED',   '2024-02-15'),
    (3,  1, 'PENDING',   '2024-03-20'),
    (4,  1, 'CANCELLED', '2024-04-05'),
    (5,  2, 'CONFIRMED', '2024-01-22'),
    (6,  2, 'SHIPPED',   '2024-03-01'),
    (7,  2, 'PENDING',   '2024-04-10'),
    (8,  3, 'CONFIRMED', '2024-02-18'),
    (9,  3, 'SHIPPED',   '2024-03-30'),
    (10, 4, 'PENDING',   '2024-04-20');

-- Order items
-- Order 1 (Alice): 2x €50.00 + 1x €30.00 = €130.00
MERGE INTO order_items (id, order_id, product_id, quantity, unit_price) KEY(id) VALUES
    (1,  1,  'PROD-001', 2, 50.00),
    (2,  1,  'PROD-002', 1, 30.00),
-- Order 2 (Alice): 3x €20.00 = €60.00
    (3,  2,  'PROD-003', 3, 20.00),
-- Order 3 (Alice): 1x €200.00 = €200.00
    (4,  3,  'PROD-004', 1, 200.00),
-- Order 4 (Alice): 2x €15.00 = €30.00
    (5,  4,  'PROD-005', 2, 15.00),
-- Alice total: €420.00

-- Order 5 (Bruno): 1x €100.00 = €100.00
    (6,  5,  'PROD-001', 1, 100.00),
-- Order 6 (Bruno): 2x €75.00 = €150.00
    (7,  6,  'PROD-006', 2, 75.00),
-- Order 7 (Bruno): 4x €10.00 = €40.00
    (8,  7,  'PROD-007', 4, 10.00),
-- Bruno total: €290.00

-- Order 8 (Carla): 1x €500.00 = €500.00
    (9,  8,  'PROD-008', 1, 500.00),
-- Order 9 (Carla): 2x €25.00 = €50.00
    (10, 9,  'PROD-009', 2, 25.00),

-- Order 10 (Daniel): 1x €80.00 = €80.00
    (11, 10, 'PROD-010', 1, 80.00);
