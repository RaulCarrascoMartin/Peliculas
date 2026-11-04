package shop.purchase.internal.persistence.repository;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.purchase.internal.domain.model.PurchaseLine;
import shop.purchase.internal.domain.repository.PurchaseLineRepository;

import java.util.List;

/** JDBC persistence of purchase lines over the {@code lineas_compra} table of {@code init.sql}. */
public class JdbcPurchaseLineRepository implements PurchaseLineRepository {

    private final JdbcClient jdbc;

    public JdbcPurchaseLineRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PurchaseLine save(PurchaseLine line) {
        if (line.getId() == null) {
            KeyHolder keys = new GeneratedKeyHolder();
            jdbc.sql("insert into lineas_compra (compra_id, pelicula_id, titulo, precio_unitario) values (?, ?, ?, ?)")
                    .param(line.getPurchaseId())
                    .param(line.getMovieId())
                    .param(line.getTitle())
                    .param(line.getUnitPrice())
                    .update(keys, "id");
            line.setId(keys.getKey().longValue());
        } else {
            jdbc.sql("update lineas_compra set compra_id = ?, pelicula_id = ?, titulo = ?, precio_unitario = ? where id = ?")
                    .param(line.getPurchaseId())
                    .param(line.getMovieId())
                    .param(line.getTitle())
                    .param(line.getUnitPrice())
                    .param(line.getId())
                    .update();
        }
        return line;
    }

    @Override
    public List<PurchaseLine> findByPurchaseIdOrderById(Long purchaseId) {
        return jdbc.sql("select id, compra_id, pelicula_id, titulo, precio_unitario from lineas_compra "
                        + "where compra_id = ? order by id")
                .param(purchaseId)
                .query((rs, n) -> {
                    var l = new PurchaseLine();
                    l.setId(rs.getLong("id"));
                    l.setPurchaseId(rs.getLong("compra_id"));
                    l.setMovieId(rs.getLong("pelicula_id"));
                    l.setTitle(rs.getString("titulo"));
                    l.setUnitPrice(rs.getBigDecimal("precio_unitario"));
                    return l;
                })
                .list();
    }
}
