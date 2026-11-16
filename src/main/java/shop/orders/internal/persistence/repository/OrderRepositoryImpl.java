package shop.orders.internal.persistence.repository;

import jakarta.inject.Named;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.orders.internal.domain.dto.OrderDto;
import shop.orders.internal.domain.dto.OrderLineDto;
import shop.orders.internal.domain.model.OrderStatus;
import shop.orders.internal.domain.repository.OrderRepository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * JDBC persistence of orders over the {@code orders} and {@code order_lines} tables (Flyway migrations). The lines
 * are children of the order: this repository inserts and reads them with their parent.
 */
@Named
public class OrderRepositoryImpl implements OrderRepository {

    private static final RowMapper<OrderLineDto> LINE_MAPPER = (rs, n) -> new OrderLineDto(
            rs.getLong("product_id"),
            rs.getString("title"),
            rs.getBigDecimal("unit_price"),
            rs.getInt("quantity"));

    private final JdbcClient jdbc;

    public OrderRepositoryImpl(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public OrderDto save(OrderDto order) {
        Instant createdAt = order.createdAt() != null ? order.createdAt() : Instant.now();
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("insert into orders (user_id, status, total, created_at) values (:userId, :status, :total, :createdAt)")
                .param("userId", order.userId())
                .param("status", order.status().name())
                .param("total", order.total())
                .param("createdAt", Timestamp.from(createdAt))
                .update(keys, "id");
        Long id = keys.getKey().longValue();
        for (OrderLineDto line : order.lines()) {
            jdbc.sql("insert into order_lines (order_id, product_id, title, unit_price, quantity) "
                            + "values (:orderId, :productId, :title, :unitPrice, :quantity)")
                    .param("orderId", id)
                    .param("productId", line.productId())
                    .param("title", line.title())
                    .param("unitPrice", line.unitPrice())
                    .param("quantity", line.quantity())
                    .update();
        }
        return new OrderDto(id, order.userId(), order.status(), createdAt, order.total(), order.lines());
    }

    @Override
    public Optional<OrderDto> findById(Long id) {
        return jdbc.sql("select id, user_id, status, total, created_at from orders where id = :id")
                .param("id", id)
                .query(orderMapper(linesOf(id)))
                .optional();
    }

    @Override
    public boolean cancelIfCreated(Long id) {
        return jdbc.sql("update orders set status = :cancelled where id = :id and status = :created")
                .param("cancelled", OrderStatus.CANCELLED.name())
                .param("id", id)
                .param("created", OrderStatus.CREATED.name())
                .update() == 1;
    }

    private List<OrderLineDto> linesOf(Long orderId) {
        return jdbc.sql("select product_id, title, unit_price, quantity from order_lines "
                        + "where order_id = :orderId order by id")
                .param("orderId", orderId)
                .query(LINE_MAPPER)
                .list();
    }

    private static RowMapper<OrderDto> orderMapper(List<OrderLineDto> lines) {
        return (rs, n) -> new OrderDto(
                rs.getLong("id"),
                rs.getLong("user_id"),
                OrderStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getBigDecimal("total"),
                lines);
    }
}
