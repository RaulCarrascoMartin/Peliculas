package shop.catalog.api.http.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import shop.catalog.api.http.dto.MoviePageResponse;
import shop.catalog.api.http.dto.MovieRequest;
import shop.catalog.api.http.dto.MovieResponse;
import shop.catalog.internal.domain.dto.MovieDetailsDto;
import shop.catalog.internal.domain.dto.MovieDto;
import shop.shared.pagination.PageQuery;
import shop.shared.pagination.PageResult;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MovieWebMapperTest {

    private final MovieWebMapper mapper = Mappers.getMapper(MovieWebMapper.class);

    private static final MovieDto ALIEN =
            new MovieDto(10L, "Alien", 1979, "Science fiction", "Ridley Scott", new BigDecimal("12.99"), 4.5, 3);
    private static final MovieResponse ALIEN_RESPONSE =
            new MovieResponse(10L, "Alien", 1979, "Science fiction", "Ridley Scott", new BigDecimal("12.99"), 4.5, 3);

    @Test
    void requestBecomesTheDtoOfTheService() {
        var request = new MovieRequest("Alien", 1979, "Science fiction", "Ridley Scott", new BigDecimal("12.99"), 3);

        assertEquals(new MovieDetailsDto("Alien", 1979, "Science fiction", "Ridley Scott", new BigDecimal("12.99"), 3),
                mapper.toDto(request));
    }

    @Test
    void requestWithoutStockKeepsItNull() {
        var request = new MovieRequest("Alien", 1979, "Science fiction", "Ridley Scott", new BigDecimal("12.99"), null);

        assertEquals(null, mapper.toDto(request).stock());
    }

    @Test
    void dtoOfTheServiceBecomesTheResponse() {
        assertEquals(ALIEN_RESPONSE, mapper.toResponse(ALIEN));
        assertEquals(List.of(ALIEN_RESPONSE), mapper.toResponses(List.of(ALIEN)));
    }

    @Test
    void movieWithoutPriceNorRatingsKeepsThemNull() {
        var response = mapper.toResponse(new MovieDto(11L, "Silent", 1950, "Drama", "Somebody", null, null, 0));

        assertEquals(new MovieResponse(11L, "Silent", 1950, "Drama", "Somebody", null, null, 0), response);
    }

    @Test
    void pageOfTheServiceBecomesContentPlusPageInfo() {
        var result = new PageResult<>(List.of(ALIEN), 5, PageQuery.of(1, 2));

        assertEquals(new MoviePageResponse(List.of(ALIEN_RESPONSE), new MoviePageResponse.PageInfo(2, 1, 5, 3)),
                mapper.toPageResponse(result));
    }

    @Test
    void emptyPageHasNoContentAndZeroTotals() {
        var response = mapper.toPageResponse(new PageResult<MovieDto>(List.of(), 0, PageQuery.of(0, 20)));

        assertTrue(response.content().isEmpty());
        assertEquals(new MoviePageResponse.PageInfo(20, 0, 0, 0), response.page());
    }

    @Test
    void paginationParametersBecomeThePageQuery() {
        PageQuery query = mapper.toPageQuery(2, 50, List.of("title", "year,desc"));

        assertEquals(new PageQuery(2, 50,
                List.of(new PageQuery.Sort("title", true), new PageQuery.Sort("year", false))), query);
        assertEquals(100, query.offset());
    }

    @Test
    void sortParameterIsAscendingUnlessItSaysDesc() {
        assertEquals(new PageQuery.Sort("title", true), mapper.toSort("title"));
        assertEquals(new PageQuery.Sort("title", true), mapper.toSort("title,asc"));
        assertEquals(new PageQuery.Sort("title", false), mapper.toSort("title,desc"));
        assertEquals(new PageQuery.Sort("title", false), mapper.toSort(" title , DESC "));
        assertEquals(new PageQuery.Sort("title", true), mapper.toSort("title,whatever"));
    }
}
