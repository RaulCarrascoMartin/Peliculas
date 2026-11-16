package shop.shared.pagination;

import java.util.List;

/** Framework-free page request: page number (from 0), size and optional sort criteria. */
public record PageQuery(int page, int size, List<Sort> sort) {

    /** Sort by one property of the model. */
    public record Sort(String property, boolean ascending) {
    }

    public PageQuery {
        sort = sort == null ? List.of() : List.copyOf(sort);
    }

    public static PageQuery of(int page, int size) {
        return new PageQuery(page, size, List.of());
    }

    public long offset() {
        return (long) page * size;
    }
}
