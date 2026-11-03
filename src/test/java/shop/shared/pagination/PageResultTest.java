package shop.shared.pagination;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResultTest {

    @Test
    void queryComputesOffsetAndDefaultsToNoSort() {
        var query = PageQuery.of(2, 10);

        assertEquals(20, query.offset());
        assertTrue(query.sort().isEmpty());
        assertTrue(new PageQuery(0, 5, null).sort().isEmpty());
    }

    @Test
    void mapKeepsTotalAndQuery() {
        var page = new PageResult<>(List.of(1, 2), 7, PageQuery.of(0, 2));

        var mapped = page.map(n -> "n" + n);

        assertEquals(List.of("n1", "n2"), mapped.content());
        assertEquals(7, mapped.total());
        assertTrue(new PageResult<Integer>(List.of(), 0, PageQuery.of(0, 2)).isEmpty());
    }
}
