-- Esquema de la aplicación (persistencia JDBC). Se ejecuta en cada arranque (spring.sql.init) y también puede
-- montarse en /docker-entrypoint-initdb.d de MariaDB; en ese caso, para recargarla hay que reiniciar el volumen.
-- Sin claves foráneas entre módulos: cada módulo es dueño de sus tablas y los limpia por código.
CREATE TABLE IF NOT EXISTS usuarios (
    id         BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    email      VARCHAR(255) NOT NULL,
    contrasena VARCHAR(255) NOT NULL,
    rol        VARCHAR(20)  NOT NULL,
    CONSTRAINT uk_usuarios_email UNIQUE (email)
);

CREATE TABLE IF NOT EXISTS peliculas (
    id               BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    titulo           VARCHAR(255)   NOT NULL,
    anio             INT,
    genero           VARCHAR(255),
    director         VARCHAR(255),
    valoracion_media DOUBLE,
    precio           DECIMAL(10, 2),
    stock            INT            NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS valoraciones (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL,
    pelicula_id BIGINT       NOT NULL,
    puntuacion  INT          NOT NULL,
    comentario  VARCHAR(255),
    CONSTRAINT uk_valoraciones_usuario_pelicula UNIQUE (usuario_id, pelicula_id)
);

CREATE TABLE IF NOT EXISTS marcas_pelicula (
    id          BIGINT      NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL,
    pelicula_id BIGINT      NOT NULL,
    tipo        VARCHAR(20) NOT NULL,
    CONSTRAINT uk_marcas_usuario_pelicula_tipo UNIQUE (usuario_id, pelicula_id, tipo)
);

CREATE TABLE IF NOT EXISTS carritos (
    id             BIGINT    NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id     BIGINT    NOT NULL,
    version        BIGINT    NOT NULL DEFAULT 0,
    actualizado_en TIMESTAMP NULL,
    CONSTRAINT uk_carritos_usuario UNIQUE (usuario_id)
);

CREATE TABLE IF NOT EXISTS lineas_carrito (
    id          BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    carrito_id  BIGINT NOT NULL,
    pelicula_id BIGINT NOT NULL,
    CONSTRAINT uk_lineas_carrito_pelicula UNIQUE (carrito_id, pelicula_id)
);

CREATE TABLE IF NOT EXISTS compras (
    id         BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    usuario_id BIGINT         NOT NULL,
    fecha      TIMESTAMP      NOT NULL,
    total      DECIMAL(10, 2) NOT NULL,
    estado     VARCHAR(20)    NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_compras_usuario_fecha ON compras (usuario_id, fecha);

CREATE TABLE IF NOT EXISTS lineas_compra (
    id              BIGINT         NOT NULL AUTO_INCREMENT PRIMARY KEY,
    compra_id       BIGINT         NOT NULL,
    pelicula_id     BIGINT         NOT NULL,
    titulo          VARCHAR(255)   NOT NULL,
    precio_unitario DECIMAL(10, 2) NOT NULL
);

-- Pedidos (módulo orders)
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
