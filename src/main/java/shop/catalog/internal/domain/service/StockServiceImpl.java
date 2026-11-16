package shop.catalog.internal.domain.service;

import shop.catalog.internal.domain.exception.InsufficientStockException;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.exception.MovieNotPurchasableException;
import shop.catalog.internal.domain.model.Movie;
import shop.catalog.internal.domain.repository.MovieRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Reserves all the lines or none, without a transaction: first validates everything against the data read and then
 * deducts each movie with a conditional subtraction; if one of those subtractions loses a race, it gives back what
 * was already deducted before failing.
 */
public class StockServiceImpl implements StockService {

    private final MovieRepository movies;

    public StockServiceImpl(MovieRepository movies) {
        this.movies = movies;
    }

    @Override
    public List<Reservation> reserve(List<StockItem> items) {
        Map<Long, Integer> quantityByMovie = groupByMovie(items);
        Map<Long, Movie> existing = movies.findAllById(quantityByMovie.keySet()).stream()
                .collect(Collectors.toMap(Movie::getId, Function.identity()));
        validate(quantityByMovie, existing);
        deduct(quantityByMovie);
        return items.stream()
                .map(i -> {
                    Movie m = existing.get(i.movieId());
                    return new Reservation(m.getId(), m.getTitle(), m.getPrice(), i.quantity());
                })
                .toList();
    }

    @Override
    public void release(List<StockItem> items) {
        items.forEach(i -> movies.incrementStock(i.movieId(), i.quantity()));
    }

    /** Adds up the quantities of lines that repeat a movie, keeping the order of appearance. */
    private static Map<Long, Integer> groupByMovie(List<StockItem> items) {
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
