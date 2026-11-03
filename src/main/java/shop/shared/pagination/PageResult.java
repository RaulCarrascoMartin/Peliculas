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

    /** Number of pages needed to hold all the elements ({@code 0} if the page size is not positive). */
    public int totalPages() {
        int size = query.size();
        return size <= 0 ? 0 : (int) Math.ceil((double) total / size);
    }

    public boolean isEmpty() {
        return content.isEmpty();
    }
}
