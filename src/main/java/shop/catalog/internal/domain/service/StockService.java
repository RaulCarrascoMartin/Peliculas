package shop.catalog.internal.domain.service;

import shop.catalog.internal.domain.dto.ReservationDto;
import shop.catalog.internal.domain.dto.StockItemDto;
import shop.catalog.internal.domain.exception.InsufficientStockException;
import shop.catalog.internal.domain.exception.MovieNotFoundException;
import shop.catalog.internal.domain.exception.MovieNotPurchasableException;

import java.util.List;

/** Stock rules: reserve all the lines or none, and give units back. */
public interface StockService {

    /**
     * Reserves units of all the lines or of none: if one fails, the stock stays as it was. Returns one entry per
     * requested line.
     *
     * @throws MovieNotFoundException       if a movie does not exist
     * @throws MovieNotPurchasableException if a movie has no price
     * @throws InsufficientStockException   if there are not enough units of a movie
     */
    List<ReservationDto> reserve(List<StockItemDto> items);

    /** Returns the units to the stock; movies that no longer exist are ignored. */
    void release(List<StockItemDto> items);
}
