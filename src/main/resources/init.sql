-- Tablas de pedidos (módulo orders, persistencia JDBC). Se ejecuta en cada arranque de la aplicación
-- (spring.sql.init) y también puede montarse en /docker-entrypoint-initdb.d de MariaDB; en ese caso,
-- para recargarla hay que reiniciar el volumen de MariaDB.
CREATE TABLE IF NOT EXISTS orders (
    id         BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT         NOT NULL,
    status     VARCHAR(20)    NOT NULL,
    total      DECIMAL(12, 2) NOT NULL,
    created_at TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS order_lines (
    id         BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_id   BIGINT         NOT NULL,
    product_id BIGINT         NOT NULL,
    title      VARCHAR(255)   NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    quantity   INT            NOT NULL,
    CONSTRAINT fk_order_lines_order FOREIGN KEY (order_id) REFERENCES orders (id)
);
