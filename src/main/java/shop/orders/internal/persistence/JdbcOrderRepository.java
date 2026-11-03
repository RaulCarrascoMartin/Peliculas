package shop.orders.internal.persistence;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import shop.orders.internal.domain.Order;
import shop.orders.internal.domain.OrderLine;
import shop.orders.internal.domain.OrderRepository;
import shop.orders.internal.domain.OrderStatus;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** JDBC persistence of orders over the {@code orders} and {@code order_lines} tables of {@code init.sql}. */
@Repository
public class JdbcOrderRepository implements OrderRepository {

    private final JdbcClient jdbc;

    public JdbcOrderRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Order save(Order order) {
        Instant createdAt = order.getCreatedAt() != null ? order.getCreatedAt() : Instant.now();
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("insert into orders (user_id, status, total, created_at) values (?, ?, ?, ?)")
                .param(order.getUserId())
                .param(order.getStatus().name())
                .param(order.getTotal())
                .param(Timestamp.from(createdAt))
                .update(keys, "id");
        Long id = keys.getKey().longValue();
        for (OrderLine line : order.getLines()) {
            jdbc.sql("insert into order_lines (order_id, product_id, title, unit_price, quantity) values (?, ?, ?, ?, ?)")
                    .param(id)
                    .param(line.getProductId())
                    .param(line.getTitle())
                    .param(line.getUnitPrice())
                    .param(line.getQuantity())
                    .update();
        }
        order.setId(id);
        order.setCreatedAt(createdAt);
        return order;
    }

    @Override
    public Optional<Order> findById(Long id) {
        Optional<Order> quantities = jdbc.sql("select id, user_id, status, total, created_at from orders where id = ?")
                .param(id)
                .query((rs, n) -> {
                    Order o = new Order();
                    o.setId(rs.getLong("id"));
                    o.setUserId(rs.getLong("user_id"));
                    o.setStatus(OrderStatus.valueOf(rs.getString("status")));
                    o.setTotal(rs.getBigDecimal("total"));
                    o.setCreatedAt(rs.getTimestamp("created_at").toInstant());
                    return o;
                })
                .optional();
        quantities.ifPresent(o -> o.setLines(linesOf(id)));
        return quantities;
    }

    private List<OrderLine> linesOf(Long orderId) {
        return jdbc.sql("select product_id, title, unit_price, quantity from order_lines where order_id = ? order by id")
                .param(orderId)
                .query((rs, n) -> {
                    OrderLine l = new OrderLine();
                    l.setProductId(rs.getLong("product_id"));
                    l.setTitle(rs.getString("title"));
                    l.setUnitPrice(rs.getBigDecimal("unit_price"));
                    l.setQuantity(rs.getInt("quantity"));
                    return l;
                })
                .list();
    }

    @Override
    public boolean cancelIfCreated(Long id) {
        return jdbc.sql("update orders set status = ? where id = ? and status = ?")
                .param(OrderStatus.CANCELLED.name())
                .param(id)
                .param(OrderStatus.CREATED.name())
                .update() == 1;
    }
}
