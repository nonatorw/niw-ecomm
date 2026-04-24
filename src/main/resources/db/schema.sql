CREATE TABLE IF NOT EXISTS customers (
    id   BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL
);

CREATE TABLE IF NOT EXISTS orders (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    customer_id BIGINT NOT NULL REFERENCES customers(id),
    status      VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at  DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS order_items (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id    BIGINT NOT NULL REFERENCES orders(id),
    product_id  VARCHAR(100) NOT NULL,
    quantity    INT NOT NULL,
    unit_price  DECIMAL(10, 2) NOT NULL
);
