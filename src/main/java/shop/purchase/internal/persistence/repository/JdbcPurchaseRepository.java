package shop.purchase.internal.persistence.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.purchase.internal.domain.model.Purchase;
import shop.purchase.internal.domain.model.PurchaseStatus;
import shop.purchase.internal.domain.repository.PurchaseRepository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

/** JDBC persistence of purchases over the {@code compras} table of {@code init.sql}. */
public class JdbcPurchaseRepository implements PurchaseRepository {

    private static final String COLUMNS = "id, usuario_id, fecha, total, estado";

    private final JdbcClient jdbc;

    public JdbcPurchaseRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Purchase save(Purchase purchase) {
        Timestamp date = purchase.getPurchasedAt() == null ? null : Timestamp.valueOf(purchase.getPurchasedAt());
        String status = purchase.getStatus() == null ? null : purchase.getStatus().name();
        if (purchase.getId() == null) {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.sql("insert into compras (usuario_id, fecha, total, estado) values (?, ?, ?, ?)")
                    .param(purchase.getUserId())
                    .param(date)
                    .param(purchase.getTotal())
                    .param(status)
                    .update(keys, "id");
            purchase.setId(keys.getKey().longValue());
        } else {
            jdbc.sql("update compras set usuario_id = ?, fecha = ?, total = ?, estado = ? where id = ?")
                    .param(purchase.getUserId())
                    .param(date)
                    .param(purchase.getTotal())
                    .param(status)
                    .param(purchase.getId())
                    .update();
        }
        return purchase;
    }

    @Override
    public List<Purchase> findByUserIdOrderByPurchasedAtDesc(Long userId) {
        return jdbc.sql("select " + COLUMNS + " from compras where usuario_id = ? order by fecha desc, id desc")
                .param(userId)
                .query(JdbcPurchaseRepository::map)
                .list();
    }

    @Override
    public Optional<Purchase> findByIdAndUserId(Long id, Long userId) {
        return jdbc.sql("select " + COLUMNS + " from compras where id = ? and usuario_id = ?")
                .param(id)
                .param(userId)
                .query(JdbcPurchaseRepository::map)
                .optional();
    }

    private static Purchase map(ResultSet rs, int row) throws SQLException {
        var p = new Purchase();
        p.setId(rs.getLong("id"));
        p.setUserId(rs.getLong("usuario_id"));
        p.setPurchasedAt(rs.getTimestamp("fecha").toLocalDateTime());
        p.setTotal(rs.getBigDecimal("total"));
        p.setStatus(PurchaseStatus.valueOf(rs.getString("estado")));
        return p;
    }
}
