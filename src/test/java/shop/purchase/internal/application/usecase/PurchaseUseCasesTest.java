package shop.purchase.internal.application.usecase;

import shop.purchase.internal.application.dto.PurchaseView;
import shop.purchase.internal.domain.fake.MovieCatalogFake;
import shop.purchase.internal.domain.fake.PurchaseLineRepositoryFake;
import shop.purchase.internal.domain.fake.PurchaseRepositoryFake;
import shop.purchase.internal.domain.fake.UserCartFake;

import shop.shared.error.ConflictException;
import shop.shared.error.ResourceNotFoundException;
import shop.purchase.internal.domain.model.Purchase;
import shop.purchase.internal.domain.service.PurchaseServiceImpl;
import shop.purchase.internal.domain.model.PurchaseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PurchaseUseCasesTest {

    private PurchaseRepositoryFake purchases;
    private PurchaseLineRepositoryFake lines;
    private UserCartFake carts;
    private MovieCatalogFake movies;
    private CompletePurchase complete;
    private ListPurchases list;
    private GetPurchase get;

    @BeforeEach
    void setUp() {
        purchases = new PurchaseRepositoryFake();
        lines = new PurchaseLineRepositoryFake();
        carts = new UserCartFake();
        movies = new MovieCatalogFake();
        var service = new PurchaseServiceImpl(purchases, lines, carts, movies);
        complete = new CompletePurchase(service);
        list = new ListPurchases(service);
        get = new GetPurchase(service);
    }

    private void cartWithTwoMovies(Long userId) {
        movies.createMovie(10L, "Matrix", new BigDecimal("7.99"));
        movies.createMovie(11L, "Inception", new BigDecimal("9.49"));
        carts.add(userId, 10L);
        carts.add(userId, 11L);
    }

    @Test
    void completeCreatesPurchaseWithTitlePriceSnapshotAndTotalAndClearsCart() {
        cartWithTwoMovies(1L);

        PurchaseView view = complete.execute(1L);

        assertNotNull(view.id());
        assertEquals(1L, view.userId());
        assertEquals(PurchaseStatus.COMPRADA.name(), view.status());
        assertNotNull(view.purchasedAt());
        assertEquals(new BigDecimal("17.48"), view.total());
        assertEquals(List.of("Matrix", "Inception"), view.lines().stream().map(l -> l.title()).toList());
        assertEquals(List.of(new BigDecimal("7.99"), new BigDecimal("9.49")),
                view.lines().stream().map(l -> l.unitPrice()).toList());
        assertEquals(2, lines.findByPurchaseIdOrderById(view.id()).size());
        assertEquals(2, lines.count());
        assertTrue(carts.findMovieIds(1L).isEmpty());
    }

    @Test
    void completeUsesCurrentPriceIfItChangedSinceAddedToCart() {
        cartWithTwoMovies(1L);
        movies.changePrice(10L, new BigDecimal("10.00"));

        PurchaseView view = complete.execute(1L);

        assertEquals(new BigDecimal("10.00"), view.lines().get(0).unitPrice());
        assertEquals(new BigDecimal("19.49"), view.total());
    }

    @Test
    void purchaseKeepsTitleAndPriceEvenIfMovieIsUpdatedOrDeleted() {
        cartWithTwoMovies(1L);
        Long purchaseId = complete.execute(1L).id();

        movies.changePrice(10L, new BigDecimal("99.00"));
        movies.changeTitle(10L, "Matrix Reloaded");
        movies.delete(11L);

        PurchaseView detail = get.execute(1L, purchaseId);
        assertEquals(new BigDecimal("17.48"), detail.total());
        assertEquals(List.of("Matrix", "Inception"), detail.lines().stream().map(l -> l.title()).toList());
        assertEquals(new BigDecimal("7.99"), detail.lines().get(0).unitPrice());
    }

    @Test
    void completeWithEmptyOrMissingCartIsConflict() {
        assertThrows(ConflictException.class, () -> complete.execute(1L));
        assertEquals(0, purchases.count());
    }

    @Test
    void completeWithVanishedMovieIsConflictAndLeavesCartIntact() {
        cartWithTwoMovies(1L);
        movies.delete(11L);

        assertThrows(ConflictException.class, () -> complete.execute(1L));

        assertEquals(List.of(10L, 11L), carts.findMovieIds(1L));
        assertEquals(0, purchases.count());
        assertEquals(0, lines.count());
    }

    @Test
    void completeWithMovieWithoutPriceIsConflictAndLeavesCartIntact() {
        cartWithTwoMovies(1L);
        movies.changePrice(10L, null);

        assertThrows(ConflictException.class, () -> complete.execute(1L));

        assertEquals(List.of(10L, 11L), carts.findMovieIds(1L));
        assertEquals(0, purchases.count());
    }

    @Test
    void listReturnsOnlyOwnPurchasesNewestFirst() {
        Long old = directPurchase(1L, LocalDateTime.of(2026, 11, 1, 9, 0)).getId();
        Long recent = directPurchase(1L, LocalDateTime.of(2026, 11, 3, 9, 0)).getId();
        directPurchase(2L, LocalDateTime.of(2026, 11, 4, 9, 0));

        List<Long> ids = list.execute(1L).stream().map(v -> v.id()).toList();

        assertEquals(List.of(recent, old), ids);
    }

    @Test
    void detailReturnsOwnPurchaseWithItsLines() {
        cartWithTwoMovies(1L);
        Long purchaseId = complete.execute(1L).id();

        PurchaseView detail = get.execute(1L, purchaseId);

        assertEquals(purchaseId, detail.id());
        assertEquals(2, detail.lines().size());
    }

    @Test
    void detailOfOthersOrUnknownPurchaseIsNotFound() {
        cartWithTwoMovies(2L);
        Long others = complete.execute(2L).id();

        assertThrows(ResourceNotFoundException.class, () -> get.execute(1L, others));
        assertThrows(ResourceNotFoundException.class, () -> get.execute(1L, 999L));
    }

    private Purchase directPurchase(Long userId, LocalDateTime purchasedAt) {
        var purchase = new Purchase();
        purchase.setUserId(userId);
        purchase.setPurchasedAt(purchasedAt);
        purchase.setTotal(BigDecimal.ZERO);
        purchase.setStatus(PurchaseStatus.COMPRADA);
        return purchases.save(purchase);
    }
}
