package shop.purchase.internal.persistence.repository;

import jakarta.inject.Named;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import shop.purchase.internal.domain.dto.PurchaseDto;
import shop.purchase.internal.domain.dto.PurchaseLineDto;
import shop.purchase.internal.domain.model.PurchaseStatus;
import shop.purchase.internal.domain.repository.PurchaseRepository;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * JDBC persistence of purchases over the {@code compras} and {@code lineas_compra} tables (Flyway migrations). The
 * lines are children of the purchase: this repository inserts and reads them with their parent.
 */
@Named
public class PurchaseRepositoryImpl implements PurchaseRepository {

    private static final String PURCHASE_COLUMNS = "id, usuario_id, fecha, total, estado";

    private static final RowMapper<PurchaseLineDto> LINE_MAPPER = (rs, n) -> new PurchaseLineDto(
            rs.getLong("id"),
            rs.getLong("compra_id"),
            rs.getLong("pelicula_id"),
            rs.getString("titulo"),
            rs.getBigDecimal("precio_unitario"));

    /** The lines are attached afterwards, once all of them have been read with a single query. */
    private static final RowMapper<PurchaseDto> PURCHASE_MAPPER = (rs, n) -> new PurchaseDto(
            rs.getLong("id"),
            rs.getLong("usuario_id"),
            rs.getTimestamp("fecha").toLocalDateTime(),
            rs.getBigDecimal("total"),
            PurchaseStatus.valueOf(rs.getString("estado")),
            List.of());

    private final JdbcClient jdbc;

    public PurchaseRepositoryImpl(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public PurchaseDto save(PurchaseDto purchase) {
        KeyHolder keys = new GeneratedKeyHolder();
        jdbc.sql("insert into compras (usuario_id, fecha, total, estado) "
                        + "values (:userId, :purchasedAt, :total, :status)")
                .param("userId", purchase.userId())
                .param("purchasedAt", purchase.purchasedAt() == null ? null : Timestamp.valueOf(purchase.purchasedAt()))
                .param("total", purchase.total())
                .param("status", purchase.status() == null ? null : purchase.status().name())
                .update(keys, "id");
        Long id = keys.getKey().longValue();
        List<PurchaseLineDto> lines = new ArrayList<>();
        for (PurchaseLineDto line : purchase.lines()) {
            KeyHolder lineKeys = new GeneratedKeyHolder();
            jdbc.sql("insert into lineas_compra (compra_id, pelicula_id, titulo, precio_unitario) "
                            + "values (:purchaseId, :movieId, :title, :unitPrice)")
                    .param("purchaseId", id)
                    .param("movieId", line.movieId())
                    .param("title", line.title())
                    .param("unitPrice", line.unitPrice())
                    .update(lineKeys, "id");
            lines.add(new PurchaseLineDto(lineKeys.getKey().longValue(), id, line.movieId(), line.title(),
                    line.unitPrice()));
        }
        return new PurchaseDto(id, purchase.userId(), purchase.purchasedAt(), purchase.total(), purchase.status(),
                List.copyOf(lines));
    }

    @Override
    public List<PurchaseDto> findByUserIdOrderByPurchasedAtDesc(Long userId) {
        List<PurchaseDto> purchases = jdbc.sql("select " + PURCHASE_COLUMNS + " from compras "
                        + "where usuario_id = :userId order by fecha desc, id desc")
                .param("userId", userId)
                .query(PURCHASE_MAPPER)
                .list();
        return withLines(purchases);
    }

    @Override
    public Optional<PurchaseDto> findByIdAndUserId(Long id, Long userId) {
        return jdbc.sql("select " + PURCHASE_COLUMNS + " from compras where id = :id and usuario_id = :userId")
                .param("id", id)
                .param("userId", userId)
                .query(PURCHASE_MAPPER)
                .optional()
                .map(purchase -> withLines(List.of(purchase)).get(0));
    }

    /** One query for the lines of all the purchases (no N+1), grouped in memory; each group in insertion order. */
    private List<PurchaseDto> withLines(List<PurchaseDto> purchases) {
        if (purchases.isEmpty()) {
            return purchases;
        }
        Map<Long, List<PurchaseLineDto>> byPurchase = jdbc.sql(
                        "select id, compra_id, pelicula_id, titulo, precio_unitario from lineas_compra "
                                + "where compra_id in (:ids) order by id")
                .param("ids", purchases.stream().map(PurchaseDto::id).toList())
                .query(LINE_MAPPER)
                .list().stream()
                .collect(Collectors.groupingBy(PurchaseLineDto::purchaseId));
        return purchases.stream()
                .map(p -> new PurchaseDto(p.id(), p.userId(), p.purchasedAt(), p.total(), p.status(),
                        byPurchase.getOrDefault(p.id(), List.of())))
                .toList();
    }
}
