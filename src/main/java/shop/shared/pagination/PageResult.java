package shop.shared.pagination;

import java.util.List;
import java.util.function.Function;

/** Framework-free page of results with the total number of elements. */
public record PageResult<T>(List<T> content, long total, PageQuery query) {

    public PageResult {
        content = List.copyOf(content);
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(content.stream().<R>map(mapper).toList(), total, query);
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }
}
