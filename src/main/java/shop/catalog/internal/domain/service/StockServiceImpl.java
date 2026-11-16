package shop.catalog.internal.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import shop.catalog.internal.domain.dto.ReservationDto;
import shop.catalog.internal.domain.dto.StockItemDto;
import shop.catalog.internal.domain.exception.InsufficientStockException;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.exception.MovieNotPurchasableException;
import shop.catalog.internal.domain.mapper.MovieMapper;
import shop.catalog.internal.domain.model.Movie;
import shop.catalog.internal.domain.repository.MovieRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reserves all the lines or none inside a transaction: first validates everything against the data read and then
 * deducts each movie with a conditional subtraction. If one of those subtractions loses a race, the exception rolls
 * the whole unit back; the explicit give-back is kept so the all-or-none guarantee also holds when the caller
 * catches the failure and keeps its own transaction going.
 */
@Service
public class StockServiceImpl implements StockService {

    private final MovieRepository movies;
    private final MovieMapper mapper;

    public StockServiceImpl(MovieRepository movies, MovieMapper mapper) {
        this.movies = movies;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public List<ReservationDto> reserve(List<StockItemDto> items) {
        Map<Long, Integer> quantityByMovie = groupByMovie(items);
        Map<Long, Movie> existing = movies.findAllById(quantityByMovie.keySet()).stream()
                .map(mapper::toModel)
                .collect(Collectors.toMap(Movie::getId, Function.identity()));
        validate(quantityByMovie, existing);
        deduct(quantityByMovie);
        return items.stream()
                .map(i -> mapper.toReservation(existing.get(i.movieId()), i.quantity()))
                .toList();
    }

    @Override
    @Transactional
    public void release(List<StockItemDto> items) {
        items.forEach(i -> movies.incrementStock(i.movieId(), i.quantity()));
    }

    /** Adds up the quantities of lines that repeat a movie, keeping the order of appearance. */
    private static Map<Long, Integer> groupByMovie(List<StockItemDto> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        items.forEach(i -> quantities.merge(i.movieId(), i.quantity(), Integer::sum));
        return quantities;
    }

    private static void validate(Map<Long, Integer> quantities, Map<Long, Movie> existing) {
        quantities.forEach((id, quantity) -> {
            Movie movie = existing.get(id);
            if (movie == null) {
                throw new MovieNotFoundException(id);
            }
            if (movie.getPrice() == null) {
                throw new MovieNotPurchasableException(id);
            }
            if (movie.getStock() < quantity) {
                throw new InsufficientStockException(id);
            }
        });
    }

    private void deduct(Map<Long, Integer> quantities) {
        List<Map.Entry<Long, Integer>> applied = new ArrayList<>();
        for (Map.Entry<Long, Integer> line : quantities.entrySet()) {
            if (movies.decrementStock(line.getKey(), line.getValue()) == 0) {
                applied.forEach(a -> movies.incrementStock(a.getKey(), a.getValue()));
                throw new InsufficientStockException(line.getKey());
            }
            applied.add(line);
        }
    }
}
